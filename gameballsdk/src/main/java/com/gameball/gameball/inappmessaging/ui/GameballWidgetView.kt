package com.gameball.gameball.inappmessaging.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import com.gameball.gameball.inappmessaging.runtime.IamLog

/**
 * A reward campaign in a WebView, which reports when it is actually drawn.
 *
 * **Its own WebView, deliberately.** The SDK builds another one for `showProfile`, and this does
 * not share it: the two have different bridge names, different lifetimes and different close
 * semantics, and every integration already shipped depends on that one behaving exactly as it
 * does. Extracting the common bridge is worth doing once this is proven, not while it is.
 *
 * The readiness contract is what this view exists for. The page fires `onPageFinished` two to
 * three seconds before the wheel is drawn — the widget renders after its own fetches resolve — so
 * revealing on page-load alone puts a loading skeleton in front of the customer every time.
 * Worse, the content lives in `about:srcdoc` iframes, so a check that only reads the top document
 * sits at about 98 nodes and never fires at all.
 */
@SuppressLint("SetJavaScriptEnabled")
internal class GameballWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val handler = Handler(Looper.getMainLooper())
    private var settled = false
    private var timeoutRunnable: Runnable? = null

    private var onReady: (() -> Unit)? = null
    private var onFailed: (() -> Unit)? = null
    private var onCloseRequested: (() -> Unit)? = null
    private var onEvent: ((String) -> Unit)? = null

    private var readyTimeoutMillis: Long = READY_TIMEOUT_MILLIS

    /** Measures the load, so trace lines line up with what was on screen. */
    private var startedAtMillis = 0L

    @VisibleForTesting
    internal val webView: WebView = WebView(context).also { view ->
        view.setBackgroundColor(Color.TRANSPARENT)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    init {
        // The gate. Held at a hair above transparent rather than at zero, and never with
        // visibility: Flutter's port of this hid the view at opacity 0 and the page stopped
        // loading entirely — a fully transparent surface is skipped in the draw path, and a
        // WebView that is never drawn has its render loop suspended. UNVERIFIED on Android's
        // Chromium engine, which may not share that behaviour; the conservative value is the one
        // that is known to work on the two platforms where it was measured. A test pins it.
        alpha = HIDDEN_ALPHA
    }

    /**
     * Loads [url] out of sight and calls back once the widget has finished drawing.
     *
     * [onReady] fires at most once; so does [onFailed], and only one of the two ever fires.
     */
    fun load(
        url: String,
        onReady: () -> Unit,
        onFailed: () -> Unit,
        onCloseRequested: () -> Unit,
        onEvent: ((String) -> Unit)? = null,
        readyTimeoutMillis: Long = READY_TIMEOUT_MILLIS
    ) {
        this.onReady = onReady
        this.onFailed = onFailed
        this.onCloseRequested = onCloseRequested
        this.onEvent = onEvent
        this.readyTimeoutMillis = readyTimeoutMillis
        startedAtMillis = System.currentTimeMillis()

        webView.addJavascriptInterface(Bridge(), BRIDGE_NAME)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                IamLog.d("widget ${elapsed()}ms onPageFinished")
                install()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                // The wheel's own sub-resources fail routinely and harmlessly; only the main
                // frame failing means there is nothing to show.
                if (request?.isForMainFrame != true) return
                IamLog.w("widget failed to load (${error?.errorCode})")
                settle(ready = false)
            }
        }
        webView.loadUrl(url)

        // Belt and braces: if the page never reaches onPageFinished, nothing would install the
        // poller and the message would hang invisible forever.
        timeoutRunnable = Runnable { settle(ready = false) }.also {
            handler.postDelayed(it, readyTimeoutMillis)
        }
    }

    /** Reveals the widget. Called by the presenter once readiness has been reported. */
    fun reveal() {
        alpha = 1f
    }

    /** Frees the WebView. A leaked one keeps a renderer process and the Activity alive. */
    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        settled = true
        // A bridge call already on its way from the page's thread must find nobody to tell: this
        // game is gone, and whatever is on screen now is not its to close.
        onReady = null
        onFailed = null
        onCloseRequested = null
        onEvent = null
        removeView(webView)
        webView.stopLoading()
        webView.removeJavascriptInterface(BRIDGE_NAME)
        webView.destroy()
    }

    private fun elapsed() = System.currentTimeMillis() - startedAtMillis

    /** The page asked to close, through `GameballWidget.closeWidget()`. */
    @VisibleForTesting
    internal fun closeRequested() {
        onCloseRequested?.invoke()
    }

    /** The page posted an event, through `WidgetEvent.postEvent(...)`. */
    @VisibleForTesting
    internal fun eventPosted(raw: String) {
        onEvent?.invoke(raw)
    }

    /** Reports the outcome exactly once. */
    @VisibleForTesting
    internal fun settle(ready: Boolean) {
        if (settled) return
        settled = true
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        IamLog.d("widget ${elapsed()}ms settled ready=$ready")
        if (ready) {
            onReady?.invoke()
        } else {
            IamLog.w(
                "widget did not become ready within ${readyTimeoutMillis / 1000}s; " +
                    "taking the message away unshown"
            )
            onFailed?.invoke()
        }
    }

    /**
     * Installs the bridges and the readiness poller.
     *
     * Everything runs across frames. The widget draws inside `about:srcdoc` iframes, which
     * inherit the parent's origin and so stay reachable; a frame that throws is skipped.
     *
     * Readiness is "the DOM stopped changing, and there is something in it". Deliberately
     * content-agnostic: keying off a selector would couple this SDK to the widget's markup and
     * break silently the next time the web team renames a class.
     */
    private fun install() {
        webView.evaluateJavascript(readinessScript(), null)
    }

    @VisibleForTesting
    internal fun readinessScript(): String = """
      (function () {
        if (window.__gbIamWatching) return;
        window.__gbIamWatching = true;

        function frames() {
          var out = [window];
          var els = document.querySelectorAll('iframe');
          for (var i = 0; i < els.length; i++) {
            try {
              var w = els[i].contentWindow;
              if (w && w.document) out.push(w);
            } catch (e) { /* cross-origin; nothing to read */ }
          }
          return out;
        }

        function shim(w) {
          try {
            if (w.__gbIamShimmed) return;
            w.__gbIamShimmed = true;
            var up = (w === window)
              ? function (k, r) { window.__gbIamPost(k, r); }
              : function (k, r) { try { w.parent.__gbIamPost(k, r); } catch (e) {} };
            w.WidgetEvent = w.WidgetEvent || {};
            w.WidgetEvent.postEvent = function (raw) { up('event', raw); };
            w.GameballWidget = w.GameballWidget || {};
            w.GameballWidget.closeWidget = function () { up('close', ''); };
          } catch (e) {}
        }

        // Child frames hand their events up rather than posting: the bridge object is only
        // reliably present in the main frame.
        window.__gbIamPost = function (kind, raw) {
          try {
            if (kind === 'event') $BRIDGE_NAME.onEvent(String(raw));
            else $BRIDGE_NAME.onClose();
          } catch (e) {}
        };

        function fingerprint() {
          var fs = frames();
          var nodes = 0, sizedCanvas = 0, imgDone = 0, textLen = 0;
          for (var f = 0; f < fs.length; f++) {
            var w = fs[f];
            shim(w);
            try {
              var d = w.document;
              nodes += d.querySelectorAll('*').length;
              var cv = d.querySelectorAll('canvas');
              for (var j = 0; j < cv.length; j++) {
                if (cv[j].clientWidth > 0 && cv[j].clientHeight > 0) sizedCanvas++;
              }
              var ims = d.querySelectorAll('img');
              for (var i = 0; i < ims.length; i++) {
                if (ims[i].complete && ims[i].naturalWidth > 0) imgDone++;
              }
              if (d.body) textLen += (d.body.innerText || '').trim().length;
            } catch (e) {}
          }
          return {
            key: fs.length + '/' + nodes + '/' + sizedCanvas + '/' + imgDone + '/' + textLen,
            // Content, and specifically NOT a sized canvas. The skeleton has one from the very
            // first sample — it is the placeholder — so including it made the predicate true
            // throughout the loading state, and an OR is only as strict as its weakest term.
            // Measured on the alpha widget: the skeleton sits at imgDone=0 textLen=0 for a full
            // second, then a half-built state at textLen=35 for another second, before the real
            // content arrives at imgDone=3 textLen=104.
            drawn: imgDone > 0 || textLen > 40
          };
        }

        var last = null, stable = 0, started = Date.now();
        function tick() {
          var fp = fingerprint();
          if (fp.key === last) stable++; else { stable = 0; last = fp.key; }

          if (fp.drawn && stable >= $STABLE_SAMPLES_REQUIRED) {
            try { $BRIDGE_NAME.onReady(); } catch (e) {}
            return;
          }
          if (Date.now() - started > $readyTimeoutMillis) {
            try { $BRIDGE_NAME.onTimeout(); } catch (e) {}
            return;
          }
          setTimeout(tick, $POLL_PERIOD_MILLIS);
        }
        setTimeout(tick, $POLL_PERIOD_MILLIS);
      })();
    """.trimIndent()

    /** Every method hops to the main thread: JavascriptInterface runs on a WebView thread. */
    private inner class Bridge {
        @JavascriptInterface
        fun onReady() = handler.post { settle(ready = true) }

        @JavascriptInterface
        fun onTimeout() = handler.post { settle(ready = false) }

        @JavascriptInterface
        fun onClose() = handler.post { closeRequested() }

        @JavascriptInterface
        fun onEvent(raw: String) = handler.post { eventPosted(raw) }
    }

    companion object {
        private const val BRIDGE_NAME = "GBIam"

        /**
         * A hair above transparent, never zero. See the note in `init`: zero opacity stopped the
         * page loading on Flutter, because a fully transparent surface is skipped in the draw
         * path and an undrawn WebView has its render loop suspended.
         */
        @VisibleForTesting
        internal const val HIDDEN_ALPHA = 1f / 255f

        /**
         * How long the widget has to become ready before the message is abandoned.
         *
         * Measured rather than guessed: on a warm load the DOM settles between 4.2s and 5.2s, and
         * a cold first load was still drawing a skeleton at 5s and complete by 10s. Twelve seconds
         * clears the cold case with room for a slow network, and a customer who would have waited
         * longer than that was going to close the card anyway.
         */
        const val READY_TIMEOUT_MILLIS = 12_000L

        private const val POLL_PERIOD_MILLIS = 250

        /**
         * How many identical consecutive samples confirm the page has come to rest.
         *
         * A secondary check, not the safeguard. The load passes through **three** stable plateaus
         * — the skeleton holds still for a full second, a half-built state for another, and only
         * then the finished wheel — so no value of this constant can tell loading from loaded.
         * Raising it only delays every message. What discriminates is the content test above.
         */
        private const val STABLE_SAMPLES_REQUIRED = 3
    }
}
