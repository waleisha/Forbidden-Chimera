package com.huziyang520.forbiddenchimera;

import com.huziyang520.forbiddenchimera.client.ChimeraPhantomGeoModel;
import com.huziyang520.forbiddenchimera.client.ChimeraPhantomGeoRenderer;
import com.huziyang520.forbiddenchimera.client.ChimeraSkullRenderer;
import com.huziyang520.forbiddenchimera.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only renderer registration.
 *
 * <p>{@code value = Dist.CLIENT} keeps this class out of the dedicated server, where the client renderer
 * types do not exist.
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForbiddenChimeraClient {

    private ForbiddenChimeraClient() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // mob1/mob2 are vanilla phantoms: the vanilla PhantomRenderer draws them, with the vanilla
        // geometry, and nothing needs to be registered or patched for them.
        //
        // The creeper / lightning / nuclear variants share geometry, animation and texture; the charged
        // variant only adds the vanilla charged creeper overlay on the creeper bones.
        event.registerEntityRenderer(ModEntities.CREEPER_PHANTOM.get(),
                context -> new ChimeraPhantomGeoRenderer(context, ChimeraPhantomGeoModel.PLAIN_TEXTURE,
                        ChimeraPhantomGeoModel.PLAIN_ANIMATION, false));
        event.registerEntityRenderer(ModEntities.LIGHTNING_CREEPER_PHANTOM.get(),
                context -> new ChimeraPhantomGeoRenderer(context, ChimeraPhantomGeoModel.PLAIN_TEXTURE,
                        ChimeraPhantomGeoModel.PLAIN_ANIMATION, true));
        event.registerEntityRenderer(ModEntities.NUCLEAR_CREEPER_PHANTOM.get(),
                context -> new ChimeraPhantomGeoRenderer(context, ChimeraPhantomGeoModel.NUCLEAR_TEXTURE,
                        ChimeraPhantomGeoModel.NUCLEAR_ANIMATION, false));
        // The enderman variant has its own geometry (enderman body and head) but the shared animation
        // set: the wing and tail bone names are identical, so no new animations are needed.
        event.registerEntityRenderer(ModEntities.ENDERMAN_PHANTOM.get(),
                context -> new ChimeraPhantomGeoRenderer(context, ChimeraPhantomGeoModel.ENDERMAN_GEOMETRY,
                        ChimeraPhantomGeoModel.ENDERMAN_TEXTURE, ChimeraPhantomGeoModel.PLAIN_ANIMATION,
                        false));
        // The boss has its own geometry, texture and seven clip animation set; it keeps the vanilla
        // charged swirl on its creeper parts (the boss_ bones) and gets a bigger shadow to match.
        event.registerEntityRenderer(ModEntities.BOSS.get(),
                context -> new ChimeraPhantomGeoRenderer(context, ChimeraPhantomGeoModel.BOSS_GEOMETRY,
                        ChimeraPhantomGeoModel.BOSS_TEXTURE, ChimeraPhantomGeoModel.BOSS_ANIMATION,
                        ChimeraPhantomGeoRenderer.BOSS_CHARGED_BONES, 1.5F));
        // Both explosive skulls use the vanilla skull model (not the item renderer), so the head points
        // along its flight direction; the lightning one adds the vanilla energy swirl on top.
        event.registerEntityRenderer(ModEntities.CREEPER_SKULL.get(),
                context -> new ChimeraSkullRenderer(context, false));
        event.registerEntityRenderer(ModEntities.LIGHTNING_CREEPER_SKULL.get(),
                context -> new ChimeraSkullRenderer(context, true));
    }
}
