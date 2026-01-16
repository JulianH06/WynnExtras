package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum Meat implements IMaterial {
    GUDGEON("Gudgeon",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/cooked_pink.png"),
            Identifier.of("wynnextras", "textures/materials/meat/pink.png")),
    TROUT("Trout",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/cooked_blue.png"),
            Identifier.of("wynnextras", "textures/materials/meat/blue.png")),
    SALMON("Salmon",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/cooked_orange.png"),
            Identifier.of("wynnextras", "textures/materials/meat/filet.png")),
    CARP("Carp",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/cooked_yellow.png"),
            Identifier.of("wynnextras", "textures/materials/meat/yellow.png")),
    ICEFISH("Icefish",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/cooked_blue.png"),
            Identifier.of("wynnextras", "textures/materials/meat/blue.png")),
    PIRANHA("Piranha",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/cooked_blue.png"),
            Identifier.of("wynnextras", "textures/materials/meat/blue.png")),
    KOI("Koi",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/cooked_pink.png"),
            Identifier.of("wynnextras", "textures/materials/meat/pink.png")),
    GYLIA("Gylia",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/cooked_pink.png"),
            Identifier.of("wynnextras", "textures/materials/meat/pink.png")),
    BASS("Bass",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/cooked_gray.png"),
            Identifier.of("wynnextras", "textures/materials/meat/black.png")),
    MOLTEN("Molten",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/cooked_orange.png"),
            Identifier.of("wynnextras", "textures/materials/meat/filet.png")),
    STARFISH("Starfish",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/cooked_yellow.png"),
            Identifier.of("wynnextras", "textures/materials/meat/yellow.png")),
    DERNIC("Dernic",
            Identifier.of("minecraft", "textures/wynn/economy/fishing/cooked_gray.png"),
            Identifier.of("wynnextras", "textures/materials/meat/black.png"));

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
