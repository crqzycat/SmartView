package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public class DimensionModule implements HudModule {

    private static final int PAD = 4, HEIGHT = 16;

    @Override public String getId()             { return "dimension"; }
    @Override public String getDisplayName()    { return "Dimension"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 110; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public boolean enabledByDefault() { return false; }
    @Override public Category getCategory()     { return Category.WORLD; }

    @Override
    public int getBaseWidth(MinecraftClient client) {
        return client.textRenderer.getWidth("Dim: The Nether") + PAD * 2;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        if (client.world == null) return;
        RegistryKey<World> dim = client.world.getRegistryKey();
        String name;
        int color;
        if (dim == World.OVERWORLD)      { name = "Overworld"; color = 0xFF55FF55; }
        else if (dim == World.NETHER)    { name = "The Nether"; color = 0xFFFF5555; }
        else if (dim == World.END)       { name = "The End";    color = 0xFFDD88FF; }
        else                             { name = dim.getValue().getPath(); color = pos.textColor; }

        String text = "Dim: " + name;
        int w = client.textRenderer.getWidth(text) + PAD * 2;
        context.fill(x, y, x + w, y + HEIGHT, pos.backgroundAlpha << 24);
        context.drawTextWithShadow(client.textRenderer, text, x + PAD, y + PAD, color);
    }
}
