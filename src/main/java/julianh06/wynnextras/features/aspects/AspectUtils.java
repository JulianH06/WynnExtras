package julianh06.wynnextras.features.aspects;

import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.ApiAspect;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

import static julianh06.wynnextras.wtshim.utils.wynn.ContainerUtils.clickOnSlot;

public class AspectUtils {
    static List<ApiAspect> apiAspects = WynncraftApiHandler.fetchAllAspects();
    private static String pendingRaidJoin = null;
    private static boolean partyFinderJoinListenerRegistered = false;
    private static boolean clickedQueue = false;
    private static boolean clickedRaid = false;
    private static int ticksSinceQueueClick = 0;
    private static int partyFinderJoinTicks = 0;

    public static List<ApiAspect> getApiAspects() {
        return apiAspects;
    }

    public static ApiAspect findApiAspectByName(String name) {
        try {
            List<ApiAspect> allAspects = apiAspects;
            return allAspects.stream()
                    .filter(a -> a.getName().equals(name))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static ItemStack createAspectFlameIcon(ApiAspect apiAspect, boolean isMaxed) {
        if (apiAspect == null || apiAspect.getIcon() == null) {
            return ItemStack.EMPTY;
        }

        try {
            ApiAspect.IconValue iv = apiAspect.getIcon().getValueObject();
            if (iv == null) return ItemStack.EMPTY;

            // Get the item from API
            Identifier id = Identifier.of(iv.getId());
            Item item = Registries.ITEM.get(id);
            ItemStack stack = new ItemStack(item);

            // Set custom model data (same as AspectsTabWidget in Profile Viewer)
            if (iv.getCustomModelData() != null && iv.getCustomModelData().getRangeDispatch() != null) {
                int cmd = iv.getCustomModelData().getRangeDispatch().getFirst() + (isMaxed ? 1 : 0);
                stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of((float) cmd), List.of(), List.of(), List.of()));
            }

            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public static void joinRaidPartyFinder(String raidCode) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        // Close this screen
        client.setScreen(null);

        // Set the raid we want to join
        pendingRaidJoin = raidCode;

        // Open party finder
        client.player.networkHandler.sendChatCommand("pf");

        clickedQueue = false;
        clickedRaid = false;
        ticksSinceQueueClick = 0;
        partyFinderJoinTicks = 0;
        registerPartyFinderJoinListener();
    }

    private static void registerPartyFinderJoinListener() {
        if (partyFinderJoinListenerRegistered) return;
        partyFinderJoinListenerRegistered = true;
        ClientTickEvents.END_CLIENT_TICK.register(clientTick -> {
            if (pendingRaidJoin == null || clickedRaid) {
                return; // Done, listener will stay registered but do nothing
            }
            if (++partyFinderJoinTicks > 200) {
                pendingRaidJoin = null;
                clickedQueue = false;
                clickedRaid = false;
                return;
            }

            if (McUtils.player() == null || clientTick.currentScreen == null) return;

            ScreenHandler menu = McUtils.containerMenu();
            if (menu == null) return;

            // Step 1: Click "Party Queue" button (slot 49)
            if (!clickedQueue && menu.slots.size() > 49) {
                Slot slot = menu.getSlot(49);
                if (slot != null && slot.getStack() != null && slot.getStack().getName() != null) {
                    String name = slot.getStack().getName().getString();
                    if (name.contains("Queue")) {
                        clickOnSlot(49, menu.syncId, 0, menu.getStacks());
                        clickedQueue = true;
                        ticksSinceQueueClick = 0;
                        return;
                    }
                }
            }

            // Wait 8 ticks after clicking queue before clicking raid
            if (clickedQueue && !clickedRaid) {
                ticksSinceQueueClick++;
                if (ticksSinceQueueClick < 8) {
                    return; // Wait more
                }
            }

            // Step 2: Click the specific raid button - search by name instead of hardcoded slot
            if (clickedQueue && !clickedRaid && menu.slots.size() > 20) {
                // Search for the raid by name in slots 10-20
                String searchName = switch (pendingRaidJoin) {
                    case "NOTG" -> "Nest of the Grootslangs";
                    case "NOL" -> "Orphion's Nexus of Light";
                    case "TCC" -> "The Canyon Colossus";
                    case "TNA" -> "The Nameless Anomaly";
                    default -> "";
                };

                for (int slotIdx = 10; slotIdx <= 20; slotIdx++) {
                    if (menu.slots.size() <= slotIdx) continue;
                    Slot slot = menu.getSlot(slotIdx);
                    if (slot != null && slot.getStack() != null && slot.getStack().getName() != null) {
                        String itemName = slot.getStack().getName().getString();
                        if (itemName.contains(searchName) || itemName.contains(pendingRaidJoin)) {
                            clickOnSlot(slotIdx, menu.syncId, 0, menu.getStacks());
                            clickedRaid = true;
                            pendingRaidJoin = null;
                            return;
                        }
                    }
                }
            }
        });
    }

    public static ItemStack toItemStack(ApiAspect aspect, boolean max, int tier) {
        ApiAspect.Icon icon = aspect.getIcon();
        if (icon == null) return ItemStack.EMPTY;

        if (icon.getValueString() != null) {
            Identifier id = Identifier.of(icon.getValueString());
            Item item = Registries.ITEM.get(id);
            return new ItemStack(item);
        }

        if (icon.getValueObject() != null) {
            ApiAspect.IconValue iv = icon.getValueObject();
            Identifier id = Identifier.of(iv.getId());
            Item item = Registries.ITEM.get(id);
            ItemStack stack = new ItemStack(item);

            if (iv.getCustomModelData() != null) {
                try {
                    int cmd = iv.getCustomModelData().getRangeDispatch().getFirst() + (max ? 1 : 0);
                    stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of((float) cmd), List.of(), List.of(), List.of()));
                } catch (NumberFormatException ignored) {}
            }

            if(aspect.getName() != null) {
                try {
                    StyledText name = StyledText.fromString(aspect.getName()).withoutFormatting();
                    String color = "";
                    if(aspect.getRarity().equals("mythic")) {
                        color = "§5";
                    }
                    if(aspect.getRarity().equals("fabled")) {
                        color = "§c";
                    }
                    if(aspect.getRarity().equals("legendary")) {
                        color = "§b";
                    }
                    stack.set(DataComponentTypes.CUSTOM_NAME, Text.of(color + name.getString()));
                } catch (NumberFormatException ignored) {}
            }

            if(aspect.getTiers() != null) {
                if(aspect.getTiers().get(String.valueOf(tier)) != null) {
                    List<String> lore = aspect.getTiers().get(String.valueOf(tier)).getDescription();
                    stack.set(DataComponentTypes.LORE, new LoreComponent(WynncraftApiHandler.parseStyledHtml(lore)));
                }
            }

            return stack;
        }

