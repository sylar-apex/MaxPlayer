package org.app.maxplayerpro.localnetwork.share

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.koin.core.module.Module
import org.koin.dsl.module

/** Hosting and discovery for Android and desktop; [platform] supplies the parts that differ. */
internal fun jvmShareModule(platform: org.koin.core.scope.Scope.() -> HostPlatform): Module = module {
    single { platform() }
    single { NearbyClient(HttpClient(CIO)) }
    single<ShareHost> { KtorShareHost(get(), get()) }
    single<NearbyDiscovery> { MulticastDiscovery(get(), get(), get(), get()) }
}
