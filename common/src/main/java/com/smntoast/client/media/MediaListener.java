package com.smntoast.client.media;

import com.smntoast.SmnToast;

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
    private final AtomicReference<String[]> startupAlert = new AtomicReference<>(null);

    private static final long RECHECK_INTERVAL_MS = 60000L;
    private long lastRecheckTime = System.currentTimeMillis();

    public MediaListener() {
        if (IS_WINDOWS) {
            SmnToast.LOGGER.info("Windows detected, using SMTC for media info");
            trackFetcher = new WindowsTrackFetcher();
        } else if (IS_LINUX) {
            SmnToast.LOGGER.info("Linux detected, using MPRIS for media info");
            trackFetcher = new LinuxTrackFetcher();
        } else if (IS_MAC) {
            SmnToast.LOGGER.info("macOS detected, using AppleScript for media info");
            trackFetcher = new MacTrackFetcher();
        } else {
            SmnToast.LOGGER.warn("Unsupported OS: {}. Media detection may not work.", OS_NAME);
            throw new IllegalStateException("Unsupported OS: " + OS_NAME);
        }
        startupAlert.set(trackFetcher.getStartupAlert());
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
                long now = System.currentTimeMillis();
                if (now - lastRecheckTime >= RECHECK_INTERVAL_MS) {
                    lastRecheckTime = now;
                    trackFetcher.recheckAvailability();
                    startupAlert.set(trackFetcher.getStartupAlert());
                }
                TrackInfo track = trackFetcher.fetchCurrentTrack();
                if (track != null) {
                    currentTrack.set(track);
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                SmnToast.LOGGER.debug("Media poll error: {}", e.getMessage());
            }
        }
    }

    public TrackInfo getCurrentTrack() {
        return currentTrack.get();
    }

    public String[] getStartupAlert() {
        return startupAlert.get();
    }
}
