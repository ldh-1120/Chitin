package dev.dh.chitin.registry;

import dev.dh.chitin.Chitin;
import dev.dh.chitin.entity.MantisEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Chitin.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<MantisEntity>> MANTIS =
            ENTITY_TYPES.register("mantis", () ->
                    EntityType.Builder.of(MantisEntity::new, MobCategory.CREATURE)
                            .sized(0.9f, 1.5f)
                            .clientTrackingRange(8)
                            .build("chitin:mantis"));
}
