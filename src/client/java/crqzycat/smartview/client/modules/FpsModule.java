package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class FpsModule implements HudModule {

    private static final int PADDING = 4;
    private static final int HEIGHT = 16;

    @Override public String getId() { return "fps"; }
    @Override public String getDisplayName() { return "FPS-Anzeige"; }
    @Override public int getDefaultX() { return 10; }
    @Override public int getDefaultY() { return 10; }

    @Override
    public int getBaseWidth(MinecraftClient client) {
        // dynamic: just wide enough to fit the current FPS text
        String sample = client.getCurrentFps() + " FPS";
        return client.textRenderer.getWidth(sample) + PADDING * 2;
    }

    @Override public int getBaseHeight() { return HEIGHT; }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        String text = client.getCurrentFps() + " FPS";
        int w = client.textRenderer.getWidth(text) + PADDING * 2;
        int bgColor = (pos.backgroundAlpha << 24); // pure black with configurable alpha
        context.fill(x, y, x + w, y + HEIGHT, bgColor);
        context.drawTextWithShadow(client.textRenderer, text, x + PADDING, y + PADDING, 0xFFFFFFFF);
    }
}
