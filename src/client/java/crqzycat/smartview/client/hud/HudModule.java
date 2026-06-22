package crqzycat.smartview.client.hud;

import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * A single toggleable, freely placeable HUD element.
 * Implementations should keep render() cheap - it runs every frame.
 */
public interface HudModule {

    String getId();
    String getDisplayName();
    int getDefaultX();
    int getDefaultY();

    /** Base width/height before scale is applied. Used for drag hit-testing. */
    int getBaseWidth(MinecraftClient client);
    int getBaseHeight();

    default boolean enabledByDefault() { return true; }

    /**
     * Draw the module. x/y is the top-left origin (already accounts for position from config).
     * Scale and matrix transforms are handled by ModuleManager before this call,
     * so draw as if scale=1. Use pos.backgroundAlpha for the background color.
     */
    void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos);
}
