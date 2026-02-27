package com.smntoast.client.media;

import com.smntoast.client.SmnToastClient;

import java.util.List;

public class LinuxTrackFetcher implements TrackFetcher {
    private final boolean isFlatpak;

    public LinuxTrackFetcher() {
        this.isFlatpak = System.getenv("FLATPAK_ID") != null ||
                new java.io.File("/.flatpak-info").exists();
        if (isFlatpak) {
            SmnToastClient.LOGGER.info("Flatpak environment detected, using D-Bus for MPRIS access");
        }
    }

    @Override
    public TrackInfo fetchCurrentTrack() {
        try {
            if (isFlatpak) {
                return fetchCurrentTrackDbus();
            }
            String status = CommandRunner.runCommand("playerctl", "status").getFirst();
            if (status == null || !status.trim().equalsIgnoreCase("Playing")) {
                return new TrackInfo("", "", "", "", false);
            }

            String title = CommandRunner.runCommand("playerctl", "metadata", "title").getFirst();
            String artist = CommandRunner.runCommand("playerctl", "metadata", "artist").getFirst();
            String album = CommandRunner.runCommand("playerctl", "metadata", "album").getFirst();
            String trackId = CommandRunner.runCommand("playerctl", "metadata", "mpris:trackid").getFirst();

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
            SmnToastClient.LOGGER.debug("Error fetching MPRIS metadata: {}", e.getMessage());
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
            SmnToastClient.LOGGER.debug("Error fetching D-Bus MPRIS metadata: {}", e.getMessage());
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
            SmnToastClient.LOGGER.debug("Error finding MPRIS player: {}", e.getMessage());
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
            SmnToastClient.LOGGER.debug("Error getting D-Bus property: {}", e.getMessage());
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
