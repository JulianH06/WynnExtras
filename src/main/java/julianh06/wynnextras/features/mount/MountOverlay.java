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
    public void onTick(TickEvent event) {
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

            //Map<MaterialType, Integer> solved = Solver.solve(stats, materialData);

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
}
