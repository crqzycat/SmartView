package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;

import java.util.Random;

public class SlimeChunkModule implements HudModule {

    private static final int PAD = 4, HEIGHT = 16;

    /**
     * The world seed is not available on the client for multiplayer servers.
     * We use 0 as a fallback seed – this gives correct results on singleplayer
     * only when the actual world seed happens to be 0, but it's the best we
     * can do client-side without a server-side mod.
     * On singleplayer the integrated server exposes the seed via
     * MinecraftClient.getServer(), so we read it from there when available.
     */
    private static long getSeed(MinecraftClient client) {
        if (client.getServer() != null) {
            // Singleplayer / LAN host – real seed available
            return client.getServer().getOverworld().getSeed();
        }
        // Multiplayer – seed unknown, return 0 and show disclaimer
        return Long.MIN_VALUE; // sentinel: unknown
    }

    @Override public String getId()             { return "slimechunk"; }
    @Override public String getDisplayName()    { return "Slime Chunk"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 90; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public boolean enabledByDefault() { return false; }
    @Override public Category getCategory()     { return Category.WORLD; }

    @Override
    public int getBaseWidth(MinecraftClient client) {
        return client.textRenderer.getWidth("Slime Chunk: YES") + PAD * 2;
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        if (client.player == null || client.world == null) return;

        long seed = getSeed(client);
        int chunkX = client.player.getChunkPos().x;
        int chunkZ = client.player.getChunkPos().z;

        String text;
        int color;

        if (seed == Long.MIN_VALUE) {
            text  = "Slime Chunk: ?";
            color = 0xFFAAAAAA; // grey – seed unknown on multiplayer
        } else {
            boolean isSlime = isSlimeChunk(seed, chunkX, chunkZ);
            text  = "Slime Chunk: " + (isSlime ? "YES" : "NO");
            color = isSlime ? 0xFF55FF55 : pos.textColor;
        }

        int w = client.textRenderer.getWidth(text) + PAD * 2;
        context.fill(x, y, x + w, y + HEIGHT, pos.backgroundAlpha << 24);
        context.drawTextWithShadow(client.textRenderer, text, x + PAD, y + PAD, color);
    }

    private static boolean isSlimeChunk(long worldSeed, int chunkX, int chunkZ) {
        Random rng = new Random(
            worldSeed
            + (long)(chunkX * chunkX * 0x4C1906)
            + (long)(chunkX * 0x5AC0DB)
            + (long)(chunkZ * chunkZ) * 0x4307A7L
            + (long)(chunkZ * 0x5F24F)
            ^ 0x3AD8025FL
        );
        return rng.nextInt(10) == 0;
    }
}
