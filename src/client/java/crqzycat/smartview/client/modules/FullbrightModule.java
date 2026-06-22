package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Fullbright: no HUD element, but a side-effect module.
 * When enabled: gamma = 10.0. When disabled: restore saved gamma.
 */
public class FullbrightModule implements HudModule {

    private static final double FULLBRIGHT_GAMMA = 10.0;
    private double savedGamma = 1.0;
    private boolean wasEnabled = false;

    @Override public String getId() { return "fullbright"; }
    @Override public String getDisplayName() { return "Fullbright"; }
    @Override public int getDefaultX() { return 0; }
    @Override public int getDefaultY() { return 0; }
    @Override public int getBaseWidth(MinecraftClient c) { return 0; }
    @Override public int getBaseHeight() { return 0; }
    @Override public boolean enabledByDefault() { return false; }

    @Override
    public void onTick(boolean enabled) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return;

        if (enabled && !wasEnabled) {
            savedGamma = client.options.getGamma().getValue();
            client.options.getGamma().setValue(FULLBRIGHT_GAMMA);
        } else if (!enabled && wasEnabled) {
            client.options.getGamma().setValue(savedGamma);
        }
        wasEnabled = enabled;
    }

    /** Fullbright renders nothing visible on the HUD. */
    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {}
}
