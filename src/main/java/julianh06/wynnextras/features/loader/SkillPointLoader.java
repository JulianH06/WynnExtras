package julianh06.wynnextras.features.loader;

import julianh06.wynnextras.features.misc.CompassMenuOverlay;
import julianh06.wynnextras.utils.ContainerUtils;
import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import julianh06.wynnextras.wynncraft.state.SkillPoint;
import julianh06.wynnextras.wynncraft.state.SkillPointState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillPointLoader {
    private static final int RESET_SLOT = 4;
    private static final int SLOT_OFFSET = 11; // slots 11-15 = Str/Dex/Int/Def/Agi
    private static final long MENU_STATE_TIMEOUT_MS = 3_000;
    private static final long RESET_CONFIRMATION_TIMEOUT_MS = 3_500;
    private static final Pattern RESET_AVAILABLE = Pattern.compile(
            "(?i)you have\\s+(\\d{1,3})\\s+skill points");

    private static SkillPointLoader instance;

    private final int[] target = new int[SkillPoint.values().length];
    private final int[] remaining = new int[SkillPoint.values().length];
    private boolean loading;
    private Phase phase = Phase.WAITING_FOR_MENU_STATE;
    private long startedAt;
    private long phaseStartedAt;
    private int assignmentTotal;
    private int assignmentSent;
    private boolean characterMenuWasOpen;

    private enum Phase {
        WAITING_FOR_MENU_STATE,
        WAITING_FOR_RESET_PROMPT,
        WAITING_FOR_RESET,
        ASSIGNING
    }

    private SkillPointLoader() {}

    public static SkillPointLoader getInstance() {
        if (instance == null) instance = new SkillPointLoader();
        return instance;
    }

    public void load(int strength, int dexterity, int intelligence, int defence, int agility) {
        if (loading) {
            CompassMenuOverlay.setStatus(CompassMenuOverlay.StatusType.ERROR,
                    "Skill point assignment is already running.");
            return;
        }

        int[] requested = {strength, dexterity, intelligence, defence, agility};
        for (int i = 0; i < target.length; i++) target[i] = Math.max(0, requested[i]);
        Arrays.fill(remaining, 0);
        loading = true;
        phase = Phase.WAITING_FOR_MENU_STATE;
        startedAt = System.currentTimeMillis();
        phaseStartedAt = startedAt;
        assignmentTotal = 0;
        assignmentSent = 0;
        CompassMenuOverlay.setStatus(CompassMenuOverlay.StatusType.INFO,
                "Reading current skill points...");

        if (WynncraftMenuService.isCurrent(MenuType.CHARACTER_INFO)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) {
            fail("Could not open the character menu.");
            return;
        }

        int previousSlot = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(7);
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        client.player.getInventory().setSelectedSlot(previousSlot);
    }

    public void reset() {
        Arrays.fill(target, 0);
        Arrays.fill(remaining, 0);
        loading = false;
        phase = Phase.WAITING_FOR_MENU_STATE;
        startedAt = 0;
        phaseStartedAt = 0;
        assignmentTotal = 0;
        assignmentSent = 0;
    }

    public boolean isLoading() {
        return loading;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SkillPointLoader loader = getInstance();
            if (client.player == null || client.world == null) {
                CompassMenuOverlay.setSelectingWeapon(false);
                CompassMenuOverlay.clearStatus();
                loader.characterMenuWasOpen = false;
                if (loader.loading) loader.reset();
                return;
            }

            boolean characterMenuOpen = WynncraftMenuService.isCurrent(MenuType.CHARACTER_INFO);
            if (loader.characterMenuWasOpen && !characterMenuOpen) {
                CompassMenuOverlay.setSelectingWeapon(false);
                CompassMenuOverlay.clearStatus();
                if (loader.loading) loader.reset();
            }
            loader.characterMenuWasOpen = characterMenuOpen;

            if (!characterMenuOpen && !loader.loading) {
                return;
            }
            if (!loader.loading) return;

            long now = System.currentTimeMillis();
            if (!(client.currentScreen instanceof HandledScreen<?> screen)
                    || !WynncraftMenuService.isCurrent(MenuType.CHARACTER_INFO)) {
                if (now - loader.startedAt >= MENU_STATE_TIMEOUT_MS) {
                    loader.fail("Character menu did not become ready in time.");
                }
                return;
            }

            switch (loader.phase) {
                case WAITING_FOR_MENU_STATE -> loader.waitForInitialState(screen, now);
                case WAITING_FOR_RESET_PROMPT -> loader.waitForResetPrompt(screen, now);
                case WAITING_FOR_RESET -> loader.waitForResetCompletion(screen, now);
                case ASSIGNING -> loader.assignNext(screen);
            }
        });
    }

    private void waitForInitialState(HandledScreen<?> screen, long now) {
        if (!hasFreshStateAfter(startedAt)) {
            if (now - phaseStartedAt >= MENU_STATE_TIMEOUT_MS) {
                fail("Could not read the current skill points.");
            }
            return;
        }

        int[] current = currentAssigned();
        if (sum(current) == 0) {
            System.arraycopy(target, 0, remaining, 0, target.length);
            startAssigning(SkillPointState.available());
            return;
        }

        if (screen.getScreenHandler().slots.size() <= RESET_SLOT
                || !screen.getScreenHandler().getSlot(RESET_SLOT).hasStack()) {
            fail("The skill point reset button was not found.");
            return;
        }

        ContainerUtils.shiftClickOnSlot(RESET_SLOT, screen.getScreenHandler().syncId, 0,
                screen.getScreenHandler().getStacks());
        phase = Phase.WAITING_FOR_RESET_PROMPT;
        phaseStartedAt = now;
        CompassMenuOverlay.setStatus(CompassMenuOverlay.StatusType.PROGRESS,
                "Opening reset confirmation...", "Waiting for the confirmation prompt.");
    }

    private void waitForResetPrompt(HandledScreen<?> screen, long now) {
        if (isResetConfirmationPrompt(screen)) {
            ContainerUtils.clickOnSlot(RESET_SLOT, screen.getScreenHandler().syncId, 0,
                    screen.getScreenHandler().getStacks());
            phase = Phase.WAITING_FOR_RESET;
            phaseStartedAt = now;
            CompassMenuOverlay.setStatus(CompassMenuOverlay.StatusType.PROGRESS,
                    "Confirming skill point reset...", "Waiting for server confirmation.");
            return;
        }

        if (now - phaseStartedAt >= RESET_CONFIRMATION_TIMEOUT_MS) {
            fail("The reset confirmation prompt did not appear.",
                    "No points were assigned.");
        }
    }

    private void waitForResetCompletion(HandledScreen<?> screen, long now) {
        if (finishResetIfReady(screen)) return;

        if (now - phaseStartedAt >= RESET_CONFIRMATION_TIMEOUT_MS) {
            fail("The skill point reset was not confirmed by the server.",
                    "No points were assigned.");
        }
    }

    private boolean finishResetIfReady(HandledScreen<?> screen) {
        if (isResetConfirmationPrompt(screen)) return false;

        Integer available = resetAvailableSkillPoints(screen);
        if (available == null) return false;

        System.arraycopy(target, 0, remaining, 0, target.length);
        startAssigning(available);
        return true;
    }

    private static boolean isResetConfirmationPrompt(HandledScreen<?> screen) {
        return resetSlotText(screen).toLowerCase(Locale.ROOT).contains("click again to confirm");
    }

    private static Integer resetAvailableSkillPoints(HandledScreen<?> screen) {
        Matcher matcher = RESET_AVAILABLE.matcher(resetSlotText(screen));
        if (!matcher.find()) return null;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String resetSlotText(HandledScreen<?> screen) {
        if (screen == null || screen.getScreenHandler().slots.size() <= RESET_SLOT) return "";
        ItemStack stack = screen.getScreenHandler().getSlot(RESET_SLOT).getStack();
        if (stack == null || stack.isEmpty()) return "";

        StringBuilder text = new StringBuilder(stack.getName().getString());
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) text.append('\n').append(line.getString());
        }
        return text.toString().replaceAll("\\u00A7[0-9a-fk-or]", "");
    }

    private void startAssigning(int available) {
        assignmentTotal = sum(remaining);
        assignmentSent = 0;
        if (assignmentTotal > available) {
            fail("This build needs " + assignmentTotal + " more skill points.",
                    "Only " + available + " are available.");
            return;
        }
        if (assignmentTotal == 0) {
            CompassMenuOverlay.setStatus(CompassMenuOverlay.StatusType.SUCCESS,
                    "Requirements are already satisfied.");
            reset();
            return;
        }

        phase = Phase.ASSIGNING;
        phaseStartedAt = System.currentTimeMillis();
        updateProgress();
    }

    private void assignNext(HandledScreen<?> screen) {
        for (int i = 0; i < remaining.length; i++) {
            if (remaining[i] <= 0) continue;

            int amount = remaining[i] >= 5 ? 5 : 1;
            int slot = SLOT_OFFSET + i;
            if (slot >= screen.getScreenHandler().slots.size()) {
                fail("A skill point button is missing.");
                return;
            }

            if (amount == 5) {
                ContainerUtils.shiftClickOnSlot(slot, screen.getScreenHandler().syncId, 0,
                        screen.getScreenHandler().getStacks());
            } else {
                ContainerUtils.clickOnSlot(slot, screen.getScreenHandler().syncId, 0,
                        screen.getScreenHandler().getStacks());
            }
            remaining[i] -= amount;
            assignmentSent += amount;

            if (sum(remaining) == 0) {
                finishAssignment();
            } else {
                updateProgress();
            }
            return;
        }
    }

    private void updateProgress() {
        CompassMenuOverlay.setStatus(CompassMenuOverlay.StatusType.PROGRESS,
                "Assigning skill points: " + assignmentSent + " / " + assignmentTotal);
    }

    private void finishAssignment() {
        CompassMenuOverlay.setStatus(CompassMenuOverlay.StatusType.SUCCESS,
                "Finished assigning " + assignmentTotal + " skill points.");
        reset();
    }

    private boolean hasFreshStateAfter(long timestamp) {
        return SkillPointState.isAvailableKnown() && SkillPointState.updatedAt() > timestamp;
    }

    private static int[] currentAssigned() {
        SkillPoint[] skills = SkillPoint.values();
        int[] result = new int[skills.length];
        for (int i = 0; i < skills.length; i++) result[i] = SkillPointState.assigned(skills[i]);
        return result;
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int value : values) total += value;
        return total;
    }

    private void fail(String primary) {
        fail(primary, "");
    }

    private void fail(String primary, String secondary) {
        CompassMenuOverlay.setStatus(CompassMenuOverlay.StatusType.ERROR, primary, secondary);
        reset();
    }
}