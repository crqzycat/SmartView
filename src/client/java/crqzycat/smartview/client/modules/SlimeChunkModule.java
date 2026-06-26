package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.Random;

public class SlimeChunkModule implements HudModule {

    private static final int PAD = 4, HEIGHT = 16;

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

        int chunkX = client.player.getChunkPos().x;
        int chunkZ = client.player.getChunkPos().z;
        long seed   = client.world.getSeed();
        boolean isSlime = isSlimeChunk(seed, chunkX, chunkZ);

        String text  = "Slime Chunk: " + (isSlime ? "YES" : "NO");
        int    color = isSlime ? 0xFF55FF55 : pos.textColor;
        int    w     = client.textRenderer.getWidth(text) + PAD * 2;

        context.fill(x, y, x + w, y + HEIGHT, pos.backgroundAlpha << 24);
        context.drawTextWithShadow(client.textRenderer, text, x + PAD, y + PAD, color);
    }

    /** Vanilla slime chunk algorithm (same as server-side). */
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
