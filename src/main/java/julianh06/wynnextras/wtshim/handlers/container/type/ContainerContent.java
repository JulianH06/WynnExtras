// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.handlers.container.type;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;

public record ContainerContent(List<ItemStack> items, Text title, ScreenHandlerType<?> menuType, int containerId) {}
