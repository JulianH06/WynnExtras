// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ContainerModel (event-driven, faithful port).
 *
 * Replaces the old tick-polling title-substring matcher. Containers are now recognised the
 * Wynntils way: each registered Container carries a title Pattern; on MenuEvent.MenuOpenedEvent.Pre
 * we match the OpenScreen packet title, and at ScreenInitEvent.Pre we fall back to the Screen
 * predicate. currentContainer is cleared on ContainerCloseEvent.Post (the shim's screen-close feed;
 * Wynntils uses ScreenClosedEvent.Post — no such event exists here).
 *
 * DEVIATION: only the container types WynnExtras (and RaidModel) actually reference are registered,
 * plus the trade-market set for completeness. Wynntils registers ~55 types; the rest pull unported
 * enums (CosmeticItemType/StoreItemType/GuildLogType) and are out of scope for phase 5.
 *
 * This model is bus-registered via WynntilsCompatInit's reflective Models registration; its
 * @SubscribeEvent methods bind automatically. No ClientTickEvents listener anymore.
 */
package julianh06.wynnextras.wtshim.models.containers;

import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.mc.event.ContainerCloseEvent;
import julianh06.wynnextras.wtshim.mc.event.MenuEvent;
import julianh06.wynnextras.wtshim.mc.event.ScreenInitEvent;
import julianh06.wynnextras.wtshim.models.containers.containers.CharacterInfoContainer;
import julianh06.wynnextras.wtshim.models.containers.containers.CharacterSelectionContainer;
import julianh06.wynnextras.wtshim.models.containers.containers.CraftingStationContainer;
import julianh06.wynnextras.wtshim.models.containers.containers.ItemIdentifierContainer;
import julianh06.wynnextras.wtshim.models.containers.containers.RaidRewardChestContainer;
import julianh06.wynnextras.wtshim.models.containers.containers.personal.AccountBankContainer;
import julianh06.wynnextras.wtshim.models.containers.containers.personal.BookshelfContainer;
import julianh06.wynnextras.wtshim.models.containers.containers.personal.CharacterBankContainer;
import julianh06.wynnextras.wtshim.models.containers.containers.personal.MiscBucketContainer;
import julianh06.wynnextras.wtshim.models.containers.containers.trademarket.TradeMarketBuyContainer;
import julianh06.wynnextras.wtshim.models.containers.containers.trademarket.TradeMarketContainer;
import julianh06.wynnextras.wtshim.models.containers.containers.trademarket.TradeMarketFiltersContainer;
import julianh06.wynnextras.wtshim.models.containers.containers.trademarket.TradeMarketOrderContainer;
import julianh06.wynnextras.wtshim.models.containers.containers.trademarket.TradeMarketSellContainer;
import julianh06.wynnextras.wtshim.models.containers.containers.trademarket.TradeMarketTradesContainer;
import julianh06.wynnextras.wtshim.models.profession.type.ProfessionType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class ContainerModel extends Model {
    // Wynncraft's crafting professions (each has its own crafting-station title glyph).
    private static final ProfessionType[] CRAFTING_PROFESSIONS = {
        ProfessionType.ARMOURING,
        ProfessionType.WEAPONSMITHING,
        ProfessionType.TAILORING,
        ProfessionType.WOODWORKING,
        ProfessionType.ALCHEMISM,
        ProfessionType.COOKING,
        ProfessionType.SCRIBING,
        ProfessionType.JEWELING
    };

    private final List<Container> containerTypes = new ArrayList<>();
    private Container currentContainer = null;

    public ContainerModel() {
        registerContainers();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMenuOpened(MenuEvent.MenuOpenedEvent.Pre e) {
        currentContainer = null;

        for (Container container : containerTypes) {
            if (container.matchesTitle(e.getTitle())) {
                currentContainer = container;
                currentContainer.setContainerId(e.getContainerId());
                break;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScreenInit(ScreenInitEvent.Pre e) {
        if (!(e.getScreen() instanceof HandledScreen<?> screen)) return;

        // Only update if we haven't already detected the container via MenuOpenedEvent
        if (currentContainer != null) return;

        for (Container container : containerTypes) {
            if (container.isScreen(screen)) {
                currentContainer = container;
                currentContainer.setContainerId(screen.getScreenHandler().syncId);
                break;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScreenClose(ContainerCloseEvent.Post e) {
        currentContainer = null;
    }

    public Container getCurrentContainer() {
        return currentContainer;
    }

    private void registerContainers() {
        // Order does not matter here.
        registerContainer(new AccountBankContainer());
        registerContainer(new CharacterBankContainer());
        registerContainer(new BookshelfContainer());
        registerContainer(new MiscBucketContainer());
        registerContainer(new CharacterInfoContainer());
        registerContainer(new CharacterSelectionContainer());
        registerContainer(new ItemIdentifierContainer());
        registerContainer(new RaidRewardChestContainer());
        registerContainer(new TradeMarketBuyContainer());
        registerContainer(new TradeMarketContainer());
        registerContainer(new TradeMarketFiltersContainer());
        registerContainer(new TradeMarketOrderContainer());
        registerContainer(new TradeMarketSellContainer());
        registerContainer(new TradeMarketTradesContainer());

        for (ProfessionType type : CRAFTING_PROFESSIONS) {
            registerContainer(new CraftingStationContainer(type));
        }
    }

    private void registerContainer(Container container) {
        containerTypes.add(container);
    }
}
