# 🎵 System Music Notification Toast
Display your currently playing music as a Minecraft toast notification.

> [!IMPORTANT]
> This documentation is written with [PrismLauncher](https://github.com/PrismLauncher/PrismLauncher) in mind. Other launchers should work, but some commands (especially for Flatpak) may need adjustments for your specific launcher.

## What it does
System Music Notification Toast (SMN Toast) is a client-side Fabric mod that detects music playing on your system and displays it as a toast notification in Minecraft. When a new track starts playing, you'll see a "Now Playing" toast showing the song title and artist name.

### Features
- Automatic notifications — A toast appears whenever a new track starts playing
- Manual trigger — Configurable keybind to show the current track on demand (unbound by default)
- Clean design — Uses the vanilla Minecraft toast style
- Cross-platform — Works on Windows, Linux and macOS

### Platform Support

| Platform | Method | Requirements |
|----------|--------|--------------|
| **Windows** | SMTC (System Media Transport Controls) | None (built-in) |
| **Linux** | MPRIS via D-Bus | None (native) or playerctl (non-Flatpak) |
| **Linux (Flatpak)** | MPRIS via D-Bus | Permission grant (see below) |
| **macOS** | AppleScript | Permission grant (see below) |

### How to use
1. Install the mod in your Fabric mods folder
2. Start playing music in any compatible player
3. A toast will appear when a new track starts

To manually show the current track, bind a key in Options → Controls → Key Binds → System Music Notification Toast.

### Compatible Players
- **Windows**: Spotify, Windows Media Player, browser media (Edge/Chrome), foobar2000, and any app using SMTC
- **Linux**: Spotify, VLC, Firefox, Chromium, and any MPRIS-compatible player
- **macOS**: Spotify, Music.app

## Flatpak Users (Linux)
If you're using a Flatpak launcher (like PrismLauncher), you need to grant permission for the mod to communicate with media players.

Run this command:
```bash
flatpak override --user org.prismlauncher.PrismLauncher '--talk-name=org.mpris.MediaPlayer2.*'
```

Or using **Flatseal**:
1. Open Flatseal and select your launcher (e.g., PrismLauncher)
2. Go to **Session Bus** → **Talk**
3. Add `org.mpris.MediaPlayer2.*`

Restart the launcher after applying the permission.

> **Note**: This permission only allows communication with media players — it does not grant access to run commands on your host system.

## macOS Permission
macOS will prompt you to allow Minecraft (or your launcher) to control Spotify and/or Music.app. Allow it to enable track detection.

If you denied it previously, go to **System Settings** → **Privacy & Security** → **Automation** and enable access for your launcher or Minecraft.
