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

import java.util.Arrays;

public class MantisEntity extends PathfinderMob implements MotionEntity {
    private static final String IDLE = "animation.mantis.idle";
    private static final String ATTACK = "animation.mantis.attack";

    private static final int LEG_COUNT = 6;
    private static final int STEP_TICKS = 8;
    private static final double STEP_DISTANCE = 0.12;

    private static final int[] GROUP = {0, 1, 1, 0, 0, 1};

    private final MotionInstance motion = new MotionInstance();
    private final MotionLayer attackLayer = motion.addLayer("attack", BoneMask.subtree("head"), MotionLayer.Mode.OVERRIDE);

    private final Vec3[] footFrom = new Vec3[LEG_COUNT];
    private final Vec3[] footTo = new Vec3[LEG_COUNT];

    private int movingGroup = -1;
    private int nextGroup = 0;
    private int stepStartTick;
    private int stepDurationTicks = STEP_TICKS;

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
        boolean reset = !onGround();
        if (!reset) {
            for (Vec3 anchor : footTo) {
                if (anchor != null && anchor.distanceToSqr(position()) > 2.25) {
                    reset = true;
                    break;
                }
            }
        }

        if (reset) {
            Arrays.fill(footFrom, null);
            Arrays.fill(footTo, null);

            movingGroup = -1;
            nextGroup = 0;
            if (!onGround())
                return;
        }

        double movementX = getX() - xo;
        double movementZ = getZ() - zo;
        double speedFactor = Math.min(1.0, Math.hypot(movementX, movementZ) / 0.15);

        double stepDistance = STEP_DISTANCE + 0.09 * speedFactor;
        Vec3[] desired = new Vec3[LEG_COUNT];

        double angle = Math.toRadians(180.0 - getYRot());
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        for (int i = 0; i < LEG_COUNT; i++) {
            double localX = (i % 2 == 0 ? 7.0 : -7.0) / 16.0;
            double localZ = (i / 2 - 1) * 6.0 / 16.0;

            double leadX = Math.max(-0.20, Math.min(0.20, movementX * 2.0));
            double leadZ = Math.max(-0.20, Math.min(0.20, movementZ * 2.0));

            double x = getX() + localX * cos + localZ * sin + leadX;
            double z = getZ() - localX * sin + localZ * cos + leadZ;

            BlockHitResult hit = level().clip(new ClipContext(new Vec3(x, getY() + 2.0, z), new Vec3(x, getY() - 2.0, z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP)
                continue;

            desired[i] = hit.getLocation().add(0.0, 0.01, 0.0);

            if (footTo[i] == null) {
                footFrom[i] = desired[i];
                footTo[i] = desired[i];
            }
        }

        if (movingGroup >= 0) {
            if (tickCount - stepStartTick < stepDurationTicks)
                return;

            nextGroup = 1 - movingGroup;
            movingGroup = -1;
        }

        int selectedGroup = -1;
        for (int candidate = 0; candidate < 2; candidate++) {
            int group = (nextGroup + candidate) % 2;

            boolean ready = true;
            boolean needsStep = false;

            for (int i = 0; i < LEG_COUNT; i++) {
                if (GROUP[i] != group)
                    continue;

                if (desired[i] == null || footTo[i] == null) {
                    ready = false;
                    break;
                }

                double dx = desired[i].x - footTo[i].x;
                double dy = desired[i].y - footTo[i].y;
                double dz = desired[i].z - footTo[i].z;
                if (dx * dx + dz * dz > stepDistance * stepDistance || Math.abs(dy) > 0.15)
                    needsStep = true;
            }

            if (ready && needsStep) {
                selectedGroup = group;
                break;
            }
        }

        if (selectedGroup < 0)
            return;

        movingGroup = selectedGroup;
        stepStartTick = tickCount;

        stepDurationTicks = STEP_TICKS - (int) Math.round(4.0 * speedFactor);

        for (int i = 0; i < LEG_COUNT; i++) {
            if (GROUP[i] != movingGroup)
                continue;

            footFrom[i] = footTo[i];
            footTo[i] = desired[i];
        }
    }

    public Vec3 footPosition(int index, float partialTick, boolean includeLift) {
        if (index < 0 || index >= LEG_COUNT)
            throw new IndexOutOfBoundsException(index);

        if (footTo[index] == null)
            return null;

        if (movingGroup < 0 || GROUP[index] != movingGroup)
            return footTo[index];

        double t = Math.max(0.0, Math.min(1.0, (tickCount + partialTick - stepStartTick) / stepDurationTicks));
        double smooth = t * t * (3.0 - 2.0 * t);

        double lift = 0.0;
        if (includeLift) {
            double stride = footFrom[index].distanceTo(footTo[index]);
            lift = Math.min(0.20, stride * 0.5);
        }

        return footFrom[index].lerp(footTo[index], smooth).add(0.0, Math.sin(Math.PI * t) * lift, 0.0);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
