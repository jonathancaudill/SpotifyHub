# Native Android Spotify Desk Controller Implementation Plan

Validated against the Spotify and Android docs on 2026-03-22.

## Implementation progress

Status updated on 2026-03-22 after Home/Search/Library tab implementation and full sidebar navigation.

### Completed in repo

- Android project scaffolded with `:app`, version catalog, manifest, resources, and Gradle wrapper
- Local toolchain installed on this machine for development: JDK, Gradle, Android command-line tools, platform-tools, Android 36 platform, and Build Tools 36.0.0
- `./gradlew assembleDebug` passes locally
- BlueStacks auth smoke test succeeded with the current PKCE + fixed loopback callback flow
- BlueStacks playback polling works end-to-end against an active Spotify Connect session
- Manual DI app shell implemented with `SpotifyHubApp`, `AppGraph`, `MainActivity`, and `MainViewModelFactory`
- Root Compose flow implemented with `RootViewModel`, `AuthViewModel`, `PlayerViewModel`, `AuthScreen`, and `NowPlayingScreen`
- Auth foundation implemented with:
  - PKCE generator
  - fixed loopback callback server on `127.0.0.1:43821`
  - browser launch path through Custom Tabs or generic browser intent
  - token exchange and refresh repository
  - encrypted token persistence
- Spotify Web API foundation implemented with Retrofit, OkHttp auth header injection, `401` token refresh, and `429` retry handling
- Initial playback polling implemented against `GET /v1/me/player`
- Previous/play-pause/next transport commands implemented against Spotify player endpoints
- Current-track library state implemented against Spotify's February 2026 library endpoints:
  - saved-state fetch through `GET /v1/me/library/contains`
  - save/remove toggle through `PUT /v1/me/library?uris=...` and `DELETE /v1/me/library?uris=...`
  - current-item save state exposed through `PlaybackRepository` and `PlayerViewModel`
- Basic kiosk shell implemented with immersive mode entry, HOME/LAUNCHER manifest wiring, boot receiver, and device-admin receiver stub
- Horizontal-first now-playing UI implemented with:
  - landscape orientation baseline
  - artwork-led composition with dedicated GL backdrop host under the Compose chrome
  - local progress interpolation between poll intervals
  - Compose preview for fast UI iteration
- Save control on the now-playing screen now matches the current Spotify pattern more closely:
  - heart placeholder removed
  - utility control uses a `+` icon that animates into a checkmark
  - button is enabled only when a current playback item exists and no command is already in flight
- The now-playing backdrop has been rewritten around actual artwork distortion instead of palette extraction:
  - `AlbumBackdropHost` embeds a `GLSurfaceView` under the Compose UI
  - the fragment shader composites multiple twisted/blurred copies of the current and previous album art
  - track changes crossfade between previous and current artwork textures over the shader transition
  - idle / missing-art states use a bundled neutral monochrome fallback texture generated in code
  - no backdrop code uses `Palette` or sampled artwork colors anymore
- The now-playing chrome has been rewritten around fixed neutral surfaces:
  - `AndroidLiquidGlass` usage is removed from the screen
  - transport, utility, and rail surfaces now use fixed graphite/white styling for readability over the GL backdrop
  - progress fill is fixed white instead of artwork-tinted
- More Spotify player controls are now implemented end-to-end:
  - playback state now maps shuffle and repeat mode from `GET /v1/me/player`
  - shuffle toggle uses `PUT /v1/me/player/shuffle`
  - repeat cycling uses `PUT /v1/me/player/repeat`
  - volume adjustment uses `PUT /v1/me/player/volume`
  - the now-playing utility tray now exposes shuffle, repeat, save, and volume controls instead of a dead placeholder device button
- Full multi-tab navigation implemented with sidebar rail:
  - sidebar now hosts 4 interactive tab icons: Home, Search, Library, Now Playing
  - clock and Spotify logo/LIVE status remain in the sidebar
  - waveform-style icon for Now Playing tab, standard Material icons for others
  - `MainScreen` composable wraps sidebar + content area with `Crossfade` transitions
  - `RootDestination` updated from `{Auth, NowPlaying}` to `{Auth, Main}`
  - `NowPlayingContent` composable extracted for embedding without redundant sidebar
- Home screen implemented with 7 sections:
  - time-aware greeting header with user display name from `GET /v1/me`
  - quick-access 2×3 compact grid of recently played unique contexts
  - "Made for You" section: user playlists filtered for Spotify-generated personalized playlists (Discover Weekly, Release Radar, Daily Mix, On Repeat, daylist, etc.)
  - "Recently Played" horizontal card row from `GET /v1/me/player/recently-played`
  - "Your Top Artists" circular artist bubbles from `GET /v1/me/top/artists`
  - "Recommended for You" seeded from top artists + tracks via `GET /v1/recommendations`
  - "Featured Playlists" from `GET /v1/browse/featured-playlists` (graceful degradation)
  - all API calls run in parallel with 5-minute cache
- Search screen implemented:
  - search bar with debounced input (300ms) via Kotlin Flow
  - results grouped by type: Songs, Artists, Albums, Playlists
  - search uses `GET /v1/search` with limit 10 per type (Feb 2026 API change)
  - each result row: artwork + title + type badge + subtitle
- Library screen implemented:
  - category chips: Playlists, Albums, Liked Songs
  - paginated scrolling lists from `GET /v1/me/playlists`, `/me/albums`, `/me/tracks`
  - infinite scroll loads more when near bottom
  - items show artwork, name, type, subtitle, track count, chevron
