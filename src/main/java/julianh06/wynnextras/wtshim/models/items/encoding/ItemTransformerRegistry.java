// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ItemTransformerRegistry (encode-only, gear-only).
 *
 * Adapted from Wynntils. Wynntils registers transformers per concrete item class
 * (Gear/Tome/Charm/CraftedGear/CraftedConsumable) and dispatches on wynnItem.getClass(). The shim
 * cannot faithfully encode tomes/charms/crafted items (they need the unported stat/damage/element
 * data), so only GearItem is supported. Dispatch is by instanceof rather than exact class, because
 * the shim's ItemModel produces GearItem *subclasses* (e.g. GearItemNamed) rather than plain GearItem.
 * The Start + <transformer blocks> + End envelope and the byte layout are unchanged.
 */
package julianh06.wynnextras.wtshim.models.items.encoding;

import julianh06.wynnextras.wtshim.models.items.WynnItem;
import julianh06.wynnextras.wtshim.models.items.encoding.data.EndData;
import julianh06.wynnextras.wtshim.models.items.encoding.data.StartData;
import julianh06.wynnextras.wtshim.models.items.encoding.impl.item.GearItemTransformer;
import julianh06.wynnextras.wtshim.models.items.encoding.type.EncodingSettings;
import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemData;
import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemTransformingVersion;
import julianh06.wynnextras.wtshim.models.items.items.game.GearItem;
import julianh06.wynnextras.wtshim.utils.EncodedByteBuffer;
import julianh06.wynnextras.wtshim.utils.type.ErrorOr;
import java.util.ArrayList;
import java.util.List;

public final class ItemTransformerRegistry {
    private final DataTransformerRegistry dataTransformerRegistry = new DataTransformerRegistry();
    private final GearItemTransformer gearItemTransformer = new GearItemTransformer();

    public ErrorOr<EncodedByteBuffer> encodeItem(WynnItem wynnItem, EncodingSettings encodingSettings) {
        if (!(wynnItem instanceof GearItem gearItem)) {
            return ErrorOr.error("Item encoding in the compat shim only supports gear items (got "
                    + wynnItem.getClass().getSimpleName() + ")");
        }

        try {
            // Name-only gear encodings are always VERSION_1 (VERSION_2 only adds shiny-reroll support,
            // which we never emit).
            ItemTransformingVersion version = ItemTransformingVersion.VERSION_1;

            List<ItemData> encodedData = new ArrayList<>();
            encodedData.add(new StartData(version));
            encodedData.addAll(gearItemTransformer.encode(gearItem, encodingSettings));
            encodedData.add(new EndData());

            return dataTransformerRegistry.encodeData(version, encodedData);
        } catch (Exception e) {
            return ErrorOr.<EncodedByteBuffer>error("Failed to encode item!").logged();
        }
    }

    public boolean canEncodeItem(WynnItem wynnItem) {
        return wynnItem instanceof GearItem;
    }
}
