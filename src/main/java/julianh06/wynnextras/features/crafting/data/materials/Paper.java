package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum Paper implements IMaterial {
    OAK("Oak",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_white.png"),
            Identifier.of("wynnextras", "textures/materials/paper/white.png")),
    BIRCH("Birch",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_white.png"),
            Identifier.of("wynnextras", "textures/materials/paper/white.png")),
    WILLOW("Willow",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_cream.png"),
            Identifier.of("wynnextras", "textures/materials/paper/cream.png")),
    ACACIA("Acacia",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_cream.png"),
            Identifier.of("wynnextras", "textures/materials/paper/cream.png")),
    SPRUCE("Spruce",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_brown.png"),
            Identifier.of("wynnextras", "textures/materials/paper/brown.png")),
    JUNGLE("Jungle",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_cream.png"),
            Identifier.of("wynnextras", "textures/materials/paper/cream.png")),
    DARK("Dark",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_brown.png"),
            Identifier.of("wynnextras", "textures/materials/paper/brown.png")),
    LIGHT("Light",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_white.png"),
            Identifier.of("wynnextras", "textures/materials/paper/white.png")),
    PINE("Pine",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_brown.png"),
            Identifier.of("wynnextras", "textures/materials/paper/brown.png")),
    AVO("Avo",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_cream.png"),
            Identifier.of("wynnextras", "textures/materials/paper/cream.png")),
    SKY("Sky",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_white.png"),
            Identifier.of("wynnextras", "textures/materials/paper/white.png")),
    DERNIC("Dernic",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_purple.png"),
            Identifier.of("wynnextras", "textures/materials/paper/purple.png"));

    private final String name;
    private final Identifier serverTexture;
    private final Identifier fallbackTexture;

    Paper(String name, Identifier serverTexture, Identifier fallbackTexture) {
        this.name = name;
        this.serverTexture = serverTexture;
        this.fallbackTexture = fallbackTexture;
    }

    @Override
    public String getName() {
        return name + " Paper";
    }

    @Override
    public Identifier getTexture() {
        return MaterialTextureResolver.resolve(serverTexture, fallbackTexture);
    }
}