- Detail screen (playlist/album) implemented:
  - header with artwork, title, subtitle, gradient background
  - "Shuffle Play" button (green)
  - numbered track list with title, artist, duration
  - tapping track starts context playback at offset via `PUT /v1/me/player/play` with `@Body`
- New Spotify API interfaces added:
  - `SpotifyBrowseApi` — recently played, top tracks/artists, user playlists, recommendations, featured playlists, user profile
  - `SpotifySearchApi` — search across tracks/albums/artists/playlists
  - `SpotifyLibraryApi` expanded with playlist/album/track listing and detail endpoints
  - `SpotifyPlayerApi` expanded with `playContext(@Body)` for context playback
- OAuth scopes updated: added `user-top-read`
- Network module refactored: shared authenticated OkHttpClient factory extracted
- DTOs, mappers, and domain models added for browse, search, and library
- Manual DI expanded: `AppGraph` now registers browse/search/library APIs and repositories
- `MainViewModelFactory` updated with all new ViewModels

### Intentionally not finished yet

- Embedded WebView auth fallback is not implemented yet; the current code uses Custom Tabs first and generic browser intent second
- Transfer playback and device switching remain pending
- HID/encoder routing is only a stub
- Device-owner lock-task provisioning code is still pending beyond manifest and receiver scaffolding
- Detail screen does not yet extract dominant colors from artwork for gradient (uses static gray gradient)
- Artist tap action is not yet implemented (could launch artist radio or top tracks)
- No pull-to-refresh gesture on Home screen yet (manual refresh via tab re-selection)
- Library and search results don't yet navigate to a dedicated detail overlay (currently switches to NowPlaying tab)

### Build notes from implementation

- AGP 9 built-in Kotlin support forced two practical changes:
  - remove the legacy `org.jetbrains.kotlin.android` plugin
  - avoid `kapt` for now
- The current build therefore uses Moshi reflection plus `KotlinJsonAdapterFactory` instead of Moshi codegen
- `EncryptedSharedPreferences` currently builds with deprecation warnings. That matches the original plan, but it should be revisited once the first auth spike is proven on hardware
- The current UI now targets landscape directly because BlueStacks validation proved that the original rotated-portrait assumption was wrong for the intended appliance layout
- The backdrop implementation now relies on OpenGL ES 2.0 rather than Compose-only drawing because the shipping target is Android 11 and cannot use `RuntimeShader` / AGSL

### Practical handoff notes for future agents

- The user already proved the auth path works in BlueStacks and explicitly reported that the API/auth structure is working.
- The user rejected the original rotated portrait assumption. Treat this app as landscape-first unless real Echo Show testing proves otherwise.
- The current redirect URI in both code and plan is the fixed loopback callback `http://127.0.0.1:43821/callback`. Do not silently switch back to a dynamic port without verifying the Spotify dashboard accepts that registration flow again.
- The user explicitly pushed back on fake palette gradients and on “CSS glass” styling.
- The current visual direction is:
  - actual artwork-driven distortion in the backdrop rather than sampled/tinted colors
  - fixed neutral chrome over the animated backdrop rather than `AndroidLiquidGlass`
  - open artwork-led main composition instead of heavy paneling
- The save button now works end-to-end and the user explicitly confirmed it. Do not regress it back to a heart icon; the current direction is Spotify-style `+` to checkmark.
- Important Spotify API gotcha already discovered in this repo:
  - `GET /v1/me/library/contains` uses the `uris` query parameter
  - the matching save/remove endpoints also expect `uris` as a query parameter
  - an earlier incorrect body-based implementation compiled and read state correctly but failed to toggle library membership
- The last pass already landed real shuffle/repeat/volume controls.
- The background treatment has now moved away from palette extraction entirely and toward a shader-based artwork distortion field.
- Home, Search, Library, and Now Playing tabs are now all implemented. The next high-value UX work is:
  - device switching overlay
  - dynamic gradient extraction for detail screens (Palette or bitmap sampling)
  - artist detail / artist radio functionality
  - pull-to-refresh on Home
  - dedicated detail overlay (instead of switching tabs)
- Local toolchain context on this machine:
  - `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`
  - `ANDROID_SDK_ROOT=$HOME/Library/Android/sdk`
  - `local.properties` can point `sdk.dir` at `/Users/jonathancaudill/Library/Android/sdk`
- Repo hygiene context:
  - `.gitignore` already excludes build output, Kotlin caches, and `gradle/gradle-daemon-jvm.properties`
  - the repo had local generated directories cleaned earlier and did not require git history rewrite

### Recommended next implementation steps

1. Implement transfer playback plus a real device switcher overlay on top of the existing player/device APIs.
2. Build the browse overlays for recently played and playlists so the utility surface is no longer playback-only.
3. Decide whether the embedded WebView fallback is acceptable, or explicitly document that the project requires Custom Tabs / browser auth only.
4. Test the current landscape UI and GL backdrop performance on the actual Echo Show hardware before increasing blur or distortion complexity.

## 0. Working assumptions

