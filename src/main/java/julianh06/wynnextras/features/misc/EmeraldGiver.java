package julianh06.wynnextras.features.misc;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.ChatEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WEModule
public class EmeraldGiver {
    public static final String DEFAULT_PLAYER = "cinfrascitizen";
    public static final int DEFAULT_ASPECT_COUNT = 20;

    /** Hotbar slot the SWAP click targets — Wynncraft uses slot 3 for emeralds, slot 1 for aspects. */
    public enum Mode {
        EMERALD(2, "emeralds"),
        ASPECT(0, "aspects");
        final int hotbarButton;
        final String label;
        Mode(int hotbarButton, String label) { this.hotbarButton = hotbarButton; this.label = label; }
    }

    private enum State { IDLE, OPEN_GUILD_MENU, WAIT_MANAGE, CLICK_BANNER, WAIT_REWARDS, PRESS_KEY_3, WAIT_PAGE_REFRESH, DONE }

    private static State state = State.IDLE;
    private static int tickWaiter = 0;
    private static int clicksRemaining = 0;
    private static String targetPlayer = DEFAULT_PLAYER;
    private static int overrideClicks = -1;
    private static Mode mode = Mode.EMERALD;
    private static int initialSyncId = -1;
    private static int clickSubMenuSyncId = -1;
    private static int playerSlotIndex = -1;
    private static int pagesScrolled = 0;
    private static final int MAX_PAGES = 12;
    private static final int PAGE_REFRESH_TIMEOUT_TICKS = 60; // ~3s
    private static java.util.List<String> prePageSnapshot = null;
    private static int confirmedClicks = 0;
    private static int totalAttempts = 0;
    private static boolean guildOutOfResources = false;
    private static final int FAST_DELAY_TICKS = 1;         // ~50ms — bulk phase: spam clicks
    private static final int SLOW_DELAY_TICKS = 5;         // ~250ms — verify phase: wait for chat confirmation
    private static final int SAFETY_BUFFER = 3;            // switch to slow phase 3 clicks before target so we never overshoot
    private static final int ATTEMPT_MULTIPLIER = 3;       // hard cap: clicks * 3 attempts before giving up
    private static final Pattern EMERALDS_PATTERN = Pattern.compile("Emeralds?:\\s*([\\d, ]+)\\s*/", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASPECTS_PATTERN = Pattern.compile("Aspects?:\\s*([\\d, ]+)", Pattern.CASE_INSENSITIVE);
    private static Pattern confirmationPattern = null;
    private static Pattern outOfResourcesPattern = null;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        if (state == State.IDLE) return;
        String raw = event.message.getString().replaceAll("§[0-9a-fk-or]", "");
        if (confirmationPattern != null && confirmationPattern.matcher(raw).find()) {
            confirmedClicks++;
        }
        if (outOfResourcesPattern != null && outOfResourcesPattern.matcher(raw).find()) {
            guildOutOfResources = true;
        }
    }

    public static void start(String player, int clicksOverride) {
        start(player, clicksOverride, Mode.EMERALD);
    }

