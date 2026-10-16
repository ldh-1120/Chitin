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
    private static final String[] LEGS = {
            "front_leg_",
            "front_left_leg_",
            "middle_leg_",
            "middle_left_leg_",
            "rear_leg_",
            "rear_left_leg_"
    };

    public MantisRenderer(EntityRendererProvider.Context context) {
        super(context, ResourceLocation.fromNamespaceAndPath(Chitin.MOD_ID, "mantis"));
    }

    @Override
    protected void modifyPose(MantisEntity entity, float entityYaw, float partialTick, SkeletonPose pose) {
        double rightHeight = 0.0;
        double leftHeight = 0.0;
        double frontHeight = 0.0;
        double rearHeight = 0.0;

        boolean grounded = true;
        for (int i = 0; i < LEGS.length; i++) {
            Vec3 foot = entity.footPosition(i, partialTick, false);
            if (foot == null) {
                grounded = false;
                break;
            }

            if (i % 2 == 0)
                rightHeight += foot.y;
            else
                leftHeight += foot.y;

            if (i < 2)
                frontHeight += foot.y;
            else if (i >= 4)
                rearHeight += foot.y;
        }

        if (grounded) {
            double sideDifference = (rightHeight - leftHeight) / 3.0;
            double lengthDifference = (rearHeight - frontHeight) / 2.0;

            float pitch = (float) Math.toDegrees(Math.atan(-lengthDifference / (12.0 / 16.0)));
            float roll = (float) Math.toDegrees(Math.atan(sideDifference / (14.0 / 16.0)));

            pitch = Math.max(-10.0f, Math.min(10.0f, pitch));
            roll = Math.max(-10.0f, Math.min(10.0f, roll));

            pose.setRotationDegrees("body", pitch, 0.0f, roll);
        }

        for (int i = 0; i < LEGS.length; i++) {
            Vec3 foot = entity.footPosition(i, partialTick, true);
            if (foot == null)
                continue;

            Vec3 relative = foot.subtract(entity.getPosition(partialTick));
            Vector3f target = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z);

            target.rotate(Axis.YP.rotationDegrees(180.0f - entityYaw).invert());
            target.mul(16.0f);

            Vector3f pole = new Vector3f(i % 2 == 0 ? 9.0f : -9.0f, 7.0f, (i / 2 - 1) * 6.0f);
            String name = LEGS[i];

            pose.solveTwoBoneIk(name + "upper", name + "lower", name + "foot", target, pole);
        }
    }
}