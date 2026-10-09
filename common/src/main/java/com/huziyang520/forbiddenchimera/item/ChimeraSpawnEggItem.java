package com.huziyang520.forbiddenchimera.item;

import com.huziyang520.forbiddenchimera.entity.ChimeraPhantom;
import com.huziyang520.forbiddenchimera.entity.ChimeraVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ForgeSpawnEggItem;
import org.jetbrains.annotations.Nullable;

/**
 * Spawn egg for the two vanilla-phantom chimeras (幻翼骑士苦力怕 and 苦力怕投手幻翼).
 *
 * <p>These two are not registered entity types - they are genuine vanilla phantoms stamped with a
 * {@link ChimeraVariant} - so an egg bound to an entity type cannot express them. This egg spawns a
 * vanilla phantom and stamps the variant on before the phantom ever ticks.
 *
 * <p><b>Why it extends {@link ForgeSpawnEggItem} rather than {@code Item}.</b> The first version of this
 * class extended {@code Item} and reimplemented the right-click by hand, which quietly lost things a
 * vanilla spawn egg does and made these two eggs behave differently from the other five:
 * <ul>
 *   <li><b>the dispenser behaviour</b> - {@code ForgeSpawnEggItem}'s handler registers a dispense
 *       behaviour for every egg it creates, so all seven eggs fire from a dispenser. A hand rolled
 *       {@code Item} gets none and silently does nothing in one;</li>
 *   <li><b>the {@code isSpawnEgg} contract</b> - code that asks "is this a spawn egg" (JEI, recipe
 *       viewers, mob automation) tests {@code instanceof SpawnEggItem}, which a plain {@code Item}
 *       fails;</li>
 *   <li><b>the creative tab colour handler</b> - {@code ForgeSpawnEggItem} registers one per egg, so
 *       the two-tone egg artwork tints correctly with no extra code here.</li>
 * </ul>
 *
 * <p>The entity type handed to {@code super} is a vanilla phantom: that is genuinely what gets created,
 * and it is what makes the inherited {@code getType}, dispenser and JEI paths report something truthful.
 */
public class ChimeraSpawnEggItem extends ForgeSpawnEggItem {

    private final ChimeraVariant variant;

    public ChimeraSpawnEggItem(ChimeraVariant variant, int primaryColor, int secondaryColor) {
        // A supplier, not the type itself: that is ForgeSpawnEggItem's whole reason for existing - it
        // resolves the entity type lazily, so the egg can be registered before the entity registry is
        // populated. Passing EntityType.PHANTOM directly does not compile.
        super(() -> EntityType.PHANTOM, primaryColor, secondaryColor, new Properties());
        this.variant = variant;
    }

    /** @return the chimera role this egg stamps onto the phantom it spawns. */
    public ChimeraVariant variant() {
        return this.variant;
    }

    /**
     * Right-click on a block: spawn on the clicked face unless the block has no collision, in which case
     * spawn inside it. Mirrors vanilla {@code SpawnEggItem#useOn}.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);

        BlockPos spawnPos = state.getCollisionShape(level, clicked).isEmpty()
                ? clicked
                : clicked.relative(context.getClickedFace());

        if (this.spawnPhantom(serverLevel, spawnPos, player) == null) {
            return InteractionResult.FAIL;
        }

        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.gameEvent(player, GameEvent.ENTITY_PLACE, clicked);
        return InteractionResult.CONSUME;
    }

    /**
     * Right-click in the air: vanilla spawn eggs do nothing there, and neither does this one.
     *
     * <p>Returns the same {@code PASS} holder a vanilla egg does, so the click falls through instead of
     * being swallowed. Note the 1.20.1 signature returns an {@code InteractionResultHolder}, unlike the
     * richer result type the 26.3 source used.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    /**
     * Spawns the phantom with its variant already stamped, so {@code PhantomMixin} attaches the cargo and
     * the goals on its first tick without re-rolling the variant.
     *
     * <p>The variant must be set <b>before</b> the entity joins the level: the mixin resolves an undecided
     * variant on the first server tick, so a phantom that ticked once as {@code UNDECIDED} would roll
     * itself into a random chimera instead of the one the player asked for.
     */
    private @Nullable Phantom spawnPhantom(ServerLevel level, BlockPos pos, @Nullable Player player) {
        Phantom phantom = EntityType.PHANTOM.create(level);
        if (phantom == null) {
            return null;
        }

        ((ChimeraPhantom) phantom).forbiddenChimera$setVariant(this.variant);
        phantom.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        phantom.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null, null);
        if (!level.addFreshEntity(phantom)) {
            return null;
        }

        level.playSound(null, pos, SoundEvents.PHANTOM_AMBIENT, SoundSource.NEUTRAL, 0.6F, 1.0F);
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return phantom;
    }
}
