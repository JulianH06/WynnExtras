// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — BankModel (event-driven, faithful port of Wynntils' page tracking).
 *
 * Replaces the old tick-polling approach that read the prev/next button names every frame.
 * Page state now updates on the same packet events Wynntils uses:
 *   - onScreenInit: latch onto the open PersonalStorageContainer (from ContainerModel)
 *   - onContainerSetContent: swapping account/character bank does NOT send set-slot packets
 *     for the nav buttons, so the page is read from the set-content packet
 *   - onContainerSetSlot: right-clicking prev/next (or quick-jumping) with a full inventory
 *     only sends set-slot packets, so we read those too
 *
 * DEVIATION: Wynntils' BankModel also persists final-page / per-page customizations (names, icons)
 * via @Persisted Storage + BankPageCustomization/QuickJumpButtonIcon/BankPageSetEvent. No WynnExtras
 * caller reads those (only getCurrentPage()); that machinery is dropped to keep the port decoupled
 * from unported persistence + I18n. getStorageContainerType() is kept as a cheap public accessor.
 */
package julianh06.wynnextras.wtshim.models.bank;

import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.mc.event.ContainerCloseEvent;
import julianh06.wynnextras.wtshim.mc.event.ContainerSetContentEvent;
import julianh06.wynnextras.wtshim.mc.event.ContainerSetSlotEvent;
import julianh06.wynnextras.wtshim.mc.event.ScreenInitEvent;
import julianh06.wynnextras.wtshim.models.containers.containers.personal.PersonalStorageContainer;
import julianh06.wynnextras.wtshim.models.containers.type.PersonalStorageType;
import java.util.List;
import java.util.regex.Matcher;
import net.minecraft.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class BankModel extends Model {
    private int currentPage = 1;
    private boolean updatedPage;
    private PersonalStorageContainer personalStorageContainer = null;
    private PersonalStorageType storageContainerType = null;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onScreenInit(ScreenInitEvent.Pre e) {
        if (!(Models.Container.getCurrentContainer() instanceof PersonalStorageContainer container)) {
            storageContainerType = null;
            return;
        }

        personalStorageContainer = container;
        storageContainerType = personalStorageContainer.getPersonalStorageType();
        updatedPage = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScreenClose(ContainerCloseEvent.Post e) {
        storageContainerType = null;
        currentPage = 1;
        updatedPage = false;
    }

    // Swapping between account/character bank or personal/island storage does not
    // send the set slot packets for the slots we need to check so we have to use
    // the set content packet
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onContainerSetContent(ContainerSetContentEvent.Pre event) {
        if (storageContainerType == null) return;

        List<ItemStack> items = event.getItems();
        int prevSlot = personalStorageContainer.getPreviousItemSlot();
        int nextSlot = personalStorageContainer.getNextItemSlot();
        if (prevSlot >= items.size() || nextSlot >= items.size()) return;

        updateState(items.get(prevSlot), items.get(nextSlot));
        updatedPage = true;
    }

    // Right clicking the next/previous buttons or using quick jumps with a full inventory
    // does not send the set content packet, so we have to check the set slot packets
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onContainerSetSlot(ContainerSetSlotEvent.Pre event) {
        if (storageContainerType == null) return;
        if (!updatedPage) return;

        if (event.getSlot() == personalStorageContainer.getPreviousItemSlot()) {
            updateState(event.getItemStack(), ItemStack.EMPTY);
        }

        if (event.getSlot() == personalStorageContainer.getNextItemSlot()) {
            updateState(ItemStack.EMPTY, event.getItemStack());
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public PersonalStorageType getStorageContainerType() {
        return storageContainerType;
    }

    private void updateState(ItemStack previousPageItem, ItemStack nextPageItem) {
        Matcher previousPageMatcher = StyledText.fromComponent(previousPageItem.getName())
                .getMatcher(personalStorageContainer.getPreviousItemPattern());

        if (previousPageMatcher.matches()) {
            currentPage = Integer.parseInt(previousPageMatcher.group(1)) + 1;
        }

        Matcher nextPageMatcher = StyledText.fromComponent(nextPageItem.getName())
                .getMatcher(personalStorageContainer.getNextItemPattern());

        if (nextPageMatcher.matches()) {
            currentPage = Integer.parseInt(nextPageMatcher.group(1)) - 1;
        }
    }
}
