package dev.dh.chitin.client.renderer;

import dev.dh.cascade.client.render.CascadeEntityRenderer;
import dev.dh.cascade.model.SkeletonPose;
import dev.dh.chitin.Chitin;
import dev.dh.chitin.entity.MantisEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

public final class MantisRenderer extends CascadeEntityRenderer<MantisEntity> {
    private static final Vector3f FOOT_TARGET = new Vector3f(7, 4, -1);
    private static final Vector3f KNEE_POLE = new Vector3f(3, 7, -8);

    public MantisRenderer(EntityRendererProvider.Context context) {
        super(context, ResourceLocation.fromNamespaceAndPath(Chitin.MOD_ID, "mantis"));
    }

    @Override
    protected void modifyPose(MantisEntity entity, float partialTick, SkeletonPose pose) {
        pose.solveTwoBoneIk("front_leg_upper", "front_leg_lower", "front_leg_foot", FOOT_TARGET, KNEE_POLE);
    }
}