- The app currently targets a landscape-first 960 x 480 layout. This replaced the earlier rotated-portrait assumption after BlueStacks validation and user feedback.
- This is a single-user appliance. Design for one signed-in Spotify account and one always-running app.
- The app controls Spotify playback on other devices only. Do not integrate Spotify App Remote or the Spotify Android SDK.
- The app should target `minSdk = 30` because the hardware is fixed to Android 11.
- Assume this is a newly created Spotify Development Mode app. As of 2026-03-15, new Development Mode apps created on or after February 11, 2026 are under the February 2026 restrictions immediately.
- Assume the app owner has Spotify Premium. Under the current Development Mode rules, if that Premium subscription lapses the app stops working until it is restored.
- Use a single Android app module on day 1. The codebase is small enough that physical feature modules add more Gradle friction than value right now.
- Treat Spotify's official OAuth docs as the source of truth for auth. Use Nocturne only for UI/state inspiration and librespot only as evidence that Spotify auth behavior is moving, not as code to copy.

## 1. Project structure

### Physical structure

Start with one Gradle module:

- `:app`

Reason:

- The app is small and kiosk-specific.
- A single module keeps build logic simple and APK iteration fast.
- Most of the runtime boundaries that matter here are package and interface boundaries, not Gradle boundaries.

If the codebase grows past roughly 15-20k Kotlin LOC, split out `:core:spotify` and `:core:ui`. Do not start there.

### Package layout

Use `com.spotifyhub` as the root package.

```text
app/src/main/java/com/spotifyhub/
  app/
    SpotifyHubApp.kt
    AppGraph.kt
    MainActivity.kt
    MainViewModelFactory.kt

  auth/
    SpotifyAuthRepository.kt
    SpotifyAccountsApi.kt
    PkceGenerator.kt
    LoopbackAuthServer.kt
    AuthLauncher.kt
    TokenStore.kt
    EncryptedPrefsTokenStore.kt
    SessionState.kt
    AuthUiState.kt

  spotify/
    api/
      SpotifyPlayerApi.kt
      SpotifyBrowseApi.kt
      SpotifyLibraryApi.kt
      SpotifyNetworkModule.kt
      AuthHeaderInterceptor.kt
      RetryAfterInterceptor.kt
      TokenRefreshAuthenticator.kt
    dto/
      player/...
      browse/...
      library/...
      auth/...
    mapper/
      PlaybackMapper.kt
      DeviceMapper.kt
      PlaylistMapper.kt

  playback/
    PlaybackRepository.kt
    PlaybackPoller.kt
    PlaybackStateDiffer.kt
    PlaybackCommandExecutor.kt
    VolumeCommandAggregator.kt
    ProgressAnchor.kt
    model/
      PlaybackSnapshot.kt
      PlaybackItem.kt
      PlaybackDevice.kt
      RepeatMode.kt
      LikeState.kt

  browse/
    BrowseRepository.kt
    BrowseCache.kt
    model/
      RecentItem.kt
      PlaylistSummary.kt
      PlaylistEntry.kt

  devices/
    DeviceRepository.kt
    DeviceSwitcherState.kt

  theme/
    SpotifyHubTheme.kt
    ArtworkTheme.kt
    ArtworkThemeRepository.kt
    ArtworkPaletteExtractor.kt
    ColorMath.kt

  system/
    kiosk/
      KioskManager.kt
      SpotifyHubDeviceAdminReceiver.kt
      BootCompletedReceiver.kt
      HomeIntentConfigurator.kt
      SystemUiController.kt
    input/
      InputRouter.kt
      HidInputTranslator.kt
      HardwareAction.kt
    network/
      ConnectivityMonitor.kt
      NetworkState.kt

  ui/
    root/
      RootRoute.kt
      RootUiState.kt
      RootScreen.kt
    auth/
      AuthScreen.kt
      AuthViewModel.kt
    nowplaying/
      NowPlayingScreen.kt
      PlayerViewModel.kt
      components/
        ArtworkHero.kt
        MetadataBlock.kt
        ProgressSection.kt
        TransportRow.kt
        SecondaryActionTray.kt
        VolumeHud.kt
    browse/
      BrowseOverlay.kt
      BrowseViewModel.kt
      PlaylistOverlay.kt
    devices/
      DeviceOverlay.kt
    common/
      GlassSurface.kt
      GlassIconButton.kt
      SpotifyAsyncImage.kt
      OfflineBanner.kt
      ErrorCard.kt
      LoadingSpinner.kt
```

### Core classes

- `SpotifyHubApp`: owns the singleton `AppGraph`.
- `AppGraph`: manual dependency injection container. Keep it explicit and tiny instead of adding Hilt.
- `MainActivity`: the only activity. Hosts Compose, enters immersive mode, routes hardware events into the input router, and owns launcher behavior.
- `PlayerViewModel`: the main feature ViewModel. Owns now-playing UI state and transport commands.
- `BrowseViewModel`: owns recently played, playlists, and playlist contents overlays.
- `AuthViewModel`: owns PKCE session bootstrap and token refresh/logout state.

### Why manual DI instead of Hilt

For this appliance, manual DI is the better trade:

- Smaller dependency surface.
- Fewer generated classes.
- Less annotation processing.
- Easier to debug on unusual kiosk hardware.

`AppGraph` plus a custom `ViewModelProvider.Factory` is enough.

## 2. Dependency baseline

Recommended baseline versions as of 2026-03-15:

