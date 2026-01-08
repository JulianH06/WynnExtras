package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import net.minecraft.util.Identifier;

public enum StringMaterial implements IMaterial {
    WHEAT("Wheat", Identifier.of("wynnextras", "textures/materials/string/white.png")),
    BARLEY("Barley", Identifier.of("wynnextras", "textures/materials/string/white.png")),
    OAT("Oat", Identifier.of("wynnextras", "textures/materials/string/green.png")),
    MALT("Malt", Identifier.of("wynnextras", "textures/materials/string/white.png")),
    HOPS("Hops", Identifier.of("wynnextras", "textures/materials/string/green.png")),
    RYE("Rye", Identifier.of("wynnextras", "textures/materials/string/white.png")),
    MILLET("Millet", Identifier.of("wynnextras", "textures/materials/string/white.png")),
    DECAY("Decay", Identifier.of("wynnextras", "textures/materials/string/brown.png")),
    RICE("Rice", Identifier.of("wynnextras", "textures/materials/string/white.png")),
    SORGHUM("Sorghum", Identifier.of("wynnextras", "textures/materials/string/yellow.png")),
    HEMP("Hemp", Identifier.of("wynnextras", "textures/materials/string/green.png")),
    DERNIC("Dernic", Identifier.of("wynnextras", "textures/materials/string/brown.png"));

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
