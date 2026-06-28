package crqzycat.smartview.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import crqzycat.smartview.client.hud.ModulePosition;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON config stored at .minecraft/config/smartview.json.
 * Supports multiple named profiles. Keybinds are stored separately
 * in Minecraft's options.txt and are shared across all profiles.
 */
public class SmartViewConfig {

    private static final Logger LOGGER     = LoggerFactory.getLogger("smartview-config");
    private static final Gson   GSON       = new GsonBuilder().setPrettyPrinting().create();
    private static final Path   CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("smartview.json");

    public static final String DEFAULT_PROFILE = "Default";
    public static final int    MAX_PROFILES    = 5;

    /** Name of the currently active profile. */
    public String activeProfile = DEFAULT_PROFILE;

    /** Map of profile name → module positions. */
    public Map<String, Map<String, ModulePosition>> profiles = new LinkedHashMap<>();

    /** Convenience accessor – modules for the active profile. */
    public Map<String, ModulePosition> modules() {
        return profiles.computeIfAbsent(activeProfile, k -> new LinkedHashMap<>());
    }

    public static SmartViewConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                SmartViewConfig loaded = GSON.fromJson(reader, SmartViewConfig.class);
                if (loaded != null) {
                    if (loaded.profiles == null) loaded.profiles = new LinkedHashMap<>();
                    if (loaded.activeProfile == null) loaded.activeProfile = DEFAULT_PROFILE;
                    return loaded;
                }
            } catch (IOException e) {
                LOGGER.error("Could not read smartview.json, falling back to defaults", e);
            }
        }
        return new SmartViewConfig();
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Could not save smartview.json", e);
        }
    }

    /** Switch to a profile by name, creating it (as a copy of current) if it doesn't exist. */
    public void switchProfile(String name) {
        if (!profiles.containsKey(name)) {
            // Clone current profile into new one
            Map<String, ModulePosition> clone = new LinkedHashMap<>();
            for (Map.Entry<String, ModulePosition> e : modules().entrySet()) {
                ModulePosition orig = e.getValue();
                ModulePosition copy = new ModulePosition(orig.x, orig.y, orig.enabled);
                copy.scale           = orig.scale;
                copy.backgroundAlpha = orig.backgroundAlpha;
                copy.textColor       = orig.textColor;
                clone.put(e.getKey(), copy);
            }
            profiles.put(name, clone);
        }
        activeProfile = name;
    }

    /** Delete a profile (cannot delete the last one). */
    public boolean deleteProfile(String name) {
        if (profiles.size() <= 1) return false;
        profiles.remove(name);
        if (activeProfile.equals(name)) {
            activeProfile = profiles.keySet().iterator().next();
        }
        return true;
    }
}