        return ItemStack.EMPTY;
    }

    public static String getAspectColorCode(LootPoolData.AspectEntry aspect) {
        if(aspect.rarity.equalsIgnoreCase("mythic")) return "§5";
        else if(aspect.rarity.equalsIgnoreCase("fabled")) return "§c";
        else if(aspect.rarity.equalsIgnoreCase("legendary")) return "§b";
        return "";
    }

    public static int romanToInt(String roman) {
        return switch (roman.toUpperCase()) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            default -> 0;
        };
    }

    public static String convertAmountToTierInfo(int amount, String rarity) {
        int[] tier1 = {1, 1, 1};
        int[] tier2 = {4, 14, 4};
        int[] tier3 = {10, 60, 25};
        int[] tier4 = {0, 0, 120};

        int rarityIndex;
        switch (rarity) {
            case "Mythic" -> rarityIndex = 0;
            case "Fabled" -> rarityIndex = 1;
            case "Legendary" -> rarityIndex = 2;
            default -> {
                return "";
            }
        }

        int maxAmount = tier1[rarityIndex] + tier2[rarityIndex] + tier3[rarityIndex] + tier4[rarityIndex];
        if (amount >= maxAmount) {
            return "[MAX]";
        }

        int tier1Total = tier1[rarityIndex];
        int tier2Total = tier1Total + tier2[rarityIndex];
        int tier3Total = tier2Total + tier3[rarityIndex];

        if (amount < tier2Total) {
            int progress = amount - tier1Total;
            return "Tier I [" + progress + "/" + tier2[rarityIndex] + "]";
        } else if (amount < tier3Total) {
            int progress = amount - tier2Total;
            return "Tier II [" + progress + "/" + tier3[rarityIndex] + "]";
        } else {
            int progress = amount - tier3Total;
            return "Tier III [" + progress + "/" + tier4[rarityIndex] + "]";
        }
    }

    public static double getRemainingToMax(int remainingToNext, String rarity, int currentTier) {
        String key = rarity.toLowerCase() + "_" + (currentTier + 1);
        int toMax = switch (key) {
            case "mythic_1" -> 14;
            case "mythic_2" -> 10;

            case "fabled_1" -> 74;
            case "fabled_2" -> 60;

            case "legendary_1" -> 149;
            case "legendary_2" -> 145;
            case "legendary_3" -> 120;

            default -> 0;
        };

        if (currentTier == 0) return toMax + 1;
        else return toMax + remainingToNext;
    }

    public static double getRarityMultiplier(String rarity) {
        return switch (rarity.toLowerCase()) {
            case "mythic" -> WynnExtrasConfig.INSTANCE.mythicAspectMultiplier;
            case "fabled" -> WynnExtrasConfig.INSTANCE.fabledAspectMultiplier;
            case "legendary" -> WynnExtrasConfig.INSTANCE.legendaryAspectMultiplier;
            default -> 1;
        };
    }
}
