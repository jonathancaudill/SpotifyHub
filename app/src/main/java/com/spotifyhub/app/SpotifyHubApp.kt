package com.spotifyhub.app

import android.app.Application

class SpotifyHubApp : Application() {
    val appGraph: AppGraph by lazy { AppGraph(this) }
}

