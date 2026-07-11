// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ItemEncodingModel.
 *
 * Encodes WynnItems to shareable item strings (the private-use-area packed UTF-16 form that real
 * Wynntils / WynnBuilder decode). Delegates to a byte-format-faithful ItemTransformerRegistry.
 *
 * Scope (compat shim): only GearItem is encodable, and only its identity (name). Roll / powder /
 * shiny data is NOT encoded — that would require the unported stat-ID (id_keys.json / Models.Stat)
 * and internal-roll subsystem (see GearItemTransformer). Non-gear items return ErrorOr.error.
 * The bytes that ARE emitted are byte-identical to Wynntils, so the string decodes as the named item.
 */
package julianh06.wynnextras.wtshim.models.items;

import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.core.persisted.config.Config;
import julianh06.wynnextras.wtshim.models.items.encoding.ItemTransformerRegistry;
import julianh06.wynnextras.wtshim.models.items.encoding.type.EncodingSettings;
import julianh06.wynnextras.wtshim.utils.EncodedByteBuffer;
import julianh06.wynnextras.wtshim.utils.type.ErrorOr;
import java.util.regex.Pattern;
import net.minecraft.text.Text;

public class ItemEncodingModel extends Model {
    // Kept as the shim's Config type (ItemUtils reads these via .get()); Wynntils uses Storage.
    public final Config<Boolean> extendedIdentificationEncoding = new Config<>(false);
    public final Config<Boolean> shareItemName = new Config<>(true);

    // Encoded data consists of characters from Unicode Supplementary Private Use Area-A and B
    // (U+F0000..U+FFFFD and U+100000..U+10FFFD)
    private static final String RANGE_A =
            "[" + new String(Character.toChars(0xF0000)) + "-" + new String(Character.toChars(0xFFFFD)) + "]";
    private static final String RANGE_B =
            "[" + new String(Character.toChars(0x100000)) + "-" + new String(Character.toChars(0x10FFFD)) + "]";
    private static final Pattern ENCODED_DATA_PATTERN =
            Pattern.compile("(?<data>(" + RANGE_A + "|" + RANGE_B + ")+)( \"(?<name>.+)\")?");

    private final ItemTransformerRegistry itemTransformerRegistry = new ItemTransformerRegistry();

    public ErrorOr<EncodedByteBuffer> encodeItem(WynnItem wynnItem, EncodingSettings encodingSettings) {
        return itemTransformerRegistry.encodeItem(wynnItem, encodingSettings);
    }

    public boolean canEncodeItem(WynnItem wynnItem) {
        return itemTransformerRegistry.canEncodeItem(wynnItem);
    }

    public Pattern getEncodedDataPattern() {
        return ENCODED_DATA_PATTERN;
    }

    /**
     * Builds the shareable string. Matches WynnExtras' call shape. Gear item names are encoded in the
     * data block, so no clear-chat name suffix is appended (the shim only encodes gear).
     */
    public String makeItemString(WynnItem wynnItem, EncodedByteBuffer encodedItem) {
        return encodedItem.toUtf16String();
    }

    /** Overloads preserved from the previous stub surface. */
    public String makeItemString(EncodedByteBuffer buffer) {
        return buffer == null ? "" : buffer.toUtf16String();
    }

    public String makeItemString(EncodedByteBuffer buffer, boolean share, boolean extended) {
        return buffer == null ? "" : buffer.toUtf16String();
    }

    public Text shareItemName(WynnItem item) {
        return Text.empty();
    }
}
