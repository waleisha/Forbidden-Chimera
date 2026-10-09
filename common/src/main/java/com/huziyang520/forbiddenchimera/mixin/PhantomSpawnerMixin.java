package com.huziyang520.forbiddenchimera.mixin;

import com.huziyang520.forbiddenchimera.world.ChimeraSpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces vanilla phantom spawning with the chimeras.
 *
 * <p>{@code PhantomSpawner} is the only source of phantoms in the game - they are not part of any
 * biome spawn list - so cancelling its tick and running {@link ChimeraSpawner} is a complete and exact
 * swap: the chimeras inherit the sky darkness check, the altitude and sky visibility requirement, the
 * difficulty roll, the {@code TIME_SINCE_REST} insomnia roll and the {@code doInsomnia} game rule.
 *
 * <p>The countdown lives here as a {@code @Unique} field so that every {@code ServerLevel} keeps its
 * own schedule, exactly like the vanilla spawner instance it replaces.
 *
 * <p><b>Two signature details that differ from the 26.3 source, both load-bearing.</b>
 * <ol>
 *   <li>1.20.1's {@code tick} takes <b>three</b> booleans
 *       ({@code spawnEnemies}, {@code spawnFriendlies}, {@code forced}); 26.3 had two. Only the first
 *       matters here - the chimera spawner replaces hostile spawning, exactly like vanilla's own
 *       {@code spawnEnemies} gate.</li>
 *   <li>The method <b>returns an int</b> (the new countdown), so the callback must be a
 *       {@link CallbackInfoReturnable}. Using a plain {@code CallbackInfo} compiles fine but throws
 *       {@code InvalidInjectionException: CallbackInfoReturnable is required} at class-apply time and
 *       takes the server down on boot.</li>
 * </ol>
 */
@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {

    @Unique
    private int forbiddenChimera$nextTick;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void forbiddenChimera$spawnChimerasInstead(ServerLevel level, boolean spawnEnemies,
                                                       boolean spawnFriendlies,
                                                       CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(
                ChimeraSpawner.tick(level, spawnEnemies, this.forbiddenChimera$nextTick));
    }
}
