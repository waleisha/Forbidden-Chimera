package com.huziyang520.forbiddenchimera.ai;

import com.huziyang520.forbiddenchimera.config.ForbiddenChimeraConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Target filtering for the chimeras.
 *
 * <p><b>How this differs from the 26.3 build.</b> There, vanilla's {@code Mob#asValidTarget} rejected
 * creative players inside {@code setTarget} itself, so the mod had to override that method through a
 * mixin <i>and</i> reproduce vanilla's rule by hand - otherwise the target written by
 * {@link ChimeraPlayerTargetGoal} was silently discarded. In 1.20.1 neither {@code asValidTarget} nor
 * that filtering exists: {@code Mob#setTarget} stores whatever it is given. So the whole override is
 * gone, and this class shrinks to the one rule that is actually ours - whether the config lets a
 * creative player be hunted.
 *
 * <p>Spectators are never targeted: {@code canBeSeenAsEnemy} is false for them and they are not playing.
 */
public final class ChimeraTargeting {

    private ChimeraTargeting() {
    }

    /**
     * @return whether {@code player} may be acquired as a target, honouring
     *         {@code attackCreativePlayers}.
     */
    public static boolean isValidTarget(Mob mob, @Nullable Player player) {
        if (player == null || !player.isAlive() || player.isSpectator()) {
            return false;
        }
        if (!ForbiddenChimeraConfig.get().attackCreativePlayers
                && (player.isCreative() || player.getAbilities().invulnerable)) {
            return false;
        }
        return mob.canAttack(player);
    }

    /**
     * Re-checks a target that is already held, so a player who turns creative mid-fight is dropped
     * again when the config says creative players are off limits.
     *
     * @return the target to keep, or {@code null} when it is no longer valid.
     */
    public static @Nullable LivingEntity retain(Mob mob, @Nullable LivingEntity target) {
        if (target instanceof Player player && !isValidTarget(mob, player)) {
            return null;
        }
        return target;
    }
}
