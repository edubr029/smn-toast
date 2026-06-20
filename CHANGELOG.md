# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4.0] - 2026-06-20

### Added
- Minecraft 26.2 support
- Playerctl availability check on Linux with alert toast
- Flatpak D-Bus permission detection with alert toast
- Warning triangle icon for alert toasts (vs music note for normal toasts)
- `recheckAvailability()` — periodic 60-second background recheck on daemon thread detects if playerctl was installed mid-game

### Changed
- Migrated from Yarn mappings to Mojang official mappings (26.1+ requirement)
- Build toolchain: Loom 1.17, Gradle 9.5.1, Fabric Loader 0.19.3, Java 25
- Toast manager access: `getToastManager()` → `gui.toastManager()` (26.2 Gui reorganization)
- Toast rendering: `draw()` → `extractRenderState()`, `GuiGraphicsExtractor` API

### Fixed
- Toast stops working after logout/rejoin (`isShowing` flag stuck true)
- Keybind shows "No music playing" instead of alert when playerctl missing

## [1.3.0] - 2026-06-18

### Added
- Minecraft 26.1 support (first unobfuscated Minecraft version)
- Mojang official mappings migration (Yarn discontinued)

### Changed
- Build toolchain: Loom 1.16, Gradle 9.4.1, Fabric API 0.151.0+26.1.2
- All API names updated from Yarn to Mojang mappings
- Java 21 → Java 25

## [1.2.1] - 2026-02-27

### Added
- macOS support via AppleScript (contributed by NatsuCamellia)
- Guard against empty list from CommandRunner in LinuxTrackFetcher

### Changed
- Package renamed from `mpris` to `media`
- Modularized track fetching logic into separate fetcher classes
- Extracted CommandRunner utility

## [1.2.0] - 2026-01-21

### Added
- Windows support via SMTC (System Media Transport Controls)
- Flatpak support for MPRIS detection via D-Bus

## [1.0.0] - 2025-12-28

### Added
- Initial release
- MPRIS music detection with toast notifications on Linux
- Configurable keybind to show current track on demand
- Custom keybind category
- English translations
