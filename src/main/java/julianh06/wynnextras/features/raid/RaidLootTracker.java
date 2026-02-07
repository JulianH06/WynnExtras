package julianh06.wynnextras.features.raid;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;

public class RaidLootTracker {

    private static final String REWARD_CHEST_TITLE = "\uDAFF\uDFEA\uE00E";
    private static final int CHEST_START = 27;
    private static final int CHEST_END = 53;

    // Reward chest coordinates for each raid
    private static final Map<String, double[]> REWARD_CHEST_COORDS = Map.of(
            "NOTG", new double[]{10342, 41, 3111},
            "NOL",  new double[]{11005, 58, 2909},
            "TCC",  new double[]{10817, 45, 3901},
            "TNA",  new double[]{24489, 8, -23878}
    );

    private static boolean loggedThisChest = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;

            Screen screen = mc.currentScreen;
            if (screen == null) {
                loggedThisChest = false;
                return;
            }

            if (!REWARD_CHEST_TITLE.equals(screen.getTitle().getString())) {
                loggedThisChest = false;
                return;
            }

            if(!(screen instanceof HandledScreen<?> handledScreen)) {
                loggedThisChest = false;
                return;
            }

            if(!handledScreen.getScreenHandler().getSlot(4).hasStack()) {
                loggedThisChest = false;
                return;
            }

