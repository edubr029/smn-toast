# 🎵 System Music Notification Toast
Display your currently playing music as a Minecraft toast notification.

> [!IMPORTANT]
> This documentation is written with [PrismLauncher](https://github.com/PrismLauncher/PrismLauncher) in mind. Other launchers should work, but some commands (especially for Flatpak) may need adjustments for your specific launcher.

## What it does
System Music Notification Toast (SMN Toast) is a client-side Fabric mod that detects music playing on your **Linux** system and displays it as a toast notification in Minecraft. When a new track starts playing, you'll see a "Now Playing" toast showing the song title and artist name.
### Features
- Automatic notifications — A toast appears whenever a new track starts playing
- Manual trigger — Configurable keybind to show the current track on demand (unbound by default)
- Clean design — Uses the vanilla Minecraft toast style
### Requirements
- **Linux only** — This mod uses MPRIS (Media Player Remote Interfacing Specification)
- playerctl — Must be installed on your system
- MPRIS-compatible player — Works with Spotify, VLC, Firefox, and most music players
### How to use
1. Install the mod in your Fabric mods folder
2. Make sure playerctl is installed on your system
3. Start playing music in any MPRIS-compatible player
4. A toast will appear when a new track starts
To manually show the current track, bind a key in Options → Controls → Key Binds → System Music Notification Toast.
## Flatpak Users
If you're using a Flatpak launcher (like PrismLauncher), you need to grant permission for the mod to communicate with the host system.

Run this command:
```bash
flatpak override --user org.prismlauncher.PrismLauncher --talk-name=org.freedesktop.Flatpak
```

Or using **Flatseal**:
1. Open Flatseal and select your launcher (e.g., PrismLauncher)
2. Go to **Session Bus** → **Talk**
3. Add `org.freedesktop.Flatpak`

Restart the launcher after applying the permission.

Warning: used permission open sandbox hole for executing arbitrary commands and may potentially provide attack vector for [malicious mods](https://github.com/trigram-mrp/fractureiser/).
## Note
This mod is designed for Linux systems only. It will not function on Windows or macOS as it relies on the MPRIS D-Bus interface. 
