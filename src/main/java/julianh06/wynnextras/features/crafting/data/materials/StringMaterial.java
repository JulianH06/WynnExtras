package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import net.minecraft.util.Identifier;

public enum StringMaterial implements IMaterial {
    WHEAT("Wheat", Identifier.of("wynnextras", "textures/materials/string/wheat.png")),
    BARLEY("Barley", Identifier.of("wynnextras", "textures/materials/string/barley.png")),
    OAT("Oat", Identifier.of("wynnextras", "textures/materials/string/oat.png")),
    MALT("Malt", Identifier.of("wynnextras", "textures/materials/string/malt.png")),
    HOPS("Hops", Identifier.of("wynnextras", "textures/materials/string/hops.png")),
    RYE("Rye", Identifier.of("wynnextras", "textures/materials/string/rye.png")),
    MILLET("Millet", Identifier.of("wynnextras", "textures/materials/string/millet.png")),
    DECAY("Decay", Identifier.of("wynnextras", "textures/materials/string/decay.png")),
    RICE("Rice", Identifier.of("wynnextras", "textures/materials/string/rice.png")),
    SORGHUM("Sorghum", Identifier.of("wynnextras", "textures/materials/string/sorghum.png")),
    HEMP("Hemp", Identifier.of("wynnextras", "textures/materials/string/hemp.png")),
    DERNIC("Dernic", Identifier.of("wynnextras", "textures/materials/string/dernic.png"));

    private final String name;
    private final Identifier texture;

    StringMaterial(String name, Identifier texture) {
        this.name = name;
        this.texture = texture;
    }

    @Override
    public String getName() {
        return name + " String";
    }

    @Override
    public Identifier getTexture() {
        return texture;
    }
}
