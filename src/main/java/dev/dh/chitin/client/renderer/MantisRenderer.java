package dev.dh.chitin.client.renderer;

import com.mojang.math.Axis;
import dev.dh.cascade.client.render.CascadeEntityRenderer;
import dev.dh.cascade.model.SkeletonPose;
import dev.dh.chitin.Chitin;
import dev.dh.chitin.entity.MantisEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class MantisRenderer extends CascadeEntityRenderer<MantisEntity> {
    private static final Vector3f KNEE_POLE = new Vector3f(3, 7, -8);

    public MantisRenderer(EntityRendererProvider.Context context) {
        super(context, ResourceLocation.fromNamespaceAndPath(Chitin.MOD_ID, "mantis"));
    }

    @Override
    protected void modifyPose(MantisEntity entity, float entityYaw, float partialTick, SkeletonPose pose) {
        for (int i = 0; i < 2; i++) {
            Vec3 foot = entity.footPosition(i, partialTick);
            if (foot == null)
                continue;

            Vec3 relative = foot.subtract(entity.getPosition(partialTick));
            Vector3f target = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z);

            target.rotate(Axis.YP.rotationDegrees(180.0f - entityYaw).invert());
            target.mul(16.0f);

            String prefix = i == 0 ? "front_leg_" : "front_left_leg_";
            Vector3f pole = new Vector3f(i == 0 ? 3.0f : -3.0f, 7.0f, -8.0f);

            pose.solveTwoBoneIk(prefix + "upper", prefix + "lower", prefix + "foot", target, pole);
        }
    }
}