package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

public class PingModule implements HudModule {

    private static final int PAD = 4;
    private static final int HEIGHT = 16;

    @Override public String getId() { return "ping"; }
    @Override public String getDisplayName() { return "Ping"; }
    @Override public int getDefaultX() { return 10; }
    @Override public int getDefaultY() { return 30; }
    @Override public int getBaseHeight() { return HEIGHT; }

    @Override
    public int getBaseWidth(MinecraftClient client) {
        return client.textRenderer.getWidth(sample(client)) + PAD * 2;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        int ping = getPing(client);
        String text = ping + " ms";
        int color = pingColor(ping);
        int w = client.textRenderer.getWidth(text) + PAD * 2;
        context.fill(x, y, x + w, y + HEIGHT, pos.backgroundAlpha << 24);
        context.drawTextWithShadow(client.textRenderer, text, x + PAD, y + PAD, color);
    }

    private static int getPing(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) return 0;
        PlayerListEntry entry = client.getNetworkHandler()
                .getPlayerListEntry(client.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    private static int pingColor(int ms) {
        if (ms < 80)  return 0xFF55FF55; // green
        if (ms < 150) return 0xFFFFFF55; // yellow
        if (ms < 300) return 0xFFFF5555; // red
        return 0xFFAA0000;               // dark red
    }

    private static String sample(MinecraftClient client) {
        return getPing(client) + " ms";
    }
    @Override public boolean enabledByDefault() { return false; }
}
