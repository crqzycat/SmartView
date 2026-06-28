package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class SprintSneakModule implements HudModule {

    private static final int PAD = 4, HEIGHT = 16;

    @Override public String getId()             { return "sprintsneak"; }
    @Override public String getDisplayName()    { return "Sprint / Sneak"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 130; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public boolean enabledByDefault() { return false; }
    @Override public Category getCategory()     { return Category.PLAYER; }

    @Override
    public int getBaseWidth(MinecraftClient client) {
        return client.textRenderer.getWidth("[SPRINT]") + PAD * 2;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        if (client.player == null) return;
        boolean sneaking  = client.player.isSneaking();
        boolean sprinting = client.player.isSprinting() && !sneaking; // sneak takes priority

        // Only show when doing something
        if (!sprinting && !sneaking) return;

        String text  = sneaking ? "[SNEAK]" : "[SPRINT]";
        int    color = sneaking ? 0xFFFFAA00 : 0xFF55FF55;

        int w = client.textRenderer.getWidth(text) + PAD * 2;
        context.fill(x, y, x + w, y + HEIGHT, pos.backgroundAlpha << 24);
        context.drawTextWithShadow(client.textRenderer, text, x + PAD, y + PAD, color);
    }
}
