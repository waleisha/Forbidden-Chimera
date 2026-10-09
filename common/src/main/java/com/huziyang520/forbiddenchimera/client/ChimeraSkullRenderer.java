package com.huziyang520.forbiddenchimera.client;

import com.huziyang520.forbiddenchimera.entity.CreeperSkullProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renderer for both creeper skull missiles, following vanilla 1.20.1 {@code WitherSkullRenderer}
 * line for line.
 *
 * <p>Why not the item renderer: that draws the skull as an <b>item</b> in the ground display transform,
 * so the head neither points along its flight direction nor lines up with the entity's rotation - the
 * player saw the head arriving sideways. Vanilla wither skulls are drawn with the real skull model driven
 * by the entity's own rotation, which is exactly what makes a head face the way it flies (and therefore
 * face the player it homed in on).
 *
 * <p>The lightning variant additionally draws the same model a second time through
 * {@code RenderType.energySwirl(creeper_armor.png, u, v)} - a verbatim copy of vanilla
 * {@code CreeperPowerLayer}: texture {@code textures/entity/creeper/creeper_armor.png}, both uv offsets
 * scrolling at {@code age * 0.01}. Same recipe as the mob's per-bone {@link LightningOverlayGeoLayer},
 * so the missile and its shooter look like the same charged material.
 *
 * <p><b>Porting notes - three real differences from the 26.3 source.</b>
 * <ol>
 *   <li>26.3 renders through a render-state object; 1.20.1 uses the classic immediate-mode
 *       {@link #render} signature, so the rotation is interpolated inline with {@link Mth#rotLerp} /
 *       {@link Mth#lerp} exactly as vanilla does.</li>
 *   <li>There is no {@code translate(0, -1.501, 0)}: that offset belongs to the modern
 *       {@code LivingEntityRenderer} pipeline. Vanilla's 1.20.1 skull renderer applies the mirror scale
 *       and nothing else.</li>
 *   <li>The {@code +180} yaw correction is <b>gone</b>. It existed to compensate for 26.3's
 *       {@code Projectile#shoot} convention ({@code atan2(z, x) - 90}) interacting with the mirror
 *       scale. 1.20.1's convention is {@code atan2(x, z)} with no offset, which is the convention the
 *       mirror scale is designed for - so the entity's rotation is used directly, exactly as vanilla
 *       wither skulls do it.</li>
 * </ol>
 */
public class ChimeraSkullRenderer extends EntityRenderer<CreeperSkullProjectile> {

    /** Vanilla skull skin for the creeper type. */
    private static final ResourceLocation CREEPER_SKIN =
            ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper.png");
    /** Vanilla charged creeper overlay. */
    private static final ResourceLocation POWER_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");
    /** Vanilla scroll speed: {@code t * 0.01} on both uv axes. */
    private static final float SCROLL_PER_TICK = 0.01F;

    private final SkullModel model;
    private final boolean powered;

    public ChimeraSkullRenderer(EntityRendererProvider.Context context, boolean powered) {
        super(context);
        this.model = new SkullModel(context.bakeLayer(ModelLayers.CREEPER_HEAD));
        this.powered = powered;
    }

    /** Both skulls glow like the vanilla wither skull, so they read at night. */
    @Override
    protected int getBlockLightLevel(CreeperSkullProjectile entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(CreeperSkullProjectile entity) {
        return CREEPER_SKIN;
    }

    @Override
    public void render(CreeperSkullProjectile entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        // The mirror scale is what orients the skull model against the entity's rotation, exactly as
        // vanilla's wither skull renderer does it.
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        // No wobble: a missile head is rigid, only its facing changes. Interpolated so it does not snap.
        float yRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        this.model.setupAnim(0.0F, yRot, xRot);

        VertexConsumer base = bufferSource.getBuffer(this.model.renderType(CREEPER_SKIN));
        this.model.renderToBuffer(poseStack, base, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        if (this.powered) {
            float scroll = entity.tickCount * SCROLL_PER_TICK % 1.0F;
            VertexConsumer swirl = bufferSource.getBuffer(RenderType.energySwirl(POWER_LOCATION, scroll, scroll));
            this.model.renderToBuffer(poseStack, swirl, packedLight, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
