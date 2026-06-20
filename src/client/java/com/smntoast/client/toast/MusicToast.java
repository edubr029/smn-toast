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
    private static final long DISPLAY_TIME = 5000L;
    
    private final Component title;
    private final Component artist;
    private final boolean alertMode;
    private long startTime;
    private boolean justUpdated;
    private Visibility visibility;
    
    public MusicToast(String songTitle, String artistName, String albumName) {
        this.title = Component.literal(truncateText(songTitle, 25));
        this.artist = Component.literal(truncateText(artistName, 30));
        this.alertMode = false;
        this.justUpdated = true;
        this.visibility = Visibility.SHOW;
    }
    
    public MusicToast(String title, String subtitle, boolean alertMode) {
        this.title = Component.literal(truncateText(title, 25));
        this.artist = Component.literal(truncateText(subtitle, 30));
        this.alertMode = alertMode;
        this.justUpdated = true;
        this.visibility = Visibility.SHOW;
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
        
        long elapsedTime = time - this.startTime;
        if (elapsedTime >= DISPLAY_TIME) {
            this.visibility = Visibility.HIDE;
        }
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, width(), height());
        
        if (alertMode) {
            String warningIcon = "\u26A0";
            graphics.text(font, Component.literal(warningIcon), 8, 12, 0xFFFFAA00, true);
            graphics.text(font, Component.literal("Warning"), 26, 7, 0xFFFFAA00, true);
        } else {
            String musicIcon = "\u266B";
            graphics.text(font, Component.literal(musicIcon), 8, 12, 0xFF55FF55, true);
            graphics.text(font, Component.literal("Now Playing"), 26, 7, 0xFFFFFF00, true);
        }
        
        graphics.text(font, this.title, 26, 18, 0xFFFFFFFF, true);
        
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
