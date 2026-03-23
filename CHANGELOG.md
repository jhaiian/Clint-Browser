# Changelog

All notable changes to Clint Browser are documented here.

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
