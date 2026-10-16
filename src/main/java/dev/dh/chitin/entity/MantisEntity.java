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
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Arrays;

public class MantisEntity extends PathfinderMob implements MotionEntity {
    private static final String IDLE = "animation.mantis.idle";
    private static final String ATTACK = "animation.mantis.attack";

    private static final int LEG_COUNT = 6;
    private static final int STEP_TICKS = 8;
    private static final double STEP_DISTANCE = 0.22;
    private static final float AIR_BLEND_SPEED = 0.25f;

    private static final double HIP_HEIGHT = 8.0 / 16.0;
    private static final double MAX_LEG_REACH = 19.0 / 16.0;
    private static final float MAX_BODY_OFFSET = 0.15f;

    private static final int[] GROUP = {0, 1, 1, 0, 0, 1};

    private final MotionInstance motion = new MotionInstance();
    private final MotionLayer attackLayer = motion.addLayer("attack", BoneMask.subtree("head"), MotionLayer.Mode.OVERRIDE);

    private final Vec3[] footFrom = new Vec3[LEG_COUNT];
    private final Vec3[] footTo = new Vec3[LEG_COUNT];
    private final Vector3f[] takeoffTargets = new Vector3f[LEG_COUNT];

    private float previousAirBlend;
    private float airBlend;
    private boolean wasGrounded = true;

    private final Vector2f previousTerrainTilt = new Vector2f();
    private final Vector2f terrainTilt = new Vector2f();

    private float previousBodyHeight;
    private float bodyHeight;

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

        previousTerrainTilt.set(terrainTilt);
        previousBodyHeight = bodyHeight;
        previousAirBlend = airBlend;

        boolean grounded = onGround();
        if (wasGrounded && !grounded)
            captureTakeoffTargets();

        wasGrounded = grounded;

        if (grounded)
            airBlend = Math.max(0.0f, airBlend - AIR_BLEND_SPEED);
        else
            airBlend = Math.min(1.0f, airBlend + AIR_BLEND_SPEED);

