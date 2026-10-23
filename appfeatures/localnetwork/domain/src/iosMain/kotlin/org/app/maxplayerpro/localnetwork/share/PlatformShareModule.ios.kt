package org.app.maxplayerpro.localnetwork.share

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

/** iOS browses other devices by address for now; hosting needs background rules worked out first. */
actual fun platformShareModule(): Module = module {
    single<ShareHost> { NoShareHost }
    single<NearbyDiscovery> { NoDiscovery }
    single { NearbyClient(HttpClient(Darwin)) }
}
