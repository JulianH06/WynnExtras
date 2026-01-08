package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import net.minecraft.util.Identifier;

public enum Grains implements IMaterial {
    WHEAT("Wheat", Identifier.of("wynnextras", "textures/materials/grains/yellow.png")),
    BARLEY("Barley", Identifier.of("wynnextras", "textures/materials/grains/yellow.png")),
    OAT("Oat", Identifier.of("wynnextras", "textures/materials/grains/green.png")),
    MALT("Malt", Identifier.of("wynnextras", "textures/materials/grains/yellow.png")),
    HOPS("Hops", Identifier.of("wynnextras", "textures/materials/grains/green.png")),
    RYE("Rye", Identifier.of("wynnextras", "textures/materials/grains/yellow.png")),
    MILLET("Millet", Identifier.of("wynnextras", "textures/materials/grains/yellow.png")),
    DECAY("Decay", Identifier.of("wynnextras", "textures/materials/grains/brown.png")),
    RICE("Rice", Identifier.of("wynnextras", "textures/materials/grains/yellow.png")),
    SORGHUM("Sorghum", Identifier.of("wynnextras", "textures/materials/grains/brown.png")),
    HEMP("Hemp", Identifier.of("wynnextras", "textures/materials/grains/green.png")),
    DERNIC("Dernic", Identifier.of("wynnextras", "textures/materials/grains/brown.png"));

    private final String name;
    private final Identifier texture;

    Grains(String name, Identifier texture) {
        this.name = name;
        this.texture = texture;
    }

    @Override
    public String getName() {
        return name + " Grains";
    }

    @Override
    public Identifier getTexture() {
        return texture;
    }
}
