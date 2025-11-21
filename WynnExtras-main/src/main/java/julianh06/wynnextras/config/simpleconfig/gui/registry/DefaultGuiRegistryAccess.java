package julianh06.wynnextras.config.simpleconfig.gui.registry;

import julianh06.wynnextras.config.simpleconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class DefaultGuiRegistryAccess implements GuiRegistryAccess {
    public List<AbstractConfigListEntry<?>> get(String name, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        LogManager.getLogger().error("No GUI provider registered for field '{}'!", field);
        return Collections.emptyList();
    }

    public List<AbstractConfigListEntry<?>> transform(List<AbstractConfigListEntry<?>> guis, String name, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        return guis;
    }
}
