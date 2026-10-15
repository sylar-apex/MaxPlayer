// VLCKit playback for every format AVPlayer cannot open (MKV, AVI, WMV, FLV, RMVB, DTS, ...).
// Add the MobileVLCKit package (SPM: https://github.com/tylerjonesio/vlckit-spm, or the CocoaPods
// pod 'MobileVLCKit' ~> 3.6); until then this file compiles to nothing and AVPlayer plays alone.
#if canImport(MobileVLCKit)
import MobileVLCKit
import Shared
import UIKit

final class VlcKitBackend: NSObject, PlaybackBackend, IosVideoBackend, VLCMediaPlayerDelegate {
    var callback: BackendCallback?

    private let player = VLCMediaPlayer()
    private let drawable = UIView()
    private var playWhenReady = false
    private var started = false
    private var lastPlaying = false

    private(set) var state: EngineState = .idle
    var videoView: UIView { drawable }

    override init() {
        super.init()
        drawable.backgroundColor = .black
        player.drawable = drawable
        player.delegate = self
    }

    var isPlaying: Bool { player.isPlaying }
    var position: Int64 { Int64(player.time.intValue).clamped(min: 0) }
    var duration: Int64 { Int64(player.media?.length.intValue ?? 0).clamped(min: 0) }
    var volume: Float = 1 { didSet { applyVolume() } }
    var speed: Float = 1 { didSet { player.rate = speed } }
    var effects: AudioEffectsController? { nil }

    func open(uri: String, startMs: Int64, playWhenReady: Bool, decoder: Decoder) {
        stop()
        guard let url = uri.hasPrefix("/") ? URL(fileURLWithPath: uri) : URL(string: uri) else {
            callback?.onError(codeName: "ERROR_CODE_IO_FILE_NOT_FOUND"); return
        }
        let media = VLCMedia(url: url)
        if startMs > 0 { media.addOption(":start-time=\(Double(startMs) / 1000)") }
        if decoder == .sw { media.addOption(":codec=avcodec") }
        player.media = media
        self.playWhenReady = playWhenReady
        setState(.buffering)
        if playWhenReady { start() }
    }

    private func start() { started = true; player.play(); player.rate = speed; applyVolume() }

    func play() { playWhenReady = true; if !started || state == .ended { start() } else { player.play() } }
    func pause() { playWhenReady = false; if started { player.pause() } }

    func stop() {
        started = false
        player.stop()
        setState(.idle)
    }

    func seekTo(positionMs: Int64, fast: Bool) {
        player.time = VLCTime(int: Int32(clamping: positionMs))
    }

    func tracks(type: TrackType) -> [TrackInfo] {
        let names = (type == .audio ? player.audioTrackNames : player.videoSubTitlesNames) as? [String] ?? []
        let ids = (type == .audio ? player.audioTrackIndexes : player.videoSubTitlesIndexes) as? [NSNumber] ?? []
        let selected = type == .audio ? player.currentAudioTrackIndex : player.currentVideoSubTitleIndex
        return zip(ids, names).filter { $0.0.int32Value >= 0 }.map {
            TrackInfo(id: $0.0.stringValue, label: $0.1, selected: $0.0.int32Value == selected)
        }
    }

    func selectTrack(type: TrackType, id: String) {
        guard let index = Int32(id) else { return }
        if type == .audio { player.currentAudioTrackIndex = index } else { player.currentVideoSubTitleIndex = index }
    }

    func setTrackTypeDisabled(type: TrackType, disabled: Bool) {
        guard disabled else { return }
        if type == .audio { player.currentAudioTrackIndex = -1 } else { player.currentVideoSubTitleIndex = -1 }
    }

    func videoFormat() -> FormatInfo? {
        let size = player.videoSize
        guard size.width > 0 else { return nil }
        return FormatInfo(containerMimeType: nil, codecs: nil, sampleMimeType: nil, width: Int32(size.width), height: Int32(size.height),
                          frameRate: 0, bitrate: 0, channelCount: 0, sampleRate: 0, language: nil)
    }

    func audioFormat() -> FormatInfo? { nil }

    func release() { stop(); player.delegate = nil }

    private func applyVolume() { player.audio?.volume = Int32(max(0, min(200, volume * 100))) }

    private func setState(_ s: EngineState) {
        guard state != s else { return }
        state = s
        callback?.onStateChanged(state: s)
    }

    // MARK: VLCMediaPlayerDelegate (main thread)

    func mediaPlayerStateChanged(_ aNotification: Notification) {
        switch player.state {
        case .buffering, .opening: setState(.buffering)
        case .playing, .paused, .esAdded:
            setState(.ready)
            let size = player.videoSize
            if size.width > 0 { callback?.onVideoSize(width: Int32(size.width), height: Int32(size.height)) }
            if player.state == .esAdded { callback?.onTracksChanged() }
        case .ended:
            setState(.ended); callback?.onEnded()
        case .error:
            setState(.idle); callback?.onError(codeName: "ERROR_CODE_DECODING_FORMAT_UNSUPPORTED")
        default: break
        }
        if player.isPlaying != lastPlaying { lastPlaying = player.isPlaying; callback?.onPlayingChanged(playing: lastPlaying) }
    }
}

private extension Int64 {
    func clamped(min lower: Int64) -> Int64 { Swift.max(lower, self) }
}
#endif
