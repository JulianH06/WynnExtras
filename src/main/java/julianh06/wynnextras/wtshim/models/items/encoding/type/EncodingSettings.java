// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — EncodingSettings. Field names aligned with Wynntils.
 * @param extendedIdentificationEncoding whether to use extended identification encoding (identifiable items only)
 * @param shareItemName whether to share the item name (crafted / custom items only)
 */
package julianh06.wynnextras.wtshim.models.items.encoding.type;

public record EncodingSettings(boolean extendedIdentificationEncoding, boolean shareItemName) {}
