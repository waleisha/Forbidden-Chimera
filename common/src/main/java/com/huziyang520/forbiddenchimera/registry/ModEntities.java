package com.huziyang520.forbiddenchimera.registry;

import com.huziyang520.forbiddenchimera.Constants;
import com.huziyang520.forbiddenchimera.entity.BossLightningCreeperPhantomKnightEntity;
import com.huziyang520.forbiddenchimera.entity.CreeperPhantomEntity;
import com.huziyang520.forbiddenchimera.entity.CreeperSkullProjectile;
import com.huziyang520.forbiddenchimera.entity.EndermanPhantomEntity;
import com.huziyang520.forbiddenchimera.entity.LightningCreeperPhantomEntity;
import com.huziyang520.forbiddenchimera.entity.LightningCreeperSkullProjectile;
import com.huziyang520.forbiddenchimera.entity.NuclearCreeperPhantomEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Entity type definitions.
 *
 * <p>Registered chimera entity types are only the ones with a hand made model: the Creeper Phantom
 * (mob3) and its charged sibling, plus their two skull projectiles.
 *
 * <p>mob1 (幻翼骑士苦力怕) and mob2 (苦力怕投手幻翼) are deliberately <b>not</b> registered here: they
 * are vanilla phantom + vanilla creeper combinations injected into the vanilla {@code Phantom} by
 * {@code PhantomMixin}, so no entity types, no loot tables and no attributes exist for them.
 *
 * <p>Porting notes from 26.3:
 * <ul>
 *   <li>the multi-loader {@code EntityRegistrar} indirection is gone - a single loader means the
 *       deferred register is used directly;</li>
 *   <li>{@code EntityType.Builder#build} takes the registry path as a plain string in 1.20.1;</li>
 *   <li>{@code eyeHeight} and {@code notInPeaceful} builder methods do not exist yet, so they are
 *       dropped. The eye height was a cosmetic hitbox detail; peacefulness is still handled by the
 *       spawner's own difficulty roll.</li>
 * </ul>
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Constants.MOD_ID);

    public static final RegistryObject<EntityType<CreeperPhantomEntity>> CREEPER_PHANTOM =
            ENTITIES.register("creeper_phantom", () -> EntityType.Builder
                    .of(CreeperPhantomEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.5F)
                    .clientTrackingRange(8)
                    .build("creeper_phantom"));

    public static final RegistryObject<EntityType<LightningCreeperPhantomEntity>> LIGHTNING_CREEPER_PHANTOM =
            ENTITIES.register("lightning_creeper_phantom", () -> EntityType.Builder
                    .of(LightningCreeperPhantomEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.5F)
                    .clientTrackingRange(8)
                    .build("lightning_creeper_phantom"));

    public static final RegistryObject<EntityType<NuclearCreeperPhantomEntity>> NUCLEAR_CREEPER_PHANTOM =
            ENTITIES.register("nuclear_creeper_phantom", () -> EntityType.Builder
                    .of(NuclearCreeperPhantomEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.5F)
                    .clientTrackingRange(8)
                    .build("nuclear_creeper_phantom"));

    public static final RegistryObject<EntityType<EndermanPhantomEntity>> ENDERMAN_PHANTOM =
            ENTITIES.register("enderman_phantom", () -> EntityType.Builder
                    .of(EndermanPhantomEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.5F)
                    .clientTrackingRange(8)
                    .build("enderman_phantom"));

    /**
     * The boss is deliberately absent from every spawn table: it can only be produced by the
     * 禁忌板材 item. Its dimensions are the same as the other chimeras on purpose - the geometry is
     * built on the same phantom model band, so the hitbox alignment proven for mob3/mob4 carries over.
     */
    public static final RegistryObject<EntityType<BossLightningCreeperPhantomKnightEntity>> BOSS =
            ENTITIES.register("boss_lightning_creeper_phantom_knight", () -> EntityType.Builder
                    .of(BossLightningCreeperPhantomKnightEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.5F)
                    .clientTrackingRange(10)
                    .build("boss_lightning_creeper_phantom_knight"));

    public static final RegistryObject<EntityType<CreeperSkullProjectile>> CREEPER_SKULL =
            ENTITIES.register("creeper_skull", () -> EntityType.Builder
                    .<CreeperSkullProjectile>of(CreeperSkullProjectile::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(6)
                    .build("creeper_skull"));

    public static final RegistryObject<EntityType<LightningCreeperSkullProjectile>> LIGHTNING_CREEPER_SKULL =
            ENTITIES.register("lightning_creeper_skull", () -> EntityType.Builder
                    .<LightningCreeperSkullProjectile>of(LightningCreeperSkullProjectile::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(6)
                    .build("lightning_creeper_skull"));

    private ModEntities() {
    }
}
