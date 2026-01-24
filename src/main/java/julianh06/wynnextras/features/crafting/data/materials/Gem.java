package julianh06.wynnextras.features.crafting.data.materials;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import net.minecraft.util.Identifier;

public enum Gem implements IMaterial {
    COPPER("Copper", Identifier.of("wynnextras", "textures/materials/gem/copper.png")),
    GRANITE("Granite", Identifier.of("wynnextras", "textures/materials/gem/granite.png")),
    GOLD("Gold", Identifier.of("wynnextras", "textures/materials/gem/gold.png")),
    SANDSTONE("Sandstone", Identifier.of("wynnextras", "textures/materials/gem/sandstone.png")),
    IRON("Iron", Identifier.of("wynnextras", "textures/materials/gem/iron.png")),
    SILVER("Silver", Identifier.of("wynnextras", "textures/materials/gem/silver.png")),
    COBALT("Cobalt", Identifier.of("wynnextras", "textures/materials/gem/cobalt.png")),
    KANDERSTONE("Kanderstone", Identifier.of("wynnextras", "textures/materials/gem/kanderstone.png")),
    DIAMOND("Diamond", Identifier.of("wynnextras", "textures/materials/gem/diamond.png")),
    MOLTEN("Molten", Identifier.of("wynnextras", "textures/materials/gem/molten.png")),
    VOIDSTONE("Voidstone", Identifier.of("wynnextras", "textures/materials/gem/voidstone.png")),
    DERNIC("Dernic", Identifier.of("wynnextras", "textures/materials/gem/dernic.png"));

    private final String name;
    private final Identifier texture;

    Gem(String name, Identifier texture) {
        this.name = name;
        this.texture = texture;
    }

    @Override
    public String getName() {
        return name + " Gem";
    }

    @Override
    public Identifier getTexture() {
        return texture;
    }
}
