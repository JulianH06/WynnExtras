package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum Grains implements IMaterial {
    WHEAT("Wheat", Identifier.of("minecraft", "textures/wynn/economy/farming/grain_wheat.png")),
    BARLEY("Barley", Identifier.of("minecraft", "textures/wynn/economy/farming/grain_barley.png")),
    OAT("Oat", Identifier.of("minecraft", "textures/wynn/economy/farming/grain_oat.png")),
    MALT("Malt", Identifier.of("minecraft", "textures/wynn/economy/farming/grain_malt.png")),
    HOPS("Hops", Identifier.of("minecraft", "textures/wynn/economy/farming/grain_hops.png")),
    RYE("Rye", Identifier.of("minecraft", "textures/wynn/economy/farming/grain_rye.png")),
    MILLET("Millet", Identifier.of("minecraft", "textures/wynn/economy/farming/grain_millet.png")),
    DECAY("Decay", Identifier.of("minecraft", "textures/wynn/economy/farming/grain_decay.png")),
    RICE("Rice", Identifier.of("minecraft", "textures/wynn/economy/farming/grain_rice.png")),
    SORGHUM("Sorghum", Identifier.of("minecraft", "textures/wynn/economy/farming/grain_sorghum.png")),
    HEMP("Hemp", Identifier.of("minecraft", "textures/wynn/economy/farming/grain_hemp.png")),
    DERNIC("Dernic", Identifier.of("minecraft", "textures/wynn/economy/farming/grain_dernic.png")),
    JUTE("Jute", Identifier.of("minecraft", "textures/wynn/economy/farming/grain_jute.png")),
    HEATHER("Heather", Identifier.of("minecraft", "textures/wynn/economy/farming/grain_heather.png"));

    private final String name;
    private final Identifier serverTexture;

    Grains(String name, Identifier serverTexture) {
        this.name = name;
        this.serverTexture = serverTexture;
    }

    @Override
    public String getName() {
        return name + " Grains";
    }

    @Override
    public Identifier getTexture() {
        return MaterialTextureResolver.resolve(serverTexture);
    }
}
