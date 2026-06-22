package crqzycat.smartview.client.hud;

import crqzycat.smartview.client.config.SmartViewConfig;
import crqzycat.smartview.client.modules.FpsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {

    private static final List<HudModule> MODULES = new ArrayList<>();
    private static final Map<String, HudModule> BY_ID = new LinkedHashMap<>();
    private static SmartViewConfig config;

    private ModuleManager() {}

    public static void init() {
        config = SmartViewConfig.load();
        register(new FpsModule());

        for (HudModule module : MODULES) {
            config.modules.computeIfAbsent(module.getId(), id ->
                    new ModulePosition(module.getDefaultX(), module.getDefaultY(), module.enabledByDefault()));
        }
        config.save();
    }

    public static void register(HudModule module) {
        if (BY_ID.containsKey(module.getId()))
            throw new IllegalStateException("Duplicate SmartView module id: " + module.getId());
        MODULES.add(module);
        BY_ID.put(module.getId(), module);
    }

    public static List<HudModule> getModules() { return MODULES; }

    public static ModulePosition getPosition(String id) {
        return config.modules.computeIfAbsent(id, k -> new ModulePosition(10, 10, true));
    }

    public static void setEnabled(String id, boolean enabled) {
        getPosition(id).enabled = enabled;
    }

    public static void save() { config.save(); }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        for (HudModule module : MODULES) {
            ModulePosition pos = getPosition(module.getId());
            if (!pos.enabled) continue;

            float scale = Math.max(0.25f, pos.scale);
            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(pos.x, pos.y);
            matrices.scale(scale, scale);
            module.render(context, client, 0, 0, pos);
            matrices.popMatrix();
        }
    }

    /**
     * Returns the scaled bounding box of a module in screen coordinates.
     * Used by the edit screen for hit-testing and outline drawing.
     */
    public static int scaledWidth(HudModule module, ModulePosition pos, MinecraftClient client) {
        return Math.round(module.getBaseWidth(client) * Math.max(0.25f, pos.scale));
    }

    public static int scaledHeight(HudModule module, ModulePosition pos) {
        return Math.round(module.getBaseHeight() * Math.max(0.25f, pos.scale));
    }
}
