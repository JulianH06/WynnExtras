// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — MapTexture (phase 8b).
 *
 * A single downloaded map tile, lazily registered with the TextureManager on first render.
 *
 * Yarn adaptation: Wynntils uses blaze3d NativeImage + DynamicTexture + RenderSystem.getSamplerCache()
 * with CLAMP_TO_EDGE / NEAREST. On Yarn 1.21.11 that maps to net.minecraft.client.texture.NativeImage
 * + NativeImageBackedTexture (a DynamicTexture) + TextureManager.registerTexture(Identifier,
 * AbstractTexture). The explicit sampler-cache/AddressMode/FilterMode configuration is DROPPED
 * (no clean Yarn equivalent surfaced via javap on build.4) — the default sampler renders fine
 * through the GUI_TEXTURED pipeline; only the (minor) filtering fidelity differs.
 */
package julianh06.wynnextras.wtshim.services.map;

import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import julianh06.wynnextras.wtshim.utils.type.BoundingBox;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class MapTexture {
    private final NativeImage texture;
    private final Identifier mapIdentifier;

    private boolean registered = false;

    private final String name;

    private final int x1;
    private final int z1;
    private final int x2;
    private final int z2;

    private final int textureWidth;
    private final int textureHeight;

    public MapTexture(String name, NativeImage texture, int x1, int z1, int x2, int z2) {
        this.name = name;
        this.texture = texture;
        this.x1 = x1;
        this.z1 = z1;
        this.x2 = x2;
        this.z2 = z2;
        this.textureWidth = texture.getWidth();
        this.textureHeight = texture.getHeight();

        // md5-named .png; lowercase hex is a valid Identifier path
        this.mapIdentifier = Identifier.of("wynnextras", "maps/" + name.toLowerCase());
    }

    public Identifier identifier() {
        if (!registered) {
            registered = true;
            NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> name, texture);
            McUtils.mc().getTextureManager().registerTexture(mapIdentifier, tex);
        }

        return mapIdentifier;
    }

    public float getTextureXPosition(double posX) {
        return (float) (posX - x1);
    }

    public float getTextureZPosition(double posZ) {
        return (float) (posZ - z1);
    }

    public int getX1() {
        return x1;
    }

    public int getZ1() {
        return z1;
    }

    public int getX2() {
        return x2;
    }

    public int getZ2() {
        return z2;
    }

    public BoundingBox getBox() {
        return new BoundingBox(x1, z1, x2, z2);
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public int getTextureWidth() {
        return textureWidth;
    }
}
