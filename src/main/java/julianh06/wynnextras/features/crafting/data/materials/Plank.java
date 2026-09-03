package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum Plank implements IMaterial {
    OAK("Oak", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_oak.png")),
    BIRCH("Birch", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_birch.png")),
    WILLOW("Willow", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_willow.png")),
    ACACIA("Acacia", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_acacia.png")),
    SPRUCE("Spruce", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_spruce.png")),
    JUNGLE("Jungle", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_jungle.png")),
    DARK("Dark", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_dark.png")),
    LIGHT("Light", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_light.png")),
    PINE("Pine", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_pine.png")),
    AVO("Avo", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_avo.png")),
    SKY("Sky", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_sky.png")),
    DERNIC("Dernic", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_dernic.png")),
    MAPLE("Maple", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_maple.png")),
    REDWOOD("Redwood", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/plank_redwood.png"));

    private final String name;
    private final Identifier serverTexture;

    Plank(String name, Identifier serverTexture) {
        this.name = name;
        this.serverTexture = serverTexture;
    }

    @Override
    public String getName() {
        return name + " Plank";
    }

    @Override
    public Identifier getTexture() {
        return MaterialTextureResolver.resolve(serverTexture);
    }
}
