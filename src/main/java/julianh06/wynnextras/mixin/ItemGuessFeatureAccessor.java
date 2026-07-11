package julianh06.wynnextras.mixin;

import julianh06.wynnextras.wtshim.models.items.items.game.GearBoxItem;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@org.spongepowered.asm.mixin.Mixin(value = julianh06.wynnextras.wtshim.features.tooltips.ItemGuessFeature.class, remap = false)
public interface ItemGuessFeatureAccessor {
    @Invoker
    List<Text> callGetTooltipAddon(GearBoxItem gearBoxItem);
}
