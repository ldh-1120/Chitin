package dev.dh.chitin.entity;

import dev.dh.cascade.animation.AnimationPlayer;
import dev.dh.cascade.animation.BoneMask;
import dev.dh.cascade.animation.MotionInstance;
import dev.dh.cascade.animation.MotionLayer;
import dev.dh.cascade.api.MotionEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class MantisEntity extends PathfinderMob implements MotionEntity {
    private static final String IDLE = "animation.mantis.idle";
    private static final String ATTACK = "animation.mantis.attack";

    private final MotionInstance motion = new MotionInstance();
    private final MotionLayer attackLayer = motion.addLayer("attack", BoneMask.subtree("head"), MotionLayer.Mode.OVERRIDE);

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
    protected void registerGoals() { }

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
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
