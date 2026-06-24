package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

public class PingModule implements HudModule {

    private static final int PAD    = 4;
    private static final int HEIGHT = 16;

    /** Cached ping value, updated every tick via onTick(). */
    private int cachedPing = 0;

    @Override public String getId()             { return "ping"; }
    @Override public String getDisplayName()    { return "Ping"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 30; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public boolean enabledByDefault() { return false; }

    @Override
    public int getBaseWidth(MinecraftClient client) {
        return client.textRenderer.getWidth(cachedPing + " ms") + PAD * 2;
    }

    /**
     * Called every client tick by ModuleManager.
     * We update the cached ping here so the render method never does live lookups.
     */
    @Override
    public void onTick(boolean enabled) {
        if (!enabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        cachedPing = fetchPing(client);
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        String text  = cachedPing + " ms";
        int    color = pingColor(cachedPing);
        int    w     = client.textRenderer.getWidth(text) + PAD * 2;
        context.fill(x, y, x + w, y + HEIGHT, pos.backgroundAlpha << 24);
        context.drawTextWithShadow(client.textRenderer, text, x + PAD, y + PAD, color);
    }

    /**
     * Reads ping from the player-list entry.
     * On singleplayer the network handler exists but the player-list entry latency
     * is always 0 – we detect this and show 0 ms honestly.
     * On multiplayer the server sends latency updates via PlayerListS2CPacket,
     * which Minecraft processes and stores in PlayerListEntry#getLatency().
     */
    private static int fetchPing(MinecraftClient client) {
        if (client.player == null) return 0;
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) return 0;
        PlayerListEntry entry = handler.getPlayerListEntry(client.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    private static int pingColor(int ms) {
        if (ms <= 0)   return 0xFFAAAAAA; // grey  – singleplayer / unknown
        if (ms < 80)   return 0xFF55FF55; // green
        if (ms < 150)  return 0xFFFFFF55; // yellow
        if (ms < 300)  return 0xFFFF5555; // red
        return 0xFFAA0000;                // dark red
    }
}
