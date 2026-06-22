package com.smntoast.client.media;

import com.smntoast.SmnToast;

import java.util.List;


public class LinuxTrackFetcher implements TrackFetcher {
    private final boolean isFlatpak;
    private String[] startupAlert;

    public LinuxTrackFetcher() {
        this.isFlatpak = System.getenv("FLATPAK_ID") != null ||
                new java.io.File("/.flatpak-info").exists();
        this.startupAlert = checkAvailability();
        if (isFlatpak) {
            SmnToast.LOGGER.info("Flatpak environment detected, using D-Bus for MPRIS access");
        }
    }

    private String[] checkAvailability() {
        if (isFlatpak) {
            List<String> dbusTest = CommandRunner.runCommand(
                    "dbus-send", "--session", "--dest=org.freedesktop.DBus",
                    "--type=method_call", "--print-reply",
                    "/org/freedesktop/DBus", "org.freedesktop.DBus.ListNames"
            );
            if (dbusTest.isEmpty()) {
                SmnToast.LOGGER.warn("dbus-send not available in Flatpak");
                return new String[]{"dbus-send not found", "D-Bus tools missing in Flatpak runtime"};
            }
            boolean mprisVisible = false;
            for (String line : dbusTest) {
                if (line.contains("org.mpris.MediaPlayer2.")) {
                    mprisVisible = true;
                    break;
                }
            }
            if (!mprisVisible) {
                SmnToast.LOGGER.warn("No MPRIS players visible via D-Bus in Flatpak — permission may not be granted");
                return new String[]{"No MPRIS players found", "Check D-Bus permission or start a player"};
            }
        } else {
            List<String> versionCheck = CommandRunner.runCommand("playerctl", "--version");
            if (versionCheck.isEmpty()) {
                SmnToast.LOGGER.warn("playerctl not found on PATH — media detection will not work");
                return new String[]{"playerctl not installed", "Install it to enable music detection"};
            }
        }
        return null;
    }

    @Override
    public String[] getStartupAlert() {
        return startupAlert;
    }

    @Override
    public void recheckAvailability() {
        this.startupAlert = checkAvailability();
    }

    @Override
    public TrackInfo fetchCurrentTrack() {
        try {
            if (isFlatpak) {
                return fetchCurrentTrackDbus();
            }
            List<String> statusOutput = CommandRunner.runCommand("playerctl", "status");
            String status = statusOutput.isEmpty() ? null : statusOutput.getFirst();
            if (status == null || !status.trim().equalsIgnoreCase("Playing")) {
                return new TrackInfo("", "", "", "", false);
            }

            List<String> titleOutput = CommandRunner.runCommand("playerctl", "metadata", "title");
            String title = titleOutput.isEmpty() ? null : titleOutput.getFirst();
            List<String> artistOutput = CommandRunner.runCommand("playerctl", "metadata", "artist");
            String artist = artistOutput.isEmpty() ? null : artistOutput.getFirst();
            List<String> albumOutput = CommandRunner.runCommand("playerctl", "metadata", "album");
            String album = albumOutput.isEmpty() ? null : albumOutput.getFirst();
            List<String> trackIdOutput = CommandRunner.runCommand("playerctl", "metadata", "mpris:trackid");
            String trackId = trackIdOutput.isEmpty() ? null : trackIdOutput.getFirst();

            if (title == null || title.isEmpty()) {
                return null;
            }

            if (trackId == null || trackId.isEmpty()) {
                trackId = (title + "-" + artist).hashCode() + "";
            }

            return new TrackInfo(
                    trackId.trim(),
                    title != null ? title.trim() : "Unknown",
                    artist != null ? artist.trim() : "Unknown Artist",
                    album != null ? album.trim() : "",
                    true
            );
        } catch (Exception e) {
            SmnToast.LOGGER.debug("Error fetching MPRIS metadata: {}", e.getMessage());
            return null;
        }
    }

