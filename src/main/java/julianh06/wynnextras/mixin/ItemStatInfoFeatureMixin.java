package julianh06.wynnextras.mixin;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.tooltips.ItemStatInfoFeature;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.models.wynnitem.parsing.WynnItemParser;
import com.wynntils.utils.mc.TooltipUtils;
import com.wynntils.utils.wynn.ColorScaleUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import julianh06.wynnextras.utils.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import static julianh06.wynnextras.features.inventory.WeightDisplay.*;

@Mixin(ItemStatInfoFeature.class)
public class ItemStatInfoFeatureMixin {
    @Redirect(
            method = "onTooltipPre",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/wynntils/utils/mc/TooltipUtils;getWynnItemTooltip(Lnet/minecraft/item/ItemStack;Lcom/wynntils/models/items/WynnItem;)Ljava/util/List;"
            )
    )
    private List<Text> redirectGetWynnItemTooltip(ItemStack itemStack, WynnItem wynnItem) {
        currentHoveredStack = itemStack;
        currentHoveredWynnitem = wynnItem;
        return TooltipUtils.getWynnItemTooltip(itemStack, wynnItem);
    }
}
