package julianh06.wynnextras.features.crafting.data;

import net.minecraft.util.Identifier;

public enum Wood implements Material {
    OAK("Oak",Identifier.of("wynnextras", "textures/materials/wood/oak.png")),
    BIRCH("Birch", Identifier.of("wynnextras", "textures/materials/wood/birch.png")),
    WILLOW("Willow", Identifier.of("wynnextras", "textures/materials/wood/willow.png")),
    ACACIA("Acacia", Identifier.of("wynnextras", "textures/materials/wood/acacia.png")),
    SPRUCE("Spruce", Identifier.of("wynnextras", "textures/materials/wood/spruce.png")),
    JUNGLE("Jungle", Identifier.of("wynnextras", "textures/materials/wood/jungle.png")),
    DARK("Dark", Identifier.of("wynnextras", "textures/materials/wood/dark.png")),
    LIGHT("Light", Identifier.of("wynnextras", "textures/materials/wood/light.png")),
    PINE("Pine", Identifier.of("wynnextras", "textures/materials/wood/pine.png")),
    AVO("Avo", Identifier.of("wynnextras", "textures/materials/wood/avo.png")),
    SKY("Sky", Identifier.of("wynnextras", "textures/materials/wood/sky.png")),
    DERNIC("Dernic", Identifier.of("wynnextras", "textures/materials/wood/dernic.png"));

    private final String name;
    private final Identifier texture;

    Wood(String name, Identifier texture) {
        this.name = name;
        this.texture = texture;
    }

    @Override
    public String getName() {
        return name + " Wood";
    }

    @Override
    public Identifier getTexture() {
        return texture;
    }
}
