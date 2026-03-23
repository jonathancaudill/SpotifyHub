# SpotifyHub

A dedicated Android music controller for Spotify, designed to turn landscape displays (like the Echo Show) into a beautiful always-on playback dashboard. Built entirely with Jetpack Compose and Kotlin, it combines a persistent now-playing surface with Home, Search, and Library browsing backed by the Spotify Web API.

## Features

- **Now Playing Dashboard** — Large album art, track metadata, and playback controls in a landscape-optimized layout
- **Sidebar Navigation Shell** — Persistent glass rail with Home, Search, Library, Rate, and Now Playing tabs
- **Browse + Library Views** — Home recommendations, search results, saved library lists, and playlist/album detail pages
- **Album Rating Tab** — Rate the currently playing album (0.0–10.0) with a touch-draggable circular dial. Ratings are submitted to a Google Sheet via Apps Script, existing ratings are detected and locked, and album art can be written into the sheet as an `IMAGE(...)` formula
- **Swipe-Over Detail Pages** — Library and browse details stay scoped to their owning tab instead of replacing the global screen
- **Shader-Driven Backdrop** — Real-time OpenGL ES 2.0 album art distortion with multi-pass Kawase blur, smooth crossfades between tracks
- **Full Playback Control** — Play/pause, skip, shuffle, repeat, volume, seek (draggable progress bar), and library save/unsave
- **Draggable Seek Bar** — Tap or drag to scrub through tracks with live time preview; seeks via the Spotify Web API
- **OAuth2 PKCE Authentication** — Secure login via loopback callback server, no client secret required. Tokens stored in `EncryptedSharedPreferences`
- **Kiosk Mode** — Immersive fullscreen, auto-launch on boot, landscape-locked single-task activity
- **Hardware Input** — Volume up/down keys routed directly to Spotify volume control
- **Connectivity Monitoring** — Offline indicator with graceful degradation
- **SF Pro Typography** — Custom font stack for a polished display aesthetic

## Architecture

```
Spotify Web API
      │
SpotifyPlayerApi / SpotifyLibraryApi / SpotifyBrowseApi / SpotifySearchApi
      │
PlaybackRepository / LibraryRepository / BrowseRepository / SearchRepository / SheetsRepository
      │
PlayerViewModel / LibraryViewModel / HomeViewModel / SearchViewModel / DetailViewModel / RatingViewModel
      │
MainScreen                                  ← Sidebar shell + tab container
  ├── persistent AlbumBackdropHost          ← Always-mounted GLSurfaceView for warm shader transitions
  ├── Home / Search / Library content pages
  ├── RatingScreen                          ← Album rating with circular drag dial + Google Sheets sync
  ├── tab-scoped swipe-over detail sheet
  └── NowPlayingContent                     ← Track metadata, transport, seek, utility controls
```

**Key patterns:**
- Manual dependency injection via `AppGraph` (no Hilt)
- `StateFlow`-based reactive state
- Repository layer owns API calls and polling
- Client-side progress interpolation (180ms tick) for smooth bar animation between polls

## Project Structure

```
app/src/main/java/com/spotifyhub/
├── app/                  # Application shell, DI graph, ViewModelFactory
├── auth/                 # OAuth2 PKCE flow, loopback server, encrypted token store
├── browse/               # Home/browse repository and models
├── library/              # Library repository and models
├── playback/             # PlaybackRepository, domain models (PlaybackSnapshot, etc.)
├── rating/               # SheetsRepository (Google Sheets integration via Apps Script)
├── search/               # Search repository and models
├── spotify/
│   ├── api/              # Retrofit interfaces (Player, Library, Browse, Search, Accounts)
│   ├── dto/              # Moshi data transfer objects
│   └── mapper/           # DTO → domain model mapping
├── system/
│   ├── kiosk/            # Immersive mode, boot receiver, device admin
│   ├── input/            # Hardware key routing (volume keys)
│   └── network/          # Connectivity monitor
├── theme/                # Material 3 theme, SF Pro typography
└── ui/
    ├── auth/             # Auth screen
    ├── common/           # Shared UI utilities (overscroll, indicators)
    ├── detail/           # Playlist/album detail sheet
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
├── album_backdrop_scene.frag     # Scene shader (twisted art copies, compositing)
└── album_backdrop_blur.frag      # Kawase blur (9-tap, multi-pass, saturation boost)
```