    public static void start(String player, int clicksOverride, Mode runMode) {
        if (state != State.IDLE) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cAlready running — wait or disconnect to reset."));
            return;
        }
        if (runMode == Mode.ASPECT && (player == null || player.isBlank())) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cAspect mode requires a player name."));
            return;
        }
        targetPlayer = (player == null || player.isBlank()) ? DEFAULT_PLAYER : player;
        overrideClicks = (runMode == Mode.ASPECT && clicksOverride <= 0) ? DEFAULT_ASPECT_COUNT : clicksOverride;
        mode = runMode;
        initialSyncId = -1;
        playerSlotIndex = -1;
        clicksRemaining = 0;
        pagesScrolled = 0;
        confirmedClicks = 0;
        totalAttempts = 0;
        guildOutOfResources = false;
        // Confirmation message formats observed:
        //   Emerald: "<sender>/<rank> rewarded 1024 Emeralds to cinfrascitizen"
        //   Aspect:  "<sender>/<rank> rewarded an Aspect to <target>/<rank>"
        // Aspects use "an Aspect" (no number); real-player targets carry an optional /rank suffix.
        String unitMatch = (runMode == Mode.EMERALD)
                ? "[\\d,]+\\s+Emeralds?"
                : "an?\\s+Aspect";
        String targetMatch = Pattern.quote(targetPlayer) + "(?:/\\S+)?";
        confirmationPattern = Pattern.compile(
                "rewarded\\s+" + unitMatch + "\\s+to\\s+" + targetMatch,
                Pattern.CASE_INSENSITIVE);
        String resourceWord = (runMode == Mode.EMERALD) ? "Emeralds?" : "Aspects?";
        outOfResourcesPattern = Pattern.compile(
                "guild does not have enough\\s+" + resourceWord,
                Pattern.CASE_INSENSITIVE);

        if (McUtils.player() == null) return;
        McUtils.player().networkHandler.sendChatCommand("guild manage");
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§eStarting: /guild manage → §f" + targetPlayer + " §7(" + mode.label + ")"));
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
                    int clicks;
                    if (mode == Mode.EMERALD) {
                        Long emeralds = readEmeraldsFromAnySlot(handler);
                        if (emeralds == null) {
                            if (--tickWaiter <= 0) abort("couldn't find emerald count in Guild Rewards");
                            return;
                        }
                        clicks = (overrideClicks > 0) ? overrideClicks : ((int) (emeralds / 1000) + 1);
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                "§7Found §a" + emeralds + "§7 emeralds → §e" + clicks + " clicks§7 for §f" + targetPlayer));
                    } else {
                        Long aspects = readAspectsFromAnySlot(handler);
                        if (aspects == null) {
                            if (--tickWaiter <= 0) abort("couldn't find aspect count in Guild Rewards");
                            return;
                        }
                        clicks = overrideClicks;
                        if (aspects < clicks) {
                            abort("guild only has §e" + aspects + "§c aspect" + (aspects == 1 ? "" : "s") + " (need " + clicks + ")");
                            return;
                        }
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                "§7Found §a" + aspects + "§7 aspects → giving §e" + clicks + "§7 to §f" + targetPlayer));
                    }
                    clicksRemaining = clicks;
                    clickSubMenuSyncId = handler.syncId;
                    state = State.PRESS_KEY_3;
                    tickWaiter = 1;
                } else if (--tickWaiter <= 0) {
                    abort("Guild Rewards menu didn't open");
                }
            }
            case PRESS_KEY_3 -> {
                if (tickWaiter-- > 0) return;
                if (handler == null) { abort("menu closed"); return; }
                int requested = clicksRemaining; // total clicks targeted (set in WAIT_REWARDS)
                if (guildOutOfResources) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                            "§eGuild ran out of " + mode.label + " — confirmed §f" + confirmedClicks + "§e/§f" + requested));
                    state = State.IDLE;
                    return;
                }
                if (confirmedClicks >= requested) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                            "§aDone — server confirmed §f" + confirmedClicks + "§a/§f" + requested));
                    state = State.IDLE;
                    return;
                }
                // Bail if Wynncraft swapped us to a different screen mid-run.
                if (handler.syncId != clickSubMenuSyncId) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                            "§aDone (menu closed; confirmed §f" + confirmedClicks + "§a/§f" + requested + ")"));
                    state = State.IDLE;
                    return;
                }
                if (totalAttempts >= requested * ATTEMPT_MULTIPLIER) {
                    abort("gave up after " + totalAttempts + " attempts (confirmed " + confirmedClicks + "/" + requested + ")");
                    return;
                }
                int target = findPlayerSlot(handler, targetPlayer);
                if (target < 0) {
                    int nextPageSlot = findSlotByName(handler, "Next Page");
                    if (nextPageSlot < 0) {
                        abort("couldn't find player '" + targetPlayer + "' in Guild Rewards");
                        return;
                    }
                    if (pagesScrolled >= MAX_PAGES) {
                        abort("scrolled " + MAX_PAGES + " pages without finding '" + targetPlayer + "'");
                        return;
                    }
                    prePageSnapshot = snapshotSlotNames(handler);
                    MinecraftClient mcPage = MinecraftClient.getInstance();
                    if (mcPage.interactionManager != null && mcPage.player != null) {
                        mcPage.interactionManager.clickSlot(handler.syncId, nextPageSlot, 0, SlotActionType.PICKUP, mcPage.player);
                    }
                    pagesScrolled++;
                    state = State.WAIT_PAGE_REFRESH;
                    tickWaiter = PAGE_REFRESH_TIMEOUT_TICKS;
                    return;
                }
                playerSlotIndex = target;
                // Phase 1 (fast): blast clicks until we're SAFETY_BUFFER short of the target.
                // Phase 2 (verify): pace by chat confirmations so we never overshoot.
                boolean fastPhase = totalAttempts < (requested - SAFETY_BUFFER);
                if (!fastPhase && confirmedClicks >= requested) {
                    // already covered by previous fast clicks — let the top-of-loop check finish
                    tickWaiter = 1;
                    return;
                }
                // SWAP with the mode's hotbar index. Sent as a slot action, not a key event,
                // so it works regardless of the user's hotbar keybinds.
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.interactionManager != null && mc.player != null) {
                    mc.interactionManager.clickSlot(handler.syncId, target, mode.hotbarButton, SlotActionType.SWAP, mc.player);
                }
                totalAttempts++;
                tickWaiter = fastPhase ? FAST_DELAY_TICKS : SLOW_DELAY_TICKS;
            }
            case WAIT_PAGE_REFRESH -> {
                if (handler == null) { abort("menu closed during pagination"); return; }
                if (handler.syncId != clickSubMenuSyncId) { abort("menu changed during pagination"); return; }
                java.util.List<String> now = snapshotSlotNames(handler);
                if (prePageSnapshot != null && !now.equals(prePageSnapshot)) {
                    prePageSnapshot = null;
                    state = State.PRESS_KEY_3;
                    tickWaiter = 1;
                    return;
                }
                if (--tickWaiter <= 0) abort("page didn't refresh after Next Page click");
            }
        }
    }

    private static java.util.List<String> snapshotSlotNames(ScreenHandler handler) {
        int max = Math.max(0, handler.slots.size() - 36);
        java.util.List<String> out = new java.util.ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            out.add(stack.isEmpty() ? "" : stack.getName().getString());
        }
        return out;
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

    private static int findSlotByName(ScreenHandler handler, String name) {
        for (int i = 0; i < handler.slots.size() - 36; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty()) continue;
            String itemName = stack.getName().getString().replaceAll("§[0-9a-fk-or]", "");
            if (itemName.contains(name)) return i;
        }
        return -1;
    }

    private static Long readEmeraldsFromAnySlot(ScreenHandler handler) {
        return readLoreNumber(handler, EMERALDS_PATTERN);
    }

    private static Long readAspectsFromAnySlot(ScreenHandler handler) {
        return readLoreNumber(handler, ASPECTS_PATTERN);
    }

    private static Long readLoreNumber(ScreenHandler handler, Pattern pattern) {
        for (int i = 0; i < Math.min(54, handler.slots.size()); i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty()) continue;
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) continue;
            for (Text line : lore.lines()) {
                String clean = line.getString().replaceAll("§[0-9a-fk-or]", "");
                Matcher m = pattern.matcher(clean);
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