```toml
[versions]
agp = "9.1.0"
kotlin = "2.3.10"
compose-bom = "2026.03.00"
activity-compose = "1.13.0"
core-ktx = "1.18.0"
lifecycle = "2.10.0"
retrofit = "3.0.0"
okhttp = "5.3.2"
moshi = "1.15.2"
coil = "2.7.0"
browser = "1.9.0"
security-crypto = "1.1.0"
palette = "1.0.0"
liquid-glass = "1.0.6"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "core-ktx" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity-compose" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-browser = { module = "androidx.browser:browser", version.ref = "browser" }
androidx-security-crypto = { module = "androidx.security:security-crypto", version.ref = "security-crypto" }
androidx-palette-ktx = { module = "androidx.palette:palette-ktx", version.ref = "palette" }

retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-converter-moshi = { module = "com.squareup.retrofit2:converter-moshi", version.ref = "retrofit" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
moshi = { module = "com.squareup.moshi:moshi", version.ref = "moshi" }
moshi-kotlin-codegen = { module = "com.squareup.moshi:moshi-kotlin-codegen", version.ref = "moshi" }

coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }
coil-okhttp = { module = "io.coil-kt:coil-network-okhttp", version.ref = "coil" }

android-liquid-glass = { module = "io.github.kyant0:backdrop", version.ref = "liquid-glass" }
```

Add Compose libraries through the BOM:

- `androidx.compose.ui:ui`
- `androidx.compose.foundation:foundation`
- `androidx.compose.material3:material3`
- `androidx.compose.animation:animation`
- `androidx.compose.material:material-icons-extended`
- debug only: `androidx.compose.ui:ui-tooling`

Build flags:

- `minSdk = 30`
- `targetSdk = 35` for the first release
- `compileSdk = 36`
- `screenOrientation = landscape`
- `minifyEnabled = true`
- `shrinkResources = true`
- `largeHeap = false`

## 3. Spotify Web API client

### Required scopes

Request only these scopes:

- `user-read-playback-state`
- `user-read-currently-playing`
- `user-modify-playback-state`
- `user-library-read`
- `user-library-modify`
- `playlist-read-private`
- `playlist-read-collaborative`
- `user-read-recently-played`

Do not request:

- `streaming`
- profile or email scopes
- social scopes

### Endpoints to implement in v1

Auth:

- `GET https://accounts.spotify.com/authorize`
- `POST https://accounts.spotify.com/api/token` for code exchange
- `POST https://accounts.spotify.com/api/token` for refresh

Playback:

- `GET /v1/me/player`
- `PUT /v1/me/player/play`
- `PUT /v1/me/player/pause`
- `POST /v1/me/player/next`
- `POST /v1/me/player/previous`
- `PUT /v1/me/player/shuffle`
- `PUT /v1/me/player/repeat`
- `PUT /v1/me/player/volume`
- `GET /v1/me/player/devices`
- `PUT /v1/me/player` for transfer playback

Browse:

- `GET /v1/me/player/recently-played`
- `GET /v1/me/playlists`
- `GET /v1/playlists/{playlist_id}/items`

Library:

- For new Development Mode apps, implement the generic library endpoints behind `SpotifyLibraryApi`:
  - `PUT /v1/me/library`
  - `DELETE /v1/me/library`
  - `GET /v1/me/library/contains`
- Do not hardcode the older content-specific saved-item endpoints such as `/me/tracks` across the app.
- Keep the library API isolated so the app can swap between Development Mode and Extended Quota mode behavior without touching UI code.

Notes:

- Treat all write-side Player API calls as Premium-dependent.
- `GET /v1/me/player` can return no active playback. Model that as a first-class empty state, not an error.
- Include `additional_types=track,episode` so the UI can degrade gracefully if the user plays a podcast.

### Auth flow

Use Authorization Code with PKCE and a loopback redirect.

Do not use a custom URI scheme for the primary implementation. Spotify's current redirect guidance favors HTTPS and loopback redirects, and the device has no guaranteed verified app-link environment.

#### Auth reference policy

- Do not use Nocturne as the auth reference. Its current codebase still contains a device-auth path and you already noted that no fixed auth release is available today.
- Do not implement librespot's `login5`, mobile-login, or `OAuthClient` internals inside the Android app. Those changes are useful as proof that Spotify auth is a moving target, but this controller should still use the public Web API OAuth surface only.
- Use `spotify-tui` as the implementation-shape reference for the loopback server pattern, not for tokens, credentials, or desktop-specific UX.

#### Redirect URI registration

Register this redirect URI in the Spotify developer dashboard:

- `http://127.0.0.1:43821/callback`

Use a fixed loopback port here because the Spotify developer dashboard currently rejects the portless loopback form in practice. Match the app and dashboard exactly.

Do not register:

- `http://localhost/...`
- custom URI schemes
- non-loopback HTTP callbacks
- Nocturne-style auth workarounds

#### Flow

1. `AuthViewModel.startLogin()` generates a `code_verifier` and `code_challenge`.
2. `LoopbackAuthServer.bind()` opens a server socket on `127.0.0.1:43821` and returns the fixed redirect URI `http://127.0.0.1:43821/callback`.
3. Launch the authorize URL in this order:
   - `CustomTabsIntent` if a compatible browser exists.
   - Embedded WebView inside `AuthScreen` as fallback.
4. Spotify redirects to `http://127.0.0.1:43821/callback?code=...`.
5. `LoopbackAuthServer` captures the code and serves a tiny HTML success page.
6. `SpotifyAuthRepository.exchangeCodeForTokens()` calls `/api/token`.
7. Store the refresh token in `EncryptedSharedPreferences`.
8. Hold the access token in memory and refresh it 5 minutes before expiry.
9. Shut down the loopback server immediately after success, cancellation, or timeout.

