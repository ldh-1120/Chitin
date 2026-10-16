package dev.dh.chitin.client.renderer;

import dev.dh.cascade.client.render.CascadeEntityRenderer;
import dev.dh.chitin.Chitin;
import dev.dh.chitin.entity.MantisEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class MantisRenderer extends CascadeEntityRenderer<MantisEntity> {
    public MantisRenderer(EntityRendererProvider.Context context) {
        super(context, ResourceLocation.fromNamespaceAndPath(Chitin.MOD_ID, "mantis"));
    }
}