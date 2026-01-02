# 🎵 System Music Notification Toast
Display your currently playing music as a Minecraft toast notification.
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
## Note
This mod is designed for Linux systems only. It will not function on Windows or macOS as it relies on the MPRIS D-Bus interface.