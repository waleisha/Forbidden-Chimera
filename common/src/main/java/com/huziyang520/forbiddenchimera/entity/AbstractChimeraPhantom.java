package com.huziyang520.forbiddenchimera.entity;

import com.huziyang520.forbiddenchimera.ai.ChimeraPlayerTargetGoal;
import com.huziyang520.forbiddenchimera.ai.ChimeraSteering;
import com.huziyang520.forbiddenchimera.world.PhantomRider;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for the registered chimera monsters.
 *
 * <p>Deliberately NOT extending {@code Phantom}: every vanilla phantom goal is a private inner class
 * driving a private {@code moveTargetPoint}, so a subclass cannot replace the AI without breaking the
 * movement control. Instead this base rebuilds the flying behaviour from public/vanilla parts:
 * {@link FlyingMoveControl} for steering and the inherited {@code FlyingMob} flight for motion. That
 * gives a mob that looks and flies like a phantom while keeping its goals fully under our control.
 *
 * <p><b>Porting notes from 26.3.</b>
 * <ul>
 *   <li>26.3's {@code LivingEntity#travelFlying(Vec3, float)} does not exist in 1.20.1 - the flying
 *       motion lives in {@link FlyingMob#travel}, so this base extends {@code FlyingMob} instead of
 *       {@code Mob} and inherits it. That also brings the "no fall damage, not climbable" behaviour for
 *       free, which is why those overrides are gone;</li>
 *   <li>{@code EntityDimensions} is not passed to the passenger attachment hook any more - 1.20.1 uses
 *       {@link #getPassengersRidingOffset()}, which returns a plain Y offset;</li>
 *   <li>the {@code asValidTarget} override is <b>gone</b>: that method only exists from 1.21.2. In
 *       1.20.1 {@code Mob#setTarget} stores whatever it is given, so
 *       {@link ChimeraPlayerTargetGoal} needs no help from the entity to hold a creative player.</li>
 * </ul>
 */
public abstract class AbstractChimeraPhantom extends FlyingMob implements Enemy, ChimeraSteering {

    protected AbstractChimeraPhantom(EntityType<? extends AbstractChimeraPhantom> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 5;
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Phantoms are meant to be seen from far away. */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    /** Steers the flying move control towards a point. Public because the goals live in another package. */
    @Override
    public void flyTowards(double x, double y, double z, double speed) {
        this.getMoveControl().setWantedPosition(x, y, z, speed);
    }

    /**
     * Where a passenger (the boss's trident rider) sits.
     *
     * <p>Vanilla would fall back to the entity's own height, i.e. the top of the <b>hitbox</b> - 0.5 for
     * every chimera. These models are flat phantoms, so the rider belongs on the modelled back instead,
     * and the value is the derived model band constant the cargo uses
     * ({@link PhantomRider#RIDER_CARRY_HEIGHT}). It is deliberately a code constant, not a config value.
     */
    @Override
    public double getPassengersRidingOffset() {
        return PhantomRider.RIDER_CARRY_HEIGHT;
    }

    /**
     * The carried rider must <b>never</b> take control of its mount.
     *
     * <p>Vanilla {@code Mob#getControllingPassenger} hands the mount over to its first passenger whenever
     * that passenger is a {@code Mob} that can control a vehicle - true for a baby zombie. The mount then
     * calls {@code updateControlFlags}, which switches <b>off</b> the {@code MOVE}, {@code LOOK} and
     * {@code JUMP} control flags for the whole goal selector: every goal of ours becomes a no-op and the
     * chimera just hangs in the air. ("It freezes until the baby zombie dies" - the rider dying empties
     * the passenger list, so the flags come back.)
     *
     * <p>Returning {@code null} keeps the mount fully in charge of its own AI, which is the only thing
     * that makes sense here: the rider is a passenger we spawned, not a player sitting on a horse.
     */
    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return null;
    }
}
