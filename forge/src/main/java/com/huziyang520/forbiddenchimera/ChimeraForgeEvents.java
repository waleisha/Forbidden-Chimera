package com.huziyang520.forbiddenchimera;

import com.huziyang520.forbiddenchimera.entity.BossLightningCreeperPhantomKnightEntity;
import com.huziyang520.forbiddenchimera.entity.CreeperPhantomEntity;
import com.huziyang520.forbiddenchimera.entity.EndermanPhantomEntity;
import com.huziyang520.forbiddenchimera.entity.LightningCreeperPhantomEntity;
import com.huziyang520.forbiddenchimera.entity.NuclearCreeperPhantomEntity;
import com.huziyang520.forbiddenchimera.registry.ModAttributes;
import com.huziyang520.forbiddenchimera.registry.ModEntities;
import com.huziyang520.forbiddenchimera.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

/**
 * The Forge event handlers that tie the shared registries into the loader.
 *
 * <p>Two things happen here that the 26.3 build did per-loader, with the NeoForge half as the template:
 * attribute registration and creative tab population.
 */
public final class ChimeraForgeEvents {

    private ChimeraForgeEvents() {
    }

    /**
     * Registers the attribute suppliers for every chimera entity type.
     *
     * <p>mob1/mob2 are vanilla phantoms and already have vanilla attributes, so they are deliberately
     * absent.
     */
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CREEPER_PHANTOM.get(), ModAttributes.creeperPhantom());
        event.put(ModEntities.LIGHTNING_CREEPER_PHANTOM.get(), ModAttributes.lightningCreeperPhantom());
        event.put(ModEntities.NUCLEAR_CREEPER_PHANTOM.get(), ModAttributes.nuclearCreeperPhantom());
        event.put(ModEntities.ENDERMAN_PHANTOM.get(), ModAttributes.endermanPhantom());
        event.put(ModEntities.BOSS.get(), ModAttributes.bossLightningCreeperPhantomKnight());
    }

    /**
     * Adds the mod's items to the vanilla tabs.
     *
     * <p>The six spawn eggs go into the spawn egg tab and the summon item into ingredients, matching the
     * 26.3 layout: eggs are found where a player looks for eggs, and the boss summon item is a crafting
     * ingredient.
     */
    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            for (Item egg : spawnEggs()) {
                event.accept(egg);
            }
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.FORBIDDEN_PLANK.get());
        }
    }

    /** Every spawn egg, including the two variants that share no entity type of their own. */
    private static List<Item> spawnEggs() {
        List<Item> eggs = new ArrayList<>(7);
        eggs.add(ModItems.PHANTOM_RIDER_CREEPER_SPAWN_EGG.get());
        eggs.add(ModItems.CREEPER_THROWER_PHANTOM_SPAWN_EGG.get());
        eggs.add(ModItems.CREEPER_PHANTOM_SPAWN_EGG.get());
        eggs.add(ModItems.LIGHTNING_CREEPER_PHANTOM_SPAWN_EGG.get());
        eggs.add(ModItems.NUCLEAR_CREEPER_PHANTOM_SPAWN_EGG.get());
        eggs.add(ModItems.ENDERMAN_PHANTOM_SPAWN_EGG.get());
        eggs.add(ModItems.BOSS_SPAWN_EGG.get());
        return eggs;
    }
}