            if (!loggedThisChest) {
                parseChest();
                loggedThisChest = true;
            }
        });
    }

    private static void parseChest() {
        // Check config toggle
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        if (!config.toggleRaidLootTracker) return;

        ScreenHandler handler = McUtils.containerMenu();
        if (handler == null) return;

        // DEBUG: Print all slots to chat
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§6§l[DEBUG] Reward Chest Slots:"), false);
            for (int i = 0; i < handler.slots.size(); i++) {
                Slot slot = handler.getSlot(i);
                if (slot != null && slot.hasStack()) {
                    ItemStack stack = slot.getStack();
                    String name = stack.getName().getString();
                    int count = stack.getCount();
                    mc.player.sendMessage(Text.literal("§7Slot " + i + ": §e" + name + " §7x" + count), false);
                }
            }
        }

        RaidLootData data = RaidLootConfig.INSTANCE.data;
        data.initSession();

        // Detect which raid we're in
        String currentRaid = detectRaid();
        RaidLootData.RaidSpecificLoot raidData = data.getOrCreateRaidData(currentRaid);
        RaidLootData.RaidSpecificLoot sessionRaidData = data.getOrCreateSessionRaidData(currentRaid);
        raidData.completionCount++;
        sessionRaidData.completionCount++;
        data.sessionData.completionCount++;

        for (int i = CHEST_START; i <= CHEST_END; i++) {
            Slot slot = handler.getSlot(i);
            if (slot == null) continue;

            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;

            String name = cleanName(stack.getName().getString());
            String rawName = stack.getName().getString();
            int count = stack.getCount();

            // ===== Emeralds =====
            if (name.equals("Emerald Block")) {
                data.emeraldBlocks += count;
                raidData.emeraldBlocks += count;
                data.sessionData.emeraldBlocks += count;
                sessionRaidData.emeraldBlocks += count;
            }
            if (name.equals("Liquid Emerald")) {
                data.liquidEmeralds += count;
                raidData.liquidEmeralds += count;
                data.sessionData.liquidEmeralds += count;
                sessionRaidData.liquidEmeralds += count;
            }

            // ===== Amplifiers =====
            if (name.contains("Amplifier")) {
                if (name.contains(" III")) {
                    data.amplifierTier3 += count;
                    raidData.amplifierTier3 += count;
                    data.sessionData.amplifierTier3 += count;
                    sessionRaidData.amplifierTier3 += count;
                } else if (name.contains(" II")) {
                    data.amplifierTier2 += count;
                    raidData.amplifierTier2 += count;
                    data.sessionData.amplifierTier2 += count;
                    sessionRaidData.amplifierTier2 += count;
                } else if (name.contains(" I")) {
                    data.amplifierTier1 += count;
                    raidData.amplifierTier1 += count;
                    data.sessionData.amplifierTier1 += count;
                    sessionRaidData.amplifierTier1 += count;
                }
            }

            // ===== Crafter Bags =====
            if (name.contains("Crafter Bag")) {
                data.totalBags += count;
                raidData.totalBags += count;
                data.sessionData.totalBags += count;
                sessionRaidData.totalBags += count;
                if (name.startsWith("Stuffed")) {
                    data.stuffedBags += count;
                    raidData.stuffedBags += count;
                    data.sessionData.stuffedBags += count;
                    sessionRaidData.stuffedBags += count;
                } else if (name.startsWith("Packed")) {
                    data.packedBags += count;
                    raidData.packedBags += count;
                    data.sessionData.packedBags += count;
                    sessionRaidData.packedBags += count;
                } else if (name.startsWith("Varied")) {
                    data.variedBags += count;
                    raidData.variedBags += count;
                    data.sessionData.variedBags += count;
                    sessionRaidData.variedBags += count;
                }
            }

            // ===== Tomes =====
            if (name.contains("Tome")) {
                data.totalTomes += count;
                raidData.totalTomes += count;
                data.sessionData.totalTomes += count;
                sessionRaidData.totalTomes += count;
                // Check tooltip for "Mythic" to determine rarity
                boolean isMythic = checkTooltipForMythic(stack);
                if (isMythic) {
                    data.mythicTomes += count;
                    raidData.mythicTomes += count;
                    data.sessionData.mythicTomes += count;
                    sessionRaidData.mythicTomes += count;
                } else {
                    data.fabledTomes += count;
                    raidData.fabledTomes += count;
                    data.sessionData.fabledTomes += count;
                    sessionRaidData.fabledTomes += count;
                }
            }

            // ===== Charms =====
            if (name.contains("Charm")) {
                data.totalCharms += count;
                raidData.totalCharms += count;
                data.sessionData.totalCharms += count;
                sessionRaidData.totalCharms += count;
            }
        }


        RaidLootConfig.INSTANCE.save();
    }

    private static String detectRaid() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return "UNKNOWN";

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();

        String closest = "UNKNOWN";
        double minDist = Double.MAX_VALUE;

        for (Map.Entry<String, double[]> entry : REWARD_CHEST_COORDS.entrySet()) {
            double[] pos = entry.getValue();
            double dist = Math.sqrt(Math.pow(px - pos[0], 2) + Math.pow(py - pos[1], 2) + Math.pow(pz - pos[2], 2));
            if (dist < minDist) {
                minDist = dist;
                closest = entry.getKey();
            }
        }

        // Sanity check: if player is too far from any known chest, return UNKNOWN
        return minDist < 100 ? closest : "UNKNOWN";
    }

    private static void sendChatDebug(RaidLootData d, String raidName) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        mc.player.sendMessage(
                Text.literal("§6Raid: §e" + raidName),
                false
        );

        mc.player.sendMessage(
                Text.literal(
                        "§aEmeralds: §e" +
                                d.getStacks() + " STX " +
                                d.getRemainingLiquidEmeralds() + " LE " +
                                d.getRemainingEmeraldBlocks() + " EB"
                ),
                false
        );

        mc.player.sendMessage(
                Text.literal(
                        "§aAmplifiers: §e" +
                                d.getTotalAmplifiers() +
                                " §7(I: " + d.amplifierTier1 +
                                " | II: " + d.amplifierTier2 +
                                " | III: " + d.amplifierTier3 + ")"
                ),
                false
        );

        mc.player.sendMessage(
                Text.literal(
                        "§bCrafter Bags: §e" + d.totalBags +
                                " §7(Stuffed: " + d.stuffedBags +
                                " | Packed: " + d.packedBags +
                                " | Varied: " + d.variedBags + ")"
                ),
                false
        );

        mc.player.sendMessage(
                Text.literal(
                        "§dTomes: §e" + d.totalTomes +
                                " §7(Mythic: " + d.mythicTomes +
                                " | Fabled: " + d.fabledTomes + ")"
                ),
                false
        );

        mc.player.sendMessage(
                Text.literal(
                        "§6Charms: §e" + d.totalCharms
                ),
                false
        );

        mc.player.sendMessage(
                Text.literal(
                        "§5Aspects: §e" + d.totalAspects +
                                " §7(§5" + d.mythicAspects +
                                " §c" + d.fabledAspects +
                                " §6" + d.legendaryAspects + "§7)"
                ),
                false
        );
    }

    private static String cleanName(String name) {
        return name.replaceAll("§.", "").trim();
    }

    private static boolean checkTooltipForMythic(ItemStack stack) {
        try {
            if (stack.getComponents() == null) return false;
            LoreComponent loreComponent = stack.getComponents().get(DataComponentTypes.LORE);
            if (loreComponent == null) return false;

            List<Text> loreLines = loreComponent.lines();
            for (Text line : loreLines) {
                String lineStr = line.getString().toLowerCase();
                if (lineStr.contains("mythic")) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static String getAspectRarity(ItemStack stack) {
        try {
            String hexCode = stack.getCustomName().getStyle().getColor().getHexCode();
            if(hexCode.equals("#AA00AA")) return "mythic";
            else if(hexCode.equals("#FF5555")) return "fabled";
            else if(hexCode.equals("#55FFFF")) return "legendary";
        } catch (Exception ignored) {}
        return null;
    }
}