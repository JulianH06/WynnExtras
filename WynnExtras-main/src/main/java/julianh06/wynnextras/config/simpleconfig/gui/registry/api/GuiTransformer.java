package julianh06.wynnextras.config.simpleconfig.gui.registry.api;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.reflect.Field;
import java.util.List;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface GuiTransformer {
    List<AbstractConfigListEntry<?>> transform(List<AbstractConfigListEntry<?>> guis, String name, Field field, Object config, Object defaults, GuiRegistryAccess registry);
}