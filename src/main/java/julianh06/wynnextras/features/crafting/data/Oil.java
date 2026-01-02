package julianh06.wynnextras.features.crafting.data;

import net.minecraft.util.Identifier;

public enum Oil implements Material {
    GUDGEON("Gudgeon",Identifier.of("wynnextras", "textures/materials/oil/black.png")),
    TROUT("Trout", Identifier.of("wynnextras", "textures/materials/oil/blue.png")),
    SALMON("Salmon", Identifier.of("wynnextras", "textures/materials/oil/red.png")),
    CARP("Carp", Identifier.of("wynnextras", "textures/materials/oil/green.png")),
    ICEFISH("Icefish", Identifier.of("wynnextras", "textures/materials/oil/blue.png")),
    PIRANHA("Piranha", Identifier.of("wynnextras", "textures/materials/oil/orange.png")),
    KOI("Koi", Identifier.of("wynnextras", "textures/materials/oil/red.png")),
    GYLIA("Gylia", Identifier.of("wynnextras", "textures/materials/oil/red.png")),
    BASS("Bass", Identifier.of("wynnextras", "textures/materials/oil/blue.png")),
    MOLTEN("Molten", Identifier.of("wynnextras", "textures/materials/oil/orange.png")),
    STARFISH("Starfish", Identifier.of("wynnextras", "textures/materials/oil/green.png")),
    DERNIC("Dernic", Identifier.of("wynnextras", "textures/materials/oil/black.png"));

    private final String name;
    private final Identifier texture;

    Oil(String name, Identifier texture) {
        this.name = name;
        this.texture = texture;
    }

    @Override
    public String getName() {
        return name + " Oil";
    }

    @Override
    public Identifier getTexture() {
        return texture;
    }
}
