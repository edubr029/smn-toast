package com.smntoast.client;

import com.smntoast.SmnToast;
import com.smntoast.client.media.MediaListener;
import com.smntoast.client.media.TrackInfo;
import com.smntoast.client.toast.MusicToast;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public class SmnToastClientRuntime {
    private static final long TOAST_COOLDOWN_MS = 6500L;

    private final KeyMapping showMusicToastKey;
    private MediaListener mediaListener;
    private String lastTrackId = "";
    private boolean startupAlertPending = false;
    private ClientLevel lastLevel = null;
    private long lastToastTime = 0;

    public SmnToastClientRuntime(KeyMapping showMusicToastKey) {
        this.showMusicToastKey = showMusicToastKey;
    }

    public void initialize() {
        SmnToast.LOGGER.info("System Music Notification Toast initializing...");

        try {
            mediaListener = new MediaListener();
            mediaListener.start();
            SmnToast.LOGGER.info("Media listener started successfully");
            String[] alert = mediaListener.getStartupAlert();
            if (alert != null) {
                startupAlertPending = true;
            }
        } catch (Exception e) {
            SmnToast.LOGGER.error("Failed to initialize media listener: {}", e.getMessage());
            SmnToast.LOGGER.error("Make sure you're running on a supported OS");
            return;
        }

        SmnToast.LOGGER.info("System Music Notification Toast initialized successfully!");
    }

    public void onClientTick(Minecraft client) {
        if (startupAlertPending && client.player != null) {
            startupAlertPending = false;
            String[] alert = mediaListener.getStartupAlert();
            if (alert != null) {
                lastToastTime = System.currentTimeMillis();
                MusicToast toast = new MusicToast(alert[0], alert[1], true);
                client.gui.toastManager().addToast(toast);
            }
        }

        if (mediaListener == null || client.player == null) {
            return;
        }

        ClientLevel currentLevel = client.level;
        if (currentLevel != lastLevel) {
            lastTrackId = "";
            lastLevel = currentLevel;
        }

        boolean keyWasPressed = false;
        while (showMusicToastKey.consumeClick()) {
            keyWasPressed = true;
        }

        if (keyWasPressed && System.currentTimeMillis() - lastToastTime >= TOAST_COOLDOWN_MS) {
            showCurrentMusicToast(client);
        }

        TrackInfo currentTrack = mediaListener.getCurrentTrack();

        if (currentTrack != null && currentTrack.isPlaying()) {
            String trackId = currentTrack.getTrackId();

            if (!trackId.equals(lastTrackId) && System.currentTimeMillis() - lastToastTime >= TOAST_COOLDOWN_MS) {
                lastTrackId = trackId;
                lastToastTime = System.currentTimeMillis();

                MusicToast toast = new MusicToast(
                    currentTrack.getTitle(),
                    currentTrack.getArtist(),
                    currentTrack.getAlbum()
                );

                client.gui.toastManager().addToast(toast);
                SmnToast.LOGGER.info("Now playing: {} - {}", currentTrack.getArtist(), currentTrack.getTitle());
            }
        }
    }

    private void showCurrentMusicToast(Minecraft client) {
        lastToastTime = System.currentTimeMillis();
        String[] alert = mediaListener.getStartupAlert();

        if (alert != null) {
            MusicToast toast = new MusicToast(alert[0], alert[1], true);
            client.gui.toastManager().addToast(toast);
            return;
        }

        TrackInfo currentTrack = mediaListener.getCurrentTrack();

        if (currentTrack != null && currentTrack.isPlaying()) {
            lastTrackId = currentTrack.getTrackId();

            MusicToast toast = new MusicToast(
                currentTrack.getTitle(),
                currentTrack.getArtist(),
                currentTrack.getAlbum()
            );

            client.gui.toastManager().addToast(toast);
            SmnToast.LOGGER.info("Manually showing current track: {} - {}", currentTrack.getArtist(), currentTrack.getTitle());
        } else {
            MusicToast toast = new MusicToast(
                "No music playing",
                "Start playing music to see info",
                ""
            );
            client.gui.toastManager().addToast(toast);
        }
    }
}
