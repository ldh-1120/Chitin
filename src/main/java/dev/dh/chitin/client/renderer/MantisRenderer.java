package dev.dh.chitin.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dh.cascade.animation.AnimationClip;
import dev.dh.cascade.animation.AnimationPlayer;
import dev.dh.cascade.animation.AnimationBlender;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MantisRenderer extends EntityRenderer<MantisEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Chitin.MOD_ID, "mantis");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Chitin.MOD_ID, "textures/entity/mantis.png");
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(Chitin.MOD_ID, "mantis");
    private static final String GEOMETRY_ID = "geometry.chitin.mantis";

    private List<BedrockGeometry> loadedGeometries;
    private Map<String, BedrockAnimation> loadedAnimations;

    private Skeleton skeleton;
    private SkeletonPose skeletonPose;

    private BedrockModelRenderer modelRenderer;
    private AnimationBlender blender;

    private Map<String, AnimationClip> animationClips = Map.of();

    public MantisRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.35f;
    }

    @Override
    public void render(MantisEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        updateModel();
        updateAnimations();

        if (modelRenderer != null && skeletonPose != null && blender != null) {
            double nowTick = entity.tickCount + (double) partialTick;

            AnimationPlayer.Frame frame = entity.motion().player().sampleFrame(nowTick);
            blender.apply(frame, entity.motion().layers(), nowTick, animationClips, skeletonPose);

            poseStack.pushPose();

            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));

            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            modelRenderer.render(poseStack, consumer, packedLight, skeletonPose);

            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private void updateModel() {
        BedrockGeometryLoader loader = BedrockGeometryLoader.INSTANCE;
        List<BedrockGeometry> geometries = loader.get(MODEL);
        if (geometries == loadedGeometries)
            return;

        loadedGeometries = geometries;

        skeleton = null;
        skeletonPose = null;
        modelRenderer = null;
        blender = null;

        loadedAnimations = null;
        animationClips = Map.of();

        List<Skeleton> skeletons = loader.getSkeletons(MODEL);
        for (int i = 0; i < geometries.size() && i < skeletons.size(); i++) {
            BedrockGeometry geometry = geometries.get(i);
            if (!GEOMETRY_ID.equals(geometry.identifier()))
                continue;

            skeleton = skeletons.get(i);

            modelRenderer = new BedrockModelRenderer(geometry, skeleton);
            skeletonPose = new SkeletonPose(skeleton);
            blender = new AnimationBlender(skeleton);

            break;
        }
    }

    private void updateAnimations() {
        Map<String, BedrockAnimation> animations = BedrockAnimationLoader.INSTANCE.get(ANIMATIONS);
        if (animations == loadedAnimations)
            return;

        loadedAnimations = animations;
        if (skeleton == null) {
            animationClips = Map.of();
            return;
        }

        Map<String, AnimationClip> compiled = new HashMap<>();

        for (Map.Entry<String, BedrockAnimation> entry : animations.entrySet())
            compiled.put(entry.getKey(), new AnimationClip(entry.getValue(), skeleton));

        animationClips = Map.copyOf(compiled);
    }

    @Override
    public ResourceLocation getTextureLocation(MantisEntity entity) {
        return TEXTURE;
    }
}
