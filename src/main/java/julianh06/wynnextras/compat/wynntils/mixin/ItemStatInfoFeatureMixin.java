package julianh06.wynnextras.compat.wynntils.mixin;

import julianh06.wynnextras.compat.wynntils.WynntilsTooltipAdapter;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.inventory.TradeMarketComparisonPanel;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(targets = "com.wynntils.features.tooltips.ItemStatInfoFeature", remap = false)
public class ItemStatInfoFeatureMixin {
    @Redirect(
            method = "onTooltipPre",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/wynntils/utils/mc/TooltipUtils;getWynnItemTooltip(Lnet/minecraft/item/ItemStack;Lcom/wynntils/models/items/WynnItem;)Ljava/util/List;"
            ),
            remap = false,
            require = 0
    )
    private List<Text> redirectGetWynnItemTooltip(ItemStack itemStack, @Coerce Object wynnItem) {
        WeightDisplay.setCurrentHoveredStack(itemStack);
        return WynntilsTooltipAdapter.getWynnItemTooltip(itemStack, wynnItem);
    }

    @Inject(method = "onTooltipPre", at = @At("RETURN"), remap = false, require = 0)
    private void captureProcessedTooltip(@Coerce Object event, CallbackInfo ci) {
        ItemStack currentHoveredStack = WeightDisplay.getCurrentHoveredStack();
        List<Text> eventTooltips = WynntilsTooltipAdapter.getTooltips(event);
        if (currentHoveredStack != null && !eventTooltips.isEmpty()) {
            List<Text> tooltips = new ArrayList<>(eventTooltips);
            TradeMarketComparisonPanel.cacheHoveredTooltip(currentHoveredStack, tooltips);
        }
    }

    @Inject(method = "onTooltipPreFinalize", at = @At("RETURN"), remap = false, require = 0)
    private void appendWeightAnnotations(@Coerce Object event, CallbackInfo ci) {
        //this will run if the user has the ItemStatInfoFeature enabled, if they dont then the annotation will be added in WeightDisplay instead

        ItemStack currentHoveredStack = WeightDisplay.getCurrentHoveredStack();
        List<Text> eventTooltips = WynntilsTooltipAdapter.getTooltips(event);
        if (currentHoveredStack == null || eventTooltips.isEmpty()) return;
        if (!WeightDisplay.isTrackedMythic(currentHoveredStack)) return;
        if (WeightDisplay.isUnidentified(currentHoveredStack)) return;

        String cleanName = WeightDisplay.extractCleanName(currentHoveredStack);
        WeightDisplay.ItemData itemData = WeightDisplay.getSelectedItemData(cleanName);
        if (itemData == null) return;

        if (WeightDisplay.hasCycleInput()) {
            itemData = WeightDisplay.applyCycleInput(cleanName);
        }

        int hash = currentHoveredStack.getComponents().hashCode();
        WeightDisplay.ItemData scaleData = WeightDisplay.weightCacheByHash.get(hash);
        if (scaleData == null) {
            scaleData = WeightDisplay.computeScale(currentHoveredStack);
            if (scaleData != null && !scaleData.data().isEmpty()) {
                WeightDisplay.weightCacheByHash.put(hash, scaleData);
            }
        }
        if (scaleData == null || scaleData.data().isEmpty()) return;

        int idx = Math.min(itemData.index(), scaleData.data().size() - 1);
        WeightDisplay.WeightData currentProfile = itemData.data().get(idx);

        List<Text> tooltipList = new ArrayList<>(eventTooltips);

        if (tooltipList.size() >= 4 && WynnExtrasConfig.INSTANCE.showWeight) {
            List<Text> scoreBlock = new ArrayList<>();
            scoreBlock.add(Text.empty());
            WynnExtrasConfig.MythicScaleSource renderedSource = null;
            for (int j = 0; j < scaleData.data().size(); j++) {
                WeightDisplay.WeightData wd = scaleData.data().get(j);
                if (WeightDisplay.isShowingBothScaleSources(cleanName) && wd.source() != renderedSource) {
                    renderedSource = wd.source();
                    scoreBlock.add(Text.literal("  ↳ " + renderedSource)
                            .styled(s -> s.withColor(0xAAAAAA)));
                }
                boolean cur = (j == idx);
                float score = wd.score();
                Text scoreText = Text.literal(String.format(" [%.1f%%]", score))
                        .styled(s -> s.withColor(WeightDisplay.getScaleColor(score)).withBold(cur));
                String indent = WeightDisplay.isShowingBothScaleSources(cleanName) ? "    ↳ " : "  ↳ ";
                Text label = Text.literal(indent + WeightDisplay.getScaleLabel(wd.weightName()))
                        .styled(s -> s.withColor(cur ? 0xFFFFFF : 0xAAAAAA).withBold(cur))
                        .copy().append(scoreText);
                scoreBlock.add(label);
            }
            if (scaleData.data().size() > 1) {
                scoreBlock.add(Text.literal("  ↳ Use ↑/↓ (W/S) to cycle").styled(s -> s.withColor(0x555555)));
            }
            if (WeightDisplay.shouldShowScaleSourceControls(cleanName)) {
                scoreBlock.add(Text.literal("  ↳ Use ←/→ (A/D) to switch source")
                        .styled(s -> s.withColor(0x555555)));
                scoreBlock.add(Text.literal("  ↳ Currently using: "
                                + WeightDisplay.getSelectedScaleSource(cleanName))
                        .styled(s -> s.withColor(0x555555)));
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

        if (added > 0 || tooltipList.size() != eventTooltips.size()) {
            WynntilsTooltipAdapter.setTooltips(event, tooltipList);
        }
    }
}
