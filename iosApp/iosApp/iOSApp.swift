import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        #if canImport(MobileVLCKit)
        // AVPlayer handles Apple formats; VLCKit takes everything else.
        IosPlayback.shared.fallbackFactory = { VlcKitBackend() }
        #endif
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}