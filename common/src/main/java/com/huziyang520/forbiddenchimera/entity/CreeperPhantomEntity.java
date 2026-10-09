package com.huziyang520.forbiddenchimera.entity;

import com.huziyang520.forbiddenchimera.ai.ChimeraPlayerTargetGoal;
import com.huziyang520.forbiddenchimera.ai.DiveBombGoal;
import com.huziyang520.forbiddenchimera.ai.WanderFlyGoal;
import com.huziyang520.forbiddenchimera.config.ForbiddenChimeraConfig;
import com.huziyang520.forbiddenchimera.registry.ModEntities;
import com.huziyang520.forbiddenchimera.world.PhantomRider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Creeper Phantom - the original chimera. It climbs above its target, dives, and fires explosive
 * creeper skulls <b>only while at its attack position</b>.
 *
 * <p>It is the model based chimera, so it implements {@link GeoEntity}. The played pose is chosen from
 * synced flags rather than raw client state, so both sides always agree on what the model is doing.
 *
 * <p><b>Porting notes from 26.3.</b> The spear variant is <b>gone</b> (1.20.1 has no spears, and
 * {@code KineticWeapon} / {@code DataComponents} do not exist), which removes a lot of this class:
 * the {@code DATA_SPEAR} synced stack, the seven-spear table, {@code rollVariant}, the knight variant
 * with its rider slot and charge arming, and the whole kinetic-weapon dive resolution. What remains is
 * the plain diver plus the boss's needs.
 *
 * <p>The other substitutions:
 * <ul>
 *   <li>{@code ValueOutput}/{@code ValueInput} became {@link CompoundTag} put/get calls;</li>
 *   <li>{@code SynchedEntityData.Builder} does not exist yet, so {@code defineSynchedData()} is
 *       parameterless and calls {@code this.entityData.define(...)};</li>
 *   <li>{@code LivingEntity#hurtServer} became {@code hurt}, and {@code kill(ServerLevel)} became
 *       plain {@code kill()};</li>
 *   <li>GeckoLib 4 packages everything under {@code software.bernie.geckolib} and keeps the animation
 *       types in {@code ...core.animation} / {@code ...core.object}.</li>
 * </ul>
 */
public class CreeperPhantomEntity extends AbstractChimeraPhantom implements GeoEntity {

    /** Animation names, exactly as declared in creeper_phantom.animation.json. */
    private static final String ANIM_FLY = "animation.creeper_phantom.fly";
    private static final String ANIM_DIVE = "animation.creeper_phantom.dive";
    private static final String ANIM_FIRE = "animation.creeper_phantom.fire";

    private static final RawAnimation FLY = RawAnimation.begin().thenLoop(ANIM_FLY);
    private static final RawAnimation DIVE = RawAnimation.begin().thenLoop(ANIM_DIVE);
    private static final RawAnimation FIRE = RawAnimation.begin().thenPlayAndHold(ANIM_FIRE);

    /** How long a freshly fired skull keeps the recoil animation alive (0.35 s + slack). */
    private static final int FIRE_ANIMATION_TICKS = 8;

    private static final EntityDataAccessor<Boolean> DATA_DIVING =
            SynchedEntityData.defineId(CreeperPhantomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FIRING =
            SynchedEntityData.defineId(CreeperPhantomEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * NBT keys for the two cooldowns.
     *
     * <p>These are part of the save format: never rename them, or every loaded chimera forgets its
     * cooldowns and dives the instant the chunk comes back.
     */
    private static final String FIRE_COOLDOWN_KEY = "ForbiddenChimeraFireCooldown";
    private static final String ATTACK_COOLDOWN_KEY = "ForbiddenChimeraAttackCooldown";

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int fireCooldown;
    /** Ticks left before the next dive run may start; owned by the entity so it never freezes. */
    private int attackCooldown;
    private int fireAnimationTicks;

    // --- rider slot (the boss uses it; the small knight variant does not exist here) ---
    /**
     * The live rider, refreshed every tick; only meaningful for the boss.
     *
     * <p>Protected rather than private because the boss also carries a rider (a trident baby zombie)
     * through its own, separately gated path - see {@code BossLightningCreeperPhantomKnightEntity}.
     */
    protected @Nullable Zombie rider;

    public CreeperPhantomEntity(EntityType<? extends CreeperPhantomEntity> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DIVING, false);
        this.entityData.define(DATA_FIRING, false);
    }

    /** The live rider, or {@code null} when there is none. */
    public @Nullable Zombie knightRider() {
        Zombie current = this.rider;
        return current != null && current.isAlive() ? current : null;
    }

    /** Lets a subclass (the boss) own the same rider slot. */
    protected void setRider(@Nullable Zombie rider) {
        this.rider = rider;
    }

    /** Whether this variant may roll a rider at all. Only the boss does on this Minecraft version. */
    protected boolean supportsKnightVariant() {
        return false;
    }

    /**
     * Host death releases the rider instead of deleting it - it drops off as an ordinary baby zombie.
     * Self destruction is not a case here: this chimera does not explode on death.
     */
    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide()) {
            Zombie current = this.rider;
            if (current != null && current.isAlive()) {
                PhantomRider.release(current);
            }
            this.rider = null;
        }
        super.die(source);
    }

    // `public` because Mob widens these two from Entity's protected to public in 1.20.1; narrowing them
    // back to protected does not compile.
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // Cooldowns live on the entity, so they have to be persisted with it: without this a chunk
        // reload zeroed them, and an unloaded-and-reloaded chimera came back with its dive immediately
        // available - which is exactly the "it charges again the instant I come back" behaviour.
        tag.putInt(FIRE_COOLDOWN_KEY, this.fireCooldown);
        tag.putInt(ATTACK_COOLDOWN_KEY, this.attackCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.fireCooldown = tag.getInt(FIRE_COOLDOWN_KEY);
        this.attackCooldown = tag.getInt(ATTACK_COOLDOWN_KEY);
    }

    // --- GeckoLib animation -------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // setAndContinue only (re)starts an animation when the state actually changes, which is what
        // lets the one shot fire animation play to its end instead of restarting every tick.
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            CreeperPhantomEntity entity = state.getAnimatable();
            if (entity.isFiring()) {
                return state.setAndContinue(FIRE);
            }
            return state.setAndContinue(entity.isDiving() ? DIVE : FLY);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    /** True while {@link DiveBombGoal} is in its dive phase; synced so the model can follow. */
    public boolean isDiving() {
        return this.entityData.get(DATA_DIVING);
    }

    public void setDiving(boolean diving) {
        this.entityData.set(DATA_DIVING, diving);
    }

    /** True for the first few ticks after a skull was launched. */
    public boolean isFiring() {
        return this.entityData.get(DATA_FIRING);
    }

    private void setFiring(boolean firing) {
        this.entityData.set(DATA_FIRING, firing);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, this.createAttackGoal());
        // Idle flight, so the mob never just hangs in the air when there is nothing to dive at.
        this.goalSelector.addGoal(3, new WanderFlyGoal(this));
        // Vanilla targeting cannot see creative players; this one can, see ChimeraPlayerTargetGoal.
        this.targetSelector.addGoal(0, new ChimeraPlayerTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.tickWingTrail();
        } else {
            if (this.fireCooldown > 0) {
                this.fireCooldown--;
            }
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }
            if (this.fireAnimationTicks > 0 && --this.fireAnimationTicks == 0) {
                this.setFiring(false);
            }
        }
    }

    /**
     * Ticks left before the next dive run may start.
     *
     * <p>Lives on the entity, not in the goal: a goal stops ticking the moment it loses its target, so
     * a cooldown field inside it freezes and the attack never resumes.
     */
    public int attackCooldown() {
        return this.attackCooldown;
    }

    public void setAttackCooldown(int ticks) {
        this.attackCooldown = ticks;
    }

    /**
     * Vanilla phantom wing trail, reused verbatim from {@code Phantom#tick}: one {@code MYCELIUM} mote
     * per wing tip every tick, plus the flap sound when the flap animation passes its peak.
     *
     * <p>Same formulas as vanilla, so the effect sits on the wing line exactly like the real phantom:
     * lateral offset {@code bbWidth * 1.48}, height {@code (0.3 + anim * 0.45) * bbHeight * 2.5}, flap
     * phase from {@code id * 3 + tickCount}. No new particle type is registered - this is the vanilla
     * effect, not a look alike.
     */
    private void tickWingTrail() {
        float radiansPerDegree = (float) (Math.PI / 180.0);
        float anim = Mth.cos((this.uniqueFlapTickOffset() + this.tickCount) * 7.448451F * radiansPerDegree
                + (float) Math.PI);
        float nextAnim = Mth.cos((this.uniqueFlapTickOffset() + this.tickCount + 1) * 7.448451F
                * radiansPerDegree + (float) Math.PI);
        if (anim > 0.0F && nextAnim <= 0.0F) {
            // The client-only "play at these coordinates" overload: 1.20.1 has no entity-taking variant.
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.PHANTOM_FLAP,
                    this.getSoundSource(), 0.95F + this.random.nextFloat() * 0.05F,
                    0.95F + this.random.nextFloat() * 0.05F, false);
        }

        float width = this.getBbWidth() * 1.48F;
        float cos = Mth.cos(this.getYRot() * radiansPerDegree) * width;
        float sin = Mth.sin(this.getYRot() * radiansPerDegree) * width;
        float height = (0.3F + anim * 0.45F) * this.getBbHeight() * 2.5F;
        this.level().addParticle(ParticleTypes.MYCELIUM, this.getX() + cos, this.getY() + height,
                this.getZ() + sin, 0.0D, 0.0D, 0.0D);
        this.level().addParticle(ParticleTypes.MYCELIUM, this.getX() - cos, this.getY() + height,
                this.getZ() - sin, 0.0D, 0.0D, 0.0D);
    }

    /** Vanilla phantom flap phase source: {@code Phantom#getUniqueFlapTickOffset}. */
    public int uniqueFlapTickOffset() {
        return this.getId() * 3;
    }

    public boolean canFire() {
        return this.fireCooldown <= 0;
    }

    /**
     * Whether a dive run may launch missiles. The enderman variant only swoops: it has no creeper part
     * to fire, so its dive is a plain body slam.
     */
    public boolean canFireSkulls() {
        return true;
    }

    /**
     * Hook for the attack goal, so a subclass can replace the whole behaviour instead of copying the
     * goal list. The nuclear variant overrides this: it is a single-use missile, not a diver.
     */
    protected Goal createAttackGoal() {
        return new DiveBombGoal(this);
    }

    /**
     * Creates the missile for one shot.
     *
     * <p>Overridden by the lightning variant, which fires a lightning creeper skull instead; the whole
     * spawn logic stays identical, only the projectile differs.
     */
    protected CreeperSkullProjectile newSkull(ServerLevel level, Vec3 direction) {
        return new CreeperSkullProjectile(ModEntities.CREEPER_SKULL.get(), this, direction, level);
    }

    /**
     * Resolves the contact hit at the end of a dive run.
     *
     * <p>In 26.3 this method could route the hit through a carried spear's own kinetic-weapon data.
     * Spears do not exist in 1.20.1, so it is always the ordinary melee attack now. It is kept as a
     * named seam rather than inlined into the goal so a subclass can still specialise it.
     */
    public void performDiveHit(ServerLevel level, LivingEntity target) {
        this.doHurtTarget(target);
    }

    /** Launches an explosive creeper skull at the target. */
    public void fireCreeperSkull(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 from = new Vec3(this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ());
        Vec3 direction = target.getEyePosition().subtract(from);
        if (direction.lengthSqr() < 1.0E-4D) {
            return;
        }

        CreeperSkullProjectile skull = this.newSkull(serverLevel, direction.normalize());
        serverLevel.addFreshEntity(skull);

        this.fireCooldown = ForbiddenChimeraConfig.get().diverFireCooldownTicks;
        // The client derives the recoil pose from the synced flag, so no extra packet is needed.
        this.fireAnimationTicks = FIRE_ANIMATION_TICKS;
        this.setFiring(true);
        this.playSound(SoundEvents.PHANTOM_SWOOP, 1.0F, 0.6F);
    }
}
