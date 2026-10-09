package com.huziyang520.forbiddenchimera.registry;

import com.huziyang520.forbiddenchimera.Constants;
import com.huziyang520.forbiddenchimera.entity.ChimeraVariant;
import com.huziyang520.forbiddenchimera.item.ChimeraSpawnEggItem;
import com.huziyang520.forbiddenchimera.item.ForbiddenPlankItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Items.
 *
 * <p>Porting note from 26.3: the modern spawn egg is <b>data driven</b> - it reads its entity type from
 * the {@code minecraft:entity_data} component. In 1.20.1 that mechanism does not exist, so the
 * registered chimeras use spawn eggs bound to an entity type and the two vanilla-phantom variants use
 * {@link ChimeraSpawnEggItem} (they have no entity type of their own to point at).
 *
 * <p><b>Why {@link ForgeSpawnEggItem} rather than vanilla {@code SpawnEggItem}.</b> Forge fires one
 * {@code RegisterEvent} per registry and populates <b>items before entity types</b>, so reading a
 * {@code RegistryObject<EntityType>} while the item suppliers run throws
 * {@code Registry Object not present: forbidden_chimera:creeper_phantom} and the whole mod fails to
 * load - which is exactly what the first server boot did. Vanilla's {@code SpawnEggItem} takes a live
 * {@code EntityType} and cannot be deferred; Forge's subclass takes a {@link java.util.function.Supplier}
 * and resolves it on first use, by which point the entity registry is filled.
 *
 * <p>Colours: the creeper line uses the vanilla creeper egg colours, the enderman variant the vanilla
 * enderman colours, so each egg reads as the hybrid it produces.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);

    /** Vanilla creeper egg colours. */
    private static final int CREEPER_PRIMARY = 0x0DA70B;
    private static final int CREEPER_SECONDARY = 0x000000;
    /** Vanilla enderman egg colours. */
    private static final int ENDERMAN_PRIMARY = 0x161616;
    private static final int ENDERMAN_SECONDARY = 0xCC00FA;
    /** The charged variants are tinted toward the lightning blue. */
    private static final int CHARGED_SECONDARY = 0x6FD8E8;
    /** The nuclear variant reads as a warning red. */
    private static final int NUCLEAR_SECONDARY = 0xC81E1E;
    /** The boss is dark, to match its mixed head cluster. */
    private static final int BOSS_PRIMARY = 0x0B3A0B;

    // --- the two vanilla-phantom variants: no entity type of their own, so custom eggs ---
    public static final RegistryObject<Item> PHANTOM_RIDER_CREEPER_SPAWN_EGG =
            ITEMS.register("phantom_rider_creeper_spawn_egg",
                    () -> new ChimeraSpawnEggItem(ChimeraVariant.RIDER,
                            CREEPER_PRIMARY, CHARGED_SECONDARY));

    public static final RegistryObject<Item> CREEPER_THROWER_PHANTOM_SPAWN_EGG =
            ITEMS.register("creeper_thrower_phantom_spawn_egg",
                    () -> new ChimeraSpawnEggItem(ChimeraVariant.THROWER,
                            CREEPER_PRIMARY, CHARGED_SECONDARY));

    // --- the registered chimeras: plain vanilla eggs ---
    public static final RegistryObject<Item> CREEPER_PHANTOM_SPAWN_EGG =
            ITEMS.register("creeper_phantom_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.CREEPER_PHANTOM,
                            CREEPER_PRIMARY, CREEPER_SECONDARY, new Item.Properties()));

    public static final RegistryObject<Item> LIGHTNING_CREEPER_PHANTOM_SPAWN_EGG =
            ITEMS.register("lightning_creeper_phantom_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.LIGHTNING_CREEPER_PHANTOM,
                            CREEPER_PRIMARY, CHARGED_SECONDARY, new Item.Properties()));

    public static final RegistryObject<Item> NUCLEAR_CREEPER_PHANTOM_SPAWN_EGG =
            ITEMS.register("nuclear_creeper_phantom_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.NUCLEAR_CREEPER_PHANTOM,
                            CREEPER_PRIMARY, NUCLEAR_SECONDARY, new Item.Properties()));

    public static final RegistryObject<Item> ENDERMAN_PHANTOM_SPAWN_EGG =
            ITEMS.register("enderman_phantom_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.ENDERMAN_PHANTOM,
                            ENDERMAN_PRIMARY, ENDERMAN_SECONDARY, new Item.Properties()));

    public static final RegistryObject<Item> BOSS_SPAWN_EGG =
            ITEMS.register("boss_lightning_creeper_phantom_knight_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.BOSS,
                            BOSS_PRIMARY, NUCLEAR_SECONDARY, new Item.Properties()));

    // --- 禁忌板材: the boss summon item ---
    /** Stacks to 16 so a few attempts can be carried at once; epic rarity to match what it costs. */
    public static final RegistryObject<Item> FORBIDDEN_PLANK =
            ITEMS.register("forbidden_plank",
                    () -> new ForbiddenPlankItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    private ModItems() {
    }
}