#### Browser strategy

- Preferred path: `CustomTabsIntent`
- Allowed fallback: in-app `WebView`
- Hard failure fallback: a tiny HTTPS auth broker you control

If the fallback WebView path fails due to cookies, modern login surfaces, or redirect behavior on this device, do not try to salvage auth through private Spotify flows. Treat that as a signal to move to the HTTPS broker fallback instead.

#### Why loopback

- Works without Google Play Services.
- Does not require a public backend.
- Maps cleanly to a single-purpose on-device browser flow.
- Avoids the `localhost` ban by using the explicit IPv4 loopback literal that Spotify currently requires.

#### Why QR auth is not the v1 path

A backend-free QR flow is awkward now because Spotify's redirect rules favor HTTPS or loopback callbacks. A QR flow that finishes on another phone cleanly usually needs a tiny companion backend or broker page. Punt that to v2 unless you are willing to host one.

### Token storage

Persist:

- `refresh_token`
- `granted_scopes`
- `expires_at_epoch_seconds`
- optional `last_user_id`

Do not persist:

- stale playback state
- large browse caches

Implement:

- `TokenStore.read(): StoredToken?`
- `TokenStore.write(token: StoredToken)`
- `TokenStore.clear()`

`EncryptedPrefsTokenStore` uses `MasterKey` plus `EncryptedSharedPreferences`.

### Auth hardening and acceptance criteria

Auth is a release gate for this appliance. Do not consider the project unblocked until all of these pass on the actual Echo Show hardware:

- fresh install -> sign in via `CustomTabsIntent` works
- fresh install -> sign in via embedded `WebView` works, or is explicitly rejected and replaced by the HTTPS broker fallback plan
- the loopback server binds `127.0.0.1:43821` and receives the callback correctly
- app restart reuses the refresh token and does not ask for consent again
- `401` and `invalid_grant` paths return cleanly to `AuthScreen`
- no code path depends on Nocturne auth code, librespot internals, `localhost`, or a custom URI scheme
- login timeout cleans up the loopback socket and returns the UI to a retryable state

### HTTP stack

Use two Retrofit instances:

- `accountsRetrofit` for `accounts.spotify.com`
- `apiRetrofit` for `api.spotify.com`

OkHttp chain:

1. `AuthHeaderInterceptor`
2. `RetryAfterInterceptor`
3. debug-only `HttpLoggingInterceptor`
4. `TokenRefreshAuthenticator`

Behavior:

- `AuthHeaderInterceptor` injects the current access token.
- `TokenRefreshAuthenticator` refreshes on `401` with a `Mutex` to prevent parallel refresh storms.
- `RetryAfterInterceptor` honors Spotify `429 Retry-After` headers.

## 4. Polling strategy

Create a dedicated `PlaybackPoller` owned by `PlaybackRepository`.

### Poll cadence

- `1500 ms` when playback is active and the app is in the foreground
- `3000 ms` when paused
- `5000 ms` when there is no active device or playback is idle
- immediate poll `250-400 ms` after any transport write
- back off to `5s -> 10s -> 20s` on repeated network failures

### Why not 1 second flat forever

- Too noisy for the API on an always-on appliance.
- Wasteful on a 1 GB device.
- You still get a visually smooth progress bar by interpolating locally.

### What counts as a meaningful playback diff

`PlaybackStateDiffer` should compare:

- item id
- item type
- `is_playing`
- `progress_ms`
- device id
- `volume_percent`
- `shuffle_state`
- `repeat_state`
- context URI

Emit a new UI snapshot only when one of those materially changes.

### Post-command refresh rules

After these commands, force a near-term refresh:

- play/pause
- next/previous
- shuffle
- repeat
- transfer playback
- volume
- like/unlike

This is necessary because Spotify write endpoints can be eventually consistent.

## 5. State architecture

### Repository layer

`PlaybackRepository`

- source of truth for current playback
- owns `PlaybackPoller`
- exposes `StateFlow<PlaybackSnapshot>`
- exposes `suspend fun dispatch(command: PlaybackCommand)`

`BrowseRepository`

- owns recent items, playlists, and playlist entries
- caches only the current overlay payloads in memory

`DeviceRepository`

- fetches devices on demand
- refreshes while the device overlay is open

`SpotifyAuthRepository`

- owns session bootstrap, refresh, and logout
- exposes `StateFlow<SessionState>`

### ViewModel layer

`AuthViewModel`

- maps `SessionState` into `AuthUiState`
- starts login
- logs out

`PlayerViewModel`

- combines `PlaybackRepository`, `ArtworkThemeRepository`, `DeviceRepository`, `ConnectivityMonitor`
- exposes `StateFlow<NowPlayingUiState>`
- handles `PlayerAction`

`BrowseViewModel`

- exposes `BrowseUiState`
- fetches recent items and playlists lazily

### UI state model

Use one high-level screen state:

```kotlin
data class RootUiState(
    val session: SessionStatus,
    val currentSurface: RootSurface,
    val overlay: OverlayDestination?,
    val isOffline: Boolean
)
```

Now-playing-specific state:

```kotlin
data class NowPlayingUiState(
    val playback: PlaybackPresentation?,
    val artworkTheme: ArtworkTheme,
    val progressAnchor: ProgressAnchor?,
    val activeDeviceName: String?,
    val volumePercent: Int?,
    val likeState: LikeState,
    val controlsEnabled: Boolean,
    val transportPending: Set<PlayerActionType>,
    val errorMessage: String?
)
```

