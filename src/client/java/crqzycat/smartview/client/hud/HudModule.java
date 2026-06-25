package crqzycat.smartview.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public interface HudModule {

    enum Category {
        GENERAL("General"),
        NETWORK("Network"),
        COMBAT("Combat"),
        VISUAL("Visual");

        public final String label;
        Category(String label) { this.label = label; }
    }

    String getId();
    String getDisplayName();
    int getDefaultX();
    int getDefaultY();
    int getBaseWidth(MinecraftClient client);
    int getBaseHeight();

    default Category getCategory() { return Category.GENERAL; }
    default boolean enabledByDefault() { return false; }
    default void onTick(boolean enabled) {}

    void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos);
}
