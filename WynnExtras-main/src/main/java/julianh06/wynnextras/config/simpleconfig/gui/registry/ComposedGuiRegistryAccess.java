package julianh06.wynnextras.config.simpleconfig.gui.registry;

import julianh06.wynnextras.config.simpleconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public class ComposedGuiRegistryAccess implements GuiRegistryAccess {
    private final List<GuiRegistryAccess> children;

    public ComposedGuiRegistryAccess(GuiRegistryAccess... children) {
        this.children = Arrays.asList(children);
    }

    public List<AbstractConfigListEntry<?>> get(String name, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        return this.children.stream().map((child) -> child.get(name, field, config, defaults, registry)).filter(Objects::nonNull).findFirst().orElseThrow(() -> new RuntimeException("No ConfigGuiProvider match!"));
    }

    public List<AbstractConfigListEntry<?>> transform(List<AbstractConfigListEntry<?>> guis, String name, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        for(GuiRegistryAccess child : this.children) {
            guis = child.transform(guis, name, field, config, defaults, registry);
        }

        return guis;
    }
}