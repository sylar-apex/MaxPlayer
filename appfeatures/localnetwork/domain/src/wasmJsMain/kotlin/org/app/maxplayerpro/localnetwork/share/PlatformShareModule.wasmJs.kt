package org.app.maxplayerpro.localnetwork.share

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import org.koin.core.module.Module
import org.koin.dsl.module

/** A browser cannot listen for connections or multicast; it only opens other devices by address. */
actual fun platformShareModule(): Module = module {
    single<ShareHost> { NoShareHost }
    single<NearbyDiscovery> { NoDiscovery }
    single<Downloader> { NoDownloader }
    single { NearbyClient(HttpClient(Js)) { SelfDevice(get<ShareStore>().deviceId, "Web browser", "Browser") } }
}
