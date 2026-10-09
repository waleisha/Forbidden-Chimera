package com.huziyang520.forbiddenchimera.entity;

import com.huziyang520.forbiddenchimera.config.ForbiddenChimeraConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * 末影人幻翼 - the enderman chimera.
 *
 * <p>Same wings and tail as the Creeper Phantom (the geometry keeps every bone name, so the shared
 * animation set plays unchanged) but the creeper torso and head are replaced with the enderman's, and
 * the vanilla enderman skin is composed onto the texture.
 *
 * <p>Behaviour is the plain phantom routine - dive in, no missiles ({@link #canFireSkulls()} is false) -
 * plus the one thing that makes it read as an enderman: it <b>teleports out of a projectile's way</b>.
 * That logic is copied from vanilla {@code EnderMan}: a projectile damage source causes up to 64
 * teleport attempts and the hit is voided entirely, so an arrow never lands.
 *
 * <p><b>Porting notes from 26.3.</b> The modern {@code randomTeleport} /
 * {@code canRandomlyTeleportTo} pair does not exist in 1.20.1, and neither does the
 * {@code ENDMAN_DOES_NOT_TELEPORT_TO} block tag. This version therefore reproduces the 1.20.1
 * {@code EnderMan} teleport sequence directly - {@code randomTeleport(x, y, z, true)} plus the same
 * level event, game event and sounds - and keeps the water check as the vanilla "never teleport into
 * water" rule it always was.
 */
public class EndermanPhantomEntity extends CreeperPhantomEntity {

    /** Vanilla {@code EnderMan#TELEPORT_ATTEMPTS}-style attempt count for the wide search. */
    private static final int TELEPORT_ATTEMPTS = 64;
    /** Fallback attempts when the vanilla wide search finds nowhere valid (a flyer is rarely near ground). */
    private static final int NEARBY_TELEPORT_ATTEMPTS = 16;

    public EndermanPhantomEntity(EntityType<? extends EndermanPhantomEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    /** No creeper part, so nothing to fire: the dive is a plain slam. */
    @Override
    public boolean canFireSkulls() {
        return false;
    }

    /**
     * Projectile dodge, copied from vanilla {@code EnderMan#hurt}: any damage source tagged
     * {@code IS_PROJECTILE} triggers a teleport and does <b>zero</b> damage - the arrow simply misses.
     *
     * <p>26.3's {@code hurtServer} does not exist in 1.20.1; {@code hurt} is the override point and has
     * the same shape here (no level parameter, so the level is read from the entity).
     */
    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) && !this.isPassenger()) {
            this.repeatedlyTryToTeleport();
            return false;
        }
        return super.hurt(source, damage);
    }

    /** The enderman's rare idle relocation, rolled on the server AI step. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        double chance = ForbiddenChimeraConfig.get().endermanIdleTeleportChance;
        if (chance > 0.0D && this.random.nextDouble() < chance) {
            this.repeatedlyTryToTeleport();
        }
    }

    /** Vanilla wide search, plus a short-range fallback for a flying mob. */
    private void repeatedlyTryToTeleport() {
        if (this.isPassenger()) {
            return;
        }
        for (int i = 0; i < TELEPORT_ATTEMPTS; i++) {
            if (this.teleport()) {
                return;
            }
        }
        // Vanilla only ever searches the ground level of a 64 block cube; a phantom is usually high in
        // the air where that finds nothing. Hop a short distance in place instead, so the dodge still
        // happens - same effect, smaller step.
        for (int i = 0; i < NEARBY_TELEPORT_ATTEMPTS; i++) {
            double x = this.getX() + (this.random.nextDouble() - 0.5D) * 16.0D;
            double y = this.getY() + (this.random.nextInt(9) - 4);
            double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 16.0D;
            if (this.teleport(x, y, z)) {
                return;
            }
        }
    }

    /** Vanilla {@code EnderMan#teleport()}: random 64 wide, 32 up and down. */
    private boolean teleport() {
        if (this.level().isClientSide() || !this.isAlive()) {
            return false;
        }
        double x = this.getX() + (this.random.nextDouble() - 0.5D) * 64.0D;
        double y = this.getY() + (this.random.nextInt(64) - 32);
        double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 64.0D;
        return this.teleport(x, y, z);
    }

    /** Vanilla {@code EnderMan#teleport(double,double,double)}, sound and level event included. */
    private boolean teleport(double x, double y, double z) {
        if (this.isPassenger()) {
            return false;
        }
        // The vanilla "never into water" rule, checked against the destination's floor.
        if (this.level().getBlockState(BlockPos.containing(x, y, z).below()).getFluidState()
                .is(FluidTags.WATER)) {
            return false;
        }

        Vec3 oldPos = this.position();
        boolean result = this.randomTeleport(x, y, z, true);
        if (result) {
            Level level = this.level();
            level.gameEvent(GameEvent.TELEPORT, oldPos, GameEvent.Context.of(this));
            if (!this.isSilent()) {
                level.playSound(null, oldPos.x, oldPos.y, oldPos.z, SoundEvents.ENDERMAN_TELEPORT,
                        this.getSoundSource(), 1.0F, 1.0F);
                this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
            }
        }
        return result;
    }

    /** Reuses the enderman's own sound set so the chimera reads as enderman, not phantom. */
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }
}
