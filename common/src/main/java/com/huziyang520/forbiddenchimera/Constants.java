package com.huziyang520.forbiddenchimera;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared constants for Forbidden Chimera. Keep {@link #MOD_ID} in sync with the
 * {@code assets/<mod_id>} and {@code data/<mod_id>} folders.
 *
 * <p>Ported from the 26.3 multi-loader source. The only change is the id type: 1.20.1 calls it
 * {@link ResourceLocation}, and {@code Identifier} does not exist yet.
 */
public final class Constants {

    public static final String MOD_ID = "forbidden_chimera";
    public static final String MOD_NAME = "Forbidden Chimera";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private Constants() {
    }

    /** Builds a namespaced id inside this mod's namespace. */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
