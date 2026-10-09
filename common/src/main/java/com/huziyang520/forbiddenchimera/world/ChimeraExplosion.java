package com.huziyang520.forbiddenchimera.world;

import com.huziyang520.forbiddenchimera.config.ForbiddenChimeraConfig;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The single place where "low power, destroys terrain, 100% of blocks drop" is implemented.
 *
 * <p>A plain {@link Level.ExplosionInteraction#BLOCK} explosion satisfies all three requirements in
 * 1.20.1, so this class deliberately stays a thin, documented wrapper.
 *
 * <p>{@code BLOCK} is used instead of {@code MOB} on purpose: {@code MOB} explosions are disabled when
 * the {@code mobGriefing} game rule is off, but a thrown explosive skull should always scar the terrain.
 *
 * <p><b>Porting note - the biggest behavioural difference in this file.</b> In 26.3 the per-entity blast
 * scaling came from overriding
 * {@code ExplosionDamageCalculator#getEntityDamageAmount(Explosion, Entity, float)}. That hook does
 * <b>not</b> exist in 1.20.1: the calculator here only decides block resistance and whether a block
 * explodes, while entity damage is computed inside {@code Explosion#finalizeExplosion} with no
 * extension point. The two reductions this mod needs - the shooter barely hurting itself, and a rider
 * being spared by its own mount - are therefore applied by {@code ChimeraExplosionProtection} (forge
 * module) through Forge's {@code LivingHurtEvent}, reading the markers published by {@link Scope}.
 */
public final class ChimeraExplosion {

    private ChimeraExplosion() {
    }

    /**
     * Detonates a chimera explosion.
     *
     * @param level  server level to detonate in.
     * @param source the entity credited with the explosion (may be null).
     * @param x      center x.
     * @param y      center y.
     * @param z      center z.
     * @param radius blast radius; keep this below the vanilla creeper's 3.0F.
     */
    public static void explode(ServerLevel level, @Nullable Entity source, double x, double y, double z, float radius) {
        explode(level, source, null, x, y, z, radius, 1.0F);
    }

    /**
     * Detonates an explosion that hurts one specific entity far less than everybody else.
     *
     * <p>This is how the Creeper Phantom survives its own missiles: the skull is a child of the mob, so
     * without this the blast would hit the shooter with full force.
     *
     * @param protectedEntity       entity that takes reduced damage, usually the shooter.
     * @param protectedEntityFactor damage multiplier for that entity, 0.1 means a 90% reduction.
     */
    public static void explode(ServerLevel level, @Nullable Entity source, @Nullable Entity protectedEntity,
                               double x, double y, double z, float radius, float protectedEntityFactor) {
        explode(level, source, protectedEntity, x, y, z, radius, protectedEntityFactor,
                (float) ForbiddenChimeraConfig.get().riderExplosionDamageFactor);
    }

    /**
     * {@link #explode(ServerLevel, Entity, Entity, double, double, double, float, float)} with an explicit
     * factor for the protected entity's <b>riders</b>.
     *
     * <p>This is what keeps the boss's baby zombie from being killed by its own mount's blast: the rider
     * is a passenger of the shooter, so it is inside every explosion the mount makes and would otherwise
     * take full damage. It is deliberately expressed as "whoever rides the protected entity" rather than
     * by checking the ownership tag, so it works for any future carried passenger and needs no mixin on
     * the vanilla zombie.
     *
     * @param riderFactor damage multiplier for the protected entity's passengers, 0.1 means 90% off.
     */
    public static void explode(ServerLevel level, @Nullable Entity source, @Nullable Entity protectedEntity,
                               double x, double y, double z, float radius, float protectedEntityFactor,
                               float riderFactor) {
        if (radius <= 0.0F) {
            return;
        }

        // An explosion resolves its entity damage synchronously inside this call, so the protection only
        // has to be visible for the duration of it. The scope publishes the markers on the way in and
        // removes them in a finally, which is also what keeps a throwing explosion from leaking them.
        try (Scope ignored = Scope.open(source, protectedEntity, protectedEntityFactor, riderFactor)) {
            // A null damage source makes vanilla build the usual explosion damage source from `source`.
            level.explode(source, null, (ExplosionDamageCalculator) null, x, y, z, radius, false,
                    Level.ExplosionInteraction.BLOCK);
        }
    }

    /**
     * The entity that should survive its own blast, and by how much.
     *
     * <p>Published rather than passed because {@code Explosion} gives the damage handler no way to know
     * who fired it: by the time {@code LivingHurtEvent} runs, the only link back to the shooter is the
     * damage source's owning entity, which is exactly the key used to look this up.
     */
    public record Protection(Entity protectedEntity, float entityFactor, float riderFactor) {
    }

    /**
     * Publishes blast protection for exactly as long as one explosion is resolving.
     *
     * <p>A map keyed by the explosion's source (rather than a single field) is used so that explosions
     * started from inside another explosion's damage pass cannot silently overwrite each other's
     * protection. Keys are compared by identity on purpose - the source is a specific entity instance,
     * and two distinct entities must never share an entry even if they happen to be equal.
     */
    public static final class Scope implements AutoCloseable {

        private static final Map<Entity, Protection> ACTIVE = new IdentityHashMap<>();

        private final @Nullable Entity source;

        private Scope(@Nullable Entity source) {
            this.source = source;
        }

        private static Scope open(@Nullable Entity source, @Nullable Entity protectedEntity,
                                  float entityFactor, float riderFactor) {
            if (source != null && protectedEntity != null) {
                ACTIVE.put(source, new Protection(protectedEntity, entityFactor, riderFactor));
            }
            return new Scope(source);
        }

        /**
         * @param source the damage source's owning entity.
         * @return the protection registered for this explosion source, or {@code null}.
         */
        public static @Nullable Protection find(@Nullable Entity source) {
            return source == null ? null : ACTIVE.get(source);
        }

        @Override
        public void close() {
            if (this.source != null) {
                ACTIVE.remove(this.source);
            }
        }
    }
}
