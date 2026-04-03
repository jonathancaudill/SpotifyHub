# SpotifyHub

A dedicated Android music controller for Spotify, designed to turn landscape displays like the Echo Show into a polished always-on playback dashboard. Built with Jetpack Compose and Kotlin, it combines a persistent now-playing surface with personalized discovery, library browsing, rich detail views, and a Google Sheets-backed album rating flow.

## Features

- **Landscape-native shell** — Persistent sidebar navigation with Home, Search, Library, Rate, and Now Playing tabs, tuned through adaptive `LandscapeUiProfile` breakpoints instead of one fixed tablet layout
- **Now Playing dashboard** — Large art, metadata, transport controls, library save/unsave, volume, repeat, shuffle, and tap-or-drag seeking in a more compact control surface
- **Optimistic playback controls** — Transport and seek actions update locally first, while queue polling caches adjacent tracks for faster skip transitions and fewer visible snap-backs
- **Personalized home discovery** — Greeting, quick-access albums, recently played history, top tracks, top artists, deep cuts, saved albums/songs, playlist shelves, and genre-driven discovery built from Spotify data
- **Pinned personalized shelves** — Optional Gradle-configured playlist IDs let you force specific Discover Weekly, Release Radar, or Daily Mix shelves into Home even when Spotify metadata is inconsistent
- **Smarter search** — Query intent parsing handles field filters like `artist:`, `album:`, `track:`, `genre:`, `year:`, `isrc:`, and `upc:` with reranking and fallback passes
- **Artist detail pages** — Artist sheets include albums, singles/EPs, featured-on releases, curated playlists, and a best-effort Wikipedia bio
- **Swipe-over detail pages** — Album, playlist, and artist detail stays scoped to the current browse tab instead of replacing the whole app shell
- **Album rating tab** — Rate the currently playing album from `0.0` to `10.0` with a circular drag dial; ratings sync to Google Sheets through Apps Script and can include album art as an `IMAGE(...)` formula
- **Custom motion and shape language** — Squircle-based surfaces, tuned bounce overscroll, and a refined sidebar/detail presentation keep the UI feeling cohesive on fixed landscape hardware
- **Shader-driven backdrop** — Real-time OpenGL ES 2.0 album art distortion with multi-pass blur, seeded motion, and smoother crossfades between tracks
- **OAuth2 PKCE authentication** — Secure login via loopback callback server, no client secret required; tokens are stored in `EncryptedSharedPreferences`
- **Kiosk mode support** — Immersive fullscreen, auto-launch on boot, landscape lock, and single-task activity behavior for dedicated-controller installs
- **Hardware input + connectivity awareness** — Volume keys route to Spotify volume control and the shell surfaces offline state gracefully

## Architecture

```text
Spotify Web API + Spotify oEmbed + Wikipedia REST API
      │
SpotifyPlayerApi / SpotifyLibraryApi / SpotifyBrowseApi / SpotifySearchApi / SpotifyArtistApi
SpotifyEmbedApi / WikipediaApi
      │
PlaybackRepository / LibraryRepository / BrowseRepository / SearchRepository / ArtistRepository / SheetsRepository
      │
PlayerViewModel / LibraryViewModel / HomeViewModel / SearchViewModel / DetailViewModel / RatingViewModel
      │
MainScreen                                  ← Sidebar shell + tab container
  ├── persistent AlbumBackdropHost          ← Always-mounted GLSurfaceView for warm shader transitions
  ├── Home / Search / Library / Rating pages
  ├── tab-scoped swipe-over DetailScreen    ← Album / playlist / artist detail
  └── NowPlayingContent                     ← Optimistic transport, seek, utility controls
```

**Key patterns:**
- Manual dependency injection via `AppGraph` (no Hilt)
- `StateFlow`-based reactive state
- Repository layer owns API calls, caching, optimistic state, and polling
- Client-side progress interpolation for smooth seek/progress animation between playback polls
- Parallel fetches for home shelves, playback state + queue, and artist detail sections where latency matters

## Project Structure