### Input-agnostic action layer

Define a sealed action model once and reuse it for touch and future HID input:

```kotlin
sealed interface PlayerAction {
    data object PlayPause : PlayerAction
    data object Next : PlayerAction
    data object Previous : PlayerAction
    data object ToggleShuffle : PlayerAction
    data object CycleRepeat : PlayerAction
    data object ToggleLike : PlayerAction
    data class SetVolume(val percent: Int) : PlayerAction
    data class AdjustVolume(val delta: Int) : PlayerAction
    data class SeekTo(val positionMs: Long) : PlayerAction
    data class TransferToDevice(val deviceId: String) : PlayerAction
    data class OpenOverlay(val destination: OverlayDestination) : PlayerAction
    data object CloseOverlay : PlayerAction
    data object Refresh : PlayerAction
}
```

Everything funnels through `PlayerViewModel.onAction(action)`.

## 6. Local progress interpolation

Do not poll every frame.

Do this instead:

1. `PlaybackRepository` emits a raw `ProgressAnchor` whenever `/me/player` changes.
2. `ProgressSection` is the only composable that animates between anchors.
3. Use `SystemClock.elapsedRealtime()` against `anchor.receivedAtElapsedRealtime`.

Model:

```kotlin
data class ProgressAnchor(
    val positionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val receivedAtElapsedRealtime: Long
)
```

Display calculation:

```kotlin
displayMs = min(
    anchor.durationMs,
    anchor.positionMs + (elapsedRealtimeNow - anchor.receivedAtElapsedRealtime)
)
```

Rules:

- When paused, freeze.
- When scrubbing, UI owns the value locally and repository polling is ignored until scrub end.
- If the next server anchor differs from the predicted position by less than `350 ms`, correct with a short `120 ms` tween.
- If the drift is larger than `350 ms`, snap.

Implementation detail:

- Put the frame loop inside `ProgressSection`, not inside the ViewModel.
- Update the timer text at `4 Hz` if full frame-rate updates are too expensive on hardware.
- The rest of the screen still re-renders only on `StateFlow` changes.

## 7. UI plan

### Navigation model

Do not use `navigation-compose`.

Use a single root screen plus overlays:

- `AuthScreen`
- `NowPlayingScreen`
- `BrowseOverlay`
- `PlaylistOverlay`
- `DeviceOverlay`
- hidden `DiagnosticsOverlay`

This app is appliance-like, not a general multi-screen mobile app.

### Default layout

Landscape layout proportions:

- left rail: narrow vertical launcher/status strip
- main stage left: artwork hero
- main stage right: metadata block
- lower band: progress plus primary liquid controls
- outer edge actions: save/devices/browse/volume as secondary controls

### Screen breakdown

#### `AuthScreen`

Contains:

- product mark and short explanation
- "Connect Spotify" primary button
- browser fallback hint if custom tabs are unavailable
- transient WebView container if fallback path is needed
- loading and token-refresh states
- small diagnostics footer: Wi-Fi connected, Spotify reachable, browser available

#### `NowPlayingScreen`

Contains:

- `ArtworkHero`
- `MetadataBlock`
- `ProgressSection`
- `TransportRow`
- `SecondaryActionTray`
- `VolumeHud`
- `OfflineBanner`

##### `ArtworkHero`

- dominant visual element
- large square art, centered
- blurred and darkened full-screen backdrop from the same artwork
- crossfade current art against previous art over `350-500 ms`
- retain previous art briefly so transitions do not flash empty

##### `MetadataBlock`

- track title, max 2 lines
- artist, single line
- album, single line with lower emphasis
- device pill showing current output device

##### `ProgressSection`

- thin luxury-style progress bar
- elapsed and remaining time
- scrubbable only if you decide to include seek in v1. If not, keep it display-only.

##### `TransportRow`

- previous
- play/pause
- next

Touch targets:

- central play/pause `72-80 dp`
- prev/next `64 dp`

##### `SecondaryActionTray`

- like
- shuffle
- repeat
- volume
- devices
- browse

Use large glass pills, not tiny icon buttons.

#### `BrowseOverlay`

Tabbed overlay with:

- `Recently played`
- `Playlists`

Rules:

- Show only 15-20 recent items.
- Show only 20-30 playlists initially.
- Load playlist contents lazily on selection.
- Full-screen overlay is better than a narrow bottom sheet on a 480 px wide display.

#### `DeviceOverlay`

Shows:

- active device pinned at the top
- available devices with type icon, volume support indicator, and active badge
- transfer action on tap

### Compose hierarchy

```text
SpotifyHubApp()
  RootScreen()
    when (session)
      AuthScreen()
      ApplianceShell()
        AdaptiveBackdrop()
        NowPlayingScreen()
        if (overlay == Browse) BrowseOverlay()
        if (overlay == Playlist) PlaylistOverlay()
        if (overlay == Devices) DeviceOverlay()
        if (overlay == Diagnostics) DiagnosticsOverlay()
```

## 8. Design system and visual direction

### Typography

Use a single bundled variable font to keep APK size down.

Recommendation:

- `Manrope Variable` or `Sora Variable`

Use one family only:

- title: semibold
- body: medium
- labels: medium with slight tracking

### Color model

Base app palette:

- fixed dark neutral foundation
- album-art-driven accent layer

