package org.app.maxplayerpro.localnetwork.share

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.koin.core.module.Module
import org.koin.dsl.module

/** Hosting and discovery for Android and desktop; [platform] supplies the parts that differ. */
internal fun jvmShareModule(platform: org.koin.core.scope.Scope.() -> HostPlatform, sink: org.koin.core.scope.Scope.() -> DownloadSink): Module = module {
    single<Downloader> { ParallelDownloader(sink()) }
    single { platform() }
    single {
        val store = get<ShareStore>()
        val host = get<HostPlatform>()
        NearbyClient(HttpClient(CIO)) { SelfDevice(store.deviceId, host.deviceName, host.platform) }
    }
    single<ShareHost> { KtorShareHost(get(), get()) }
    single<NearbyDiscovery> { MulticastDiscovery(get(), get(), get(), get()) }
}
