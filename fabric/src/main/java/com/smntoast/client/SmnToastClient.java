package com.smntoast.client;

import com.smntoast.SmnToast;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class SmnToastClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMapping.Category smnToastCategory = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(SmnToast.MOD_ID, "keybindings")
        );

        KeyMapping showMusicToastKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.smn-toast.show_music",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            smnToastCategory
        ));

        SmnToastClientRuntime runtime = new SmnToastClientRuntime(showMusicToastKey);
        runtime.initialize();

        ClientTickEvents.END_CLIENT_TICK.register(runtime::onClientTick);
    }
}
