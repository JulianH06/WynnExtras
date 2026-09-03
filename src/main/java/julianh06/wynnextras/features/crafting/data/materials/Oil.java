package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum Oil implements IMaterial {
    GUDGEON("Gudgeon", Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_gudgeon.png")),
    TROUT("Trout", Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_trout.png")),
    SALMON("Salmon", Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_salmon.png")),
    CARP("Carp", Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_carp.png")),
    ICEFISH("Icefish", Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_ice.png")),
    PIRANHA("Piranha", Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_piranha.png")),
    KOI("Koi", Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_koi.png")),
    GYLIA("Gylia", Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_gylia.png")),
    BASS("Bass", Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_bass.png")),
    MOLTEN("Molten", Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_molten.png")),
    STARFISH("Starfish", Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_star.png")),
    DERNIC("Dernic", Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_dernic.png")),
    STURGEON("Sturgeon", Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_sturgeon.png")),
    MAHSEER("Mahseer", Identifier.of("minecraft", "textures/wynn/economy/fishing/oil_mahseer.png"));

    private final String name;
    private final Identifier serverTexture;

    Oil(String name, Identifier serverTexture) {
        this.name = name;
        this.serverTexture = serverTexture;
    }

    @Override
    public String getName() {
        return name + " Oil";
    }

    @Override
    public Identifier getTexture() {
        return MaterialTextureResolver.resolve(serverTexture);
    }
}
