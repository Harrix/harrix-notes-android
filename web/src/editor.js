// CodeMirror 6 Markdown editor for the Android WebView editor pane.
//
// The document never crosses the bridge as a single string: it is pulled from
// and pushed to Kotlin in chunks, so multi-megabyte notes stay responsive.
// Kotlin talks to this file through `window.notesEditor`, this file talks back
// through the injected `NotesEditorHost` object.
//
// Bridge numbers travel as strings: WebView's Java↔JS glue is unreliable for
// primitive ints on older Android System WebView builds.

import { Compartment, EditorState } from "@codemirror/state";
import { EditorView, keymap } from "@codemirror/view";
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

// Chunk sizes come back from the host: it shortens a chunk rather than split a
// surrogate pair, which the bridge would turn into a replacement character.
function readHostText() {
  const bridge = host();
  const length = Number(bridge.textLength());
  if (!Number.isFinite(length) || length <= 0) {
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
        fontFamily: "monospace",
        lineHeight: "1.45",
        overflow: "auto",
        WebkitOverflowScrolling: "touch",
      },
      // Bottom padding keeps the last lines reachable above the soft keyboard.
      ".cm-content": {
        padding: "12px 12px 50vh",
        caretColor: config.caret,
      },
      ".cm-cursor, .cm-dropCursor": { borderLeftColor: config.caret },
      "::selection": { backgroundColor: config.selection },
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

// Large notes drop Markdown parsing entirely and stay plain text.
function buildLanguage() {
  if (!config.highlight) {
    return [];
  }
  return [
    markdown({ base: markdownLanguage }),
    syntaxHighlighting(buildHighlightStyle()),
  ];
}

function onUpdate(update) {
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
    EditorView.updateListener.of(onUpdate),
  ];
}

function withoutPush(action) {
  suppressPush = true;
  try {
    action();
  } finally {
    suppressPush = false;
  }
}

window.notesEditor = {
  /** Replaces the document with the text currently staged on the Kotlin side. */
  reload: function () {
    if (!view) {
      return;
    }
    try {
      cancelPush();
      const doc = readHostText();
      withoutPush(function () {
        view.setState(EditorState.create({ doc: doc, extensions: buildExtensions() }));
      });
      host().onReady();
    } catch (error) {
      reportError(error);
    }
  },

  /** Applies theme / font size / highlight changes without touching the document. */
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
    } catch (error) {
      reportError(error);
    }
  },

  /** Sends the document to Kotlin right away, cancelling the pending debounce. */
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
};

function boot() {
  try {
    config = JSON.parse(host().configJson());
    const doc = readHostText();
    view = new EditorView({
      doc: doc,
      extensions: buildExtensions(),
      parent: document.body,
    });
    host().onReady();
  } catch (error) {
    reportError(error);
  }
}

boot();
