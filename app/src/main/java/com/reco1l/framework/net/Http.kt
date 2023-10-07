package com.reco1l.framework.net

import okhttp3.OkHttpClient
import okhttp3.Request


/**
 * Build an OkHttps3 HTTP request.
 */
fun buildRequest(block: Request.Builder.() -> Unit) = Request.Builder().apply(block).build()


/**
 *  Build an OkHttps3 HTTP client.
 */
fun buildClient(block: OkHttpClient.Builder.() -> Unit) = OkHttpClient.Builder().apply(block).build()