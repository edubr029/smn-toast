package com.smntoast.client.mpris;

public class TrackInfo {
    private final String trackId;
    private final String title;
    private final String artist;
    private final String album;
    private final boolean playing;

    public TrackInfo(String trackId, String title, String artist, String album, boolean playing) {
        this.trackId = trackId;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.playing = playing;
    }

    public String getTrackId() {
        return trackId;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public boolean isPlaying() {
        return playing;
    }
}

