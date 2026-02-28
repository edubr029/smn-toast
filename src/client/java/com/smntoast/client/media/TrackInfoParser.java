package com.smntoast.client.media;

import java.util.List;

public class TrackInfoParser {
    public static TrackInfo parseTrackInfo(List<String> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        String status = null;
        String artist = "";
        String title = "";
        String album = "";

        for (String line : data) {
            if (line.startsWith("STATUS:")) {
                status = line.substring(7);
            } else if (line.startsWith("ARTIST:")) {
                artist = line.substring(7);
            } else if (line.startsWith("TITLE:")) {
                title = line.substring(6);
            } else if (line.startsWith("ALBUM:")) {
                album = line.substring(6);
            }
        }

        if (!"Playing".equals(status)) {
            return new TrackInfo("", "", "", "", false);
        }

        if (title == null || title.isEmpty()) {
            return null;
        }

        String trackId = (title + "-" + artist).hashCode() + "";

        return new TrackInfo(
                trackId,
                title.trim(),
                artist != null && !artist.isEmpty() ? artist.trim() : "Unknown Artist",
                album != null ? album.trim() : "",
                true
        );
    }
}
