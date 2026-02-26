package com.smntoast.client.mpris;

import com.smntoast.client.SmnToastClient;

public class MacTrackFetcher implements TrackFetcher {
    @Override
    public TrackInfo fetchCurrentTrack() {
        TrackInfo spotify = fetchCurrentTrack("Spotify");
        if (spotify != null && spotify.isPlaying()) {
            return spotify;
        }
        return fetchCurrentTrack("Music");
    }

    private TrackInfo fetchCurrentTrack(String app) {
        try {
            return TrackInfoParser.parseTrackInfo(CommandRunner.runCommand(
                    "osascript",
                    "-e", "tell application \"" + app + "\"",
                    "-e", "  if it is running then",
                    "-e", "    set playerState to player state as string",
                    "-e", "    if playerState is \"playing\" then",
                    "-e", "      set trackName to name of current track",
                    "-e", "      set trackArtist to artist of current track",
                    "-e", "      set trackAlbum to album of current track",
                    "-e", "      return \"STATUS:Playing\" & linefeed & \"TITLE:\" & trackName & linefeed & \"ARTIST:\" & trackArtist & linefeed & \"ALBUM:\" & trackAlbum",
                    "-e", "    else",
                    "-e", "      return \"STATUS:Paused\"",
                    "-e", "    end if",
                    "-e", "  else",
                    "-e", "    return \"STATUS:NotRunning\"",
                    "-e", "  end if",
                    "-e", "end tell"
            ));
        } catch (Exception e) {
            SmnToastClient.LOGGER.debug("Error fetching Mac metadata: {}", e.getMessage());
            return null;
        }
    }
}
