package com.huziyang520.forbiddenchimera;

import com.huziyang520.forbiddenchimera.world.ChimeraExplosion;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Applies the per-entity blast reductions that 26.3 expressed through an
 * {@code ExplosionDamageCalculator}.
 *
 * <p><b>Why this class exists at all.</b> The mod needs two entities to survive a blast they are standing
 * in: the chimera that fired the missile (or blew itself up), and whoever is riding it. The 26.3 build did
 * that by overriding {@code ExplosionDamageCalculator#getEntityDamageAmount(Explosion, Entity, float)}.
 * That hook does <b>not</b> exist in 1.20.1 - the calculator only answers block questions, and entity
 * damage is computed inside {@code Explosion#finalizeExplosion} with no extension point - so the reduction
 * has to be applied after the fact, on the damage event.
 *
 * <p>The link back to the shooter is the damage source's owning entity, which is exactly the key
 * {@link ChimeraExplosion.Scope} publishes its protection under. An entry only exists while one of this
 * mod's explosions is resolving, so a vanilla TNT blast is never affected even if it happens to be
 * attributed to the same entity.
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID)
public final class ChimeraExplosionProtection {

    private ChimeraExplosionProtection() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();

        ChimeraExplosion.Protection protection = ChimeraExplosion.Scope.find(attacker);
        if (protection == null) {
            return;
        }

        if (victim == protection.protectedEntity()) {
            event.setAmount(event.getAmount() * protection.entityFactor());
            return;
        }
        // getRootVehicle is the mount itself for a passenger, and the entity itself otherwise, so this
        // matches direct and nested riders alike without walking the vehicle chain by hand.
        if (victim.getRootVehicle() == protection.protectedEntity()) {
            event.setAmount(event.getAmount() * protection.riderFactor());
        }
    }
}
