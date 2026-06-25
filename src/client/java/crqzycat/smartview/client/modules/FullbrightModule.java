package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Fullbright: kein HUD-Element, aber ein Side-Effect-Modul.
 * Gamma wird live durch GammaMixin überschrieben solange enabled = true.
 */
public class FullbrightModule implements HudModule {

    @Override public String getId()             { return "fullbright"; }
    @Override public String getDisplayName()    { return "Fullbright"; }
    @Override public int getDefaultX()          { return 0; }
    @Override public int getDefaultY()          { return 0; }
    @Override public int getBaseWidth(MinecraftClient c) { return 0; }
    @Override public int getBaseHeight()        { return 0; }
    @Override public HudModule.Category getCategory() { return HudModule.Category.VISUAL; }
    @Override public boolean enabledByDefault() { return false; }

    @Override
    public void onTick(boolean enabled) {
        // Gamma-Override läuft über GammaMixin – hier nichts nötig.
    }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {}
}
