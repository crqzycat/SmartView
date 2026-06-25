package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class PacketLossModule implements HudModule {

    private static final int PAD    = 4;
    private static final int HEIGHT = 16;

    // Tracked by PacketLossMixin
    private static int sentCount     = 0;
    private static int receivedCount = 0;
    private static float cachedLoss  = 0f;

    /** Called by PacketLossMixin when a keep-alive is sent (client → server). */
    public static void onSent() {
        sentCount++;
        recalculate();
    }

    /** Called by PacketLossMixin when a keep-alive reply arrives (server → client). */
    public static void onReceived() {
        receivedCount++;
        recalculate();
    }

    private static void recalculate() {
        if (sentCount == 0) { cachedLoss = 0f; return; }
        // Clamp: received can't exceed sent
        int lost = Math.max(0, sentCount - receivedCount);
        cachedLoss = (lost / (float) sentCount) * 100f;
    }

    @Override public String getId()             { return "packetloss"; }
    @Override public String getDisplayName()    { return "Packet Loss"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 70; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public HudModule.Category getCategory() { return HudModule.Category.NETWORK; }
    @Override public boolean enabledByDefault() { return false; }

    @Override
    public int getBaseWidth(MinecraftClient client) {
        return client.textRenderer.getWidth(getLabel()) + PAD * 2;
    }

    @Override
    public void onTick(boolean enabled) {
        // Reset counters when disabled so we start fresh when re-enabled
        if (!enabled) {
            sentCount = 0;
            receivedCount = 0;
            cachedLoss = 0f;
        }
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        String text  = getLabel();
        int    color = lossColor(cachedLoss);
        int    w     = client.textRenderer.getWidth(text) + PAD * 2;
        context.fill(x, y, x + w, y + HEIGHT, pos.backgroundAlpha << 24);
        context.drawTextWithShadow(client.textRenderer, text, x + PAD, y + PAD, color);
    }

    private static String getLabel() {
        return String.format("Loss: %.1f%%", cachedLoss);
    }

    private static int lossColor(float loss) {
        if (loss <= 0f)  return 0xFF55FF55; // green  – no loss
        if (loss < 5f)   return 0xFFFFFF55; // yellow – minor
        if (loss < 15f)  return 0xFFFF5555; // red    – significant
        return 0xFFAA0000;                   // dark red – severe
    }
}
