package dev.dh.chitin.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dh.cascade.client.render.CascadeEntityRenderer;
import dev.dh.cascade.model.SkeletonPose;
import dev.dh.chitin.Chitin;
import dev.dh.chitin.entity.MantisEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
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
        float height = entity.getBodyHeight(partialTick);
        pose.addTranslation("body", 0.0f, height * 16.0f, 0.0f);

        Vector2f tilt = entity.getTerrainTilt(partialTick);
        pose.rotateLocalDegrees("body", tilt.x(), 0.0f, tilt.y());

        for (int i = 0; i < LEGS.length; i++) {
            Vec3 foot = entity.footPosition(i, partialTick, true);
            if (foot == null)
                continue;

            Vector3f target = toModelPosition(entity, entityYaw, partialTick, foot);
            Vector3f pole = new Vector3f(i % 2 == 0 ? 9.0f : -9.0f, 7.0f, (i / 2 - 1) * 6.0f);

            String name = LEGS[i];
            pose.solveTwoBoneIk(name + "upper", name + "lower", name + "foot", target, pole);
        }
    }

    @Override
    protected void renderPoseOverlay(MantisEntity entity, float entityYaw, float partialTick, PoseStack stack, MultiBufferSource buffers, SkeletonPose pose) {
        if (!Minecraft.getInstance().getDebugOverlay().showDebugScreen())
            return;

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        stack.pushPose();

        try {
            stack.scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);
            for (int i = 0; i < LEGS.length; i++) {
                Vec3 foot = entity.footPosition(i, partialTick, true);
                if (foot == null)
                    continue;

                String name = LEGS[i];
                Vector3f target = toModelPosition(entity, entityYaw, partialTick, foot);
                Vector3f actual = pose.getModelPivot(name + "foot");

                drawMarker(stack, lines, target, 0.45f, 0.2f, 1.0f, 0.4f);
                drawMarker(stack, lines, actual, 0.25f, 0.2f, 0.75f, 1.0f);

                Vec3 ground = entity.footPosition(i, partialTick, false);
                if (ground != null && ground.distanceToSqr(foot) > 0.000001) {
                    Vector3f groundPoint = toModelPosition(entity, entityYaw, partialTick, ground);
                    drawMarker(stack, lines, groundPoint, 0.18f, 0.6f, 0.6f, 0.6f);
                }

                float error = actual.distance(target);
                if (error > 0.05f) {
                    Vector3f direction = new Vector3f(target).sub(actual).normalize();
                    int green = error > 0.5f ? 50 : 170;

                    lines.addVertex(stack.last(), actual.x(), actual.y(), actual.z())
                            .setColor(255, green, 60, 255)
                            .setNormal(stack.last(), direction.x(), direction.y(), direction.z());

                    lines.addVertex(stack.last(), target.x(), target.y(), target.z())
                            .setColor(255, green, 60, 255)
                            .setNormal(stack.last(), direction.x(), direction.y(), direction.z());
                }
            }
        } finally {
            stack.popPose();
        }
    }

    private static Vector3f toModelPosition(MantisEntity entity, float entityYaw, float partialTick, Vec3 worldPosition) {
        Vec3 relative = worldPosition.subtract(entity.getPosition(partialTick));
        Vector3f result = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z);

        result.rotate(Axis.YP.rotationDegrees(180.0f - entityYaw).invert());

        return result.mul(16.0f);
    }

    private static void drawMarker(PoseStack stack, VertexConsumer consumer, Vector3f position, float radius, float red, float green, float blue) {
        LevelRenderer.renderLineBox(stack, consumer, position.x() - radius, position.y() - radius, position.z() - radius, position.x() + radius, position.y() + radius, position.z() + radius, red, green, blue, 1.0f);
    }
}