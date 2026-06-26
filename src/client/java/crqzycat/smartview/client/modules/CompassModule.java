package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class CompassModule implements HudModule {

    private static final int PAD = 4, HEIGHT = 16;

    private static final String[] DIRECTIONS = {
        "S", "SW", "W", "NW", "N", "NE", "E", "SE"
    };

    @Override public String getId()             { return "compass"; }
    @Override public String getDisplayName()    { return "Compass"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 70; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public boolean enabledByDefault() { return false; }
    @Override public Category getCategory()     { return Category.VISUAL; }

    @Override
    public int getBaseWidth(MinecraftClient client) {
        return client.textRenderer.getWidth(getLabel(client)) + PAD * 2;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        String text = getLabel(client);
        int w = client.textRenderer.getWidth(text) + PAD * 2;
        context.fill(x, y, x + w, y + HEIGHT, pos.backgroundAlpha << 24);
        context.drawTextWithShadow(client.textRenderer, text, x + PAD, y + PAD, pos.textColor);
    }

    private static String getLabel(MinecraftClient client) {
        if (client.player == null) return "Dir: ---";
        // yaw: 0 = south, 90 = west, -90 = east, 180/-180 = north
        float yaw = client.player.getYaw() % 360;
        if (yaw < 0) yaw += 360;
        int index = (int)((yaw + 22.5f) / 45f) % 8;
        return "Dir: " + DIRECTIONS[index] + " (" + (int)yaw + "°)";
    }
}
