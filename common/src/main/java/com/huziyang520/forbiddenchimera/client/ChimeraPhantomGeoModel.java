package com.huziyang520.forbiddenchimera.client;

import com.huziyang520.forbiddenchimera.Constants;
import com.huziyang520.forbiddenchimera.entity.CreeperPhantomEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model shared by every model based chimera.
 *
 * <p>Geometry, texture and animation set are all constructor parameters:
 * <ul>
 *   <li>the creeper / lightning / nuclear variants share one geometry and differ in texture and (for
 *       the nuclear one) animation set;</li>
 *   <li>the enderman variant reuses the <b>same bone names and the same wings and tail</b> but swaps the
 *       body and head, so it needs its own geometry file while still playing the shared animation set -
 *       which only works because the bone names are identical.</li>
 * </ul>
 *
 * <p><b>Porting note - asset paths.</b> GeckoLib 5 (26.3) moved its assets under a {@code geckolib/}
 * root and derived the cache key by stripping the folder and extension from a bare name. GeckoLib 4
 * (1.20.1) does the opposite: it takes the <b>full</b> path inside the namespace, so
 * {@code getModelResource} returns {@code <ns>:geo/entity/<name>.geo.json} and the animation resource
 * {@code <ns>:animations/entity/<name>.animation.json}. The files were moved to match.
 */
public class ChimeraPhantomGeoModel extends GeoModel<CreeperPhantomEntity> {

    /** Geometry of the creeper chimera family; also used by the lightning and nuclear variants. */
    public static final ResourceLocation CREEPER_GEOMETRY = Constants.id("geo/entity/creeper_phantom.geo.json");
    /** Geometry of the enderman chimera: identical wings and tail, enderman body and head. */
    public static final ResourceLocation ENDERMAN_GEOMETRY = Constants.id("geo/entity/enderman_phantom.geo.json");
    /** Geometry of the boss: creeper phantom base plus the head cluster and rocket. */
    public static final ResourceLocation BOSS_GEOMETRY =
            Constants.id("geo/entity/boss_lightning_creeper_phantom_knight.geo.json");

    /** Animation set of the creeper phantom and its lightning sibling. */
    public static final ResourceLocation PLAIN_ANIMATION =
            Constants.id("animations/entity/creeper_phantom.animation.json");
    /** Animation set of the nuclear variant (fly / hover / charge / detonate). */
    public static final ResourceLocation NUCLEAR_ANIMATION =
            Constants.id("animations/entity/nuclear_creeper_phantom.animation.json");
    /** Animation set of the boss (fly / dive / volley / dragon breath / sonic boom / darkness / dash). */
    public static final ResourceLocation BOSS_ANIMATION =
            Constants.id("animations/entity/boss_lightning_creeper_phantom_knight.animation.json");

    /** Shared texture of the creeper phantom and its lightning sibling. */
    public static final ResourceLocation PLAIN_TEXTURE = Constants.id("textures/entity/creeper_phantom.png");
    /** Nuclear variant: same pattern, red filtered on the creeper torso and head only. */
    public static final ResourceLocation NUCLEAR_TEXTURE = Constants.id("textures/entity/nuclear_creeper_phantom.png");
    /** Enderman variant: vanilla enderman skin on the body and head, phantom pixels untouched. */
    public static final ResourceLocation ENDERMAN_TEXTURE = Constants.id("textures/entity/enderman_phantom.png");
    /** Boss texture: creeper phantom base plus the vanilla head textures it assembles. */
    public static final ResourceLocation BOSS_TEXTURE =
            Constants.id("textures/entity/boss_lightning_creeper_phantom_knight.png");

    private final ResourceLocation geometry;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    /** Creeper family convenience constructor: the shared geometry is implied. */
    public ChimeraPhantomGeoModel(ResourceLocation texture, ResourceLocation animation) {
        this(CREEPER_GEOMETRY, texture, animation);
    }

    public ChimeraPhantomGeoModel(ResourceLocation geometry, ResourceLocation texture, ResourceLocation animation) {
        this.geometry = geometry;
        this.texture = texture;
        this.animation = animation;
    }

    @Override
    public ResourceLocation getModelResource(CreeperPhantomEntity animatable) {
        return this.geometry;
    }

    @Override
    public ResourceLocation getTextureResource(CreeperPhantomEntity animatable) {
        return this.texture;
    }

    @Override
    public ResourceLocation getAnimationResource(CreeperPhantomEntity animatable) {
        return this.animation;
    }
}
