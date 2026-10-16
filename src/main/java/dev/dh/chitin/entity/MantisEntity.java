package dev.dh.chitin.entity;

import dev.dh.cascade.animation.AnimationPlayer;
import dev.dh.cascade.animation.MotionInstance;
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

    private static final float ATTACK_DURATION = 0.6f;

    private final MotionInstance motion = new MotionInstance();

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

        AnimationPlayer player = motion.player();
        if (player.isPlaying())
            player.play(IDLE, tickCount);

        if (tickCount % 100 == 40)
            player.play(ATTACK, tickCount);

        if (ATTACK.equals(player.currentAnimation()) && player.getTimeSeconds(tickCount) >= ATTACK_DURATION)
            player.play(IDLE, tickCount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
