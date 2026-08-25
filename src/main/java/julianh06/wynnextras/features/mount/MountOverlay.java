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
    public record RequiredMaterialInfo(Identifier texture, String name, Integer quantity, Integer level) {
    }

    public record StatEntry(Integer current, Integer limit, Integer max) {
    }

    private static final Map<ItemStack, List<RequiredMaterialInfo>> cache = new HashMap<>();

    public static void render(DrawContext context, int mouseX, int mouseY) {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen == null) {
            cache.clear();
            return;
        }
        if (!WynnExtrasConfig.INSTANCE.showMountHelper)
            return;
        List<Text> siblings = currentScreen.getTitle().getSiblings();
        if (siblings == null || siblings.isEmpty())
            return;
        if (!siblings.getFirst().getString().equals("\uDAFF\uDFED\uE058"))
            return;

        if (currentScreen instanceof GenericContainerScreen container) {
            List<Slot> slots = container.getScreenHandler().slots;
            if (slots.size() < 46)
                return;

            List<Slot> feederSlots = List.of(slots.get(9), slots.get(18), slots.get(27), slots.get(36), slots.get(45));
            List<List<RequiredMaterialInfo>> materials = feederSlots.stream().map(MountOverlay::solve).toList();

            renderMaterialHeader(context, container, feederSlots, materials);
            for (int i = 0; i < feederSlots.size(); i++) {
                renderMaterialReqs(context, container, feederSlots.get(i), materials.get(i), mouseX, mouseY);
            }
        }
    }

    private static void renderMaterialHeader(DrawContext context, GenericContainerScreen container, List<Slot> slots,
            List<List<RequiredMaterialInfo>> materials) {
        int maxMaterials = materials.stream().mapToInt(List::size).max().orElse(0);
        if (maxMaterials == 0)
            return;

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
                0.85f);
    }

    public static void renderMaterialReqs(DrawContext context, GenericContainerScreen container, Slot slot,
            List<RequiredMaterialInfo> solved, int mouseX, int mouseY) {
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
        List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player,
                TooltipType.BASIC);
        Map<MountStat, StatEntry> result = new HashMap<>();

        tooltip.forEach(line -> {
            Matcher matcher = MountStat.PATTERN.matcher(line.getString());
            if (matcher.matches()) {
                String stat = matcher.group("stat");
                int current = Integer.parseInt(matcher.group("current"));
                int limit = Integer.parseInt(matcher.group("limit"));
                int max = Integer.parseInt(matcher.group("max"));

                MountStat mountStat = MountStat.fromString(stat);
                if (mountStat == null)
                    return;

                result.put(mountStat, new StatEntry(current, limit, max));
            }
        });

        return result;
    }

    private static OptionalInt findHighestLevel(Map<MountStat, StatEntry> mountStats) {
        if (mountStats.isEmpty())
            return OptionalInt.of(0);
        return mountStats.values().stream().mapToInt(s -> s.current).reduce(Math::max);
    }

    public static List<RequiredMaterialInfo> solve(Slot slot) {
        if (slot.getStack() == null)
            return new ArrayList<>();
        var cached = cache.get(slot.getStack());
        if (cached != null)
            return cached;

        Map<MountStat, StatEntry> goal = getStats(slot.getStack());

        int highestLevel = findHighestLevel(goal).orElse(0);

        // Calculate needed for each stat
        Map<MountStat, Integer> needed = new HashMap<>();
        for (Map.Entry<MountStat, StatEntry> entry : goal.entrySet()) {
            int need = entry.getValue().max() - entry.getValue().limit();
            if (need > 0)
                needed.put(entry.getKey(), need);
        }

        Map<MaterialType, Integer> result = new HashMap<>();
        highestLevel = optimizeNeededv2(result, highestLevel, needed);

        List<RequiredMaterialInfo> finalR = new ArrayList<>();
        int lvl = highestLevel;
        result.forEach((type, quantity) -> finalR
                .add(new RequiredMaterialInfo(type.getTexture(lvl), type.getName(lvl), quantity, lvl)));

        cache.put(slot.getStack(), finalR);
        return finalR;
    }

    public static int optimizeNeeded(Map<MaterialType, Integer> result, int highestLevel,
            Map<MountStat, Integer> needed) {
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

            if (bestType == null)
                break;

            result.merge(bestType, 1, Integer::sum);
            MaterialStats stats = MaterialStats.get(bestType, highestLevel);
            stats.getStats().forEach((stat, value) -> {
                int remaining = needed.getOrDefault(stat, 0) - value;
                if (remaining <= 0)
                    needed.remove(stat);
                else
                    needed.put(stat, remaining);
            });
        }
        return highestLevel;
    }

    static int statCount = MountStat.values().length;
    static int materialCount = MaterialType.values().length;

    // Algorithm based on
    // https://github.com/Wynnbreeder/wynnbreeder/blob/main/app.js
    public static int optimizeNeededv2(Map<MaterialType, Integer> result, int highestLevel,
            Map<MountStat, Integer> needed) {
        // Keep adding the most efficient material for remaining needs

        boolean done = true;
        for (Integer num : needed.values()) {
            if (num.intValue() != 0) {
                done = false;
                break;
            }
        }
        if (done)
            return highestLevel;

        int[][] materialStatsTable = new int[materialCount][statCount];

        for (MaterialType type : MaterialType.values()) {
            MaterialStats stats = MaterialStats.get(type, highestLevel);
            highestLevel = stats.getLevel(); // rounds it to an actual level

            for (Map.Entry<MountStat, Integer> s : stats.getStats().entrySet()) {
                materialStatsTable[type.ordinal()][s.getKey().ordinal()] = s.getValue();
            }
        }

        int[] need = new int[materialCount];

        int[] maxPerStat = new int[statCount];

        for (int i = 0; i < statCount; i++) {
            for (int j = 0; j < materialCount; j++) {
                if (materialStatsTable[j][i] > maxPerStat[i])
                    maxPerStat[i] = materialStatsTable[j][i];
            }
        }

        for (Map.Entry<MountStat, Integer> s : needed.entrySet()) {
            need[s.getKey().ordinal()] = s.getValue();
            // no possible list
            if (maxPerStat[s.getKey().ordinal()] == 0 && s.getValue() > 0)
                return highestLevel;
        }

        int[] greedyCounts = new int[materialCount];
        int[] remaining = need.clone();
        int greedySteps = 0;
        int maxGreedySteps = 2000;
        while (greedySteps < maxGreedySteps) {
            boolean allCovered = true;
            for (int i = 0; i < statCount; i++) {
                if (remaining[i] > 0) {
                    allCovered = false;
                    break;
                }
            }
            if (allCovered) {
                break;
            }

            int bestIndex = -1;
            int bestScore = -1;

            for (int m = 0; m < materialCount; m++) {
                int score = 0;
                for (int i = 0; i < statCount; i++) {
                    int contribution = materialStatsTable[m][i];
                    if (remaining[i] > 0 && contribution > 0) {
                        score += Math.min(remaining[i], contribution);
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = m;
                }
            }

            // no possible list
            if (bestIndex < 0 || bestScore < 0) {
                return highestLevel;
            }

            greedyCounts[bestIndex]++;
            greedySteps++;
            for (int i = 0; i < statCount; i++) {
                remaining[i] = Math.max(0, remaining[i] - materialStatsTable[bestIndex][i]);
            }
        }

        // no list found after max steps
        if (greedySteps >= maxGreedySteps)
            return highestLevel;

        int lowerBound = 0;
        for (int i = 0; i < statCount; i++) {
            if (need[i] > 0) {
                lowerBound = (int) Math.max(lowerBound, Math.ceil(need[i] * 1.0 / maxPerStat[i]));
            }
        }
        int upperBound = greedySteps;

        // not sure exactly what this does, but its used in canStillReach
        // it starts at max per stat and decreases until 0s.
        // I think this is maxPerStat excluding materials that have already been
        // considered by search
        int[][] perIndexMax = new int[materialCount + 1][statCount];
        for (int m = materialCount - 1; m >= 0; m--) {
            for (int i = 0; i < statCount; i++) {
                perIndexMax[m][i] = Math.max(materialStatsTable[m][i], perIndexMax[m + 1][i]);
            }
        }

        int[] counts = new int[materialCount];
        int[] solvedCounts = null;

        for (int total = lowerBound; total <= upperBound; total++) {
            for (int i = 0; i < statCount; i++) {
                counts[i] = 0;
            }

            if (search(counts, 0, total, need, new int[statCount], materialStatsTable, perIndexMax)) {
                solvedCounts = counts.clone();
                break;
            }

        }

        // unsolvable
        if (solvedCounts == null)
            return highestLevel;

        for (int m = 0; m < materialCount; m++) {
            if (solvedCounts[m] != 0) // Don't display 0 count ingredients
                result.put(MaterialType.values()[m], solvedCounts[m]);
        }

        return highestLevel;
    }

    private static boolean search(
            int[] counts,
            int materialIndex,
            int slotsLeft,
            int[] need,
            int[] coverage,
            int[][] materialStatsTable,
            int[][] perIndexMax

    ) {
        if (materialIndex == materialStatsTable.length) {
            return slotsLeft == 0 && meetsNeed(coverage, need);
        }

        if (!canStillReach(coverage, need, materialIndex, slotsLeft, perIndexMax)) {
            return false;
        }

        int[] material = materialStatsTable[materialIndex];
        for (int take = slotsLeft; take >= 0; take--) {
            counts[materialIndex] = take;
            int[] nextCoverage = new int[statCount];
            for (int i = 0; i < statCount; i++) {
                nextCoverage[i] = coverage[i] + take * material[i];
            }

            if (search(counts, materialIndex + 1, slotsLeft - take, need, nextCoverage, materialStatsTable, perIndexMax)) {
                return true;
            }
        }

        counts[materialIndex] = 0;
        return false;
    }

    private static boolean canStillReach(int[] coverage, int[] need, int materialIndex, int slotsLeft, int[][] perIndexMax) {
        int[] maxFromHere = perIndexMax[materialIndex];
        for (int i = 0; i < statCount; i++) {
            if (coverage[i] >= need[i]) {
                continue;
            }
            if (maxFromHere[i] == 0) {
                return false;
            }
            int remainingNeed = need[i] - coverage[i];
            if (remainingNeed > slotsLeft * maxFromHere[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean meetsNeed(int[] coverage, int[] need) {
        for (int i = 0; i < statCount; i++) {
            if (coverage[i] < need[i])
                return false;
        }
        return true;
    }

}
