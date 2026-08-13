// CodeMirror 6 Markdown editor for the Android WebView editor pane.
//
// Boot is deferred: Kotlin calls `window.notesEditorBoot()` after the WebView has
// a non-zero size. Creating EditorView at 0×0 leaves a permanently blank pane.
//
// The document never crosses the bridge as a single string: it is pulled from
// and pushed to Kotlin in chunks. Numeric bridge arguments travel as strings —
// older System WebView builds mishandle primitive ints.

import { Compartment, EditorState, StateEffect, StateField } from "@codemirror/state";
import { Decoration, EditorView, keymap } from "@codemirror/view";
import { defaultKeymap, history, historyKeymap } from "@codemirror/commands";
import { HighlightStyle, syntaxHighlighting } from "@codemirror/language";
import { markdown, markdownLanguage } from "@codemirror/lang-markdown";
import { tags } from "@lezer/highlight";

const CHUNK_SIZE = 1 << 18;
const PUSH_DELAY_MS = 400;

const themeConf = new Compartment();
const languageConf = new Compartment();

let view = null;
let config = null;
let pushTimer = null;
let suppressPush = false;
let booted = false;

/** Exact-match find-in-note state (UTF-16 offsets, same as CodeMirror doc). */
let findMatches = [];
let findIndex = -1;
let findQuery = "";

const setFindDecorations = StateEffect.define();
const findMatchMark = Decoration.mark({ class: "cm-find-match" });
const findCurrentMark = Decoration.mark({ class: "cm-find-current" });

const findDecorationsField = StateField.define({
  create() {
    return Decoration.none;
  },
  update(value, tr) {
    for (const effect of tr.effects) {
      if (effect.is(setFindDecorations)) {
        return effect.value;
      }
    }
    return value.map(tr.changes);
  },
  provide: (field) => EditorView.decorations.from(field),
});

function host() {
  const bridge = window.NotesEditorHost;
  if (!bridge) {
    throw new Error("NotesEditorHost bridge is missing");
  }
  return bridge;
}

function reportError(error) {
  const message = error && error.message ? String(error.message) : String(error);
  try {
    host().onError(message);
  } catch (_) {
    // Bridge may itself be unavailable during early failures.
  }
  if (typeof console !== "undefined" && console.error) {
    console.error("[notesEditor]", message);
  }
}

function isHighSurrogate(text) {
  if (!text) {
    return false;
  }
  const last = text.charCodeAt(text.length - 1);
  return last >= 0xd800 && last <= 0xdbff;
}

function readHostText() {
  const bridge = host();
  const length = Number(bridge.textLength());
  if (!isFinite(length) || length <= 0) {
    return "";
  }
  const parts = [];
  let from = 0;
  while (from < length) {
    const to = Math.min(length, from + CHUNK_SIZE);
    const chunk = bridge.textChunk(String(from), String(to));
    if (chunk == null || chunk === "") {
      break;
    }
    parts.push(chunk);
    from += chunk.length;
  }
  return parts.join("");
}

function pushText() {
  pushTimer = null;
  if (!view) {
    return;
  }
  const bridge = host();
  const doc = view.state.doc;
  const length = doc.length;
  bridge.beginText(String(length));
  let from = 0;
  while (from < length) {
    const to = Math.min(length, from + CHUNK_SIZE);
    let chunk = doc.sliceString(from, to);
    if (to < length && isHighSurrogate(chunk)) {
      chunk = chunk.slice(0, -1);
    }
    if (!chunk) {
      break;
    }
    bridge.appendText(chunk);
    from += chunk.length;
  }
  bridge.commitText();
}

function cancelPush() {
  if (pushTimer !== null) {
    clearTimeout(pushTimer);
    pushTimer = null;
  }
}

function buildTheme() {
  return EditorView.theme(
    {
      "&": {
        height: "100%",
        width: "100%",
        backgroundColor: config.background,
        color: config.foreground,
        fontSize: config.fontSize + "px",
      },
      "&.cm-focused": { outline: "none" },
      ".cm-scroller": {
        fontFamily: config.fontFamily || "monospace",
        lineHeight: "1.45",
        overflow: "auto",
        WebkitOverflowScrolling: "touch",
      },
      ".cm-content": {
        padding: "12px 12px 50vh",
        caretColor: config.caret,
      },
      ".cm-cursor, .cm-dropCursor": { borderLeftColor: config.caret },
      "::selection": { backgroundColor: config.selection },
      ".cm-find-match": {
        backgroundColor: config.dark
          ? "rgba(255, 213, 0, 0.35)"
          : "rgba(255, 213, 0, 0.55)",
      },
      ".cm-find-current": {
        backgroundColor: config.dark
          ? "rgba(255, 152, 0, 0.55)"
          : "rgba(255, 152, 0, 0.75)",
      },
    },
    { dark: !!config.dark },
  );
}

