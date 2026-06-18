package com.smntoast.client;

import com.smntoast.client.media.MediaListener;
import com.smntoast.client.media.TrackInfo;
import com.smntoast.client.toast.MusicToast;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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
        if (mediaListener == null || client.player == null) {
            return;
        }
        
        // Check if the show music toast key was pressed (consume all queued presses)
        boolean keyWasPressed = false;
        while (showMusicToastKey.consumeClick()) {
            keyWasPressed = true;
        }
        
        // Only show toast once per key press batch, and only if no toast is showing
        if (keyWasPressed && !MusicToast.isCurrentlyShowing()) {
            showCurrentMusicToast(client);
        }
        
        TrackInfo currentTrack = mediaListener.getCurrentTrack();
        
        if (currentTrack != null && currentTrack.isPlaying()) {
            String trackId = currentTrack.getTrackId();
            
            // Only show toast when a new track starts playing (and no toast is already showing)
            if (!trackId.equals(lastTrackId) && !MusicToast.isCurrentlyShowing()) {
                lastTrackId = trackId;
                
                MusicToast toast = new MusicToast(
                    currentTrack.getTitle(),
                    currentTrack.getArtist(),
                    currentTrack.getAlbum()
                );
                
                client.getToastManager().addToast(toast);
                LOGGER.info("Now playing: {} - {}", currentTrack.getArtist(), currentTrack.getTitle());
            }
        }
    }
    
    /**
     * Shows the current music toast on demand (triggered by keybinding).
     * Caller must check MusicToast.isCurrentlyShowing() before calling.
     */
    private void showCurrentMusicToast(Minecraft client) {
        TrackInfo currentTrack = mediaListener.getCurrentTrack();
        
        if (currentTrack != null && currentTrack.isPlaying()) {
            // Update lastTrackId to prevent automatic detection from showing the same track again
            lastTrackId = currentTrack.getTrackId();
            
            MusicToast toast = new MusicToast(
                currentTrack.getTitle(),
                currentTrack.getArtist(),
                currentTrack.getAlbum()
            );
            
            client.getToastManager().addToast(toast);
            LOGGER.debug("Manually showing current track: {} - {}", currentTrack.getArtist(), currentTrack.getTitle());
        } else {
            // Show a toast indicating no music is playing
            MusicToast toast = new MusicToast(
                "No music playing",
                "Start playing music to see info",
                ""
            );
            client.getToastManager().addToast(toast);
        }
    }
}