## Recent Changes Since The Last README Update

- Replaced the rating tab's vertical scroll wheel with a thermostat-style circular drag dial and rebalanced the layout so album art, metadata, lock state, and submit action fit more cleanly in landscape
- Fixed Google Apps Script submission reliability by manually following the script redirect flow and increasing network timeouts to tolerate Apps Script cold starts
- Updated Google Sheets payloads so submitted album art can render directly in the sheet through an `IMAGE(...)` formula
- Hardened Spotify search mapping against `null` items in API paging responses to avoid search result parsing failures
- Expanded Spotify auth scopes to include `user-top-read` and `user-read-private`

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 21
- Android SDK 36
- A [Spotify Developer](https://developer.spotify.com/dashboard) application

### Spotify App Setup

1. Create an app in the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Add this redirect URI to your app settings:
   ```
   http://127.0.0.1:43821/callback
   ```
3. Copy your **Client ID**

### Build Configuration

Add your Spotify Client ID and (optionally) your Google Sheets script URL to `~/.gradle/gradle.properties`:

```properties
SPOTIFY_CLIENT_ID=your_client_id_here
SHEETS_SCRIPT_URL=https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/exec
```

#### Google Sheets Rating Integration (optional)

To enable the album rating tab:

1. Open your Google Sheet → **Extensions → Apps Script**
2. Paste the contents of `google-apps-script/Code.gs`
3. **Deploy → New deployment → Web app** → Execute as "Me", Access "Anyone"
4. Copy the deployment URL and set it as `SHEETS_SCRIPT_URL` in your Gradle properties
5. Expect the deployed Apps Script endpoint to issue a redirect before returning JSON; SpotifyHub now follows that flow explicitly and uses longer timeouts to handle cold starts

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

## Spotify API Endpoints Used

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/v1/me/player` | Current playback state (polled every 1.5s) |
| `GET` | `/v1/me/playlists` | User playlists for browse/library |
| `GET` | `/v1/me/albums` | Saved albums |
| `GET` | `/v1/me/tracks` | Saved tracks |
| `GET` | `/v1/playlists/{id}` | Playlist detail |
| `GET` | `/v1/playlists/{id}/items` | Playlist tracks |
| `GET` | `/v1/albums/{id}/tracks` | Album tracks |
| `GET` | `/v1/search` | Search across tracks, albums, artists, playlists |
| `GET` | `/v1/me/player/recently-played` | Home tab recents |
| `GET` | `/v1/browse/featured-playlists` | Home tab featured playlists |
| `PUT` | `/v1/me/player/play` | Resume playback |
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

### Required Scopes

```
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

1. **Scene pass** — Generates 4 twisted/rotated copies of the album artwork with Apple-style deformation, each with seeded random parameters for deterministic yet varied motion
2. **Blur passes** — 8 stacked Kawase blur passes with coprime offsets (`5, 11, 19, 13, 37, 23, 71, 43`) to avoid grid artifacts
3. **Final composite** — Saturation boost + vignette fade on the last pass

Artwork is downsampled to 512x512 and rendered to an offscreen framebuffer at 50% viewport resolution for performance. Smooth crossfades occur when tracks change, and the GLSurfaceView host remains mounted in the main shell so the shader stays warm when switching back to the Now Playing tab.

## Tech Stack

| Component | Library | Version |
|-----------|---------|---------|
| UI | Jetpack Compose (Material 3) | BOM 2026.03.00 |
| Language | Kotlin | 2.3.10 |
| HTTP | Retrofit + OkHttp | 3.0.0 / 5.3.2 |
| JSON | Moshi | 1.15.2 |
| Images | Coil Compose | 2.7.0 |
| Async | Kotlin Coroutines | 1.10.2 |
| Security | AndroidX Security Crypto | 1.1.0 |
| Graphics | OpenGL ES 2.0 | (Android built-in) |
| Min SDK | Android 11 | API 30 |

## License

All rights reserved.
