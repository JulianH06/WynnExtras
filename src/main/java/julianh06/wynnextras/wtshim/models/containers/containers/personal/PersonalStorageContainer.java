// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — PersonalStorageContainer.
 * Navigation slots and page patterns match Wynntils:
 *   previous button: slot 51    next button: slot 52
 * The prev/next page hover-name patterns feed BankModel's event-driven page tracking.
 *
 * DEVIATION: Wynntils' PersonalStorageContainer implements SearchableContainerProperty
 * (getBounds/supportedProviderTypes/renderYOffset), which pulls services.itemfilter.ItemProviderType
 * and ContainerBounds — neither is ported yet. Those members are dropped; the shim keeps the
 * (existing) ScrollableContainerProperty so ContainerScrollFeatureMixin's getScrollButton redirect
 * still resolves. The static NEXT_PAGE_SLOT/PREVIOUS_PAGE_SLOT fields are kept because WynnExtras'
 * PersonalStorageUtilitiesFeature references them directly.
 */
package julianh06.wynnextras.wtshim.models.containers.containers.personal;

import julianh06.wynnextras.wtshim.models.containers.Container;
import julianh06.wynnextras.wtshim.models.containers.type.PersonalStorageType;
import julianh06.wynnextras.wtshim.models.containers.type.ScrollableContainerProperty;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public abstract class PersonalStorageContainer extends Container implements ScrollableContainerProperty {
    public static final int PREVIOUS_PAGE_SLOT = 51;
    public static final int NEXT_PAGE_SLOT = 52;

    private static final Pattern NEXT_PAGE_PATTERN = Pattern.compile("§f§lPage (\\d+)§a >§2>§a>§2>§a>");
    private static final Pattern PREVIOUS_PAGE_PATTERN = Pattern.compile("§f§lPage (\\d+)§a <§2<§a<§2<§a<");

    private final PersonalStorageType personalStorageType;
    private final int finalPage;
    private final List<Integer> quickJumpDestinations;

    protected PersonalStorageContainer(
            Pattern titlePattern, PersonalStorageType storageType, int finalPage, List<Integer> quickJumpDestinations) {
        super(titlePattern);

        this.personalStorageType = storageType;
        this.finalPage = finalPage;
        this.quickJumpDestinations = quickJumpDestinations;
    }

    public PersonalStorageType getPersonalStorageType() {
        return personalStorageType;
    }

    public int getFinalPage() {
        return finalPage;
    }

    public List<Integer> getQuickJumpDestinations() {
        return Collections.unmodifiableList(quickJumpDestinations);
    }

    public Pattern getNextItemPattern() {
        return NEXT_PAGE_PATTERN;
    }

    public Pattern getPreviousItemPattern() {
        return PREVIOUS_PAGE_PATTERN;
    }

    @Override
    public int getNextItemSlot() {
        return NEXT_PAGE_SLOT;
    }

    @Override
    public int getPreviousItemSlot() {
        return PREVIOUS_PAGE_SLOT;
    }
}
