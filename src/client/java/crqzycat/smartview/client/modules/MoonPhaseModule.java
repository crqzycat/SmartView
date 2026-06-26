package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class MoonPhaseModule implements HudModule {

    private static final int PAD = 4, HEIGHT = 16;

    private static final String[] PHASES = {
        "🌕 Full Moon",
        "🌖 Waning Gibbous",
        "🌗 Last Quarter",
        "🌘 Waning Crescent",
        "🌑 New Moon",
        "🌒 Waxing Crescent",
        "🌓 First Quarter",
        "🌔 Waxing Gibbous"
    };

    @Override public String getId()             { return "moonphase"; }
    @Override public String getDisplayName()    { return "Moon Phase"; }
    @Override public int getDefaultX()          { return 10; }
    @Override public int getDefaultY()          { return 110; }
    @Override public int getBaseHeight()        { return HEIGHT; }
    @Override public boolean enabledByDefault() { return false; }
    @Override public Category getCategory()     { return Category.WORLD; }

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
        if (client.world == null) return "Moon: ---";
        int phase = client.world.getMoonPhase();
        return PHASES[phase % PHASES.length];
    }
}
