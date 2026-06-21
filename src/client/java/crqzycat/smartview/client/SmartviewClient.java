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
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class SmartviewClient implements ClientModInitializer {

	private static final KeyBinding.Category CATEGORY =
			KeyBinding.Category.create(Identifier.of("smartview", "main"));

	public static final KeyBinding OPEN_EDIT_SCREEN_KEY = new KeyBinding(
			"key.smartview.open_edit_screen",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_SHIFT,
			CATEGORY
	);

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		KeyBindingHelper.registerKeyBinding(OPEN_EDIT_SCREEN_KEY);

		// Open the edit menu on key press. Only when no other screen is open,
		// so we don't steal the keybind out of inventories/chat/etc.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_EDIT_SCREEN_KEY.wasPressed()) {
				if (client.currentScreen == null) {
					client.setScreen(new HudEditScreen());
				}
			}
		});

		// Normal in-game rendering. The edit screen draws the modules itself
		// (with drag outlines), so skip the plain HUD pass while it's open.
		HudRenderCallback.EVENT.register((context, tickCounter) -> {
			if (!(MinecraftClient.getInstance().currentScreen instanceof HudEditScreen)) {
				ModuleManager.render(context);
			}
		});
	}
}
