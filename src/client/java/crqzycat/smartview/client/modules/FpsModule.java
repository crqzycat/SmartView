package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Minimal first module: just shows the current FPS.
 * Mainly exists to prove the toggle/drag/persist pipeline end to end -
 * later modules (Armor/Effect Status, Cooldowns, Trefferstatistik, ...) follow the same pattern.
 */
public class FpsModule implements HudModule {

    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int PADDING = 3;
    private static final int WIDTH = 72;
    private static final int HEIGHT = 16;

    @Override
    public String getId() {
        return "fps";
    }

    @Override
    public String getDisplayName() {
        return "FPS-Anzeige";
    }

    @Override
    public int getDefaultX() {
        return 10;
    }

    @Override
    public int getDefaultY() {
        return 10;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y) {
        String text = client.getCurrentFps() + " FPS";
        context.fill(x, y, x + WIDTH, y + HEIGHT, BACKGROUND_COLOR);
        context.drawTextWithShadow(client.textRenderer, text, x + PADDING, y + PADDING, TEXT_COLOR);
    }
}
