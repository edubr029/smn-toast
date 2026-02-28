package com.smntoast.client.media;

import com.smntoast.client.SmnToastClient;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Cross-platform media listener that monitors music playback.
 * - Linux: Uses MPRIS via playerctl (supports native and Flatpak)
 * - Windows: Uses SMTC (System Media Transport Controls) via PowerShell
 * - macOS: Uses AppleScript via osascript
 */
public class MediaListener {
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS_NAME.contains("win");
    private static final boolean IS_LINUX = OS_NAME.contains("nux") || OS_NAME.contains("nix");
    private static final boolean IS_MAC = OS_NAME.contains("mac");

    private final TrackFetcher trackFetcher;
    private Thread listenerThread;
    private volatile boolean running = false;
    private final AtomicReference<TrackInfo> currentTrack = new AtomicReference<>(null);

    public MediaListener() {
        if (IS_WINDOWS) {
            SmnToastClient.LOGGER.info("Windows detected, using SMTC for media info");
            trackFetcher = new WindowsTrackFetcher();
        } else if (IS_LINUX) {
            SmnToastClient.LOGGER.info("Linux detected, using MPRIS for media info");
            trackFetcher = new LinuxTrackFetcher();
        } else if (IS_MAC) {
            SmnToastClient.LOGGER.info("macOS detected, using AppleScript for media info");
            trackFetcher = new MacTrackFetcher();
        } else {
            SmnToastClient.LOGGER.warn("Unsupported OS: {}. Media detection may not work.", OS_NAME);
            throw new IllegalStateException("Unsupported OS: " + OS_NAME);
        }
    }
    
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        listenerThread = new Thread(this::pollMedia, "Media-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    public void stop() {
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
    
    private void pollMedia() {
        while (running) {
            try {
                TrackInfo track = trackFetcher == null ? null : trackFetcher.fetchCurrentTrack();
                if (track != null) {
                    currentTrack.set(track);
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                SmnToastClient.LOGGER.debug("Media poll error: {}", e.getMessage());
            }
        }
    }

    public TrackInfo getCurrentTrack() {
        return currentTrack.get();
    }
}
