package com.smntoast.client;

import com.smntoast.client.mpris.MprisListener;
import com.smntoast.client.mpris.TrackInfo;
import com.smntoast.client.toast.MusicToast;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmnToastClient implements ClientModInitializer {
    public static final String MOD_ID = "smn-toast";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private MprisListener mprisListener;
    private String lastTrackId = "";
    
    // Keybinding for showing current music toast (default: unbound)
    private static KeyBinding showMusicToastKey;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("System Music Notification Toast initializing...");
        
        // Create custom category for SMN Toast keybindings
        KeyBinding.Category smnToastCategory = KeyBinding.Category.create(
            Identifier.of(MOD_ID, "keybindings")
        );
        
        // Register keybinding for showing music toast manually (unbound by default)
        showMusicToastKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.smn-toast.show_music",       // Translation key for the keybinding name
            InputUtil.Type.KEYSYM,            // Key type
            GLFW.GLFW_KEY_UNKNOWN,            // Default: unbound (user must configure)
            smnToastCategory                  // Custom category
        ));
        
        // Initialize MPRIS listener for Linux D-Bus
        try {
            mprisListener = new MprisListener();
            mprisListener.start();
            LOGGER.info("MPRIS listener started successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize MPRIS listener: {}", e.getMessage());
            LOGGER.error("Make sure you're running on Linux with D-Bus support");
            return;
        }
        
        // Register tick event to check for music changes and key presses
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        LOGGER.info("System Music Notification Toast initialized successfully!");
    }
    
    private void onClientTick(MinecraftClient client) {
        if (mprisListener == null || client.player == null) {
            return;
        }
        
        // Check if the show music toast key was pressed (consume all queued presses)
        boolean keyWasPressed = false;
        while (showMusicToastKey.wasPressed()) {
            keyWasPressed = true;
        }
        
        // Only show toast once per key press batch, and only if no toast is showing
        if (keyWasPressed && !MusicToast.isCurrentlyShowing()) {
            showCurrentMusicToast(client);
        }
        
        TrackInfo currentTrack = mprisListener.getCurrentTrack();
        
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
                
                client.getToastManager().add(toast);
                LOGGER.info("Now playing: {} - {}", currentTrack.getArtist(), currentTrack.getTitle());
            }
        }
    }
    
    /**
     * Shows the current music toast on demand (triggered by keybinding).
     * Caller must check MusicToast.isCurrentlyShowing() before calling.
     */
    private void showCurrentMusicToast(MinecraftClient client) {
        TrackInfo currentTrack = mprisListener.getCurrentTrack();
        
        if (currentTrack != null && currentTrack.isPlaying()) {
            // Update lastTrackId to prevent automatic detection from showing the same track again
            lastTrackId = currentTrack.getTrackId();
            
            MusicToast toast = new MusicToast(
                currentTrack.getTitle(),
                currentTrack.getArtist(),
                currentTrack.getAlbum()
            );
            
            client.getToastManager().add(toast);
            LOGGER.debug("Manually showing current track: {} - {}", currentTrack.getArtist(), currentTrack.getTitle());
        } else {
            // Show a toast indicating no music is playing
            MusicToast toast = new MusicToast(
                "No music playing",
                "Start playing music to see info",
                ""
            );
            client.getToastManager().add(toast);
        }
    }
}
