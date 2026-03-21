package com.spotifyhub.system.input

import android.view.KeyEvent

class InputRouter {

    private var volumeHandler: ((deltaPercent: Int) -> Unit)? = null

    /** Register a callback for hardware volume key presses. */
    fun setVolumeHandler(handler: (deltaPercent: Int) -> Unit) {
        volumeHandler = handler
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                volumeHandler?.invoke(5)
                volumeHandler != null
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                volumeHandler?.invoke(-5)
                volumeHandler != null
            }
            else -> false
        }
    }
}
