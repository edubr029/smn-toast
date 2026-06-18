package com.smntoast.client.toast;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class MusicToast implements Toast {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("toast/advancement");
    private static final long DISPLAY_TIME = 5000L; // 5 seconds in milliseconds
    private static final long FADE_OUT_TIME = 600L; // Fade-out animation buffer
    
    // Static flag to track if a MusicToast is currently being displayed
    private static boolean isShowing = false;
    
    private final Component title;
    private final Component artist;
    private long startTime;
    private boolean justUpdated;
    private Visibility visibility;
    
    public MusicToast(String songTitle, String artistName, String albumName) {
        this.title = Component.literal(truncateText(songTitle, 25));
        this.artist = Component.literal(truncateText(artistName, 30));
        this.justUpdated = true;
        this.visibility = Visibility.SHOW;
        isShowing = true;
    }
    
    /**
     * Check if a MusicToast is currently being displayed
     */
    public static boolean isCurrentlyShowing() {
        return isShowing;
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "Unknown";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
    
    @Override
    public Visibility getWantedVisibility() {
        return this.visibility;
    }
    
    @Override
    public void update(ToastManager manager, long time) {
        if (this.justUpdated) {
            this.startTime = time;
            this.justUpdated = false;
        }
        
        // Check if display time has elapsed
        long elapsedTime = time - this.startTime;
        if (elapsedTime >= DISPLAY_TIME) {
            this.visibility = Visibility.HIDE;
        }
        
        // Only clear the flag after fade-out animation completes
        if (elapsedTime >= DISPLAY_TIME + FADE_OUT_TIME) {
            isShowing = false;
        }
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
        // Draw toast background
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, width(), height());
        
        // Draw music icon (note symbol) - ARGB format with full alpha (0xFF prefix)
        String musicIcon = "\u266B"; // Music note symbol
        graphics.text(font, Component.literal(musicIcon), 8, 12, 0xFF55FF55, true);
        
        // Draw "Now Playing" header - yellow with shadow
        graphics.text(font, Component.literal("Now Playing"), 26, 7, 0xFFFFFF00, true);
        
        // Draw song title - white with shadow
        graphics.text(font, this.title, 26, 18, 0xFFFFFFFF, true);
        
        // Draw artist name - gray with shadow
        if (this.artist != null) {
            graphics.text(font, this.artist, 26, 28, 0xFFAAAAAA, true);
        }
    }
    
    @Override
    public int width() {
        return 160;
    }
    
    @Override
    public int height() {
        return 42;
    }
}
