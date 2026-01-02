package julianh06.wynnextras.features.crafting.data;

import net.minecraft.util.Identifier;

public enum Grains implements Material {
    WHEAT("Wheat", Identifier.of("wynnextras", "textures/materials/grains/wheat.png")),
    BARLEY("Barley", Identifier.of("wynnextras", "textures/materials/grains/barley.png")),
    OAT("Oat", Identifier.of("wynnextras", "textures/materials/grains/oat.png")),
    MALT("Malt", Identifier.of("wynnextras", "textures/materials/grains/malt.png")),
    HOPS("Hops", Identifier.of("wynnextras", "textures/materials/grains/hops.png")),
    RYE("Rye", Identifier.of("wynnextras", "textures/materials/grains/rye.png")),
    MILLET("Millet", Identifier.of("wynnextras", "textures/materials/grains/millet.png")),
    DECAY("Decay", Identifier.of("wynnextras", "textures/materials/grains/decay.png")),
    RICE("Rice", Identifier.of("wynnextras", "textures/materials/grains/rice.png")),
    SORGHUM("Sorghum", Identifier.of("wynnextras", "textures/materials/grains/sorghum.png")),
    HEMP("Hemp", Identifier.of("wynnextras", "textures/materials/grains/hemp.png")),
    DERNIC("Dernic", Identifier.of("wynnextras", "textures/materials/grains/dernic.png"));

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
