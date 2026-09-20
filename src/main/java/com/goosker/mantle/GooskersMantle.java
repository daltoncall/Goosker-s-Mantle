package com.goosker.mantle;

import com.goosker.mantle.config.MantleConfig;
import com.goosker.mantle.mantle.MantleHandler;
import com.goosker.mantle.network.MantleNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GooskersMantle implements ModInitializer {
    public static final String MOD_ID = "gooskersmantle";
    public static final Logger LOGGER = LoggerFactory.getLogger("Goosker's Mantle");
    public static final Identifier MANTLE_SOUND_ID = id("mantle");
    public static final SoundEvent MANTLE_SOUND = SoundEvent.of(MANTLE_SOUND_ID);

    private static MantleConfig config;

    @Override
    public void onInitialize() {
        config = MantleConfig.load(LOGGER);
        Registry.register(Registries.SOUND_EVENT, MANTLE_SOUND_ID, MANTLE_SOUND);
        MantleNetworking.registerServerReceivers();
        ServerTickEvents.END_SERVER_TICK.register(MantleHandler::endServerTick);
        LOGGER.info("Goosker's Mantle initialized");
    }

    public static MantleConfig config() {
        return config;
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
