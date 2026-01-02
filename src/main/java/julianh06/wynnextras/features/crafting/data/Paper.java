package julianh06.wynnextras.features.crafting.data;

import net.minecraft.util.Identifier;

public enum Paper implements Material {
    OAK("Oak",Identifier.of("wynnextras", "textures/materials/paper/white.png")),
    BIRCH("Birch", Identifier.of("wynnextras", "textures/materials/paper/white.png")),
    WILLOW("Willow", Identifier.of("wynnextras", "textures/materials/paper/cream.png")),
    ACACIA("Acacia", Identifier.of("wynnextras", "textures/materials/paper/cream.png")),
    SPRUCE("Spruce", Identifier.of("wynnextras", "textures/materials/paper/brown.png")),
    JUNGLE("Jungle", Identifier.of("wynnextras", "textures/materials/paper/cream.png")),
    DARK("Dark", Identifier.of("wynnextras", "textures/materials/paper/brown.png")),
    LIGHT("Light", Identifier.of("wynnextras", "textures/materials/paper/white.png")),
    PINE("Pine", Identifier.of("wynnextras", "textures/materials/paper/brown.png")),
    AVO("Avo", Identifier.of("wynnextras", "textures/materials/paper/cream.png")),
    SKY("Sky", Identifier.of("wynnextras", "textures/materials/paper/white.png")),
    DERNIC("Dernic", Identifier.of("wynnextras", "textures/materials/paper/purple.png"));

    private final String name;
    private final Identifier texture;

    Paper(String name, Identifier texture) {
        this.name = name;
        this.texture = texture;
    }

    @Override
    public String getName() {
        return name + " Paper";
    }

    @Override
    public Identifier getTexture() {
        return texture;
    }
}
