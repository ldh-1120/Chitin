package dev.dh.chitin.event;

import dev.dh.chitin.Chitin;
import dev.dh.chitin.entity.MantisEntity;
import dev.dh.chitin.registry.ModEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = Chitin.MOD_ID)
public final class ModEntityEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.MANTIS.get(), MantisEntity.createAttributes().build());
    }
}
