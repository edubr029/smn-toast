package com.smntoast.client.toast;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MusicToast implements Toast {
    private static final Identifier TEXTURE = Identifier.ofVanilla("toast/advancement");
    private static final long DISPLAY_TIME = 5000L; // 5 seconds in milliseconds
    private static final long FADE_OUT_TIME = 600L; // Fade-out animation buffer
    
    // Static flag to track if a MusicToast is currently being displayed
    private static boolean isShowing = false;
    
    private final Text title;
    private final Text artist;
    private long startTime;
    private boolean justUpdated;
    private Visibility visibility;
    
    public MusicToast(String songTitle, String artistName, String albumName) {
        this.title = Text.literal(truncateText(songTitle, 25));
        this.artist = Text.literal(truncateText(artistName, 30));
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
    public Visibility getVisibility() {
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
    public void draw(DrawContext context, TextRenderer textRenderer, long time) {
        // Draw toast background
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, this.getWidth(), this.getHeight());
        
        // Draw music icon (note symbol) - ARGB format with full alpha (0xFF prefix)
        String musicIcon = "\u266B"; // Music note symbol
        context.drawText(textRenderer, musicIcon, 8, 12, 0xFF55FF55, true);
        
        // Draw "Now Playing" header - yellow with shadow
        context.drawText(textRenderer, Text.literal("Now Playing"), 26, 7, 0xFFFFFF00, true);
        
        // Draw song title - white with shadow
        context.drawText(textRenderer, this.title, 26, 18, 0xFFFFFFFF, true);
        
        // Draw artist name - gray with shadow
        if (this.artist != null) {
            context.drawText(textRenderer, this.artist, 26, 28, 0xFFAAAAAA, true);
        }
    }
    
    @Override
    public int getWidth() {
        return 160;
    }
    
    @Override
    public int getHeight() {
        return 42;
    }
}