Do not rebuild the entire Material 3 color scheme on every track change. That creates noisy, unstable UI.

Instead:

- keep neutrals stable
- tint the backdrop, active controls, progress fill, and device badge from the artwork

### Liquid glass usage

Use `AndroidLiquidGlass` sparingly:

- sidebar surface
- transport controls
- utility action buttons
- selected secondary pills or overlay headers when they need emphasis

Do not use it for:

- full-screen blur
- the entire background
- every card, row, chip, and content container

On this hardware, treat liquid glass as an accent effect, not the base rendering model.

Add a runtime switch:

- `GlassMode.Off`
- `GlassMode.Subtle`
- `GlassMode.Full`

Default to `Subtle` on the Echo Show until real hardware proves `Full` is safe.

## 9. Adaptive theming

Create `ArtworkThemeRepository`.

### Pipeline

1. Coil fetches album art.
2. A second downsampled decode is generated for theming, at around `128 x 128`.
3. `ArtworkPaletteExtractor` runs off the main thread with `Palette`.
4. Normalize the result:
   - clamp saturation
   - clamp luminance
   - ensure `onAccent` contrast
5. Cache `ArtworkTheme` by artwork URL in a small in-memory LRU cache.

### `ArtworkTheme`

```kotlin
data class ArtworkTheme(
    val seed: Color,
    val backdropStart: Color,
    val backdropEnd: Color,
    val accent: Color,
    val accentMuted: Color,
    val glassTint: Color,
    val onAccent: Color
)
```

### Background treatment

- blurred art layer at low resolution
- dark gradient scrim over it
- accent glow near the artwork edges

### Transition behavior

- crossfade from previous `ArtworkTheme` to next over `500 ms`
- do not immediately hard-cut background tints on track change

## 10. Memory and performance budget

Target steady-state RSS:

- `50-80 MB`

Rough budget:

- Compose UI and state: `15-20 MB`
- current and previous artwork plus blur buffers: `12-18 MB`
- OkHttp, Retrofit, models, and caches: `8-12 MB`
- safety margin: `20+ MB`

### Specific tactics

- no Room
- no offline database in v1
- no Hilt
- no navigation-compose
- no giant list caches
- keep only the current overlay dataset in memory
- keep only current art and previous art in memory
- use Coil memory cache around `12 MB`
- use a small disk cache around `48-64 MB`
- load hero art around the displayed size, not full 1024 px assets
- use a lower-resolution decode for blur and palette extraction

### Coil config

In `SpotifyHubApp`:

- custom `ImageLoader`
- small memory cache
- small disk cache
- `crossfade(true)` only for hero art
- `allowHardware(true)` for visible art
- `allowHardware(false)` for palette extraction requests

## 11. Kiosk and launcher mode

### Manifest configuration

`MainActivity` should advertise:

- `ACTION_MAIN`
- `CATEGORY_HOME`
- `CATEGORY_DEFAULT`
- `CATEGORY_LAUNCHER`

Other manifest details:

- `excludeFromRecents = true`
- `launchMode = singleTask`
- `resizeableActivity = false`
- `keepScreenOn = true`

Add:

- `BOOT_COMPLETED` receiver
- optional `LOCKED_BOOT_COMPLETED` receiver
- device admin receiver

### Immersive mode

Use `WindowCompat.setDecorFitsSystemWindows(window, false)` plus `WindowInsetsControllerCompat`.

On every `onResume()` and `onWindowFocusChanged(true)`:

- hide status bar
- hide navigation bar
- set transient swipe behavior

Also keep a small `SystemUiController` utility because dialogs and overlays can cause bars to reappear.

### True kiosk behavior

For real kiosk mode, use device owner APIs. Home intent plus immersive mode alone is not enough.

Implement:

- `SpotifyHubDeviceAdminReceiver : DeviceAdminReceiver`
- `KioskManager`

If the app is device owner:

- `setLockTaskPackages(...)`
- `addPersistentPreferredActivity(...)`
- optionally disable status bar
- optionally disable keyguard
- start lock task in `onResume()`

Without device owner:

- the app still behaves like a launcher
- but it is not a hardened kiosk

### Provisioning path on LineageOS

Best path:

1. factory reset device
2. install APK
3. set device owner with `adb shell dpm set-device-owner com.spotifyhub/.system.kiosk.SpotifyHubDeviceAdminReceiver`
4. launch the app once so `KioskManager` can register preferred HOME and lock-task packages
5. reboot and verify the device returns directly to the app

## 12. Encoder and future HID input

Design the app so hardware input is a transport source, not a separate logic path.

### Activity hooks

In `MainActivity`, override:

- `dispatchKeyEvent`
- `dispatchGenericMotionEvent`

Translate raw Android input into `PlayerAction` through:

- `HidInputTranslator`
- `InputRouter`

### Input rules

- touch button press -> `PlayerAction`
- hardware key press -> same `PlayerAction`
- rotary tick -> `AdjustVolume(+/-step)`
- long press on encoder button -> `PlayPause`
- double press on encoder button -> `Next`

### Volume event aggregation

Do not send one network request per detent.

Use `VolumeCommandAggregator`:

- update UI volume immediately
- debounce outbound volume writes for `100-150 ms`
- flush the latest value only

This matters a lot once a physical encoder is attached.

### USB specifics

Add this manifest feature:

- `android.hardware.usb.host`, `required = false`

