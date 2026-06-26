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
    @Override public String getDisplayName()    { return "Server IP"; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public HudModule.Category getCategory() { return HudModule.Category.NETWORK; }
    @Override public boolean enabledByDefault() { return false; }

    @Override
    public int getDefaultX() { return 4; }

    /**
     * Bottom-left default. Returns a large value that looks correct on most
     * resolutions; the user can reposition via the HUD editor anyway.
     * We cannot call getWindow() here because init() runs before the window exists.
     */
    @Override
    public int getDefaultY() {
        return 200; // safe default, user can reposition via HUD editor // ~bottom of a 400px scaled screen
    }

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
        ServerInfo info = client.getCurrentServerEntry();
        if (info != null) return info.address;
        if (client.isInSingleplayer()) return "Singleplayer";
        return "Unknown";
    }
}
