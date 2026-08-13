package dev.harrix.notes.ui.notes

import android.webkit.WebView

/**
 * Find-in-page for the Markdown HTML preview WebView using the platform
 * [WebView.findAllAsync] / [WebView.findNext] APIs (exact match + native highlight).
 */
class NotesPreviewFindController {
    private var webView: WebView? = null

    /** Active match is 1-based; both are 0 when there are no matches. */
    var findResultListener: (activeMatch: Int, totalMatches: Int) -> Unit = { _, _ -> }

    fun attach(target: WebView) {
        webView = target
        target.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            if (!isDoneCounting) {
                return@setFindListener
            }
            val total = numberOfMatches.coerceAtLeast(0)
            val active =
                if (total == 0) {
                    0
                } else {
                    (activeMatchOrdinal + 1).coerceIn(1, total)
                }
            findResultListener(active, total)
        }
    }

    fun detach(target: WebView) {
        if (webView === target) {
            target.setFindListener(null)
            webView = null
        }
    }

    fun find(query: String) {
        val target = webView ?: return
        if (query.isEmpty()) {
            clearFind()
            findResultListener(0, 0)
            return
        }
        target.findAllAsync(query)
    }

    fun findNext() {
        webView?.findNext(true)
    }

    fun findPrev() {
        webView?.findNext(false)
    }

    fun clearFind() {
        webView?.clearMatches()
        findResultListener(0, 0)
    }
}
