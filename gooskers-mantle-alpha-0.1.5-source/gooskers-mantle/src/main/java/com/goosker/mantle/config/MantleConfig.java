package com.goosker.mantle.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MantleConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "gooskers-mantle.json";
    private static final int CURRENT_CONFIG_VERSION = 2;

    public int config_version = CURRENT_CONFIG_VERSION;
    public double minimum_mantle_height = 1.1D;
    public double maximum_mantle_height = 2.0D;
    public int cooldown_ticks = 30;
    public double upward_velocity = 0.4D;
    public double detection_reach = 0.45D;
    public int animation_ticks = 14;

    public static MantleConfig load(Logger logger) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        MantleConfig config = new MantleConfig();

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                JsonElement rootElement = JsonParser.parseReader(reader);
                if (!rootElement.isJsonObject()) {
                    throw new JsonParseException("Mantle config root must be a JSON object");
                }

                JsonObject root = rootElement.getAsJsonObject();
                MantleConfig loaded = GSON.fromJson(root, MantleConfig.class);
                if (loaded != null) {
                    if (!root.has("config_version")) {
                        loaded.migrateLegacyDefaults(logger);
                    }
                    config = loaded;
                }
            } catch (IOException | JsonParseException exception) {
                logger.error("Could not read {}; using safe defaults", path, exception);
            }
        }

        config.normalize(logger);
        config.save(path, logger);
        return config;
    }

    private void normalize(Logger logger) {
        config_version = CURRENT_CONFIG_VERSION;
        double oldMinimum = minimum_mantle_height;
        double oldMaximum = maximum_mantle_height;
        int oldCooldown = cooldown_ticks;
        double oldVelocity = upward_velocity;
        double oldReach = detection_reach;
        int oldAnimation = animation_ticks;

        minimum_mantle_height = clampFinite(minimum_mantle_height, 0.1D, 4.0D, 1.1D);
        maximum_mantle_height = clampFinite(maximum_mantle_height, minimum_mantle_height, 4.0D, 2.0D);
        cooldown_ticks = Math.max(0, Math.min(cooldown_ticks, 200));
        upward_velocity = clampFinite(upward_velocity, 0.05D, 1.5D, 0.4D);
        detection_reach = clampFinite(detection_reach, 0.1D, 1.0D, 0.45D);
        animation_ticks = Math.max(1, Math.min(animation_ticks, 40));

        if (oldMinimum != minimum_mantle_height || oldMaximum != maximum_mantle_height
                || oldCooldown != cooldown_ticks || oldVelocity != upward_velocity
                || oldReach != detection_reach || oldAnimation != animation_ticks) {
            logger.warn("Invalid mantle config values were clamped to safe limits");
        }
    }

    private void migrateLegacyDefaults(Logger logger) {
        if (cooldown_ticks == 20) {
            cooldown_ticks = 30;
        }
        if (animation_ticks == 8) {
            animation_ticks = 14;
        }
        config_version = CURRENT_CONFIG_VERSION;
        logger.info("Updated legacy mantle config defaults for alpha-0.1.1");
    }

    private static double clampFinite(double value, double minimum, double maximum, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(minimum, Math.min(value, maximum));
    }

    private void save(Path path, Logger logger) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException exception) {
            logger.error("Could not write {}", path, exception);
        }
    }
}
