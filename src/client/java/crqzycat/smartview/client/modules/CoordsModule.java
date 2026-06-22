package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class CoordsModule implements HudModule {

    private static final int PAD    = 4;
    private static final int LINE_H = 10;
    private static final int LINES  = 4; // X, Y, Z, facing
    private static final int WIDTH  = 110;

    @Override public String getId() { return "coords"; }
    @Override public String getDisplayName() { return "Coordinates"; }
    @Override public int getDefaultX() { return 10; }
    @Override public int getDefaultY() { return 230; }
    @Override public int getBaseWidth(MinecraftClient c) { return WIDTH; }
    @Override public int getBaseHeight() { return LINES * LINE_H + PAD * 2; }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        if (client.player == null) return;

        int bx = (int) Math.floor(client.player.getX());
        int by = (int) Math.floor(client.player.getY());
        int bz = (int) Math.floor(client.player.getZ());
        Direction facing = Direction.fromHorizontalDegrees(client.player.getYaw());

        int h = getBaseHeight();
        context.fill(x, y, x + WIDTH, y + h, pos.backgroundAlpha << 24);

        int tx = x + PAD;
        int ty = y + PAD;
        context.drawTextWithShadow(client.textRenderer, "X: " + bx, tx, ty,           0xFFFF5555);
        context.drawTextWithShadow(client.textRenderer, "Y: " + by, tx, ty + LINE_H,  0xFF55FF55);
        context.drawTextWithShadow(client.textRenderer, "Z: " + bz, tx, ty + LINE_H * 2, 0xFF5555FF);
        context.drawTextWithShadow(client.textRenderer, facing.asString().toUpperCase(),
                tx, ty + LINE_H * 3, 0xFFFFFFFF);
    }
}
