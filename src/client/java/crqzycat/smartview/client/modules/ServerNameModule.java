package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;

public class ServerNameModule implements HudModule {

    private static final int PAD    = 4;
    private static final int HEIGHT = 16;

    @Override public String getId()             { return "servername"; }
    @Override public String getDisplayName()    { return "Server Name"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 50; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public boolean enabledByDefault() { return false; }

    @Override
    public int getBaseWidth(MinecraftClient client) {
        return client.textRenderer.getWidth(getLabel(client)) + PAD * 2;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        String text = getLabel(client);
        int w = client.textRenderer.getWidth(text) + PAD * 2;
        context.fill(x, y, x + w, y + HEIGHT, pos.backgroundAlpha << 24);
        context.drawTextWithShadow(client.textRenderer, text, x + PAD, y + PAD, 0xFFFFFFFF);
    }

    private static String getLabel(MinecraftClient client) {
        // Multiplayer: use server name or address
        ServerInfo info = client.getCurrentServerEntry();
        if (info != null) {
            String name = info.name;
            return (name != null && !name.isBlank()) ? name : info.address;
        }
        // Singleplayer
        if (client.isInSingleplayer()) {
            return "Singleplayer";
        }
        return "Unknown";
    }
}
