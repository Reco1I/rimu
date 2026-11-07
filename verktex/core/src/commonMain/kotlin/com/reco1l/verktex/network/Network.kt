package com.reco1l.verktex.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * HTTP clients over Ktor's default engine for the target: on the JVM the one on the classpath (OkHttp,
 * brought by the platform modules), found through `ServiceLoader`.
 */
object Network {

    /**
     * A client with Ktor's defaults, for requests that need nothing else.
     */
    val client: HttpClient by lazy { createClient() }

    /**
     * Creates a client over the default engine, configured with [configure], e.g. to install
     * plugins. Clients hold connections and threads, so create one per use and keep it.
     *
     * @throws IllegalStateException If there's no engine on the classpath.
     */
    fun createClient(configure: HttpClientConfig<*>.() -> Unit = {}): HttpClient = HttpClient(configure)
}
