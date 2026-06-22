package com.smntoast.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.smntoast.SmnToast;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = "smn_toast", dist = Dist.CLIENT)
public class SmnToastNeoForge {
    private KeyMapping showMusicToastKey;
    private SmnToastClientRuntime runtime;

    public SmnToastNeoForge(IEventBus modBus) {
        modBus.addListener(this::registerKeyMappings);
        modBus.addListener(this::onClientSetup);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        KeyMapping.Category smnToastCategory = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(SmnToast.MOD_ID, "keybindings")
        );

        showMusicToastKey = new KeyMapping(
            "key.smn-toast.show_music",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            smnToastCategory
        );

        event.registerCategory(smnToastCategory);
        event.register(showMusicToastKey);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        runtime = new SmnToastClientRuntime(showMusicToastKey);
        runtime.initialize();
    }

    private void onClientTick(ClientTickEvent.Post event) {
        if (runtime != null) {
            runtime.onClientTick(Minecraft.getInstance());
        }
    }
}
