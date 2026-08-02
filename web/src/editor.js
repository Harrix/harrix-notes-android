// CodeMirror 6 Markdown editor for the Android WebView editor pane.
//
// The document never crosses the bridge as a single string: it is pulled from
// and pushed to Kotlin in chunks, so multi-megabyte notes stay responsive.
// Kotlin talks to this file through `window.notesEditor`, this file talks back
// through the injected `NotesEditorHost` object.

import { Compartment, EditorState } from "@codemirror/state";
import { EditorView, keymap } from "@codemirror/view";
import { defaultKeymap, history, historyKeymap } from "@codemirror/commands";
import { HighlightStyle, syntaxHighlighting } from "@codemirror/language";
import { markdown, markdownLanguage } from "@codemirror/lang-markdown";
import { tags } from "@lezer/highlight";

const CHUNK_SIZE = 1 << 18;
const PUSH_DELAY_MS = 400;

const host = window.NotesEditorHost;
const themeConf = new Compartment();
const languageConf = new Compartment();

let view = null;
let config = null;
let pushTimer = null;
let suppressPush = false;

function isHighSurrogate(text) {
  const last = text.charCodeAt(text.length - 1);
  return last >= 0xd800 && last <= 0xdbff;
}

// Chunk sizes come back from the host: it shortens a chunk rather than split a
// surrogate pair, which the bridge would turn into a replacement character.
function readHostText() {
  const length = host.textLength();
  if (length <= 0) {
    return "";
  }
  const parts = [];
  let from = 0;
  while (from < length) {
    const chunk = host.textChunk(from, Math.min(length, from + CHUNK_SIZE));
    if (!chunk) {
      break;
    }
    parts.push(chunk);
    from += chunk.length;
  }
  return parts.join("");
}

function pushText() {
  pushTimer = null;
  const doc = view.state.doc;
  const length = doc.length;
  host.beginText(length);
  let from = 0;
  while (from < length) {
    const to = Math.min(length, from + CHUNK_SIZE);
    let chunk = doc.sliceString(from, to);
    if (to < length && isHighSurrogate(chunk)) {
      chunk = chunk.slice(0, -1);
    }
    host.appendText(chunk);
    from += chunk.length;
  }
  host.commitText();
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
        backgroundColor: config.background,
        color: config.foreground,
        fontSize: `${config.fontSize}px`,
      },
      "&.cm-focused": { outline: "none" },
      ".cm-scroller": {
        fontFamily: "monospace",
        lineHeight: "1.45",
        overflow: "auto",
        WebkitOverflowScrolling: "touch",
      },
      // Bottom padding keeps the last lines reachable above the soft keyboard.
      ".cm-content": { padding: "12px 12px 50vh", caretColor: config.caret },
      ".cm-cursor, .cm-dropCursor": { borderLeftColor: config.caret },
      "::selection": { backgroundColor: config.selection },
    },
    { dark: config.dark },
  );
}

function buildHighlightStyle() {
  const tokens = config.tokens;
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
    { tag: [tags.list, tags.processingInstruction, tags.escape], color: tokens.listMarker },
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
    keymap.of([...defaultKeymap, ...historyKeymap]),
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
  reload() {
    if (!view) {
      return;
    }
    cancelPush();
    const doc = readHostText();
    withoutPush(() => {
      view.setState(EditorState.create({ doc, extensions: buildExtensions() }));
    });
    host.onReady();
  },

  /** Applies theme / font size / highlight changes without touching the document. */
  applyConfig(json) {
    if (!view) {
      return;
    }
    const highlight = config.highlight;
    config = JSON.parse(json);
    const effects = [themeConf.reconfigure(buildTheme())];
    if (config.highlight !== highlight) {
      effects.push(languageConf.reconfigure(buildLanguage()));
    }
    view.dispatch({ effects });
  },

  /** Sends the document to Kotlin right away, cancelling the pending debounce. */
  flush() {
    if (!view) {
      return;
    }
    cancelPush();
    pushText();
  },
};

function boot() {
  config = JSON.parse(host.configJson());
  const doc = readHostText();
  view = new EditorView({
    doc,
    extensions: buildExtensions(),
    parent: document.body,
  });
  host.onReady();
}

boot();