```text
app/src/main/java/com/spotifyhub/
├── app/                  # Application shell, DI graph, ViewModelFactory
├── artist/               # ArtistRepository and artist detail models
├── auth/                 # OAuth2 PKCE flow, loopback server, encrypted token store
├── browse/               # Home/browse repository and models
├── library/              # Library repository and models
├── playback/             # PlaybackRepository, domain models (PlaybackSnapshot, etc.)
├── rating/               # SheetsRepository (Google Sheets integration via Apps Script)
├── search/               # Search repository and models
├── spotify/
│   ├── api/              # Retrofit interfaces (Spotify APIs, oEmbed, Wikipedia, Accounts)
│   ├── dto/              # Moshi data transfer objects
│   └── mapper/           # DTO → domain model mapping
├── system/
│   ├── kiosk/            # Immersive mode, boot receiver, device admin
│   ├── input/            # Hardware key routing (volume keys)
│   └── network/          # Connectivity monitor
├── theme/                # Material 3 theme, SF Pro typography
└── ui/
    ├── auth/             # Auth screen
    ├── common/           # Shared UI utilities (overscroll, indicators, landscape profiles)
    ├── detail/           # Album / playlist / artist detail sheet
    ├── home/             # Home tab
    ├── library/          # Library tab
    ├── main/             # Main shell, sidebar, tab navigation
    ├── nowplaying/       # Now Playing screen, PlayerViewModel
    │   └── backdrop/     # OpenGL renderer, shaders, bitmap controller
    ├── rating/           # Rating tab (circular dial, album info, submit)
    ├── root/             # Root auth/main routing
    └── search/           # Search tab

app/src/main/assets/shaders/
├── album_backdrop.vert           # Vertex shader (pass-through quad)
├── album_backdrop_scene.frag     # Scene shader (distorted art copies, compositing)
├── album_backdrop_twist.frag     # Twist deformation pass
├── album_backdrop_sprite.frag    # Sprite compositing helpers
├── album_backdrop_present.frag   # Final present/composite pass
└── album_backdrop_blur.frag      # Multi-pass blur
```

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 21
- Android SDK 36
- A [Spotify Developer](https://developer.spotify.com/dashboard) application

### Spotify App Setup

1. Create an app in the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Add this redirect URI to your app settings:
   ```text
   http://127.0.0.1:43821/callback
   ```
3. Copy your **Client ID**

### Build Configuration

Add your Spotify Client ID and optional integration properties to `~/.gradle/gradle.properties`:

```properties
SPOTIFY_CLIENT_ID=your_client_id_here
SHEETS_SCRIPT_URL=https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/exec
SPOTIFY_HOME_DISCOVER_RELEASE_IDS=spotify:playlist:...,https://open.spotify.com/playlist/...
SPOTIFY_HOME_DAILY_MIX_IDS=37i9dQZF...,37i9dQZF...
```

- `SPOTIFY_CLIENT_ID` is required
- `SHEETS_SCRIPT_URL` enables the rating tab submission flow
- `SPOTIFY_HOME_DISCOVER_RELEASE_IDS` optionally pins specific playlist IDs or Spotify playlist URLs into the "Discover Weekly & Release Radar" shelf
- `SPOTIFY_HOME_DAILY_MIX_IDS` optionally pins specific playlist IDs or URLs into the "Your Daily Mixes" shelf

### Release Signing

Release builds can be signed from Gradle using values from any of these sources:

- `keystore.properties` in the project root
- `~/.gradle/gradle.properties`
- environment variables

The expected keys are:

```properties
RELEASE_STORE_FILE=keystore/spotifyhub-release.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=spotifyhub-release
RELEASE_KEY_PASSWORD=your_key_password
```

- for the recommended PKCS12 keystore, `RELEASE_KEY_PASSWORD` should match `RELEASE_STORE_PASSWORD`
- `keystore.properties.example` is included as a template
- release tasks fail fast with a clear Gradle error if signing is not configured

Generate a keystore once:

```bash
mkdir -p keystore
keytool -genkeypair \
  -v \
  -storetype PKCS12 \
  -keystore keystore/spotifyhub-release.jks \
  -alias spotifyhub-release \
  -keyalg RSA \
  -keysize 4096 \
  -validity 9125
```

#### Google Sheets Rating Integration (optional)

To enable the album rating tab:

1. Open your Google Sheet -> **Extensions -> Apps Script**
2. Paste the contents of `google-apps-script/Code.gs`
3. **Deploy -> New deployment -> Web app** -> Execute as "Me", Access "Anyone"
4. Copy the deployment URL and set it as `SHEETS_SCRIPT_URL` in your Gradle properties
5. Expect the deployed Apps Script endpoint to issue a redirect before returning JSON; SpotifyHub follows that redirect explicitly and uses longer timeouts to tolerate cold starts

### Fonts

This project uses SF Pro fonts which are not included in the repository due to licensing. Place the following font files in `app/src/main/res/font/`:

- `sf_pro_text_light.otf`
- `sf_pro_text_regular.otf`
- `sf_pro_text_medium.otf`
- `sf_pro_text_semibold.otf`
- `sf_pro_text_bold.otf`
- `sf_pro_display_light.otf`
- `sf_pro_display_regular.otf`
- `sf_pro_display_medium.otf`
- `sf_pro_display_semibold.otf`
- `sf_pro_display_bold.otf`

### Build & Run

```bash
./gradlew assembleDebug
```

Install on a connected device:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Build a signed release APK:

```bash
./gradlew assembleRelease
```

The signed APK is written to `app/build/outputs/apk/release/app-release.apk`.

Build a signed release bundle:

```bash
./gradlew bundleRelease
```

The signed bundle is written to `app/build/outputs/bundle/release/app-release.aab`.

## Spotify API Endpoints Used

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/v1/me/player` | Current playback state |
| `GET` | `/v1/me/player/queue` | Queue snapshot for adjacent-track caching |
| `GET` | `/v1/me/playlists` | User playlists for browse/library |
| `GET` | `/v1/me/albums` | Saved albums |
| `GET` | `/v1/me/tracks` | Saved tracks |
| `GET` | `/v1/me` | Current user profile for greeting/display name |
| `GET` | `/v1/me/top/tracks` | Personalized top tracks shelves |
| `GET` | `/v1/me/top/artists` | Personalized top artists shelves and genre seeding |
| `GET` | `/v1/playlists/{id}` | Playlist detail |
| `GET` | `/v1/playlists/{id}/items` | Playlist tracks |
| `GET` | `/v1/albums/{id}/tracks` | Album tracks |
| `GET` | `/v1/artists/{id}` | Artist detail |
| `GET` | `/v1/artists/{id}/albums` | Artist releases by group |
| `GET` | `/v1/search` | Search across tracks, albums, artists, and playlists |
| `GET` | `/v1/me/player/recently-played` | Recently played history |
| `PUT` | `/v1/me/player/play` | Resume playback |
| `PUT` | `/v1/me/player/play` + body | Start album/playlist/track context playback |
| `PUT` | `/v1/me/player/pause` | Pause playback |
| `POST` | `/v1/me/player/next` | Skip to next track |
| `POST` | `/v1/me/player/previous` | Skip to previous track |
| `PUT` | `/v1/me/player/shuffle` | Toggle shuffle |
| `PUT` | `/v1/me/player/repeat` | Cycle repeat mode |
| `PUT` | `/v1/me/player/volume` | Set volume percent |
| `PUT` | `/v1/me/player/seek` | Seek to position in track |
| `GET` | `/v1/me/library/contains` | Check if track is saved |
| `PUT` | `/v1/me/library` | Save track to library |
| `DELETE` | `/v1/me/library` | Remove track from library |

## External Integrations

| Service | Endpoint | Purpose |
|---------|----------|---------|
| Spotify oEmbed | `/oembed` | Fallback metadata/artwork for pinned playlists when Web API fetches fail |
| Wikipedia API | `/w/api.php?action=query&list=search...` | Artist page lookup |
| Wikipedia REST | `/api/rest_v1/page/summary/{title}` | Artist bio summary cards |

### Required Scopes

```text
user-read-playback-state
user-read-currently-playing
user-modify-playback-state
user-library-read
user-library-modify
playlist-read-private
playlist-read-collaborative
user-read-recently-played
user-top-read
user-read-private
```

## Authentication Flow

SpotifyHub uses **OAuth2 with PKCE** (no client secret needed):

1. Generate a random `code_verifier` and its SHA-256 `code_challenge`
2. Start a loopback HTTP server on `127.0.0.1:43821`
3. Open Spotify authorization in a Custom Tab (or fallback browser)
4. Spotify redirects to `http://127.0.0.1:43821/callback?code=...`
5. The loopback server captures the authorization code
6. Exchange the code for access + refresh tokens
7. Store the refresh token in `EncryptedSharedPreferences` (AES256-GCM)
8. Auto-refresh the access token 5 minutes before expiry; 401 responses also trigger a refresh via OkHttp `Authenticator`

## The Backdrop Shader

The album art backdrop uses a multi-stage OpenGL ES 2.0 pipeline:

1. **Scene generation** — Produces multiple distorted artwork instances with seeded transforms so each album gets stable but non-static motion
2. **Twist + sprite compositing** — Additional fragment stages shape and layer the artwork before blur
3. **Blur stack** — Up to 8 blur passes build the soft backdrop while preserving enough color structure to feel alive
4. **Present pass** — Final saturation and compositing treatment before the image hits the screen

Artwork is downsampled to `512x512` and rendered to an offscreen framebuffer at 50% viewport resolution for performance. Smooth crossfades occur when tracks change, and the `GLSurfaceView` host remains mounted in the main shell so the shader stays warm when switching back to the Now Playing tab.

## Tech Stack

| Component | Library | Version |
|-----------|---------|---------|
| UI | Jetpack Compose (Material 3) | BOM `2026.03.00` |
| Language | Kotlin | `2.3.10` |
| HTTP | Retrofit + OkHttp | `3.0.0` / `5.3.2` |
| JSON | Moshi | `1.15.2` |
| Images | Coil Compose | `2.7.0` |
| Async | Kotlin Coroutines | `1.10.2` |
| Security | AndroidX Security Crypto | `1.1.0` |
| Icons | Cupertino + extended icons | `0.1.0-alpha04` |
| Shapes | squircle-shape-android | `5.2.0` |
| Graphics | OpenGL ES 2.0 | Android built-in |
| Min SDK | Android 11 | API 30 |

## License

All rights reserved.
