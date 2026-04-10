package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum Oil implements IMaterial {
    GUDGEON("Gudgeon",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_gudgeon.png"),
            Identifier.of("wynnextras", "textures/materials/oil/black.png")),
    TROUT("Trout",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_trout.png"),
            Identifier.of("wynnextras", "textures/materials/oil/blue.png")),
    SALMON("Salmon",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_salmon.png"),
            Identifier.of("wynnextras", "textures/materials/oil/red.png")),
    CARP("Carp",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_carp.png"),
            Identifier.of("wynnextras", "textures/materials/oil/green.png")),
    ICEFISH("Icefish",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_ice.png"),
            Identifier.of("wynnextras", "textures/materials/oil/blue.png")),
    PIRANHA("Piranha",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_piranha.png"),
            Identifier.of("wynnextras", "textures/materials/oil/orange.png")),
    KOI("Koi",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_koi.png"),
            Identifier.of("wynnextras", "textures/materials/oil/red.png")),
    GYLIA("Gylia",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_gylia.png"),
            Identifier.of("wynnextras", "textures/materials/oil/red.png")),
    BASS("Bass",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_bass.png"),
            Identifier.of("wynnextras", "textures/materials/oil/blue.png")),
    MOLTEN("Molten",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_molten.png"),
            Identifier.of("wynnextras", "textures/materials/oil/orange.png")),
    STARFISH("Starfish",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_starfish.png"),
            Identifier.of("wynnextras", "textures/materials/oil/green.png")),
    DERNIC("Dernic",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_dernic.png"),
            Identifier.of("wynnextras", "textures/materials/oil/black.png")),
    STURGEON("Sturgeon",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_sturgeon.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/dernic.png")),
    MAHSEER("Mahseer",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_mahseer.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/dernic.png"));

    private final String name;
    private final Identifier serverTexture;
    private final Identifier fallbackTexture;

    Oil(String name, Identifier serverTexture, Identifier fallbackTexture) {
        this.name = name;
        this.serverTexture = serverTexture;
        this.fallbackTexture = fallbackTexture;
    }

    @Override
    public String getName() {
        return name + " Oil";
    }

    @Override
    public Identifier getTexture() {
        return MaterialTextureResolver.resolve(serverTexture, fallbackTexture);
    }
}
