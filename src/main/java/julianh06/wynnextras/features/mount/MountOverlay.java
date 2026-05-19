package julianh06.wynnextras.features.mount;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.utils.UI.UIUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.regex.Matcher;

@WEModule
public class MountOverlay {
    public record RequiredMaterialInfo(Identifier texture, String name, Integer quantity, Integer level) {}
    public record StatEntry(Integer current, Integer limit, Integer max) {}

    private static final Map<ItemStack, List<RequiredMaterialInfo>> cache = new HashMap<>();

    public static void render(DrawContext context, int mouseX, int mouseY) {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen == null) {
            cache.clear();
            return;
        }
        if (!WynnExtrasConfig.INSTANCE.showMountHelper) return;
        List<Text> siblings = currentScreen.getTitle().getSiblings();
        if (siblings == null || siblings.isEmpty()) return;
        if (!siblings.getFirst().getString().equals("\uDAFF\uDFED\uE058")) return;

        if (currentScreen instanceof GenericContainerScreen container) {
            List<Slot> slots = container.getScreenHandler().slots;
            if (slots.size() < 46) return;

            List<Slot> feederSlots = List.of(slots.get(9), slots.get(18), slots.get(27), slots.get(36), slots.get(45));
            List<List<RequiredMaterialInfo>> materials = feederSlots.stream().map(MountOverlay::solve).toList();

            renderMaterialHeader(context, container, feederSlots, materials);
            for (int i = 0; i < feederSlots.size(); i++) {
                renderMaterialReqs(context, container, feederSlots.get(i), materials.get(i), mouseX, mouseY);
            }
        }
    }

    private static void renderMaterialHeader(DrawContext context, GenericContainerScreen container, List<Slot> slots, List<List<RequiredMaterialInfo>> materials) {
        int maxMaterials = materials.stream().mapToInt(List::size).max().orElse(0);
        if (maxMaterials == 0) return;

        HandledScreenAccessor screen = (HandledScreenAccessor) container;
        Slot firstSlotWithMaterials = slots.stream()
                .filter(slot -> !materials.get(slots.indexOf(slot)).isEmpty())
                .findFirst()
                .orElse(slots.getFirst());
        int firstMaterialX = screen.getX() + firstSlotWithMaterials.x - 30;
        int materialAreaWidth = 16 + (maxMaterials - 1) * 20;
        int headerX = firstMaterialX - (maxMaterials - 1) * 20 + materialAreaWidth / 2;
        int headerY = screen.getY() + firstSlotWithMaterials.y - 13;

        UIUtils ui = new UIUtils(context, 1, 0, 0);
        ui.drawText(
                "Required materials",
                headerX,
                headerY,
                CustomColor.fromHexString("FFFFFF"),
                HorizontalAlignment.CENTER,
                VerticalAlignment.TOP,
                0.85f
        );
    }

    public static void renderMaterialReqs(DrawContext context, GenericContainerScreen container, Slot slot, List<RequiredMaterialInfo> solved, int mouseX, int mouseY) {
        HandledScreenAccessor screen = (HandledScreenAccessor) container;
        UIUtils ui = new UIUtils(context, 1, 0, 0);
        Text hoverText = null;
        for (int i = 0; i < solved.size(); i++) {
            int xPos = screen.getX() + slot.x - i * 20 - 30;
            int yPos = screen.getY() + slot.y;
            RequiredMaterialInfo info = solved.get(i);
            ui.drawImage(info.texture, xPos, yPos, 16, 16);
            ui.drawText(Text.literal(String.valueOf(info.quantity)), xPos + 10, yPos + 10, CustomColor.NONE, 1f);
            if (mouseX > xPos && mouseX < xPos + 16 && mouseY > yPos && mouseY < yPos + 16) {
                hoverText = Text.literal(info.quantity + "x " + info.name + " (lvl " + info.level + ")");
            }
        }
        if (hoverText != null) {
            context.drawTooltip(hoverText, mouseX, mouseY);
        }
    }

    public static Map<MountStat, StatEntry> getStats(ItemStack stack) {
        List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);
        Map<MountStat, StatEntry> result = new HashMap<>();

        tooltip.forEach(line -> {
            Matcher matcher = MountStat.PATTERN.matcher(line.getString());
            if (matcher.matches()) {
                String stat = matcher.group("stat");
                int current = Integer.parseInt(matcher.group("current"));
                int limit = Integer.parseInt(matcher.group("limit"));
                int max = Integer.parseInt(matcher.group("max"));

                MountStat mountStat = MountStat.fromString(stat);
                if (mountStat == null) return;

                result.put(mountStat, new StatEntry(current, limit, max));
            }
        });

        return result;
    }

    private static OptionalInt findHighestLevel(Map<MountStat, StatEntry> mountStats) {
        if (mountStats.isEmpty()) return OptionalInt.of(0);
        return mountStats.values().stream().mapToInt(s -> s.current).reduce(Math::max);
    }

    public static List<RequiredMaterialInfo> solve(Slot slot) {
        if (slot.getStack() == null) return new ArrayList<>();
        var cached = cache.get(slot.getStack());
        if (cached != null) return cached;

        Map<MountStat, StatEntry> goal = getStats(slot.getStack());

        Map<MaterialType, Integer> result = new HashMap<>();
        int highestLevel = findHighestLevel(goal).orElse(0);

        // Calculate needed for each stat
        Map<MountStat, Integer> needed = new HashMap<>();
        for (Map.Entry<MountStat, StatEntry> entry : goal.entrySet()) {
            int need = entry.getValue().max() - entry.getValue().limit();
            if (need > 0) needed.put(entry.getKey(), need);
        }

        // Keep adding the most efficient material for remaining needs
        while (!needed.isEmpty()) {
            MaterialType bestType = null;
            int bestScore = 0;

            for (MaterialType type : MaterialType.values()) {
                MaterialStats stats = MaterialStats.get(type, highestLevel);
                highestLevel = stats.getLevel(); // rounds it to an actual level
                int score = 0;
                for (Map.Entry<MountStat, Integer> need : needed.entrySet()) {
                    score += stats.getStats().getOrDefault(need.getKey(), 0);
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestType = type;
                }
            }

            if (bestType == null) break;

            result.merge(bestType, 1, Integer::sum);
            MaterialStats stats = MaterialStats.get(bestType, highestLevel);
            stats.getStats().forEach((stat, value) -> {
                int remaining = needed.getOrDefault(stat, 0) - value;
                if (remaining <= 0) needed.remove(stat);
                else needed.put(stat, remaining);
            });
        }

        List<RequiredMaterialInfo> finalR = new ArrayList<>();
        int lvl = highestLevel;
        result.forEach((type, quantity) ->
                finalR.add(new RequiredMaterialInfo(type.getTexture(lvl), type.getName(lvl), quantity, lvl)));

        cache.put(slot.getStack(), finalR);
        return finalR;
    }
}
