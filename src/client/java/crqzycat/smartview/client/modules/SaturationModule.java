package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.world.GameMode;

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
        if (client.player == null) return;
        // Only show in Survival or Adventure
        var interaction = client.interactionManager;
        if (interaction == null) return;
        GameMode gm = interaction.getCurrentGameMode();
        if (gm != GameMode.SURVIVAL && gm != GameMode.ADVENTURE) return;
        float sat = client.player.getHungerManager().getSaturationLevel();
        if (sat <= 0f) return; // hide when saturation is depleted
        String text  = String.format("Sat: %.1f", sat);
        int    color = sat >= 10f ? 0xFF55FF55 : sat >= 5f ? 0xFFFFFF55 : 0xFFFF5555;
        int    w     = client.textRenderer.getWidth(text) + PAD * 2;
        context.fill(x, y, x + w, y + HEIGHT, pos.backgroundAlpha << 24);
        context.drawTextWithShadow(client.textRenderer, text, x + PAD, y + PAD, color);
    }
}
