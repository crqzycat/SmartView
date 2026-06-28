package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class O2Module implements HudModule {

    private static final int PAD = 4, HEIGHT = 16;

    @Override public String getId()             { return "o2"; }
    @Override public String getDisplayName()    { return "O2"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 90; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public boolean enabledByDefault() { return false; }
    @Override public Category getCategory()     { return Category.PLAYER; }

    @Override
    public int getBaseWidth(MinecraftClient client) {
        return client.textRenderer.getWidth("O2: 300 ticks") + PAD * 2;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        if (client.player == null) return;

        int air = client.player.getAir();
        int maxAir = client.player.getMaxAir(); // 300 ticks = 15s

        // Only show when not at full air (i.e. underwater or drowning)
        if (air >= maxAir) return;

        float ratio = (float) Math.max(0, air) / maxAir;
        int seconds = Math.max(0, air) / 20;
        String text = String.format("O2: %ds (%.0f%%)", seconds, ratio * 100f);

        int color = ratio > 0.5f ? 0xFF55FFFF   // cyan
                  : ratio > 0.2f ? 0xFFFFFF55   // yellow
                  :                0xFFFF5555;  // red

        int w = client.textRenderer.getWidth(text) + PAD * 2;
        context.fill(x, y, x + w, y + HEIGHT, pos.backgroundAlpha << 24);
        context.drawTextWithShadow(client.textRenderer, text, x + PAD, y + PAD, color);
    }
}
