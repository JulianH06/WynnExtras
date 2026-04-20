package julianh06.wynnextras.features.misc;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import julianh06.wynnextras.core.WynnExtras;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmeraldGiver {
    public static final String DEFAULT_PLAYER = "cinfrascitizen";

    private enum State { IDLE, OPEN_GUILD_MENU, WAIT_MANAGE, CLICK_BANNER, WAIT_REWARDS, FIND_PLAYER, CLICK_PLAYER, WAIT_PLAYER_MENU, PRESS_KEY_3, DONE }

    private static State state = State.IDLE;
    private static int tickWaiter = 0;
    private static int clicksRemaining = 0;
    private static String targetPlayer = DEFAULT_PLAYER;
    private static int overrideClicks = -1;
    private static int initialSyncId = -1;
    private static int clickSubMenuSyncId = -1;
    private static int clicksAtLastCheck = 0;
    private static int checkCounter = 0;
    private static int playerSlotIndex = -1;
    private static final Pattern EMERALDS_PATTERN = Pattern.compile("Emeralds?:\\s*([\\d, ]+)\\s*/", Pattern.CASE_INSENSITIVE);

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static void start(String player, int clicksOverride) {
        if (state != State.IDLE) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cAlready running — wait or disconnect to reset."));
            return;
        }
        targetPlayer = (player == null || player.isBlank()) ? DEFAULT_PLAYER : player;
        overrideClicks = clicksOverride;
        initialSyncId = -1;
        playerSlotIndex = -1;
        clicksRemaining = 0;

        if (McUtils.player() == null) return;
        McUtils.player().networkHandler.sendChatCommand("guild manage");
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§eStarting: /guild manage → target §f" + targetPlayer));
        state = State.WAIT_MANAGE;
        tickWaiter = 60; // ~3s timeout
    }

    private static void abort(String reason) {
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cAborted: " + reason));
        state = State.IDLE;
    }

    private static HandledScreen<?> currentHandled() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return (mc.currentScreen instanceof HandledScreen<?> hs) ? hs : null;
    }

    private static void tick() {
        if (state == State.IDLE) return;

        HandledScreen<?> screen = currentHandled();
        ScreenHandler handler = screen != null ? screen.getScreenHandler() : null;

        switch (state) {
            case WAIT_MANAGE -> {
                if (screen != null && handler != null && handler.slots.size() >= 10) {
                    ItemStack slot0 = handler.getSlot(0).getStack();
                    if (!slot0.isEmpty()) {
                        initialSyncId = handler.syncId;
                        state = State.CLICK_BANNER;
                        tickWaiter = 2;
                        return;
                    }
                }
                if (--tickWaiter <= 0) abort("guild manage menu didn't open in time");
            }
            case CLICK_BANNER -> {
                if (tickWaiter-- > 0) return;
                if (handler == null) { abort("menu closed unexpectedly"); return; }
                ContainerUtils.clickOnSlot(0, handler.syncId, 0, handler.getStacks());
                state = State.WAIT_REWARDS;
                tickWaiter = 60;
            }
            case WAIT_REWARDS -> {
                if (handler != null && handler.syncId != initialSyncId) {
                    // New menu opened — this should be Guild Rewards
                    Long emeralds = readEmeraldsFromAnySlot(handler);
                    if (emeralds == null) {
                        if (--tickWaiter <= 0) abort("couldn't find emerald count in Guild Rewards");
                        return;
                    }
                    int clicks = (overrideClicks > 0) ? overrideClicks : ((int) (emeralds / 1000) + 1);
                    clicksRemaining = clicks;
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                            "§7Found §a" + emeralds + "§7 emeralds → §e" + clicks + " clicks§7 for §f" + targetPlayer));
                    initialSyncId = handler.syncId;
                    state = State.FIND_PLAYER;
                    tickWaiter = 60;
                } else if (--tickWaiter <= 0) {
                    abort("Guild Rewards menu didn't open");
                }
            }
            case FIND_PLAYER -> {
                if (handler == null) { abort("menu closed"); return; }
                int found = findPlayerSlot(handler, targetPlayer);
                if (found >= 0) {
                    playerSlotIndex = found;
                    ContainerUtils.clickOnSlot(found, handler.syncId, 0, handler.getStacks());
                    state = State.WAIT_PLAYER_MENU;
                    tickWaiter = 60;
                } else if (--tickWaiter <= 0) {
                    abort("couldn't find player '" + targetPlayer + "' in Guild Rewards");
                }
            }
            case WAIT_PLAYER_MENU -> {
                if (handler != null && handler.syncId != initialSyncId) {
                    clickSubMenuSyncId = handler.syncId;
                    clicksAtLastCheck = clicksRemaining;
                    checkCounter = 0;
                    state = State.PRESS_KEY_3;
                    tickWaiter = 1;
                } else if (--tickWaiter <= 0) {
                    abort("player sub-menu didn't open");
                }
            }
            case PRESS_KEY_3 -> {
                if (tickWaiter-- > 0) return;
                if (handler == null) { abort("menu closed"); return; }
                if (clicksRemaining <= 0) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aDone."));
                    state = State.IDLE;
                    return;
                }
                // Every 20 ticks: verify we're still in the same sub-menu and actually progressing
                if (++checkCounter >= 20) {
                    checkCounter = 0;
                    if (handler.syncId != clickSubMenuSyncId) {
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                "§aDone (menu closed; " + (overrideClicks > 0 ? overrideClicks : clicksAtLastCheck)
                                        + " target, " + (clicksAtLastCheck - clicksRemaining) + " clicks this window)."));
                        state = State.IDLE;
                        return;
                    }
                    clicksAtLastCheck = clicksRemaining;
                }
                // SWAP action: button=2 corresponds to hotbar slot 3 (keyboard "3")
                int target = firstNonEmptySlot(handler);
                if (target < 0) target = 0;
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.interactionManager != null && mc.player != null) {
                    mc.interactionManager.clickSlot(handler.syncId, target, 2, SlotActionType.SWAP, mc.player);
                }
                clicksRemaining--;
                tickWaiter = 1;
            }
        }
    }

    private static int findPlayerSlot(ScreenHandler handler, String name) {
        String lower = name.toLowerCase();
        for (int i = 0; i < handler.slots.size() - 36; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty()) continue;
            String itemName = stack.getName().getString().replaceAll("§[0-9a-fk-or]", "");
            if (itemName.toLowerCase().contains(lower)) return i;
        }
        return -1;
    }

    private static int firstNonEmptySlot(ScreenHandler handler) {
        for (int i = 0; i < handler.slots.size() - 36; i++) {
            if (!handler.getSlot(i).getStack().isEmpty()) return i;
        }
        return -1;
    }

    private static Long readEmeraldsFromAnySlot(ScreenHandler handler) {
        for (int i = 0; i < Math.min(54, handler.slots.size()); i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty()) continue;
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) continue;
            for (Text line : lore.lines()) {
                String clean = line.getString().replaceAll("§[0-9a-fk-or]", "");
                Matcher m = EMERALDS_PATTERN.matcher(clean);
                if (m.find()) {
                    try {
                        return Long.parseLong(m.group(1).replaceAll("[,\\s]", ""));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return null;
    }
}
