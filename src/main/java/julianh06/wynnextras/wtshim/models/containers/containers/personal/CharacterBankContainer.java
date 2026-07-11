// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras - CharacterBankContainer. Title pattern + page metadata verbatim from Wynntils.
 * DEVIATION: Wynntils' property interfaces (unported ContainerBounds/ItemProviderType) dropped. */
package julianh06.wynnextras.wtshim.models.containers.containers.personal;

import julianh06.wynnextras.wtshim.models.containers.type.PersonalStorageType;
import java.util.List;
import java.util.regex.Pattern;

public class CharacterBankContainer extends PersonalStorageContainer {
    private static final Pattern TITLE_PATTERN = Pattern.compile("\uDAFF\uDFF0\uE00F\uDAFF\uDF68\uF001");
    private static final int FINAL_PAGE = 12;
    private static final List<Integer> QUICK_JUMP_DESTINATIONS = List.of(1, 3, 5, 7, 9, 11);

    public CharacterBankContainer() {
        super(TITLE_PATTERN, PersonalStorageType.CHARACTER_BANK, FINAL_PAGE, QUICK_JUMP_DESTINATIONS);
    }
}
