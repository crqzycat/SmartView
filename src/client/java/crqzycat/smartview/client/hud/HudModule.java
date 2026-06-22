package crqzycat.smartview.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public interface HudModule {

    String getId();
    String getDisplayName();
    int getDefaultX();
    int getDefaultY();
    int getBaseWidth(MinecraftClient client);
    int getBaseHeight();

    /** All modules off by default – user enables what they want. */
    default boolean enabledByDefault() { return false; }

    /**
     * Called every client tick. Modules with side-effects (e.g. Fullbright)
     * use this to apply/remove their effect based on the enabled flag.
     */
    default void onTick(boolean enabled) {}

    /**
     * Draw the module. x/y is already at (0,0) – the caller applies the matrix transform.
     * Only called when the module is enabled.
     */
    void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos);
}
