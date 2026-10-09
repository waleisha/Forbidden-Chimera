package com.huziyang520.forbiddenchimera.client;

import com.huziyang520.forbiddenchimera.entity.CreeperPhantomEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Vanilla charged creeper effect: the creeper bones keep their normal texture and get the energy swirl
 * <b>added on top</b>. Exact same recipe as vanilla {@code CreeperPowerLayer}: texture
 * {@code textures/entity/creeper/creeper_armor.png} through
 * {@code RenderType.energySwirl(texture, u, v)} with both offsets scrolling at {@code age * 0.01}, and
 * the vanilla {@code 0.5} grey tint.
 *
 * <p><b>Why {@code renderCubesOfBone} and not {@code renderRecursively}.</b> This is the difference
 * between an overlay and a stack overflow. {@code GeoRenderer#renderRecursively} runs the render-layer
 * pass for the bone as part of its own work, so calling it from inside a layer calls straight back into
 * this method:
 * <pre>
 *   renderRecursively -> applyRenderLayersForBone -> renderForBone -> renderRecursively -> ...
 * </pre>
 * which ends in {@code java.lang.StackOverflowError: Rendering entity in world}. The 26.3 source got
 * this for free because GeckoLib 5 shipped {@code CustomBoneTextureGeoLayer}; GeckoLib 4 has no such
 * class, so the per-bone re-draw is written here directly - and it must use
 * {@code renderCubesOfBone}, which draws geometry only and never re-enters the layer pass.
 *
 * <p>Drawing the bone's children too is harmless here: every bone in these models is a flat sibling
 * under {@code root} (body and head are leaves), so this renders exactly the target bone's cubes.
 */
public class LightningOverlayGeoLayer<T extends CreeperPhantomEntity> extends GeoRenderLayer<T> {

    /** Identical to vanilla {@code CreeperPowerLayer#POWER_LOCATION}. */
    private static final ResourceLocation POWER_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");

    /** Vanilla scroll speed: {@code t * 0.01} for both uv axes. */
    private static final float SCROLL_PER_TICK = 0.01F;

    /**
     * Vanilla {@code EnergySwirlLayer} tint: a mid grey, applied on top of the armour texture so the
     * swirl reads as an overlay rather than a flat colour.
     */
    private static final float SWIRL_TINT = 0.5F;

    /** The single bone this layer instance is responsible for. */
    private final String boneName;

    public LightningOverlayGeoLayer(GeoRenderer<T> renderer, String boneName) {
        super(renderer);
        this.boneName = boneName;
    }

    /**
     * Draws this bone a second time with the vanilla energy swirl, leaving the normal pass untouched.
     *
     * <p>The pose stack handed in is already positioned for this bone (GeckoLib prepares the matrix
     * before running the layer pass), so the cubes land exactly on top of the ones the base pass drew.
     */
    @Override
    public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        if (!this.boneName.equals(bone.getName())) {
            return;
        }

        // The age comes from the animatable itself: GeckoLib 4 has no render-state object to carry it.
        float scroll = animatable.tickCount * SCROLL_PER_TICK % 1.0F;
        RenderType swirl = RenderType.energySwirl(POWER_LOCATION, scroll, scroll);

        this.getRenderer().renderCubesOfBone(poseStack, bone, bufferSource.getBuffer(swirl),
                packedLight, packedOverlay, SWIRL_TINT, SWIRL_TINT, SWIRL_TINT, 1.0F);
    }

    /** Nothing has to be prepared ahead of the pass; the bone must stay visible with its normal texture. */
    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType,
                          MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                          int packedLight, int packedOverlay) {
        // Intentionally empty.
    }
}
