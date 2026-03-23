package com.spotifyhub.ui.root

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spotifyhub.ui.auth.AuthScreen
import com.spotifyhub.ui.auth.AuthViewModel
import com.spotifyhub.ui.detail.DetailViewModel
import com.spotifyhub.ui.home.HomeViewModel
import com.spotifyhub.ui.library.LibraryViewModel
import com.spotifyhub.ui.main.MainScreen
import com.spotifyhub.ui.main.MainViewModel
import com.spotifyhub.ui.nowplaying.PlayerViewModel
import com.spotifyhub.ui.rating.RatingViewModel
import com.spotifyhub.ui.search.SearchViewModel

@Composable
fun RootScreen(
    rootViewModel: RootViewModel,
    authViewModel: AuthViewModel,
    playerViewModel: PlayerViewModel,
    mainViewModel: MainViewModel,
    homeViewModel: HomeViewModel,
    searchViewModel: SearchViewModel,
    libraryViewModel: LibraryViewModel,
    ratingViewModel: RatingViewModel,
    detailViewModel: DetailViewModel,
) {
    val rootState = rootViewModel.uiState.collectAsStateWithLifecycle().value

    when (rootState.destination) {
        RootDestination.Auth -> AuthScreen(
            viewModel = authViewModel,
            isOffline = rootState.isOffline,
        )

        RootDestination.Main -> MainScreen(
            mainViewModel = mainViewModel,
            playerViewModel = playerViewModel,
            homeViewModel = homeViewModel,
            searchViewModel = searchViewModel,
            libraryViewModel = libraryViewModel,
            ratingViewModel = ratingViewModel,
            detailViewModel = detailViewModel,
            isOffline = rootState.isOffline,
        )
    }
}