If the encoder presents as a normal keyboard/HID device, Android will likely deliver key or motion events without you doing explicit `UsbManager` work.

## 13. Build and deployment

### Build setup

Use release signing from the start, even for local kiosk testing.

Files:

- `keystore.properties`
- `local.properties` or `secrets.properties` for `SPOTIFY_CLIENT_ID`

Keep the client secret out of the app completely. PKCE does not need one.
Register the Spotify redirect URI as `http://127.0.0.1:43821/callback` in the developer dashboard before the first on-device auth test.

### Commands

Build:

```bash
./gradlew assembleRelease
```

Install:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Launch manually for first-run testing:

```bash
adb shell am start -n com.spotifyhub/.app.MainActivity
```

Set device owner on a clean device:

```bash
adb shell dpm set-device-owner com.spotifyhub/.system.kiosk.SpotifyHubDeviceAdminReceiver
```

Verify preferred HOME:

```bash
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

### First boot checklist

1. connect Wi-Fi
2. sideload APK
3. provision device owner if desired
4. launch app
5. sign into Spotify
6. transfer playback from another Spotify device
7. confirm app survives Home, Back, and reboot behavior

## 14. Implementation order

### Phase 0: auth and environment gate

- create the Spotify app entry in the dashboard
- register `http://127.0.0.1:43821/callback` as the redirect URI
- implement `PkceGenerator`
- implement `LoopbackAuthServer.bind()` with a fixed loopback port
- implement the accounts Retrofit client
- implement `/authorize` launch, `/api/token` exchange, and refresh
- prove the full login cycle on the Echo Show with `CustomTabsIntent`
- test the embedded `WebView` fallback and either bless it or explicitly reject it
- test app restart and refresh-token reuse
- test invalid-refresh-token recovery

Exit criteria:

- auth works on the real device today
- no private Spotify auth surfaces are required
- `localhost` is gone and the fixed loopback port is proven in the current implementation
- the fallback path is decided: either WebView is good enough, or the project carries an HTTPS auth broker as a v1 prerequisite

### Phase 1: bootstrap

- create project
- wire Compose shell
- implement `AppGraph`
- implement immersive mode
- implement simple `RootScreen`

### Phase 2: auth

- auth screen polish
- browser capability detection
- timeout and retry UX
- auth success page copy
- token-expiry handling in UI

### Phase 3: playback core

- Retrofit player API
- `PlaybackRepository`
- polling loop
- now-playing UI state
- play/pause, next, previous

### Phase 4: premium surface

- hero artwork
- adaptive theming
- smooth progress interpolation
- shuffle, repeat, like
- device switcher
- volume control

### Phase 5: browse

- recently played overlay
- playlist list overlay
- playlist contents

### Phase 6: kiosk hardening

- device admin
- lock task
- boot completed handling
- default launcher flow

### Phase 7: hardware input

- input router
- HID translation
- volume debouncing

## 15. Risks and open questions

### Highest-risk items

1. OAuth on this device
   - The loopback flow is the right design, but it must be proven on the actual Echo Show browser/WebView stack first. Nocturne is not a trustworthy auth reference for this.

2. Device-owner provisioning
   - Hard kiosk mode is straightforward only if `dpm set-device-owner` is possible on the LineageOS build you are using.

3. AndroidLiquidGlass performance
   - The MT8163 is old. Even subtle blur and glass effects may be too expensive if overused.

4. Spotify development-mode rules
   - As of February 2026, development-mode apps have tighter endpoint and user limits. For a personal appliance this is usually fine, but it still affects how you implement saved-item endpoints.

5. Volume semantics
   - Some Spotify Connect targets either do not support remote volume or apply it inconsistently.

6. Auth fallback complexity
   - If `CustomTabsIntent` or embedded `WebView` cannot complete the loopback flow reliably on this device, the correct fallback is an HTTPS auth broker, which adds deployment and operational scope.

### Open questions to answer with prototypes

1. Does `CustomTabsIntent` have a working provider on the LineageOS build, or will auth need to fall back to WebView immediately?
2. Does the browser or WebView reliably reach `127.0.0.1:43821` loopback on-device?
3. Is the embedded `WebView` path acceptable on this firmware, or should the project immediately adopt the HTTPS broker fallback?
4. Is true device-owner provisioning available without reflashing again?
5. How much liquid-glass effect can the device render before frame time becomes unacceptable?
6. Is the device physically mounted in portrait with a fixed orientation, or do you need landscape support too?

### v2 candidates

- QR-based remote auth broker flow with a tiny backend
- queue management
- search
- better playlist browsing and pagination
- configurable screensaver / idle dim mode
- richer encoder mapping
- baseline profile and macrobenchmark tuning

## 16. References used

- Spotify Web API PKCE tutorial
- Spotify Web API refresh-token tutorial
- Spotify Web API redirect URI rules
- Spotify OAuth migration reminder from October 14, 2025
- Spotify February 2026 development-mode migration guide
- Spotify Web API scopes
- Spotify Web API player, devices, playback, recently-played, and playlist-items references
- Android immersive mode docs
- Android dedicated-device lock-task docs
- Android persistent preferred activity cookbook
- `spotify-tui` for loopback-server implementation shape
- Nocturne UI for layout/state ideas only, explicitly excluding auth
- librespot release notes for auth-volatility awareness only, explicitly excluding direct implementation
- Jetispot, Pasta for Spotify, and AndroidLiquidGlass for UI and theming patterns