    private TrackInfo fetchCurrentTrackDbus() {
        try {
            String player = findMprisPlayer();
            if (player == null) {
                return new TrackInfo("", "", "", "", false);
            }

            String status = getDbusProperty(player, "PlaybackStatus");
            if (status == null || !status.contains("Playing")) {
                return new TrackInfo("", "", "", "", false);
            }

            String metadata = getDbusProperty(player, "Metadata");
            if (metadata == null) {
                return null;
            }

            String title = extractMetadataValue(metadata, "xesam:title");
            String artist = extractMetadataValue(metadata, "xesam:artist");
            String album = extractMetadataValue(metadata, "xesam:album");
            String trackId = extractMetadataValue(metadata, "mpris:trackid");

            if (title == null || title.isEmpty()) {
                return null;
            }

            if (trackId == null || trackId.isEmpty()) {
                trackId = (title + "-" + artist).hashCode() + "";
            }

            return new TrackInfo(
                    trackId.trim(),
                    title.trim(),
                    artist != null && !artist.isEmpty() ? artist.trim() : "Unknown Artist",
                    album != null ? album.trim() : "",
                    true
            );
        } catch (Exception e) {
            SmnToast.LOGGER.debug("Error fetching D-Bus MPRIS metadata: {}", e.getMessage());
            return null;
        }
    }

    private String findMprisPlayer() {
        try {
            List<String> output = CommandRunner.runCommand(
                    "dbus-send", "--session", "--dest=org.freedesktop.DBus",
                    "--type=method_call", "--print-reply",
                    "/org/freedesktop/DBus", "org.freedesktop.DBus.ListNames"
            );

            for (String line : output) {
                if (line.contains("org.mpris.MediaPlayer2.")) {
                    int start = line.indexOf("org.mpris.MediaPlayer2.");
                    int end = line.indexOf("\"", start);
                    if (end > start) {
                        return line.substring(start, end);
                    }
                }
            }

            return null;
        } catch (Exception e) {
            SmnToast.LOGGER.debug("Error finding MPRIS player: {}", e.getMessage());
            return null;
        }
    }

    private String getDbusProperty(String player, String property) {
        try {
            List<String> output = CommandRunner.runCommand(
                    "dbus-send", "--session", "--dest=" + player,
                    "--type=method_call", "--print-reply",
                    "/org/mpris/MediaPlayer2",
                    "org.freedesktop.DBus.Properties.Get",
                    "string:org.mpris.MediaPlayer2.Player",
                    "string:" + property
            );

            return String.join("\n", output);
        } catch (Exception e) {
            SmnToast.LOGGER.debug("Error getting D-Bus property: {}", e.getMessage());
            return null;
        }
    }

    private String extractMetadataValue(String metadata, String key) {
        try {
            int keyIndex = metadata.indexOf(key);
            if (keyIndex == -1) {
                return null;
            }

            String afterKey = metadata.substring(keyIndex);

            if (key.equals("xesam:artist")) {
                int arrayStart = afterKey.indexOf("array [");
                if (arrayStart != -1) {
                    int stringStart = afterKey.indexOf("string \"", arrayStart);
                    if (stringStart != -1) {
                        int valueStart = stringStart + 8;
                        int valueEnd = afterKey.indexOf("\"", valueStart);
                        if (valueEnd > valueStart) {
                            return afterKey.substring(valueStart, valueEnd);
                        }
                    }
                }
            }

            int stringStart = afterKey.indexOf("string \"");
            if (stringStart != -1) {
                int valueStart = stringStart + 8;
                int valueEnd = afterKey.indexOf("\"", valueStart);
                if (valueEnd > valueStart) {
                    return afterKey.substring(valueStart, valueEnd);
                }
            }

            int variantStart = afterKey.indexOf("variant");
            if (variantStart != -1) {
                String afterVariant = afterKey.substring(variantStart);
                stringStart = afterVariant.indexOf("string \"");
                if (stringStart != -1) {
                    int valueStart = stringStart + 8;
                    int valueEnd = afterVariant.indexOf("\"", valueStart);
                    if (valueEnd > valueStart) {
                        return afterVariant.substring(valueStart, valueEnd);
                    }
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
