package com.smntoast.client.mpris;

import com.smntoast.client.SmnToastClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MPRIS listener that uses playerctl to monitor music playback on Linux.
 * This is a simpler approach than using dbus-java directly.
 * Supports both native and Flatpak installations.
 */
public class MprisListener {
    private Thread listenerThread;
    private volatile boolean running = false;
    private final AtomicReference<TrackInfo> currentTrack = new AtomicReference<>(null);
    private final boolean isFlatpak;
    
    public MprisListener() {
        // Detect if running inside a Flatpak sandbox
        this.isFlatpak = System.getenv("FLATPAK_ID") != null || 
                         new java.io.File("/.flatpak-info").exists();
        if (isFlatpak) {
            SmnToastClient.LOGGER.info("Flatpak environment detected, using flatpak-spawn for host access");
        }
    }
    
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        listenerThread = new Thread(this::pollMpris, "MPRIS-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    public void stop() {
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
    
    private void pollMpris() {
        while (running) {
            try {
                TrackInfo track = fetchCurrentTrack();
                if (track != null) {
                    currentTrack.set(track);
                }
                
                // Poll every 500ms
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                SmnToastClient.LOGGER.debug("MPRIS poll error: {}", e.getMessage());
            }
        }
    }
    
    private TrackInfo fetchCurrentTrack() {
        try {
            // Check playback status
            String status = executeCommand("playerctl", "status");
            if (status == null || !status.trim().equalsIgnoreCase("Playing")) {
                return new TrackInfo("", "", "", "", false);
            }
            
            // Get metadata
            String title = executeCommand("playerctl", "metadata", "title");
            String artist = executeCommand("playerctl", "metadata", "artist");
            String album = executeCommand("playerctl", "metadata", "album");
            String trackId = executeCommand("playerctl", "metadata", "mpris:trackid");
            
            if (title == null || title.isEmpty()) {
                return null;
            }
            
            // Use a combination of title + artist as track ID if no trackid available
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
    
    private String executeCommand(String... command) {
        try {
            String[] actualCommand;
            
            // If running in Flatpak, use flatpak-spawn to run command on host
            if (isFlatpak) {
                actualCommand = new String[command.length + 2];
                actualCommand[0] = "flatpak-spawn";
                actualCommand[1] = "--host";
                System.arraycopy(command, 0, actualCommand, 2, command.length);
            } else {
                actualCommand = command;
            }
            
            ProcessBuilder pb = new ProcessBuilder(actualCommand);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // Only log at debug level - exit code 1 is normal when no player or metadata available
                SmnToastClient.LOGGER.debug("Command returned exit code {}: {}", 
                    exitCode, String.join(" ", actualCommand));
                return null;
            }
            
            return output.toString();
        } catch (Exception e) {
            SmnToastClient.LOGGER.debug("Command exception: {}", e.getMessage());
            return null;
        }
    }
    
    public TrackInfo getCurrentTrack() {
        return currentTrack.get();
    }
    
    public static class TrackInfo {
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
}
