package julianh06.wynnextras.features.mount;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.TickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.*;
import java.util.regex.Matcher;

@WEModule
public class MountOverlay {

    @SubscribeEvent
    public void onTick(TickEvent event) { // TODO cache and display results
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen == null) return;
        List<Text> siblings = currentScreen.getTitle().getSiblings();
        if (siblings == null || siblings.isEmpty()) return;
        if (!siblings.getFirst().getString().equals("\uDAFF\uDFED\uE058")) return;

        if (currentScreen instanceof GenericContainerScreen container) {
            List<Slot> slots = container.getScreenHandler().slots;
            if (slots.size() < 46) {
                WynnExtras.LOGGER.warn("Found to small mount container");
                return;
            }
            ItemStack mountOne = slots.get(9).getStack();
            ItemStack mountTwo = slots.get(18).getStack();
            ItemStack mountThree = slots.get(27).getStack();
            ItemStack mountFour = slots.get(36).getStack();
            ItemStack mountFive = slots.get(45).getStack();

            Map<MountStat, StatEntry> stats = getStats(mountOne);
            Map<MaterialType, MaterialStats> materialData = findHighestLevel(stats);

            Map<MaterialType, Integer> solved = solve(stats, materialData);
            System.out.println("found result: " + solved);

        } else WynnExtras.LOGGER.warn("mount screen is not a container");
    }

    public Map<MountStat, StatEntry> getStats(ItemStack stack) {
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
                if (mountStat == null) {
                    WynnExtras.LOGGER.warn("mount matched in one place but not another how did i even manage that");
                    return;
                }

                result.put(mountStat, new StatEntry(current, limit, max));
            }
        });

        return result;
    }

    private Map<MaterialType, MaterialStats> findHighestLevel(Map<MountStat, StatEntry> mountStats) {
        Map<MaterialType, MaterialStats> result = new HashMap<>();
        for (MaterialType mat : MaterialType.values()) {
            Set<MountStat> statTypesOnMaterial = mat.getStats();
            int lowest = Integer.MAX_VALUE;
            for (MountStat stat : statTypesOnMaterial) {
                lowest = Math.min(lowest, mountStats.get(stat).current());
            }

            MaterialStats materialStats = MaterialStats.get(mat, lowest);
            result.put(mat, materialStats);
        }
        return result; // TODO we also need to return the lvl
    }

    public static Map<MaterialType, Integer> solve(Map<MountStat, StatEntry> goal, Map<MaterialType, MaterialStats> materialData) {
        Map<MaterialType, Integer> result = new HashMap<>();

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
                MaterialStats stats = materialData.get(type);
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
            MaterialStats stats = materialData.get(bestType);
            stats.getStats().forEach((stat, value) -> {
                int remaining = needed.getOrDefault(stat, 0) - value;
                if (remaining <= 0) needed.remove(stat);
                else needed.put(stat, remaining);
            });
        }

        return result;
    }
}
