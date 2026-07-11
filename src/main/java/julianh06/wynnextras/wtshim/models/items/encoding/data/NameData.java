// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — NameData block.
 * Adapted from Wynntils: the fromSafeName(IdentifiableItemProperty) factory is replaced by a plain
 * of(String) factory, because the shim's slim item classes do not implement Wynntils'
 * IdentifiableItemProperty hierarchy. The byte format (see NameDataTransformer) is unchanged.
 */
package julianh06.wynnextras.wtshim.models.items.encoding.data;

import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemData;
import java.util.Optional;
import java.util.regex.Pattern;

public record NameData(Optional<String> name) implements ItemData {
    private static final NameData EMPTY = new NameData(Optional.empty());

    private static final int MAX_NAME_LENGTH = 50;
    private static final Pattern SANITIZE_PATTERN = Pattern.compile("[^a-zA-Z0-9'\\-.,!?\\s]");

    /** Creates a {@link NameData} from a (assumed-safe) item name. */
    public static NameData of(String name) {
        return new NameData(Optional.ofNullable(name));
    }

    /**
     * Sanitizes the given name (byte-identical to Wynntils' NameData.sanitized).
     * If any characters are not alphanumeric, an apostrophe, or a space, the name is dropped.
     * The name is trimmed, consecutive spaces collapsed, and truncated to {@value #MAX_NAME_LENGTH}.
     */
    public static NameData sanitized(String name) {
        if (SANITIZE_PATTERN.matcher(name).find()) {
            return EMPTY;
        }

        name = name.trim();
        name = name.replaceAll("\\s+", " ");
        name = name.substring(0, Math.min(name.length(), MAX_NAME_LENGTH));

        return new NameData(Optional.of(name));
    }
}
