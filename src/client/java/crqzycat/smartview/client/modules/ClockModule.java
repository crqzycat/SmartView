package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClockModule implements HudModule {

    private static final int PAD = 4, HEIGHT = 16;
    private static final DateTimeFormatter REAL_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override public String getId()             { return "clock"; }
    @Override public String getDisplayName()    { return "Clock"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 50; }
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
        // Real time
        String real = LocalTime.now().format(REAL_FMT);

        // Minecraft time
        String mc = "---";
        if (client.world != null) {
            long ticks = client.world.getTimeOfDay() % 24000;
            long hours   = (ticks / 1000 + 6) % 24;
            long minutes = (ticks % 1000) * 60 / 1000;
            mc = String.format("%02d:%02d", hours, minutes);
        }

        return real + " ☀" + mc;
    }
}
