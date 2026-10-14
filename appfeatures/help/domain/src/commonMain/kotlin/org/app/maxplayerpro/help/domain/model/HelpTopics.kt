package org.app.maxplayerpro.help.domain.model

data class HelpTopic(val question: String, val answer: String)

val helpTopics = listOf(
    HelpTopic("Gestures", "Swipe left/right to seek (speed set in Display Settings → Navigation). Swipe up/down on the left half for brightness, right half for volume. Pinch to zoom, drag with two fingers to pan. Double tap left/right to rewind/forward, centre to pause. Long press for fast-forward speed."),
    HelpTopic("Lock", "Tap the lock button to block all touches. Tap the small lock icon in the top-left corner to unlock."),
    HelpTopic("Background play", "Tap the headphones shortcut or choose Settings → Player → Background/PiP mode. Audio continues with a notification and lock-screen controls."),
    HelpTopic("Subtitles", "Subtitles with the same name next to the video load automatically. Use Subtitles → Open to pick another .srt or .vtt file, and Synchronization to shift timing."),
    HelpTopic("Private Folder", "Choose ⋮ → Lock in Private Folder on a video. Locked files move into app-private storage and need your PIN."),
)
