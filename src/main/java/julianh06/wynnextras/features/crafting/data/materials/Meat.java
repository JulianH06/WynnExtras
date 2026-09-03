package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum Meat implements IMaterial {
    GUDGEON("Gudgeon", Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_gudgeon.png")),
    TROUT("Trout", Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_trout.png")),
    SALMON("Salmon", Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_salmon.png")),
    CARP("Carp", Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_carp.png")),
    ICEFISH("Icefish", Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_ice.png")),
    PIRANHA("Piranha", Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_piranha.png")),
    KOI("Koi", Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_koi.png")),
    GYLIA("Gylia", Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_gylia.png")),
    BASS("Bass", Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_bass.png")),
    MOLTEN("Molten", Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_molten.png")),
    STARFISH("Starfish", Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_star.png")),
    DERNIC("Dernic", Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_dernic.png")),
    STURGEON("Sturgeon", Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_sturgeon.png")),
    MAHSEER("Mahseer", Identifier.of("minecraft", "textures/wynn/economy/fishing/meat_mahseer.png"));

    private final String name;
    private final Identifier serverTexture;

    Meat(String name, Identifier serverTexture) {
        this.name = name;
        this.serverTexture = serverTexture;
    }

    @Override
    public String getName() {
        return name + " Meat";
    }

    @Override
    public Identifier getTexture() {
        return MaterialTextureResolver.resolve(serverTexture);
    }
}
