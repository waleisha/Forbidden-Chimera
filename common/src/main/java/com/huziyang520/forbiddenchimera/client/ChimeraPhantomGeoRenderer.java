package com.huziyang520.forbiddenchimera.client;

import com.huziyang520.forbiddenchimera.entity.CreeperPhantomEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for every model based chimera: the Creeper Phantom (mob3), the Lightning Creeper Phantom
 * (mob4), the Nuclear Creeper Phantom (mob5), the Enderman Phantom (mob6) and the boss.
 *
 * <p>They share geometry and animation plumbing, so this renderer takes the geometry, the texture, the
 * animation set and which bones carry the charged overlay as arguments instead of being copied five
 * times.
 *
 * <p><b>Porting note - the largest client change.</b> GeckoLib 5 (26.3) renders through the vanilla
 * render-state pipeline ({@code GeoEntityRenderer<T, LivingEntityRenderState>}, {@code GeoRenderState},
 * {@code RenderPassInfo}, {@code SubmitNodeCollector}), and layers there are typed against that state.
 * GeckoLib 4 has none of it: {@code GeoEntityRenderer<T>} extends the classic {@code EntityRenderer<T>}
 * and the model is resolved straight from the animatable, so this class no longer needs a state type
 * parameter at all. The layers follow suit - see {@link LightningOverlayGeoLayer}.
 */
public class ChimeraPhantomGeoRenderer extends GeoEntityRenderer<CreeperPhantomEntity> {

    /**
     * Bones that make up the <b>creeper</b> part of the model - the charged overlay is applied exactly
     * to these, so the phantom wings and tail stay plain.
     *
     * <p>Verified against {@code creeper_phantom.geo.json} (parsed, not eyeballed):
     * <ul>
     *   <li>{@code body} - the torso: 6x5x16 at UV (0,64) plus 5x3.5x2 at UV (0,64). This is the part
     *       the model reads as the chimera's body, so it is charged;</li>
     *   <li>{@code head} - 8x8x8 with the vanilla creeper head UV layout, which is why vanilla
     *       {@code creeper_armor.png} maps onto it correctly;</li>
     *   <li>wings sit at UV (96,12)/(102,12)/(64,64) and the tail at (96,12)/(64,64) - never charged.</li>
     * </ul>
     */
    private static final String[] CREEPER_BONES = {"body", "head"};

    /**
     * The boss's equivalent: it is the same model with every bone renamed with a {@code boss_} prefix.
     * {@code boss_body} is the same phantom torso and {@code boss_head} the same creeper head, so the
     * vanilla {@code creeper_armor.png} swirl maps onto both exactly as it does for mob4. The warden and
     * dragon heads are deliberately <b>not</b> charged.
     */
    public static final String[] BOSS_CHARGED_BONES = {"boss_body", "boss_head"};

    /** Nothing charged: for variants that keep the plain texture. */
    private static final String[] NO_BONES = new String[0];

    /** Vanilla phantom sized blob: the shadow every chimera had before the boss existed. */
    private static final float DEFAULT_SHADOW_RADIUS = 0.75F;

    /** Creeper family: shares {@code creeper_phantom} geometry. */
    public ChimeraPhantomGeoRenderer(EntityRendererProvider.Context context, ResourceLocation texture,
                                     ResourceLocation animation, boolean charged) {
        this(context, ChimeraPhantomGeoModel.CREEPER_GEOMETRY, texture, animation,
                charged ? CREEPER_BONES : NO_BONES, DEFAULT_SHADOW_RADIUS);
    }

    /** Creeper family with an explicit geometry (the enderman swaps the body and head). */
    public ChimeraPhantomGeoRenderer(EntityRendererProvider.Context context, ResourceLocation geometry,
                                     ResourceLocation texture, ResourceLocation animation, boolean charged) {
        this(context, geometry, texture, animation, charged ? CREEPER_BONES : NO_BONES,
                DEFAULT_SHADOW_RADIUS);
    }

    /**
     * The canonical constructor.
     *
     * @param chargedBones bones that get the vanilla charged creeper swirl added on top of their normal
     *                     texture; empty means "no overlay".
     * @param shadowRadius vanilla shadow size, which has to grow with the model.
     */
    public ChimeraPhantomGeoRenderer(EntityRendererProvider.Context context, ResourceLocation geometry,
                                     ResourceLocation texture, ResourceLocation animation, String[] chargedBones,
                                     float shadowRadius) {
        super(context, new ChimeraPhantomGeoModel(geometry, texture, animation));
        this.shadowRadius = shadowRadius;
        for (String bone : chargedBones) {
            this.addRenderLayer(new LightningOverlayGeoLayer<>(this, bone));
        }
    }
}
