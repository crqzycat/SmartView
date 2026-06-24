package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class PingModule implements HudModule {

    private static final int PAD    = 4;
    private static final int HEIGHT = 16;

    // Static so PingMixin can write to it from any instance
    private static long keepAliveReceivedAt = 0L;
    private static long keepAliveSentAt     = 0L;
    private static int  measuredPing        = 0;

    /** Called by PingMixin when a KeepAliveS2CPacket arrives (server → client leg). */
    public static void updatePing() {
        long now = System.currentTimeMillis();
        if (keepAliveSentAt > 0) {
            measuredPing = (int) (now - keepAliveSentAt);
        }
        // Next sent time starts now (client will reply immediately)
        keepAliveReceivedAt = now;
        keepAliveSentAt     = now;
    }

    @Override public String getId()             { return "ping"; }
    @Override public String getDisplayName()    { return "Ping"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 30; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public boolean enabledByDefault() { return false; }

    @Override
    public int getBaseWidth(MinecraftClient client) {
        return client.textRenderer.getWidth(measuredPing + " ms") + PAD * 2;
    }

    @Override
    public void onTick(boolean enabled) {
        // Nothing needed – ping is updated by PingMixin on keep-alive
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        String text  = measuredPing + " ms";
        int    color = pingColor(measuredPing);
        int    w     = client.textRenderer.getWidth(text) + PAD * 2;
        context.fill(x, y, x + w, y + HEIGHT, pos.backgroundAlpha << 24);
        context.drawTextWithShadow(client.textRenderer, text, x + PAD, y + PAD, color);
    }

    private static int pingColor(int ms) {
        if (ms <= 0)  return 0xFFAAAAAA; // grey – not yet measured
        if (ms < 80)  return 0xFF55FF55; // green
        if (ms < 150) return 0xFFFFFF55; // yellow
        if (ms < 300) return 0xFFFF5555; // red
        return 0xFFAA0000;               // dark red
    }
}
