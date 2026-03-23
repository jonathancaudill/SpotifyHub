package com.spotifyhub.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Backward
import io.github.alexzhirkevich.cupertino.icons.filled.CheckmarkCircle
import io.github.alexzhirkevich.cupertino.icons.filled.Forward
import io.github.alexzhirkevich.cupertino.icons.filled.House
import io.github.alexzhirkevich.cupertino.icons.filled.Pause
import io.github.alexzhirkevich.cupertino.icons.filled.Play
import io.github.alexzhirkevich.cupertino.icons.filled.SquareStack
import io.github.alexzhirkevich.cupertino.icons.filled.Star
import io.github.alexzhirkevich.cupertino.icons.outlined.Gobackward
import io.github.alexzhirkevich.cupertino.icons.outlined.Goforward
import io.github.alexzhirkevich.cupertino.icons.outlined.House
import io.github.alexzhirkevich.cupertino.icons.outlined.MagnifyingGlass
import io.github.alexzhirkevich.cupertino.icons.outlined.PlusCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.Repeat
import io.github.alexzhirkevich.cupertino.icons.outlined.Shuffle
import io.github.alexzhirkevich.cupertino.icons.outlined.Star
import io.github.alexzhirkevich.cupertino.icons.outlined.Waveform

object AppIcons {
    // Use selected glyph for both states per app design.
    val library: ImageVector = CupertinoIcons.Filled.SquareStack
    val librarySelected: ImageVector = CupertinoIcons.Filled.SquareStack

    val home: ImageVector = CupertinoIcons.Outlined.House
    val homeSelected: ImageVector = CupertinoIcons.Filled.House

    // magnifyingglass.circle(.fill) is not available in current compose-cupertino release.
    val search: ImageVector = CupertinoIcons.Outlined.MagnifyingGlass
    val searchSelected: ImageVector = CupertinoIcons.Outlined.MagnifyingGlass

    // star.square(.fill) is not available in current compose-cupertino release.
    val rate: ImageVector = CupertinoIcons.Outlined.Star
    val rateSelected: ImageVector = CupertinoIcons.Filled.Star

    // Use selected glyph for both states per app design.
    val nowPlaying: ImageVector = CupertinoIcons.Outlined.Waveform
    val nowPlayingSelected: ImageVector = CupertinoIcons.Outlined.Waveform

    val play: ImageVector = CupertinoIcons.Filled.Play
    val pause: ImageVector = CupertinoIcons.Filled.Pause

    val skipTrackForward: ImageVector = CupertinoIcons.Filled.Forward
    val skipTrackBackward: ImageVector = CupertinoIcons.Filled.Backward

    // 15/30 trianglehead clock symbols are not available in current compose-cupertino release.
    val skip30Forward: ImageVector = CupertinoIcons.Outlined.Goforward
    val skip15Backward: ImageVector = CupertinoIcons.Outlined.Gobackward

    val saveTrack: ImageVector = CupertinoIcons.Outlined.PlusCircle
    val trackSaved: ImageVector = CupertinoIcons.Filled.CheckmarkCircle

    val shuffle: ImageVector = CupertinoIcons.Outlined.Shuffle
    val repeat: ImageVector = CupertinoIcons.Outlined.Repeat
    // repeat.1 is rendered as repeat + "1" badge in UI.
    val repeatOne: ImageVector = CupertinoIcons.Outlined.Repeat
}
