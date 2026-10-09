package com.huziyang520.forbiddenchimera.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Player targeting that also accepts creative players when the config allows it.
 *
 * <p>The goal never actually runs: it only writes the target and answers {@code false}, so it cannot
 * conflict with the combat goals that read that target.
 *
 * <p>Unlike the 26.3 build this needs no accompanying {@code asValidTarget} override, because 1.20.1's
 * {@code Mob#setTarget} does not filter creative players out. See {@link ChimeraTargeting}.
 */
public class ChimeraPlayerTargetGoal extends Goal {

    /** How often the scan runs, in ticks. */
    private static final int SCAN_INTERVAL = 10;

    private final Mob mob;
    private int ticksUntilScan;

    public ChimeraPlayerTargetGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        // Drop a target that stopped being valid (left the range, turned creative while disallowed...).
        if (this.mob.getTarget() instanceof Player current
                && !ChimeraTargeting.isValidTarget(this.mob, current)) {
            this.mob.setTarget(null);
        }

        if (this.ticksUntilScan > 0) {
            this.ticksUntilScan--;
            return false;
        }
        this.ticksUntilScan = SCAN_INTERVAL;

        Player player = this.findNearestPlayer();
        if (player != null) {
            this.mob.setTarget(player);
        }
        // Setting the target is the entire job; the goal itself must never run.
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    private @Nullable Player findNearestPlayer() {
        double range = this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        double bestDistance = range * range;
        Player best = null;

        for (Player player : this.mob.level().players()) {
            if (!ChimeraTargeting.isValidTarget(this.mob, player)) {
                continue;
            }
            double distance = this.mob.distanceToSqr(player);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }
}
