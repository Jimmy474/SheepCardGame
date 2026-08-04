package com.jimmy.sheepcardgame

import kotlinx.browser.window

class WasmPlatform : Platform {
    override val name: String get() = window.location.hostname
}

actual fun getPlatform(): Platform = WasmPlatform()