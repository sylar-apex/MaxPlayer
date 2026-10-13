package org.app.nxplayer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform