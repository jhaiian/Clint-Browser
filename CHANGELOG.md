# Changelog

All notable changes to Clint Browser are documented here.

---

## v1.0.1-beta-1

### New Features

#### Bookmarks
- Save any page with a single tap from the bookmark button in the navigation bar
- Bookmark icon updates live to reflect the saved state of the current page
- Dedicated Bookmarks screen — view, open, and delete saved pages
- Bookmarks accessible from the ⋮ menu
- All bookmarks stored locally on device — never synced or uploaded

#### Scroll-Hide Toolbar & Navigation Bar
- Toolbar and navigation bar automatically slide away when scrolling down, and return when scrolling up
- Smooth height-based animation — WebView resizes in sync, no content is hidden
- Pull-to-refresh is automatically disabled while bars are hidden to prevent accidental refresh
- Toggled from Settings → General → Hide bars when scrolling (enabled by default)

#### General Settings
- New General section added to Settings
- Houses the scroll-hide behavior toggle

#### Permissions
- Added `REQUEST_INSTALL_PACKAGES` — used to install APK files downloaded through the browser and to apply in-app updates. Installation always requires explicit user confirmation.

### Improvements

- **Address bar now updates in real time** — URL reflects redirects and SPA route changes as they happen, not just on page start and finish
- **Address bar displays the full URL** — protocol and full path shown as-is

### Dependencies

- Raised `compileSdk` and `targetSdk` from 34 to 36
- `androidx.core:core-ktx` 1.12.0 → 1.16.0
- `androidx.appcompat:appcompat` 1.6.1 → 1.7.0
- `com.google.android.material:material` 1.11.0 → 1.12.0
- `androidx.webkit:webkit` 1.10.0 → 1.13.0

### CI/CD

- `release.yml` now automatically bumps `versionName` and `versionCode` in `build.gradle` when a tag is pushed and commits the change back to `main`
- `release.yml` now writes `versionCode` into the update manifest alongside `version`
- Draft releases for the same tag are automatically deleted before a new release is created
- Manifest commit now uses `GIT_USERNAME` and `GIT_EMAIL` secrets instead of the generic `github-actions[bot]` identity
- Added `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24` environment variable for Actions compatibility

### Internal

- Full package restructure — all source files reorganised into `activities/`, `bookmarks/`, `crash/`, `downloads/`, `network/`, `settings/`, `tabs/`, `update/`, and `webview/` packages

### Documentation

- Privacy Policy updated to cover the bookmark system and the `REQUEST_INSTALL_PACKAGES` permission
- README updated with screenshots, beta version badge, and corrected release tag links
- Contributing.md project structure updated to reflect the new package layout

### About Page

- Author link now points to `linktr.ee/jhaiian` instead of `github.com/jhaiian`
- Author hypertext label updated to `jhaiian`
- PayPal donate link replaced with a dedicated Contact section using the email address
- New Contributors section added — links to `Contributors.md` on GitHub
- Email intent subject line changed from `"Clint Browser Support"` to `"Clint Browser"`

### Credits

- **Vonjooo** — improvements to `release.yml` ([#1](https://github.com/jhaiian/Clint-Browser/pull/1))
- **snashyturner** — reported unable to install APK from Downloads ([#2](https://github.com/jhaiian/Clint-Browser/issues/2))

---

## v1.0.0 — First Release

### Browser Core
- WebView-based browsing with full tab management
- Multi-tab support with tab switcher
- Incognito mode with no cookies, cache, or history saved
- Address bar with select-all on focus for easy URL replacement
- Back, forward, refresh, and home navigation
- Pull-to-refresh support
- Desktop Mode toggle via the ⋮ menu
- Intent support — websites can open installed apps (e.g. YouTube, Spotify)
- Full screen video and media support

### Privacy & Security
- Tracker and analytics domain blocking at the network level
- Third-party cookie blocking
- Generic User-Agent to reduce fingerprinting
- DNS over HTTPS (DoH) with four modes: Off, Default, Increased, Max
- DoH provider choice: Cloudflare or Quad9
- SSL error enforcement — invalid certificates are always rejected
- Incognito tabs fully isolated from normal session data

### Search
- Default search engine selection: DuckDuckGo, Brave Search, or Google
- Google privacy warning shown when switching to Google
- Search engine changeable at any time from settings

### Downloads
- Custom download engine built on OkHttp — no system DownloadManager
- Real-time download progress screen with percentage and file size
- Cancel downloads in-app or from the notification
- Open completed files directly from the downloads screen or notification
- Duplicate filename handling — auto-renames to avoid overwrites

### UI & Experience
- Dark purple theme throughout
- Custom popup menu replacing the system overflow menu
- Adaptive launcher icon with black background
- Full screen support for video playback

### Updates
- In-app update checker for Stable and Beta channels
- Architecture-aware download links (arm64-v8a, armeabi-v7a, x86, x86_64, universal)
- Check for updates on launch (optional)
- Beta enrolment with channel description

### Setup & Onboarding
- First-launch setup wizard: Privacy Policy & Terms consent, search engine selection, and DoH configuration
- Privacy Policy and Terms of Service linked to GitHub — always up to date

### About
- App version, architecture, and build info
- Links to GitHub repository, Privacy Policy, Terms of Service, Discord community, Ko-fi, and PayPal
