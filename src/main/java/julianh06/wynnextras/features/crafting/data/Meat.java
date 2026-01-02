package julianh06.wynnextras.features.crafting.data;

import net.minecraft.util.Identifier;

public enum Meat implements Material {
    GUDGEON("Gudgeon", Identifier.of("wynnextras", "textures/materials/meat/gudgeon.png")),
    TROUT("Trout", Identifier.of("wynnextras", "textures/materials/meat/trout.png")),
    SALMON("Salmon", Identifier.of("wynnextras", "textures/materials/meat/salmon.png")),
    CARP("Carp", Identifier.of("wynnextras", "textures/materials/meat/carp.png")),
    ICEFISH("Icefish", Identifier.of("wynnextras", "textures/materials/meat/icefish.png")),
    PIRANHA("Piranha", Identifier.of("wynnextras", "textures/materials/meat/piranha.png")),
    KOI("Koi", Identifier.of("wynnextras", "textures/materials/meat/koi.png")),
    GYLIA("Gylia", Identifier.of("wynnextras", "textures/materials/meat/gylia.png")),
    BASS("Bass", Identifier.of("wynnextras", "textures/materials/meat/bass.png")),
    MOLTEN("Molten", Identifier.of("wynnextras", "textures/materials/meat/molten.png")),
    STARFISH("Starfish", Identifier.of("wynnextras", "textures/materials/meat/starfish.png")),
    DERNIC("Dernic", Identifier.of("wynnextras", "textures/materials/meat/dernic.png"));

    private final String name;
    private final Identifier texture;

    Meat(String name, Identifier texture) {
        this.name = name;
        this.texture = texture;
    }

    @Override
    public String getName() {
        return name + " Meat";
    }

    @Override
    public Identifier getTexture() {
        return texture;
    }
}
