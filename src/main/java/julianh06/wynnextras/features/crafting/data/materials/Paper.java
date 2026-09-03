package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum Paper implements IMaterial {
    OAK("Oak", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_oak.png")),
    BIRCH("Birch", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_birch.png")),
    WILLOW("Willow", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_willow.png")),
    ACACIA("Acacia", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_acacia.png")),
    SPRUCE("Spruce", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_spruce.png")),
    JUNGLE("Jungle", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_jungle.png")),
    DARK("Dark", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_dark.png")),
    LIGHT("Light", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_light.png")),
    PINE("Pine", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_pine.png")),
    AVO("Avo", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_avo.png")),
    SKY("Sky", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_sky.png")),
    DERNIC("Dernic", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_dernic.png")),
    MAPLE("Maple", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_maple.png")),
    REDWOOD("Redwood", Identifier.of("minecraft", "textures/wynn/economy/woodcutting/paper_redwood.png"));

    private final String name;
    private final Identifier serverTexture;

    Paper(String name, Identifier serverTexture) {
        this.name = name;
        this.serverTexture = serverTexture;
    }

    @Override
    public String getName() {
        return name + " Paper";
    }

    @Override
    public Identifier getTexture() {
        return MaterialTextureResolver.resolve(serverTexture);
    }
}
