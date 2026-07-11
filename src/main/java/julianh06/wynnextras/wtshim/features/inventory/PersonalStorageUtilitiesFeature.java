// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — PersonalStorageUtilitiesFeature.
 *
 * jumpToDestination strategy:
 *   • If the bank type has quick-jump destinations reachable in one SWAP-click, try that first.
 *   • Otherwise spam single-step clicks on the next/prev button as fast as the manual click rate
 *     (one every 2 ticks = 10 clicks/sec) until BankModel.getCurrentPage() reports the target.
 *
 * No wait-for-server-ack between clicks — that was making multi-page jumps slower than manual
 * clicking. The server queues clicks and processes them sequentially; we just keep firing.
 *
 * Source (quick-jump logic): Wynntils PersonalStorageUtilitiesFeature#tryToQuickJump.
 */
package julianh06.wynnextras.wtshim.features.inventory;

import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.core.consumers.features.Feature;
import julianh06.wynnextras.wtshim.models.containers.Container;
import julianh06.wynnextras.wtshim.models.containers.containers.personal.PersonalStorageContainer;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import julianh06.wynnextras.wtshim.utils.wynn.ContainerUtils;
import java.util.List;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

public class PersonalStorageUtilitiesFeature extends Feature {
    /** Hammer rate — one click per N ticks. 2 ≈ 100ms, matches fast manual clicking. */
    private static final int TICKS_BETWEEN_CLICKS = 2;
    /** Total safety cap — stop chasing after ~5 seconds regardless. */
    private static final int MAX_TOTAL_TICKS = 100;

    /** SHADOWED by WynnExtras PersonalStorageUtilitiesFeatureAccessor — do not rename.
     *  Source: Wynntils PersonalStorageUtilitiesFeature#lastPage. Default 21 = account-bank max. */
    private int lastPage = 21;

    private int pageDestination = -1;
    private int cooldown = 0;
    private int totalTicks = 0;
    private boolean quickJumpAttempted = false;

    public PersonalStorageUtilitiesFeature() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick());
    }

    public void jumpToDestination(int page) {
        pageDestination = page;
        cooldown = 0;
        totalTicks = 0;
        quickJumpAttempted = false;
    }

    private void tick() {
        if (pageDestination < 0) return;

        ScreenHandler menu = McUtils.containerMenu();
        if (menu == null) { cancel(); return; }

        int current = Models.Bank.getCurrentPage();
        if (current == pageDestination) { cancel(); return; }

        totalTicks++;
        if (totalTicks > MAX_TOTAL_TICKS) { cancel(); return; }

        if (cooldown > 0) { cooldown--; return; }

        Container container = Models.Container.getCurrentContainer();
        if (!(container instanceof PersonalStorageContainer personal)) { cancel(); return; }

        // On the very first tick try a quick-jump — one SWAP-click can skip many pages.
        if (!quickJumpAttempted) {
            quickJumpAttempted = true;
            if (Math.abs(pageDestination - current) > 1 && tryQuickJump(personal, menu, current)) {
                cooldown = TICKS_BETWEEN_CLICKS;
                return;
            }
        }

        int slot = current < pageDestination
                ? PersonalStorageContainer.NEXT_PAGE_SLOT
                : PersonalStorageContainer.PREVIOUS_PAGE_SLOT;
        ContainerUtils.clickOnSlot(slot, menu.syncId, 0, List.copyOf(menu.getStacks()));
        cooldown = TICKS_BETWEEN_CLICKS;
    }

    /** Source: Wynntils PersonalStorageUtilitiesFeature#tryToQuickJump. */
    private boolean tryQuickJump(PersonalStorageContainer container, ScreenHandler menu, int currentPage) {
        List<Integer> destinations = container.getQuickJumpDestinations();
        if (destinations.isEmpty()) return false;

        int target;
        if (destinations.contains(pageDestination)) {
            target = destinations.indexOf(pageDestination);
        } else {
            int closest = destinations.get(0);
            for (int d : destinations) {
                if (Math.abs(pageDestination - d) < Math.abs(pageDestination - closest)) closest = d;
            }
            target = destinations.indexOf(closest);
        }
        int targetPage = destinations.get(target);

        if (pageDestination >= currentPage && currentPage >= targetPage) return false;

        int buttonSlot = (currentPage != container.getFinalPage())
                ? PersonalStorageContainer.NEXT_PAGE_SLOT
                : PersonalStorageContainer.PREVIOUS_PAGE_SLOT;
        ItemStack buttonStack = menu.getSlot(buttonSlot).getStack();
        if (buttonStack == null || buttonStack.isEmpty()) return false;

        if (!buttonLoreContainsPage(buttonStack, targetPage)) return false;

        ContainerUtils.pressKeyOnSlot(buttonSlot, menu.syncId, target, List.copyOf(menu.getStacks()));
        return true;
    }

    private static boolean buttonLoreContainsPage(ItemStack stack, int page) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return false;
        Pattern wholeWord = Pattern.compile(".*\\bPage\\s+" + page + "\\b.*");
        for (Text line : lore.lines()) {
            String plain = line.getString().replaceAll("§[0-9a-fk-or]", "").trim();
            if (wholeWord.matcher(plain).matches()) return true;
        }
        return false;
    }

    private void cancel() {
        pageDestination = -1;
        cooldown = 0;
        totalTicks = 0;
        quickJumpAttempted = false;
    }
}
