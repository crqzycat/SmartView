package crqzycat.smartview.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * A single toggleable, freely placeable HUD element.
 * Implementations should keep render() cheap - it runs every frame.
 */
public interface HudModule {

    /** Stable, unique id used as the config key. Never change this once shipped. */
    String getId();

    /** Human readable name shown in the edit menu. */
    String getDisplayName();

    /** Default top-left position when the module is first registered. */
    int getDefaultX();

    int getDefaultY();

    /** Bounding box used for drag handles and overlap detection in the edit screen. */
    int getWidth();

    int getHeight();

    /** Whether this module is enabled the first time the mod runs. */
    default boolean enabledByDefault() {
        return true;
    }

    /**
     * Draw the module content. x/y is the top-left corner from the saved/edited position.
     * Do not call enable/visibility checks here - ModuleManager already handles that.
     */
    void render(DrawContext context, MinecraftClient client, int x, int y);
}
