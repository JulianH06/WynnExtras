package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import net.minecraft.util.Identifier;

public enum Meat implements IMaterial {
    GUDGEON("Gudgeon", Identifier.of("wynnextras", "textures/materials/meat/pink.png")),
    TROUT("Trout", Identifier.of("wynnextras", "textures/materials/meat/blue.png")),
    SALMON("Salmon", Identifier.of("wynnextras", "textures/materials/meat/filet.png")),
    CARP("Carp", Identifier.of("wynnextras", "textures/materials/meat/yellow.png")),
    ICEFISH("Icefish", Identifier.of("wynnextras", "textures/materials/meat/blue.png")),
    PIRANHA("Piranha", Identifier.of("wynnextras", "textures/materials/meat/blue.png")),
    KOI("Koi", Identifier.of("wynnextras", "textures/materials/meat/pink.png")),
    GYLIA("Gylia", Identifier.of("wynnextras", "textures/materials/meat/pink.png")),
    BASS("Bass", Identifier.of("wynnextras", "textures/materials/meat/black.png")),
    MOLTEN("Molten", Identifier.of("wynnextras", "textures/materials/meat/filet.png")),
    STARFISH("Starfish", Identifier.of("wynnextras", "textures/materials/meat/yellow.png")),
    DERNIC("Dernic", Identifier.of("wynnextras", "textures/materials/meat/black.png"));

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
