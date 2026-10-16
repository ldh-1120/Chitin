package dev.dh.chitin.entity;

import dev.dh.cascade.animation.AnimationPlayer;
import dev.dh.cascade.animation.BoneMask;
import dev.dh.cascade.animation.MotionInstance;
import dev.dh.cascade.animation.MotionLayer;
import dev.dh.cascade.api.MotionEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MantisEntity extends PathfinderMob implements MotionEntity {
    private static final String IDLE = "animation.mantis.idle";
    private static final String ATTACK = "animation.mantis.attack";

    private static final int STEP_TICKS = 6;
    private static final double STEP_DISTANCE = 0.16;

    private final MotionInstance motion = new MotionInstance();
    private final MotionLayer attackLayer = motion.addLayer("attack", BoneMask.subtree("head"), MotionLayer.Mode.OVERRIDE);

    private final Vec3[] footFrom = new Vec3[2];
    private final Vec3[] footTo = new Vec3[2];
    private final int[] stepStartTick = {-1, -1};

    public MantisEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    public MotionInstance motion() {
        return motion;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.35, 40));
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide)
            return;

        AnimationPlayer base = motion.player();
        if (!base.isPlaying())
            base.play(IDLE, tickCount);

        int phase = tickCount % 100;
        if (phase == 40) {
            attackLayer.player().play(ATTACK, tickCount);
            attackLayer.fadeTo(1.0f, tickCount, 0.1f);
        }

        if (phase == 48)
            attackLayer.fadeTo(0.0f, tickCount, 0.2f);

        if (phase == 53)
            attackLayer.player().stop();

        updateFeet();
    }

    private void updateFeet() {
        if (!onGround()) {
            for (int i = 0; i < 2; i++) {
                footFrom[i] = null;
                footTo[i] = null;
                stepStartTick[i] = -1;
            }
            return;
        }

        float rotation = (float) Math.toRadians(180.0f - getYRot());

        for (int i = 0; i < 2; i++) {
            float xOffset = i == 0 ? 7.0f / 16.0f : -7.0f / 16.0f;

            Vector3f offset = new Vector3f(xOffset, 0.0f, -1.0f / 16.0f).rotateY(rotation);

            double x = getX() + offset.x();
            double z = getZ() + offset.z();

            Vec3 start = new Vec3(x, getY() + 2.0, z);
            Vec3 end = new Vec3(x, getY() - 2.0, z);

            BlockHitResult hit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) {
                footFrom[i] = null;
                footTo[i] = null;
                stepStartTick[i] = -1;
                continue;
            }

            Vec3 ground = hit.getLocation().add(0.0, 0.01, 0.0);
            if (footTo[i] == null || footTo[i].distanceToSqr(position()) > 4.0) {
                footFrom[i] = ground;
                footTo[i] = ground;
                stepStartTick[i] = -1;
                continue;
            }

            if (stepStartTick[i] >= 0) {
                if (tickCount - stepStartTick[i] < STEP_TICKS)
                    continue;

                stepStartTick[i] = -1;
                footFrom[i] = footTo[i];
            }

            int other = 1 - i;
            if (stepStartTick[other] >= 0)
                continue;

            double dx = ground.x - footTo[i].x;
            double dz = ground.z - footTo[i].z;

            if (dx * dx + dz * dz > STEP_DISTANCE * STEP_DISTANCE) {
                footFrom[i] = footTo[i];
                footTo[i] = ground;
                stepStartTick[i] = tickCount;
            }
        }
    }

    public Vec3 footPosition(int index, float partialTick) {
        if (index < 0 || index >= 2)
            throw new IndexOutOfBoundsException(index);

        if (footTo[index] == null)
            return null;

        if (stepStartTick[index] < 0)
            return footTo[index];

        double t = Math.max(0.0, Math.min(1.0, (tickCount + partialTick - stepStartTick[index]) / STEP_TICKS));
        double smooth = t * t * (3.0 - 2.0 * t);

        return footFrom[index].lerp(footTo[index], smooth).add(0.0, Math.sin(Math.PI * t) * 0.18, 0.0);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
