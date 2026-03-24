package com.spotifyhub.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.spotifyhub.ui.auth.AuthViewModel
import com.spotifyhub.ui.detail.DetailViewModel
import com.spotifyhub.ui.home.HomeViewModel
import com.spotifyhub.ui.library.LibraryViewModel
import com.spotifyhub.ui.main.MainViewModel
import com.spotifyhub.ui.nowplaying.PlayerViewModel
import com.spotifyhub.ui.rating.RatingViewModel
import com.spotifyhub.ui.root.RootViewModel
import com.spotifyhub.ui.search.SearchViewModel

class MainViewModelFactory(
    private val appGraph: AppGraph,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(RootViewModel::class.java) -> {
                RootViewModel(
                    authRepository = appGraph.authRepository,
                    connectivityMonitor = appGraph.connectivityMonitor,
                ) as T
            }

            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(appGraph.authRepository) as T
            }

            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> {
                PlayerViewModel(appGraph.playbackRepository) as T
            }

            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(appGraph.tabSelectedEvent) as T
            }

            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(appGraph.browseRepository, appGraph.tabSelectedEvent) as T
            }

            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(appGraph.searchRepository) as T
            }

            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> {
                LibraryViewModel(appGraph.libraryRepository) as T
            }

            modelClass.isAssignableFrom(RatingViewModel::class.java) -> {
                RatingViewModel(
                    playbackRepository = appGraph.playbackRepository,
                    sheetsRepository = appGraph.sheetsRepository,
                ) as T
            }

            modelClass.isAssignableFrom(DetailViewModel::class.java) -> {
                DetailViewModel(
                    libraryRepository = appGraph.libraryRepository,
                    artistRepository = appGraph.artistRepository,
                ) as T
            }

            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
