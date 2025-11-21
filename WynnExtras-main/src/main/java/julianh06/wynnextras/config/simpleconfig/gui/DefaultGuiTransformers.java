package julianh06.wynnextras.config.simpleconfig.gui;

import julianh06.wynnextras.config.simpleconfig.annotations.ConfigEntry;
import julianh06.wynnextras.config.simpleconfig.gui.registry.GuiRegistry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Environment(EnvType.CLIENT)
public class DefaultGuiTransformers {
    private static final ConfigEntryBuilder ENTRY_BUILDER = ConfigEntryBuilder.create();

    private DefaultGuiTransformers() {
        // Prevent instantiation
    }

    public static GuiRegistry apply(GuiRegistry registry) {
        registry.registerAnnotationTransformer((guis, name, field, config, defaults, guiProvider) -> guis.stream().peek((gui) -> {
            if (!(gui instanceof TextListEntry)) {
                ConfigEntry.Tooltip tooltip = field.getAnnotation(ConfigEntry.Tooltip.class);

                tryApplyTooltip(gui, Arrays.stream(tooltip.value()).map(s -> (Text) Text.literal(s)).toArray(Text[]::new));
            }

        }).collect(Collectors.toList()), ConfigEntry.Tooltip.class);

        return registry;
    }

    private static void tryApplyTooltip(AbstractConfigListEntry<?> gui, Text[] text) {
        if (gui instanceof TooltipListEntry<?> tooltipGui) {
            tooltipGui.setTooltipSupplier(() -> Optional.of(text));
        }

    }

    private static void tryRemoveTooltip(AbstractConfigListEntry<?> gui) {
        if (gui instanceof TooltipListEntry<?> tooltipGui) {
            tooltipGui.setTooltipSupplier(Optional::empty);
        }

    }
}
