package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

public class PingModule implements HudModule {

    private static final int PAD    = 4;
    private static final int HEIGHT = 16;

    private static volatile int measuredPing = -1; // -1 = not yet measured

    /** Called by PingMixin with the measured RTT in ms. */
    public static void updatePing(int rttMs) {
        measuredPing = rttMs;
    }

    @Override public String getId()             { return "ping"; }
    @Override public String getDisplayName()    { return "Ping"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 30; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public boolean enabledByDefault() { return false; }

    @Override
    public int getBaseWidth(MinecraftClient client) {
        String s = measuredPing < 0 ? "--- ms" : measuredPing + " ms";
        return client.textRenderer.getWidth(s) + PAD * 2;
    }

    @Override
    public void onTick(boolean enabled) {
        // If we haven't received a keep-alive yet, fall back to PlayerListEntry
        if (!enabled || measuredPing >= 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.getNetworkHandler() == null) return;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        if (entry != null && entry.getLatency() > 0) {
            measuredPing = entry.getLatency();
        }
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        String text  = measuredPing < 0 ? "--- ms" : measuredPing + " ms";
        int    color = measuredPing < 0 ? 0xFFAAAAAA : pingColor(measuredPing);
        int    w     = client.textRenderer.getWidth(text) + PAD * 2;
        context.fill(x, y, x + w, y + HEIGHT, pos.backgroundAlpha << 24);
        context.drawTextWithShadow(client.textRenderer, text, x + PAD, y + PAD, color);
    }

    private static int pingColor(int ms) {
        if (ms < 80)  return 0xFF55FF55; // green
        if (ms < 150) return 0xFFFFFF55; // yellow
        if (ms < 300) return 0xFFFF5555; // red
        return 0xFFAA0000;               // dark red
    }
}