function buildHighlightStyle() {
  const tokens = config.tokens || {};
  return HighlightStyle.define([
    {
      tag: [
        tags.heading1,
        tags.heading2,
        tags.heading3,
        tags.heading4,
        tags.heading5,
        tags.heading6,
      ],
      color: tokens.heading,
      fontWeight: "bold",
    },
    { tag: tags.quote, color: tokens.quote },
    {
      tag: [tags.list, tags.processingInstruction, tags.escape],
      color: tokens.listMarker,
    },
    { tag: tags.strong, color: tokens.emphasis, fontWeight: "bold" },
    { tag: tags.emphasis, color: tokens.emphasis, fontStyle: "italic" },
    { tag: tags.monospace, color: tokens.inlineCode, fontStyle: "italic" },
    { tag: tags.labelName, color: tokens.codeBlock },
    { tag: tags.link, color: tokens.linkText, fontStyle: "italic" },
    {
      tag: [tags.url, tags.string],
      color: tokens.linkUrl,
      textDecoration: "underline",
    },
    { tag: tags.contentSeparator, color: tokens.separator, fontWeight: "bold" },
    {
      tag: tags.strikethrough,
      color: tokens.strikethrough,
      textDecoration: "line-through",
    },
  ]);
}

function buildLanguage() {
  if (!config.highlight) {
    return [];
  }
  return [
    markdown({ base: markdownLanguage }),
    syntaxHighlighting(buildHighlightStyle()),
  ];
}

function reportScroll() {
  try {
    if (!view) {
      return;
    }
    const scroller = view.scrollDOM;
    host().onScroll(
      String(Math.round(scroller.scrollTop)),
      String(Math.round(scroller.scrollHeight)),
      String(Math.round(scroller.clientHeight)),
    );
  } catch (_) {
    // Host may not expose onScroll yet.
  }
}

/** Keep the HTML shell sized to the Android WebView (IME adjustResize). */
function setViewportHeight(cssPx) {
  const value = Number(cssPx);
  if (!isFinite(value) || value <= 0) {
    return;
  }
  const height = String(value) + "px";
  document.documentElement.style.height = height;
  document.body.style.height = height;
}

/**
 * After the WebView shrinks for the soft keyboard, remeasure and scroll the
 * caret back into the visible scroller if it fell below/above the fold.
 */
function keepCaretVisible() {
  if (!view) {
    return;
  }
  view.requestMeasure();
  const scroller = view.scrollDOM;
  const rect = scroller.getBoundingClientRect();
  const head = view.state.selection.main.head;
  const coords = view.coordsAtPos(head);
  if (!coords) {
    requestAnimationFrame(reportScroll);
    return;
  }
  const margin = 28;
  const above = coords.top < rect.top + margin;
  const below = coords.bottom > rect.bottom - margin;
  if (above || below) {
    view.dispatch({
      effects: EditorView.scrollIntoView(head, {
        y: below ? "end" : "start",
        yMargin: margin,
      }),
    });
  }
  requestAnimationFrame(reportScroll);
}

function scheduleKeepCaretVisible() {
  // Two frames: wait for WebView + CodeMirror layout after IME animation.
  requestAnimationFrame(function () {
    requestAnimationFrame(keepCaretVisible);
  });
}

function onUpdate(update) {
  if (update.docChanged || update.viewportChanged || update.heightChanged) {
    requestAnimationFrame(reportScroll);
  }
  if (!update.docChanged || suppressPush) {
    return;
  }
  cancelPush();
  pushTimer = setTimeout(pushText, PUSH_DELAY_MS);
}

function buildExtensions() {
  return [
    history(),
    keymap.of(defaultKeymap.concat(historyKeymap)),
    EditorView.lineWrapping,
    EditorState.tabSize.of(4),
    EditorView.contentAttributes.of({
      spellcheck: "false",
      autocapitalize: "off",
      autocorrect: "off",
    }),
    themeConf.of(buildTheme()),
    languageConf.of(buildLanguage()),
    findDecorationsField,
    EditorView.updateListener.of(onUpdate),
  ];
}

function reportFindResult() {
  try {
    const total = findMatches.length;
    const active = total === 0 || findIndex < 0 ? 0 : findIndex + 1;
    host().onFindResult(String(active), String(total));
  } catch (_) {
    // Older hosts without onFindResult.
  }
}

function buildFindDecorations() {
  if (findMatches.length === 0) {
    return Decoration.none;
  }
  const ranges = [];
  for (let i = 0; i < findMatches.length; i++) {
    const match = findMatches[i];
    const mark = i === findIndex ? findCurrentMark : findMatchMark;
    ranges.push(mark.range(match.from, match.to));
  }
  return Decoration.set(ranges, true);
}

function applyFindDecorations() {
  if (!view) {
    return;
  }
  view.dispatch({
    effects: setFindDecorations.of(buildFindDecorations()),
  });
}

function clearFindState() {
  findMatches = [];
  findIndex = -1;
  findQuery = "";
  applyFindDecorations();
  reportFindResult();
}

