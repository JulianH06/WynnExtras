package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum Grains implements IMaterial {
    WHEAT("Wheat",
            Identifier.of("minecraft", "textures/wynn/economy/farming/seeds_pumpkin.png"),
            Identifier.of("wynnextras", "textures/materials/grains/yellow.png")),
    BARLEY("Barley",
            Identifier.of("minecraft", "textures/wynn/economy/farming/seeds_pumpkin.png"),
            Identifier.of("wynnextras", "textures/materials/grains/yellow.png")),
    OAT("Oat",
            Identifier.of("minecraft", "textures/wynn/economy/farming/seeds_wheat.png"),
            Identifier.of("wynnextras", "textures/materials/grains/green.png")),
    MALT("Malt",
            Identifier.of("minecraft", "textures/wynn/economy/farming/seeds_pumpkin.png"),
            Identifier.of("wynnextras", "textures/materials/grains/yellow.png")),
    HOPS("Hops",
            Identifier.of("minecraft", "textures/wynn/economy/farming/seeds_wheat.png"),
            Identifier.of("wynnextras", "textures/materials/grains/green.png")),
    RYE("Rye",
            Identifier.of("minecraft", "textures/wynn/economy/farming/seeds_pumpkin.png"),
            Identifier.of("wynnextras", "textures/materials/grains/yellow.png")),
    MILLET("Millet",
            Identifier.of("minecraft", "textures/wynn/economy/farming/seeds_pumpkin.png"),
            Identifier.of("wynnextras", "textures/materials/grains/yellow.png")),
    DECAY("Decay",
            Identifier.of("minecraft", "textures/wynn/economy/farming/seeds_sorghum.png"),
            Identifier.of("wynnextras", "textures/materials/grains/brown.png")),
    RICE("Rice",
            Identifier.of("minecraft", "textures/wynn/economy/farming/seeds_pumpkin.png"),
            Identifier.of("wynnextras", "textures/materials/grains/yellow.png")),
    SORGHUM("Sorghum",
            Identifier.of("minecraft", "textures/wynn/economy/farming/seeds_sorghum.png"),
            Identifier.of("wynnextras", "textures/materials/grains/brown.png")),
    HEMP("Hemp",
            Identifier.of("minecraft", "textures/wynn/economy/farming/seeds_wheat.png"),
            Identifier.of("wynnextras", "textures/materials/grains/green.png")),
    DERNIC("Dernic",
            Identifier.of("minecraft", "textures/wynn/economy/farming/seeds_sorghum.png"),
            Identifier.of("wynnextras", "textures/materials/grains/brown.png"));

    private final String name;
    private final Identifier serverTexture;
    private final Identifier fallbackTexture;

    Grains(String name, Identifier serverTexture, Identifier fallbackTexture) {
        this.name = name;
        this.serverTexture = serverTexture;
        this.fallbackTexture = fallbackTexture;
    }

    @Override
    public String getName() {
        return name + " Grains";
    }

    @Override
    public Identifier getTexture() {
        return MaterialTextureResolver.resolve(serverTexture, fallbackTexture);
    }
}
