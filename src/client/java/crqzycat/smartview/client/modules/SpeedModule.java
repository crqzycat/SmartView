package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

public class SpeedModule implements HudModule {

    private static final int PAD = 4, HEIGHT = 16;

    @Override public String getId()             { return "speed"; }
    @Override public String getDisplayName()    { return "Speed"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 70; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public boolean enabledByDefault() { return false; }
    @Override public Category getCategory()     { return Category.PLAYER; }

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
        if (client.player == null) return "Speed: ---";
        Vec3d vel = client.player.getVelocity();
        // Horizontal speed only (ignore vertical / falling)
        double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        // Convert blocks/tick to blocks/second (* 20)
        double bps = hSpeed * 20.0;
        return String.format("Speed: %.2f b/s", bps);
    }
}
