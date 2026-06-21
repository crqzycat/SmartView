package crqzycat.smartview.client.hud;

import crqzycat.smartview.client.config.SmartViewConfig;
import crqzycat.smartview.client.modules.FpsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns every registered HudModule plus its persisted position/enabled state.
 * Register new modules in init() - the edit screen and HUD renderer both
 * read from getModules() so nothing else needs to change when a module is added.
 */
public final class ModuleManager {

    private static final List<HudModule> MODULES = new ArrayList<>();
    private static final Map<String, HudModule> BY_ID = new LinkedHashMap<>();
    private static SmartViewConfig config;

    private ModuleManager() {
    }

    public static void init() {
        config = SmartViewConfig.load();

        // Register modules here as they're built. Order = default list order in the edit menu.
        register(new FpsModule());

        for (HudModule module : MODULES) {
            config.modules.computeIfAbsent(module.getId(), id ->
                    new ModulePosition(module.getDefaultX(), module.getDefaultY(), module.enabledByDefault()));
        }
        config.save();
    }

    public static void register(HudModule module) {
        if (BY_ID.containsKey(module.getId())) {
            throw new IllegalStateException("Duplicate SmartView module id: " + module.getId());
        }
        MODULES.add(module);
        BY_ID.put(module.getId(), module);
    }

    public static List<HudModule> getModules() {
        return MODULES;
    }

    public static ModulePosition getPosition(String id) {
        return config.modules.computeIfAbsent(id, k -> new ModulePosition(10, 10, true));
    }

    public static void setPosition(String id, int x, int y) {
        ModulePosition pos = getPosition(id);
        pos.x = x;
        pos.y = y;
    }

    public static void setEnabled(String id, boolean enabled) {
        getPosition(id).enabled = enabled;
    }

    public static void save() {
        config.save();
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        for (HudModule module : MODULES) {
            ModulePosition pos = getPosition(module.getId());
            if (pos.enabled) {
                module.render(context, client, pos.x, pos.y);
            }
        }
    }
}