        updateFeet();
        updateTerrainTilt();
        updateBodyHeight();
    }

    private void captureTakeoffTargets() {
        double angle = Math.toRadians(180.0 - getYRot());
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        for (int i = 0; i < LEG_COUNT; i++) {
            Vec3 foot = footPosition(i, 0.0f, true);
            if (foot == null) {
                takeoffTargets[i] = null;
                continue;
            }

            Vec3 relative = foot.subtract(position());

            double localX = relative.x * cos - relative.z * sin;
            double localZ = relative.x * sin + relative.z * cos;

            takeoffTargets[i] = new Vector3f((float) (localX * 16.0), (float) (relative.y * 16.0), (float) (localZ * 16.0));
        }
    }

    public float getAirBlend(float partialTick) {
        float t = Math.max(0.0f, Math.min(1.0f, partialTick));
        return previousAirBlend + (airBlend - previousAirBlend) * t;
    }

    public Vector3f getTakeoffTarget(int index) {
        if (index < 0 || index >= LEG_COUNT)
            throw new IndexOutOfBoundsException(index);

        Vector3f target = takeoffTargets[index];
        return target == null ? null : new Vector3f(target);
    }

    private void updateBodyHeight() {
        double angle = Math.toRadians(180.0 - getYRot());
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double totalOffset = 0.0;
        double lowerBound = Double.NEGATIVE_INFINITY;
        double upperBound = Double.POSITIVE_INFINITY;

        int count = 0;

        for (int i = 0; i < LEG_COUNT; i++) {
            Vec3 foot = footTo[i];
            if (foot == null)
                continue;

            if (movingGroup >= 0 && GROUP[i] == movingGroup)
                continue;

            int side = i % 2 == 0 ? 1 : -1;
            int row = i / 2 - 1;

            double hipX = side * 3.0 / 16.0;
            double hipZ = row / 16.0;

            double worldX = getX() + hipX * cos + hipZ * sin;
            double worldZ = getZ() - hipX * sin + hipZ * cos;

            double dx = foot.x - worldX;
            double dz = foot.z - worldZ;

            double horizontalSquared = dx * dx + dz * dz;
            double reachSquared = MAX_LEG_REACH * MAX_LEG_REACH;

            if (horizontalSquared >= reachSquared)
                continue;

            double verticalReach = Math.sqrt(reachSquared - horizontalSquared);
            double baseHipY = getY() + HIP_HEIGHT;

            double minimum = foot.y - verticalReach - baseHipY;
            double maximum = foot.y + verticalReach - baseHipY;

            lowerBound = Math.max(lowerBound, minimum);
            upperBound = Math.min(upperBound, maximum);

            totalOffset += foot.y - getY() - 0.01;

            count++;
        }

        double target = 0.0;

        if (count >= 3) {
            target = totalOffset / count;
            if (lowerBound <= upperBound)
                target = Math.max(lowerBound, Math.min(upperBound, target));

            target = Math.max(-MAX_BODY_OFFSET, Math.min(MAX_BODY_OFFSET, target));
        }

        bodyHeight += (float) (target - bodyHeight) * 0.20f;
    }

    public float getBodyHeight(float partialTick) {
        float t = Math.max(0.0f, Math.min(1.0f, partialTick));
        return previousBodyHeight + (bodyHeight - previousBodyHeight) * t;
    }

    private void updateTerrainTilt() {
        double angle = Math.toRadians(180.0 - getYRot());
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double sx = 0.0;
        double sy = 0.0;
        double sz = 0.0;

        double sxx = 0.0;
        double sxz = 0.0;
        double szz = 0.0;
        double sxy = 0.0;
        double szy = 0.0;

        int count = 0;

        for (int i = 0; i < LEG_COUNT; i++) {
            Vec3 foot = footTo[i];
            if (foot == null)
                continue;

            if (movingGroup >= 0 && GROUP[i] == movingGroup)
                continue;

            double dx = foot.x - getX();
            double dz = foot.z - getZ();

            double x = dx * cos - dz * sin;
            double z = dx * sin + dz * cos;
            double y = foot.y;

            sx += x;
            sy += y;
            sz += z;

            sxx += x * x;
            sxz += x * z;
            szz += z * z;
            sxy += x * y;
            szy += z * y;

            count++;
        }

        float pitch = 0.0f;
        float roll = 0.0f;

        if (count >= 3) {
            double xx = sxx - sx * sx / count;
            double xz = sxz - sx * sz / count;
            double zz = szz - sz * sz / count;
            double xy = sxy - sx * sy / count;
            double zy = szy - sz * sy / count;

            double determinant = xx * zz - xz * xz;
            if (determinant > 0.000001) {
                double a = (xy * zz - zy * xz) / determinant;
                double b = (zy * xx - xy * xz) / determinant;

                pitch = (float) Math.toDegrees(-Math.atan(b));
                roll = (float) Math.toDegrees(Math.atan(a));

                pitch = Math.max(-10.0f, Math.min(10.0f, pitch));
                roll = Math.max(-10.0f, Math.min(10.0f, roll));
            }
        }

        terrainTilt.set(terrainTilt.x + (pitch - terrainTilt.x) * 0.20f, terrainTilt.y + (roll - terrainTilt.y) * 0.20f);
    }

    public Vector2f getTerrainTilt(float partialTick) {
        float t = Math.max(0.0f, Math.min(1.0f, partialTick));
        return new Vector2f(previousTerrainTilt).lerp(terrainTilt, t);
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
            desired[i] = findFootLanding(i, cos, sin, movementX, movementZ);
            if (desired[i] != null && footTo[i] == null) {
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

            boolean needsStep = false;

            for (int i = 0; i < LEG_COUNT; i++) {
                if (GROUP[i] != group)
                    continue;

                if (desired[i] == null || footTo[i] == null)
                    continue;

                double dx = desired[i].x - footTo[i].x;
                double dy = desired[i].y - footTo[i].y;
                double dz = desired[i].z - footTo[i].z;

                if (dx * dx + dz * dz > stepDistance * stepDistance || Math.abs(dy) > 0.15)
                    needsStep = true;
            }

            if (needsStep) {
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
            if (desired[i] != null)
                footTo[i] = desired[i];
        }
    }

    private Vec3 findFootLanding(int index, double cos, double sin, double movementX, double movementZ) {
        int side = index % 2 == 0 ? 1 : -1;
        int row = index / 2 - 1;

        double localMovementX = movementX * cos - movementZ * sin;
        double localMovementZ = movementX * sin + movementZ * cos;

        double leadX = Math.max(-2.0 / 16.0, Math.min(2.0 / 16.0, localMovementX * 2.0));
        double leadZ = Math.max(-2.0 / 16.0, Math.min(2.0 / 16.0, localMovementZ * 2.0));

        double hipX = side * 3.0 / 16.0;
        double hipY = 8.0 / 16.0;
        double hipZ = row / 16.0;

        double maxReach = MAX_LEG_REACH;
        for (int attempt = 0; attempt < 2; attempt++) {
            double footX = side * 12.0 / 16.0 + (attempt == 0 ? leadX : 0.0);
            double footZ = row * 10.0 / 16.0 + (attempt == 0 ? leadZ : 0.0);

            double worldX = getX() + footX * cos + footZ * sin;
            double worldZ = getZ() - footX * sin + footZ * cos;

            BlockHitResult hit = level().clip(new ClipContext(new Vec3(worldX, getY() + 2.0, worldZ), new Vec3(worldX, getY() - 4.0, worldZ), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP)
                continue;

            Vec3 landing = hit.getLocation().add(0.0, 0.01, 0.0);

            double dx = footX - hipX;
            double dy = landing.y - getY() - hipY - bodyHeight;
            double dz = footZ - hipZ;

            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared > maxReach * maxReach)
                continue;

            return landing;
        }

        return null;
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
