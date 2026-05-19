package julianh06.wynnextras.mixin;

import com.wynntils.features.tooltips.ItemStatInfoFeature;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import com.wynntils.models.items.WynnItem;
import com.wynntils.utils.mc.TooltipUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.inventory.TradeMarketComparisonPanel;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStatInfoFeature.class)
public class ItemStatInfoFeatureMixin {
    @Redirect(
            method = "onTooltipPre",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/wynntils/utils/mc/TooltipUtils;getWynnItemTooltip(Lnet/minecraft/item/ItemStack;Lcom/wynntils/models/items/WynnItem;)Ljava/util/List;"
            ),
            remap = false
    )
    private List<Text> redirectGetWynnItemTooltip(ItemStack itemStack, WynnItem wynnItem) {
        WeightDisplay.setCurrentHoveredStack(itemStack);
        return TooltipUtils.getWynnItemTooltip(itemStack, wynnItem);
    }

    @Inject(method = "onTooltipPre", at = @At("RETURN"), remap = false)
    private void captureProcessedTooltip(ItemTooltipRenderEvent.Pre event, CallbackInfo ci) {
        ItemStack currentHoveredStack = WeightDisplay.getCurrentHoveredStack();
        if (currentHoveredStack != null && event.getTooltips() != null) {
            List<Text> tooltips = new ArrayList<>(event.getTooltips());
            TradeMarketComparisonPanel.cacheHoveredTooltip(currentHoveredStack, tooltips);
        }
    }

    @Inject(method = "onTooltipPreFinalize", at = @At("RETURN"), remap = false)
    private void appendWeightAnnotations(ItemTooltipRenderEvent.Pre event, CallbackInfo ci) {
        //this will run if the user has the ItemStatInfoFeature enabled, if they dont then the annotation will be added in WeightDisplay instead

        ItemStack currentHoveredStack = WeightDisplay.getCurrentHoveredStack();
        if (currentHoveredStack == null || event.getTooltips() == null) return;
        if (WeightDisplay.isUnidentified(currentHoveredStack)) return;

        String cleanName = WeightDisplay.extractCleanName(currentHoveredStack);
        WeightDisplay.ItemData itemData = WeightDisplay.itemCache.get(cleanName);
        if (itemData == null) return;

        if ((WeightDisplay.isUpPressed() || WeightDisplay.isDownPressed()) && !itemData.data().isEmpty()) {
            int nextIndex = itemData.index();
            if (WeightDisplay.isDownPressed()) nextIndex = (nextIndex + 1) % itemData.data().size();
            else nextIndex = (nextIndex - 1 + itemData.data().size()) % itemData.data().size();
            itemData = new WeightDisplay.ItemData(itemData.name(), itemData.data(), nextIndex);
            WeightDisplay.itemCache.put(cleanName, itemData);
            WeightDisplay.clearCycleInput();
        }

        int hash = currentHoveredStack.getComponents().hashCode();
        WeightDisplay.ItemData scaleData = WeightDisplay.weightCacheByHash.get(hash);
        if (scaleData == null || scaleData.data().isEmpty()) return;

        int idx = Math.min(itemData.index(), scaleData.data().size() - 1);
        WeightDisplay.WeightData currentProfile = itemData.data().get(idx);

        List<Text> tooltipList = new ArrayList<>(event.getTooltips());

        if (tooltipList.size() >= 4 && WynnExtrasConfig.INSTANCE.showWeight) {
            List<Text> scoreBlock = new ArrayList<>();
            scoreBlock.add(Text.empty());
            for (int j = 0; j < scaleData.data().size(); j++) {
                WeightDisplay.WeightData wd = scaleData.data().get(j);
                boolean cur = (j == idx);
                float score = wd.score();
                Text scoreText = Text.literal(String.format(" [%.1f%%]", score))
                        .styled(s -> s.withColor(WeightDisplay.getScaleColor(score)).withBold(cur));
                Text label = Text.literal("  ↳ " + wd.weightName() + " Scale")
                        .styled(s -> s.withColor(cur ? 0xFFFFFF : 0xAAAAAA).withBold(cur))
                        .copy().append(scoreText);
                scoreBlock.add(label);
            }
            if (scaleData.data().size() > 1) {
                scoreBlock.add(Text.literal("  ↳ Use ↑/↓ (W/S) to cycle").styled(s -> s.withColor(0x555555)));
            }
            tooltipList.addAll(4, scoreBlock);
        }


        int added = 0;
        if (WynnExtrasConfig.INSTANCE.showScales && WynnExtrasConfig.INSTANCE.showWeight) {
            for (int i = tooltipList.size() - 1; i >= 0; i--) {
                String[] parts = WeightDisplay.extractStatFromLine(tooltipList.get(i).getString());
                if (parts == null) continue;
                String apiName = WeightDisplay.resolveIdentKey(parts[0], parts[1])[0];
                Float scale = currentProfile.identifications().getOrDefault(apiName, 0f);
                if (scale == null || scale == 0f) continue;
                tooltipList.add(i + 1, Text.literal(String.format("  ↳ Weight: %.1f%%", scale * 100))
                        .styled(s -> s.withColor(0x555555)));
                added++;
            }
        }

        if (added > 0 || tooltipList.size() != event.getTooltips().size()) event.setTooltips(tooltipList);
    }
}
