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
 * Simple JSON config stored at .minecraft/config/smartview.json.
 * Holds one ModulePosition per registered module id.
 */
public class SmartViewConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("smartview-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("smartview.json");

    public Map<String, ModulePosition> modules = new LinkedHashMap<>();

    public static SmartViewConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                SmartViewConfig loaded = GSON.fromJson(reader, SmartViewConfig.class);
                if (loaded != null) {
                    if (loaded.modules == null) {
                        loaded.modules = new LinkedHashMap<>();
                    }
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
}
