package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum Gem implements IMaterial {
    COPPER("Copper", Identifier.of("minecraft", "textures/wynn/economy/mining/gem_copper.png")),
    GRANITE("Granite", Identifier.of("minecraft", "textures/wynn/economy/mining/gem_granite.png")),
    GOLD("Gold", Identifier.of("minecraft", "textures/wynn/economy/mining/gem_gold.png")),
    SANDSTONE("Sandstone", Identifier.of("minecraft", "textures/wynn/economy/mining/gem_sandstone.png")),
    IRON("Iron", Identifier.of("minecraft", "textures/wynn/economy/mining/gem_iron.png")),
    SILVER("Silver", Identifier.of("minecraft", "textures/wynn/economy/mining/gem_silver.png")),
    COBALT("Cobalt", Identifier.of("minecraft", "textures/wynn/economy/mining/gem_cobalt.png")),
    KANDERSTONE("Kanderstone", Identifier.of("minecraft", "textures/wynn/economy/mining/gem_kander.png")),
    DIAMOND("Diamond", Identifier.of("minecraft", "textures/wynn/economy/mining/gem_diamond.png")),
    MOLTEN("Molten", Identifier.of("minecraft", "textures/wynn/economy/mining/gem_molten.png")),
    VOIDSTONE("Voidstone", Identifier.of("minecraft", "textures/wynn/economy/mining/gem_void.png")),
    DERNIC("Dernic", Identifier.of("minecraft", "textures/wynn/economy/mining/gem_dernic.png")),
    TITANIUM("Titanium", Identifier.of("minecraft", "textures/wynn/economy/mining/gem_titanium.png")),
    CINNABAR("Cinnabar", Identifier.of("minecraft", "textures/wynn/economy/mining/gem_cinnabar.png"));

    private final String name;
    private final Identifier serverTexture;

    Gem(String name, Identifier serverTexture) {
        this.name = name;
        this.serverTexture = serverTexture;
    }

    @Override
    public String getName() {
        return name + " Gem";
    }

    @Override
    public Identifier getTexture() {
        return MaterialTextureResolver.resolve(serverTexture);
    }
}
