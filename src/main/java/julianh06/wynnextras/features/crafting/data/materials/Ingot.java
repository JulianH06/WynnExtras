package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import net.minecraft.util.Identifier;

public enum Ingot implements IMaterial {
    COPPER("Copper", Identifier.of("wynnextras", "textures/materials/ingot/copper.png")),
    GRANITE("Granite", Identifier.of("wynnextras", "textures/materials/ingot/granite.png")),
    GOLD("Gold", Identifier.of("wynnextras", "textures/materials/ingot/gold.png")),
    SANDSTONE("Sandstone", Identifier.of("wynnextras", "textures/materials/ingot/sandstone.png")),
    IRON("Iron", Identifier.of("wynnextras", "textures/materials/ingot/iron.png")),
    SILVER("Silver", Identifier.of("wynnextras", "textures/materials/ingot/silver.png")),
    COBALT("Cobalt", Identifier.of("wynnextras", "textures/materials/ingot/cobalt.png")),
    KANDERSTONE("Kanderstone", Identifier.of("wynnextras", "textures/materials/ingot/kanderstone.png")),
    DIAMOND("Diamond", Identifier.of("wynnextras", "textures/materials/ingot/diamond.png")),
    MOLTEN("Molten", Identifier.of("wynnextras", "textures/materials/ingot/molten.png")),
    VOIDSTONE("Voidstone", Identifier.of("wynnextras", "textures/materials/ingot/voidstone.png")),
    DERNIC("Dernic", Identifier.of("wynnextras", "textures/materials/ingot/dernic.png"));

    private final String name;
    private final Identifier texture;

    Ingot(String name, Identifier texture) {
        this.name = name;
        this.texture = texture;
    }

    @Override
    public String getName() {
        return name + " Ingot";
    }

    @Override
    public Identifier getTexture() {
        return texture;
    }
}
