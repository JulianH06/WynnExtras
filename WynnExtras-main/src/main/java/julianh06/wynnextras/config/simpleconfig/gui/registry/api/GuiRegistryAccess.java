package julianh06.wynnextras.config.simpleconfig.gui.registry.api;


import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.reflect.Field;
import java.util.List;

@Environment(EnvType.CLIENT)
public interface GuiRegistryAccess extends GuiProvider, GuiTransformer {
    default List<AbstractConfigListEntry<?>> getAndTransform(String name, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        return this.transform(this.get(name, field, config, defaults, registry), name, field, config, defaults, registry);
    }
}