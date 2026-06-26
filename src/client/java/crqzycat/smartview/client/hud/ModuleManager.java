package crqzycat.smartview.client.hud;

import crqzycat.smartview.client.config.SmartViewConfig;
import crqzycat.smartview.client.modules.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public final class ModuleManager {

    private static final List<HudModule>            MODULES   = new ArrayList<>();
    private static final Map<String, HudModule>     BY_ID     = new LinkedHashMap<>();
    private static final Map<String, KeyBinding>    KEYBINDS  = new LinkedHashMap<>();
    private static SmartViewConfig config;

    private ModuleManager() {}

    public static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("smartview", "main"));

    public static void init() {
        config = SmartViewConfig.load();

        register(new FpsModule());
        register(new PingModule());
        register(new ArmorStatusModule());
        register(new EffectStatusModule());
        register(new CoordsModule());
        register(new FullbrightModule());
        register(new ServerNameModule());
        register(new PacketLossModule());
        register(new BiomeModule());
        register(new LightLevelModule());
        register(new SlimeChunkModule());
        register(new MoonPhaseModule());
        register(new ClockModule());
        register(new CompassModule());
        register(new TargetBlockModule());

        for (HudModule module : MODULES) {
            config.modules.computeIfAbsent(module.getId(), id ->
                new ModulePosition(module.getDefaultX(), module.getDefaultY(), module.enabledByDefault()));

            KeyBinding kb = new KeyBinding(
                "key.smartview.module." + module.getId(),
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
            );
            KeyBindingHelper.registerKeyBinding(kb);
            KEYBINDS.put(module.getId(), kb);
        }
        config.save();
    }

    public static void register(HudModule module) {
        if (BY_ID.containsKey(module.getId()))
            throw new IllegalStateException("Duplicate SmartView module id: " + module.getId());
        MODULES.add(module);
        BY_ID.put(module.getId(), module);
    }

    public static List<HudModule> getModules()            { return MODULES; }
    public static KeyBinding      getKeybind(String id)   { return KEYBINDS.get(id); }

    public static ModulePosition getPosition(String id) {
        return config.modules.computeIfAbsent(id, k ->
            new ModulePosition(10, 10, false));
    }

    public static void setEnabled(String id, boolean enabled) {
        getPosition(id).enabled = enabled;
    }

    public static void save() { config.save(); }

    /** Called every client tick – handles keybind toggles and module side-effects. */
    public static void tick() {
        for (HudModule module : MODULES) {
            ModulePosition pos = getPosition(module.getId());

            // Toggle via keybind
            KeyBinding kb = KEYBINDS.get(module.getId());
            if (kb != null) {
                while (kb.wasPressed()) {
                    pos.enabled = !pos.enabled;
                }
            }

            // Notify module of current enabled state (e.g. Fullbright gamma management)
            module.onTick(pos.enabled);
        }
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        for (HudModule module : MODULES) {
            ModulePosition pos = getPosition(module.getId());
            if (!pos.enabled) continue;
            // Skip zero-size modules (Fullbright etc.)
            if (module.getBaseWidth(client) == 0 && module.getBaseHeight() == 0) continue;

            float scale = Math.max(0.25f, pos.scale);
            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(pos.x, pos.y);
            matrices.scale(scale, scale);
            module.render(context, client, 0, 0, pos);
            matrices.popMatrix();
        }
    }

    public static int scaledWidth(HudModule module, ModulePosition pos, MinecraftClient client) {
        return Math.round(module.getBaseWidth(client) * Math.max(0.25f, pos.scale));
    }

    public static int scaledHeight(HudModule module, ModulePosition pos) {
        return Math.round(module.getBaseHeight() * Math.max(0.25f, pos.scale));
    }

    /** Wird vom GammaMixin genutzt um Fullbright live zu erzwingen. */
    public static boolean isFullbrightEnabled() {
        if (config == null) return false;
        ModulePosition pos = config.modules.get("fullbright");
        return pos != null && pos.enabled;
    }
}
