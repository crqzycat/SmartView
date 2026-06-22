package crqzycat.smartview.client;

import crqzycat.smartview.client.gui.HudEditScreen;
import crqzycat.smartview.client.hud.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SmartviewClient implements ClientModInitializer {

    public static final KeyBinding OPEN_EDIT_SCREEN_KEY = new KeyBinding(
            "key.smartview.open_edit_screen",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            ModuleManager.CATEGORY
    );

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        KeyBindingHelper.registerKeyBinding(OPEN_EDIT_SCREEN_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Open/close HUD editor
            while (OPEN_EDIT_SCREEN_KEY.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new HudEditScreen());
                }
            }
            // Module keybinds + side-effects (Fullbright etc.)
            ModuleManager.tick();
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            if (!(MinecraftClient.getInstance().currentScreen instanceof HudEditScreen)) {
                ModuleManager.render(context);
            }
        });
    }
}
