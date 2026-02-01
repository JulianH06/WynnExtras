package julianh06.wynnextras.features.lootruns;

import com.wynntils.core.components.Models;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.achievements.AchievementManager;
import julianh06.wynnextras.features.aspects.LootrunLootPoolData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Tracks lootrun chest opens and records statistics
 */
@WEModule
public class LootrunTracker {

    private static boolean wasInLootrunChest = false;
    private static Set<String> processedItems = new HashSet<>();
    private static int tickCounter = 0;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!Models.WorldState.onWorld()) return;

        tickCounter++;
        if (tickCounter % 5 != 0) return; // Check every 5 ticks

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Check if we're in a lootrun chest
        boolean inLootrunChest = isInLootrunChest();

        if (inLootrunChest && !wasInLootrunChest) {
            // Just opened a lootrun chest
            onLootrunChestOpened();
        } else if (!inLootrunChest && wasInLootrunChest) {
            // Just closed a lootrun chest
            onLootrunChestClosed();
        } else if (inLootrunChest) {
            // Still in chest - scan for items
            scanChestItems();
        }

        wasInLootrunChest = inLootrunChest;
    }

    private boolean isInLootrunChest() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) return false;

        String title = mc.currentScreen.getTitle().getString();
        return LootrunLootPoolData.isLootrunChest(title);
    }

    private void onLootrunChestOpened() {
        processedItems.clear();

        // Record pull
        LootrunStatistics.INSTANCE.recordPull();

        // Achievement integration
        AchievementManager.INSTANCE.onLootrunChestOpened();

        // Play sound if enabled
        if (WynnExtrasConfig.INSTANCE.lootrunSoundAlerts) {
            // Sound will be played when finding mythics
        }
    }

    private void onLootrunChestClosed() {
        // Save statistics
        LootrunStatistics.save();
    }

    private void scanChestItems() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.currentScreenHandler == null) return;

        for (Slot slot : mc.player.currentScreenHandler.slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            String itemId = getItemId(stack);
            if (processedItems.contains(itemId)) continue;

            processItem(stack);
            processedItems.add(itemId);
        }
    }

    private void processItem(ItemStack stack) {
        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(stack);
        if (wynnItemOpt.isEmpty()) return;

        WynnItem wynnItem = wynnItemOpt.get();

        if (wynnItem instanceof GearItem gear) {
            GearTier tier = gear.getGearTier();
            if (tier == null) return;

            switch (tier) {
                case MYTHIC:
                    LootrunStatistics.INSTANCE.recordMythic();
                    AchievementManager.INSTANCE.onLootrunMythicFound();
                    playMythicSound();
                    break;
                case FABLED:
                    LootrunStatistics.INSTANCE.recordFabled();
                    break;
                case LEGENDARY:
                    LootrunStatistics.INSTANCE.recordLegendary();
                    break;
                case RARE:
                    LootrunStatistics.INSTANCE.recordRare();
                    break;
                case UNIQUE:
                    LootrunStatistics.INSTANCE.recordUnique();
                    break;
                default:
                    break;
            }
        }

        // Check for shiny items
        String itemName = stack.getName().getString();
        if (itemName.toLowerCase().contains("shiny")) {
            LootrunStatistics.INSTANCE.recordShiny();
        }
    }

    private String getItemId(ItemStack stack) {
        // Create a unique identifier for this item
        return stack.getName().getString() + "_" + stack.getCount();
    }

    private void playMythicSound() {
        if (!WynnExtrasConfig.INSTANCE.lootrunSoundAlerts) return;

        McUtils.playSoundAmbient(
                net.minecraft.sound.SoundEvent.of(net.minecraft.util.Identifier.of("entity.player.levelup")),
                1.0f, 1.0f
        );
    }

    public static void register() {
        LootrunStatistics.load();
        LootrunHudOverlay.register();
    }
}
