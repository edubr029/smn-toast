package com.smntoast.client.media;

public interface TrackFetcher {
    TrackInfo fetchCurrentTrack();

    default String[] getStartupAlert() {
        return null;
    }

    default void recheckAvailability() {
    }
}
