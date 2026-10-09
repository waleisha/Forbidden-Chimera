package com.huziyang520.forbiddenchimera;

import com.huziyang520.forbiddenchimera.config.ForbiddenChimeraConfig;
import com.huziyang520.forbiddenchimera.registry.ModEntities;
import com.huziyang520.forbiddenchimera.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Forge entry point.
 *
 * <p>Ported from the 26.3 multi-loader source, where this work was split between a loader independent
 * {@code ForbiddenChimera#init} plus a Fabric and a NeoForge module. The 1.20.1 line only targets Forge,
 * so the {@code Services}/{@code IPlatformHelper} indirection is unnecessary and registration happens
 * directly here - the shared logic itself still lives in the {@code common} module.
 */
@Mod(Constants.MOD_ID)
public final class ForbiddenChimeraForge {

    public ForbiddenChimeraForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);

        // Attribute suppliers must be attached through the event or spawning one of these entity types
        // crashes the game - none of them exist in vanilla DefaultAttributes.
        modBus.addListener(ChimeraForgeEvents::onEntityAttributeCreation);
        modBus.addListener(ChimeraForgeEvents::onBuildCreativeTabContents);

        // First launch writes config/forbidden_chimera.json; it is plain JSON, open it in any editor.
        ForbiddenChimeraConfig.load(FMLPaths.CONFIGDIR.get());

        Constants.LOG.info("Forbidden Chimera loaded on Forge 1.20.1");
    }
}
