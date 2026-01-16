package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import net.minecraft.util.Identifier;

public enum Ingot implements IMaterial {
    COPPER("Copper",
            Identifier.of("minecraft", "textures/wynn/economy/mining/ingot_copper.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/copper.png")),
    GRANITE("Granite",
            Identifier.of("minecraft", "textures/wynn/economy/mining/ingot_granite.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/granite.png")),
    GOLD("Gold",
            Identifier.of("minecraft", "textures/wynn/economy/mining/ingot_gold.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/gold.png")),
    SANDSTONE("Sandstone",
            Identifier.of("minecraft", "textures/wynn/economy/mining/ingot_sandstone.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/sandstone.png")),
    IRON("Iron",
            Identifier.of("minecraft", "textures/wynn/economy/mining/ingot_iron.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/iron.png")),
    SILVER("Silver",
            Identifier.of("minecraft", "textures/wynn/economy/mining/ingot_silver.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/silver.png")),
    COBALT("Cobalt",
            Identifier.of("minecraft", "textures/wynn/economy/mining/ingot_cobalt.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/cobalt.png")),
    KANDERSTONE("Kanderstone",
            Identifier.of("minecraft", "textures/wynn/economy/mining/ingot_kanderstone.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/kanderstone.png")),
    DIAMOND("Diamond",
            Identifier.of("minecraft", "textures/wynn/economy/mining/ingot_diamond.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/diamond.png")),
    MOLTEN("Molten",
            Identifier.of("minecraft", "textures/wynn/economy/mining/ingot_molten.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/molten.png")),
    VOIDSTONE("Voidstone",
            Identifier.of("minecraft", "textures/wynn/economy/mining/ingot_void.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/voidstone.png")),
    DERNIC("Dernic",
            Identifier.of("minecraft", "textures/wynn/economy/mining/ingot_dernic.png"),
            Identifier.of("wynnextras", "textures/materials/ingot/dernic.png"));

    private final String name;
    private final Identifier serverTexture;
    private final Identifier fallbackTexture;

    Ingot(String name, Identifier serverTexture, Identifier fallbackTexture) {
        this.name = name;
        this.serverTexture = serverTexture;
        this.fallbackTexture = fallbackTexture;
    }

    @Override
    public String getName() {
        return name + " Ingot";
    }

    @Override
    public Identifier getTexture() {
        return MaterialTextureResolver.resolve(serverTexture, fallbackTexture);
    }
}
