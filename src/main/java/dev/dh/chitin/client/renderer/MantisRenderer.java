package dev.dh.chitin.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dh.cascade.animation.AnimationClip;
import dev.dh.cascade.animation.AnimationSampler;
import dev.dh.cascade.bedrock.BedrockAnimation;
import dev.dh.cascade.bedrock.BedrockGeometry;
import dev.dh.cascade.client.BedrockAnimationLoader;
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
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(Chitin.MOD_ID, "mantis");
    private static final String GEOMETRY_ID = "geometry.chitin.mantis";

    private static final String IDLE_ANIMATION = "animation.mantis.idle";

    private List<BedrockGeometry> loadedGeometries;
    private BedrockModelRenderer modelRenderer;
    private SkeletonPose skeletonPose;
    private Skeleton skeleton;

    private BedrockAnimation loadedAnimation;
    private AnimationClip animationClip;

    public MantisRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.35f;
    }

    @Override
    public void render(MantisEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        updateModel();
        updateAnimation();

        if (modelRenderer != null && skeletonPose != null) {
            if (animationClip != null) {
                float timeSeconds = (entity.tickCount + partialTick) / 20.0f;
                AnimationSampler.apply(animationClip, skeletonPose, timeSeconds);
            } else
                skeletonPose.reset();

            poseStack.pushPose();

            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));

            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            modelRenderer.render(poseStack, consumer, packedLight, skeletonPose);

            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void updateModel() {
        BedrockGeometryLoader loader = BedrockGeometryLoader.INSTANCE;
        List<BedrockGeometry> geometries = loader.get(MODEL);
        if (geometries == loadedGeometries)
            return;

        loadedGeometries = geometries;
        modelRenderer = null;
        skeleton = null;
        skeletonPose = null;

        loadedAnimation = null;
        animationClip = null;

        List<Skeleton> skeletons = loader.getSkeletons(MODEL);
        for (int i = 0; i < geometries.size() && i < skeletons.size(); i++) {
            BedrockGeometry geometry = geometries.get(i);
            if (!GEOMETRY_ID.equals(geometry.identifier()))
                continue;

            this.skeleton = skeletons.get(i);

            modelRenderer = new BedrockModelRenderer(geometry, skeleton);
            skeletonPose = new SkeletonPose(skeleton);

            break;
        }
    }

    private void updateAnimation() {
        BedrockAnimation animation = BedrockAnimationLoader.INSTANCE.get(ANIMATIONS, IDLE_ANIMATION);
        if (animation == loadedAnimation)
            return;

        loadedAnimation = animation;
        animationClip = null;
        if (animation == null || skeleton == null)
            return;

        animationClip = new AnimationClip(animation, skeleton);
    }

    @Override
    public ResourceLocation getTextureLocation(MantisEntity entity) {
        return TEXTURE;
    }
}
