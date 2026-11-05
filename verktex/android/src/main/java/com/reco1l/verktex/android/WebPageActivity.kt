package com.reco1l.verktex.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar

/**
 * Shows a web page in a web view, see [com.reco1l.verktex.Platform.openWebPage]. Back goes back in the
 * page's history, and closes it at its start.
 */
class WebPageActivity : Activity() {

    private lateinit var webView: WebView


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val loading = ProgressBar(this).apply { isIndeterminate = true }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            intent.getStringExtra(EXTRA_USER_AGENT)?.let { settings.userAgentString = it }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    loading.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    loading.visibility = View.GONE
                }
            }
        }

        setContentView(FrameLayout(this).apply {
            addView(webView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(loading, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER))
        })

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(intent.getStringExtra(EXTRA_URL) ?: "about:blank")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }


    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_USER_AGENT = "userAgent"

        /**
         * Opens [url] in a new [WebPageActivity].
         */
        fun open(context: Context, url: String, userAgent: String?) {
            val intent = Intent(context, WebPageActivity::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_USER_AGENT, userAgent)

            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        }
    }
}
