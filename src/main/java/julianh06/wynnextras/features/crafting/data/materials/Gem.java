package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum Gem implements IMaterial {
    COPPER("Copper",
            Identifier.of("minecraft", "textures/wynn/economy/mining/gem_copper.png"),
            Identifier.of("wynnextras", "textures/materials/gem/copper.png")),
    GRANITE("Granite",
            Identifier.of("minecraft", "textures/wynn/economy/mining/gem_granite.png"),
            Identifier.of("wynnextras", "textures/materials/gem/granite.png")),
    GOLD("Gold",
            Identifier.of("minecraft", "textures/wynn/economy/mining/gem_gold.png"),
            Identifier.of("wynnextras", "textures/materials/gem/gold.png")),
    SANDSTONE("Sandstone",
            Identifier.of("minecraft", "textures/wynn/economy/mining/gem_sandstone.png"),
            Identifier.of("wynnextras", "textures/materials/gem/sandstone.png")),
    IRON("Iron",
            Identifier.of("minecraft", "textures/wynn/economy/mining/gem_iron.png"),
            Identifier.of("wynnextras", "textures/materials/gem/iron.png")),
    SILVER("Silver",
            Identifier.of("minecraft", "textures/wynn/economy/mining/gem_silver.png"),
            Identifier.of("wynnextras", "textures/materials/gem/silver.png")),
    COBALT("Cobalt",
            Identifier.of("minecraft", "textures/wynn/economy/mining/gem_cobalt.png"),
            Identifier.of("wynnextras", "textures/materials/gem/cobalt.png")),
    KANDERSTONE("Kanderstone",
            Identifier.of("minecraft", "textures/wynn/economy/mining/gem_kander.png"),
            Identifier.of("wynnextras", "textures/materials/gem/kanderstone.png")),
    DIAMOND("Diamond",
            Identifier.of("minecraft", "textures/wynn/economy/mining/gem_diamond.png"),
            Identifier.of("wynnextras", "textures/materials/gem/diamond.png")),
    MOLTEN("Molten",
            Identifier.of("minecraft", "textures/wynn/economy/mining/gem_molten.png"),
            Identifier.of("wynnextras", "textures/materials/gem/molten.png")),
    VOIDSTONE("Voidstone",
            Identifier.of("minecraft", "textures/wynn/economy/mining/gem_void.png"),
            Identifier.of("wynnextras", "textures/materials/gem/voidstone.png")),
    DERNIC("Dernic",
            Identifier.of("minecraft", "textures/wynn/economy/mining/gem_dernic.png"),
            Identifier.of("wynnextras", "textures/materials/gem/dernic.png"));

    private final String name;
    private final Identifier serverTexture;
    private final Identifier fallbackTexture;

    Gem(String name, Identifier serverTexture, Identifier fallbackTexture) {
        this.name = name;
        this.serverTexture = serverTexture;
        this.fallbackTexture = fallbackTexture;
    }

    @Override
    public String getName() {
        return name + " Gem";
    }

    @Override
    public Identifier getTexture() {
        return MaterialTextureResolver.resolve(serverTexture, fallbackTexture);
    }
}
