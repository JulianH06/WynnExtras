package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum StringMaterial implements IMaterial {
    WHEAT("Wheat",
            Identifier.of("minecraft", "textures/wynn/economy/farming/string.png"),
            Identifier.of("wynnextras", "textures/materials/string/white.png")),
    BARLEY("Barley",
            Identifier.of("minecraft", "textures/wynn/economy/farming/string.png"),
            Identifier.of("wynnextras", "textures/materials/string/white.png")),
    OAT("Oat",
            Identifier.of("minecraft", "textures/wynn/economy/farming/string_green.png"),
            Identifier.of("wynnextras", "textures/materials/string/green.png")),
    MALT("Malt",
            Identifier.of("minecraft", "textures/wynn/economy/farming/string.png"),
            Identifier.of("wynnextras", "textures/materials/string/white.png")),
    HOPS("Hops",
            Identifier.of("minecraft", "textures/wynn/economy/farming/string_green.png"),
            Identifier.of("wynnextras", "textures/materials/string/green.png")),
    RYE("Rye",
            Identifier.of("minecraft", "textures/wynn/economy/farming/string.png"),
            Identifier.of("wynnextras", "textures/materials/string/white.png")),
    MILLET("Millet",
            Identifier.of("minecraft", "textures/wynn/economy/farming/string.png"),
            Identifier.of("wynnextras", "textures/materials/string/white.png")),
    DECAY("Decay",
            Identifier.of("minecraft", "textures/wynn/economy/farming/string_brown.png"),
            Identifier.of("wynnextras", "textures/materials/string/brown.png")),
    RICE("Rice",
            Identifier.of("minecraft", "textures/wynn/economy/farming/string.png"),
            Identifier.of("wynnextras", "textures/materials/string/white.png")),
    SORGHUM("Sorghum",
            Identifier.of("minecraft", "textures/wynn/economy/farming/string_yellow.png"),
            Identifier.of("wynnextras", "textures/materials/string/yellow.png")),
    HEMP("Hemp",
            Identifier.of("minecraft", "textures/wynn/economy/farming/string_green.png"),
            Identifier.of("wynnextras", "textures/materials/string/green.png")),
    DERNIC("Dernic",
            Identifier.of("minecraft", "textures/wynn/economy/farming/string_brown.png"),
            Identifier.of("wynnextras", "textures/materials/string/brown.png"));

    private final String name;
    private final Identifier serverTexture;
    private final Identifier fallbackTexture;

    StringMaterial(String name, Identifier serverTexture, Identifier fallbackTexture) {
        this.name = name;
        this.serverTexture = serverTexture;
        this.fallbackTexture = fallbackTexture;
    }

    @Override
    public String getName() {
        return name + " String";
    }

    @Override
    public Identifier getTexture() {
        return MaterialTextureResolver.resolve(serverTexture, fallbackTexture);
    }
}
