package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.HungerManager;

public class SaturationModule implements HudModule {

    private static final int PAD = 4, HEIGHT = 16;

    @Override public String getId()             { return "saturation"; }
    @Override public String getDisplayName()    { return "Saturation"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 50; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public boolean enabledByDefault() { return false; }
    @Override public Category getCategory()     { return Category.PLAYER; }

    @Override
    public int getBaseWidth(MinecraftClient client) {
        return client.textRenderer.getWidth(getLabel(client)) + PAD * 2;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        String text  = getLabel(client);
        int    color = getColor(client);
        int    w     = client.textRenderer.getWidth(text) + PAD * 2;
        context.fill(x, y, x + w, y + HEIGHT, pos.backgroundAlpha << 24);
        context.drawTextWithShadow(client.textRenderer, text, x + PAD, y + PAD, color);
    }

    private static String getLabel(MinecraftClient client) {
        if (client.player == null) return "Sat: ---";
        HungerManager hunger = client.player.getHungerManager();
        return String.format("Sat: %.1f", hunger.getSaturationLevel());
    }

    private static int getColor(MinecraftClient client) {
        if (client.player == null) return 0xFFAAAAAA;
        float sat = client.player.getHungerManager().getSaturationLevel();
        if (sat >= 10f) return 0xFF55FF55; // green
        if (sat >= 5f)  return 0xFFFFFF55; // yellow
        if (sat > 0f)   return 0xFFFF5555; // red
        return 0xFFAA0000;                  // dark red – depleted
    }
}
