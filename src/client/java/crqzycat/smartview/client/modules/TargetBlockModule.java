package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class TargetBlockModule implements HudModule {

    private static final int PAD = 4, HEIGHT = 16;

    @Override public String getId()             { return "targetblock"; }
    @Override public String getDisplayName()    { return "Target Block"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 90; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public boolean enabledByDefault() { return false; }
    @Override public Category getCategory()     { return Category.VISUAL; }

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
        if (client.crosshairTarget == null
                || client.crosshairTarget.getType() != HitResult.Type.BLOCK
                || client.world == null) {
            return "Target: ---";
        }
        BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
        BlockPos pos = hit.getBlockPos();
        Block block = client.world.getBlockState(pos).getBlock();
        String name = block.getName().getString();
        return name + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
