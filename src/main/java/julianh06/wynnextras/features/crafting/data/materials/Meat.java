package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum Meat implements IMaterial {
    GUDGEON("Gudgeon",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_gudgeon.png"),
            Identifier.of("wynnextras", "textures/materials/meat/pink.png")),
    TROUT("Trout",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_trout.png"),
            Identifier.of("wynnextras", "textures/materials/meat/blue.png")),
    SALMON("Salmon",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_salmon.png"),
            Identifier.of("wynnextras", "textures/materials/meat/filet.png")),
    CARP("Carp",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_carp.png"),
            Identifier.of("wynnextras", "textures/materials/meat/yellow.png")),
    ICEFISH("Icefish",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_ice.png"),
            Identifier.of("wynnextras", "textures/materials/meat/blue.png")),
    PIRANHA("Piranha",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_piranha.png"),
            Identifier.of("wynnextras", "textures/materials/meat/blue.png")),
    KOI("Koi",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_koi.png"),
            Identifier.of("wynnextras", "textures/materials/meat/pink.png")),
    GYLIA("Gylia",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_gylia.png"),
            Identifier.of("wynnextras", "textures/materials/meat/pink.png")),
    BASS("Bass",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_bass.png"),
            Identifier.of("wynnextras", "textures/materials/meat/black.png")),
    MOLTEN("Molten",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_molten.png"),
            Identifier.of("wynnextras", "textures/materials/meat/filet.png")),
    STARFISH("Starfish",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_starfish.png"),
            Identifier.of("wynnextras", "textures/materials/meat/yellow.png")),
    DERNIC("Dernic",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_dernic.png"),
            Identifier.of("wynnextras", "textures/materials/meat/black.png")),
    STURGEON("Sturgeon",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_sturgeon.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/dernic.png")),
    MAHSEER("Mahseer",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_mahseer.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/dernic.png"));

    private final String name;
    private final Identifier serverTexture;
    private final Identifier fallbackTexture;

    Meat(String name, Identifier serverTexture, Identifier fallbackTexture) {
        this.name = name;
        this.serverTexture = serverTexture;
        this.fallbackTexture = fallbackTexture;
    }

    @Override
    public String getName() {
        return name + " Meat";
    }

    @Override
    public Identifier getTexture() {
        return MaterialTextureResolver.resolve(serverTexture, fallbackTexture);
    }
}
