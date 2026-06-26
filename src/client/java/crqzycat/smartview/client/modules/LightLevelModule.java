package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;

public class LightLevelModule implements HudModule {

    private static final int PAD = 4, HEIGHT = 16;

    @Override public String getId()             { return "lightlevel"; }
    @Override public String getDisplayName()    { return "Light Level"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 70; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public boolean enabledByDefault() { return false; }
    @Override public Category getCategory()     { return Category.WORLD; }

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
        if (client.player == null || client.world == null) return "Light: ---";
        BlockPos pos = client.player.getBlockPos();
        int block = client.world.getLightLevel(LightType.BLOCK, pos);
        int sky   = client.world.getLightLevel(LightType.SKY,   pos);
        return "Light: " + block + " ☀" + sky;
    }
}
