package com.reco1l.verktex.network

import com.reco1l.verktex.Platform
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * HTTP clients over the platform's engine ([Platform.Implementation.httpEngine]).
 */
object Network {

    /**
     * A client with Ktor's defaults, for requests that need nothing else.
     */
    val client: HttpClient by lazy { createClient() }

    /**
     * Creates a client over the platform's engine, configured with [configure], e.g. to install
     * plugins. Clients hold connections and threads, so create one per use and keep it.
     *
     * @throws IllegalStateException If the platform has no HTTP engine.
     */
    fun createClient(configure: HttpClientConfig<*>.() -> Unit = {}): HttpClient {
        val engine = Platform.implementation.httpEngine ?: throw IllegalStateException("The platform has no HTTP engine")
        return HttpClient(engine, configure)
    }
}
