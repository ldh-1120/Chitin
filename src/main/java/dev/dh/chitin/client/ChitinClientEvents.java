package dev.dh.chitin.client;

import dev.dh.cascade.client.render.CascadeEntityRenderer;
import dev.dh.chitin.Chitin;
import dev.dh.chitin.client.renderer.MantisRenderer;
import dev.dh.chitin.registry.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Chitin.MOD_ID, value = Dist.CLIENT)
public final class ChitinClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MANTIS.get(), context -> new CascadeEntityRenderer<>(context, ResourceLocation.fromNamespaceAndPath(Chitin.MOD_ID, "mantis")));
    }
}
