package dev.dh.chitin.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dh.cascade.bedrock.BedrockGeometry;
import dev.dh.cascade.client.BedrockGeometryLoader;
import dev.dh.cascade.client.render.BedrockModelRenderer;
import dev.dh.cascade.model.Skeleton;
import dev.dh.cascade.model.SkeletonPose;
import dev.dh.chitin.Chitin;
import dev.dh.chitin.entity.MantisEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class MantisRenderer extends EntityRenderer<MantisEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Chitin.MOD_ID, "mantis");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Chitin.MOD_ID, "textures/entity/mantis.png");
    private static final String GEOMETRY_ID = "geometry.chitin.mantis";

    private List<BedrockGeometry> loadedGeometries;
    private BedrockModelRenderer modelRenderer;
    private SkeletonPose skeletonPose;

    public MantisRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.35f;
    }

    @Override
    public void render(MantisEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        updateModel();

        if (modelRenderer != null && skeletonPose != null) {
            poseStack.pushPose();

            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));

            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            modelRenderer.render(poseStack, consumer, packedLight, skeletonPose);

            poseStack.popPose();
        }
    }

    private void updateModel() {
        BedrockGeometryLoader loader = BedrockGeometryLoader.INSTANCE;
        List<BedrockGeometry> geometries = loader.get(MODEL);
        if (geometries == loadedGeometries)
            return;

        loadedGeometries = geometries;
        modelRenderer = null;
        skeletonPose = null;

        List<Skeleton> skeletons = loader.getSkeletons(MODEL);
        for (int i = 0; i < geometries.size() && i < skeletons.size(); i++) {
            BedrockGeometry geometry = geometries.get(i);
            if (!GEOMETRY_ID.equals(geometry.identifier()))
                continue;

            Skeleton skeleton = skeletons.get(i);

            modelRenderer = new BedrockModelRenderer(geometry, skeleton);
            skeletonPose = new SkeletonPose(skeleton);

            break;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(MantisEntity entity) {
        return TEXTURE;
    }
}