function collectExactMatches(docText, query) {
  const matches = [];
  if (!query) {
    return matches;
  }
  let from = 0;
  while (from <= docText.length - query.length) {
    const idx = docText.indexOf(query, from);
    if (idx < 0) {
      break;
    }
    matches.push({ from: idx, to: idx + query.length });
    from = idx + query.length;
  }
  return matches;
}

function goToFindIndex(index) {
  if (!view || findMatches.length === 0) {
    findIndex = -1;
    applyFindDecorations();
    reportFindResult();
    return;
  }
  findIndex = ((index % findMatches.length) + findMatches.length) % findMatches.length;
  const match = findMatches[findIndex];
  applyFindDecorations();
  view.dispatch({
    selection: { anchor: match.from, head: match.to },
    effects: EditorView.scrollIntoView(match.from, { y: "center" }),
  });
  reportFindResult();
}

function runFind(query) {
  if (!view) {
    findMatches = [];
    findIndex = -1;
    findQuery = "";
    reportFindResult();
    return;
  }
  findQuery = String(query || "");
  if (!findQuery) {
    clearFindState();
    return;
  }
  findMatches = collectExactMatches(view.state.doc.toString(), findQuery);
  if (findMatches.length === 0) {
    findIndex = -1;
    applyFindDecorations();
    reportFindResult();
    return;
  }
  goToFindIndex(0);
}

function withoutPush(action) {
  suppressPush = true;
  try {
    action();
  } finally {
    suppressPush = false;
  }
}

function destroyView() {
  cancelPush();
  if (view) {
    view.destroy();
    view = null;
  }
  booted = false;
}

function boot(viewportHeight) {
  try {
    if (booted && view) {
      host().onReady();
      return;
    }
    destroyView();
    setViewportHeight(viewportHeight);
    config = JSON.parse(host().configJson());
    const doc = readHostText();
    if (doc.length !== Number(config.expectedLength)) {
      throw new Error(
        "Editor received " +
          String(doc.length) +
          " of " +
          String(config.expectedLength) +
          " characters",
      );
    }
    view = new EditorView({
      doc: doc,
      extensions: buildExtensions(),
      parent: document.body,
    });
    // Force a measure after the WebView reports a real size.
    view.requestMeasure();
    view.scrollDOM.addEventListener("scroll", reportScroll, { passive: true });
    requestAnimationFrame(reportScroll);
    booted = true;
    host().onReady();
  } catch (error) {
    destroyView();
    reportError(error);
  }
}

window.notesEditorBoot = boot;

window.notesEditor = {
  reload: function () {
    if (!view) {
      boot();
      return;
    }
    try {
      cancelPush();
      findMatches = [];
      findIndex = -1;
      findQuery = "";
      const doc = readHostText();
      withoutPush(function () {
        view.setState(EditorState.create({ doc: doc, extensions: buildExtensions() }));
      });
      view.requestMeasure();
      host().onReady();
      reportFindResult();
    } catch (error) {
      reportError(error);
    }
  },

  applyConfig: function (json) {
    if (!view) {
      return;
    }
    try {
      const previousHighlight = config.highlight;
      config = JSON.parse(json);
      const effects = [themeConf.reconfigure(buildTheme())];
      if (config.highlight !== previousHighlight) {
        effects.push(languageConf.reconfigure(buildLanguage()));
      }
      view.dispatch({ effects: effects });
      view.requestMeasure();
    } catch (error) {
      reportError(error);
    }
  },

  flush: function () {
    if (!view) {
      return;
    }
    try {
      cancelPush();
      pushText();
    } catch (error) {
      reportError(error);
    }
  },

  scrollTo: function (y) {
    if (!view) {
      return;
    }
    view.scrollDOM.scrollTop = Number(y) || 0;
    reportScroll();
  },

  reportScroll: function () {
    reportScroll();
  },

  /** Called from Kotlin when the WebView height changes (soft keyboard). */
  onViewportResize: function (cssPx) {
    setViewportHeight(cssPx);
    if (!view) {
      return;
    }
    scheduleKeepCaretVisible();
  },

  keepCaretVisible: function () {
    scheduleKeepCaretVisible();
  },

  /** Exact substring find; highlights all matches and selects the first. */
  find: function (query) {
    try {
      runFind(query);
    } catch (error) {
      reportError(error);
    }
  },

  findNext: function () {
    try {
      if (findMatches.length === 0) {
        reportFindResult();
        return;
      }
      goToFindIndex(findIndex + 1);
    } catch (error) {
      reportError(error);
    }
  },

  findPrev: function () {
    try {
      if (findMatches.length === 0) {
        reportFindResult();
        return;
      }
      goToFindIndex(findIndex - 1);
    } catch (error) {
      reportError(error);
    }
  },

  clearFind: function () {
    try {
      clearFindState();
    } catch (error) {
      reportError(error);
    }
  },
};

// Signal that the script parsed and the bridge entry points exist.
try {
  host().onScriptLoaded();
} catch (_) {
  // Kotlin may not expose onScriptLoaded on older builds; boot still works.
}
