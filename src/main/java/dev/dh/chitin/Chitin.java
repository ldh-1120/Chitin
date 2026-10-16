package dev.dh.chitin;

import com.mojang.logging.LogUtils;
import dev.dh.cascade.api.CascadeApi;
import dev.dh.chitin.registry.ModEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Chitin.MOD_ID)
public final class Chitin {

    public static final String MOD_ID = "chitin";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Chitin(IEventBus modBus) {
        ModEntities.ENTITY_TYPES.register(modBus);

        LOGGER.info("Chitin initialized");
        LOGGER.info("Cascade API version: {}", CascadeApi.getVersion());
    }
}