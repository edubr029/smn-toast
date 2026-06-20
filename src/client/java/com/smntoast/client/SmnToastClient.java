package com.smntoast.client;

import com.smntoast.client.media.MediaListener;
import com.smntoast.client.media.TrackInfo;
import com.smntoast.client.toast.MusicToast;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmnToastClient implements ClientModInitializer {
    public static final String MOD_ID = "smn-toast";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private MediaListener mediaListener;
    private String lastTrackId = "";
    private boolean startupAlertPending = false;
    private ClientLevel lastLevel = null;
    private long lastToastTime = 0;
    private static final long TOAST_COOLDOWN_MS = 6500L;
    
    // Keybinding for showing current music toast (default: unbound)
    private static KeyMapping showMusicToastKey;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("System Music Notification Toast initializing...");
        
        // Create custom category for SMN Toast keybindings
        KeyMapping.Category smnToastCategory = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "keybindings")
        );
        
        // Register keybinding for showing music toast manually (unbound by default)
        showMusicToastKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.smn-toast.show_music",       // Translation key for the keybinding name
            InputConstants.Type.KEYSYM,       // Key type
            GLFW.GLFW_KEY_UNKNOWN,            // Default: unbound (user must configure)
            smnToastCategory                  // Custom category
        ));
        
        // Initialize media listener
        try {
            mediaListener = new MediaListener();
            mediaListener.start();
            LOGGER.info("Media listener started successfully");
            String[] alert = mediaListener.getStartupAlert();
            if (alert != null) {
                startupAlertPending = true;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize media listener: {}", e.getMessage());
            LOGGER.error("Make sure you're running on a supported OS");
            return;
        }
        
        // Register tick event to check for music changes and key presses
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        LOGGER.info("System Music Notification Toast initialized successfully!");
    }
    
    private void onClientTick(Minecraft client) {
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
                LOGGER.info("Now playing: {} - {}", currentTrack.getArtist(), currentTrack.getTitle());
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
            LOGGER.info("Manually showing current track: {} - {}", currentTrack.getArtist(), currentTrack.getTitle());
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
