package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum Wood implements IMaterial {
    OAK("Oak",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_oak.png"),
            Identifier.of("wynnextras", "textures/materials/wood/oak.png")),
    BIRCH("Birch",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_birch.png"),
            Identifier.of("wynnextras", "textures/materials/wood/birch.png")),
    WILLOW("Willow",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_willow.png"),
            Identifier.of("wynnextras", "textures/materials/wood/willow.png")),
    ACACIA("Acacia",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_acacia.png"),
            Identifier.of("wynnextras", "textures/materials/wood/acacia.png")),
    SPRUCE("Spruce",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_spruce.png"),
            Identifier.of("wynnextras", "textures/materials/wood/spruce.png")),
    JUNGLE("Jungle",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_jungle.png"),
            Identifier.of("wynnextras", "textures/materials/wood/jungle.png")),
    DARK("Dark",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_dark.png"),
            Identifier.of("wynnextras", "textures/materials/wood/dark.png")),
    LIGHT("Light",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_light.png"),
            Identifier.of("wynnextras", "textures/materials/wood/light.png")),
    PINE("Pine",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_pine.png"),
            Identifier.of("wynnextras", "textures/materials/wood/pine.png")),
    AVO("Avo",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_avo.png"),
            Identifier.of("wynnextras", "textures/materials/wood/avo.png")),
    SKY("Sky",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_sky.png"),
            Identifier.of("wynnextras", "textures/materials/wood/sky.png")),
    DERNIC("Dernic",
            Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_dernic.png"),
            Identifier.of("wynnextras", "textures/materials/wood/dernic.png"));

    private final String name;
    private final Identifier serverTexture;
    private final Identifier fallbackTexture;

    Wood(String name, Identifier serverTexture, Identifier fallbackTexture) {
        this.name = name;
        this.serverTexture = serverTexture;
        this.fallbackTexture = fallbackTexture;
    }

    @Override
    public String getName() {
        return name + " Wood";
    }

    @Override
    public Identifier getTexture() {
        return MaterialTextureResolver.resolve(serverTexture, fallbackTexture);
    }
}
