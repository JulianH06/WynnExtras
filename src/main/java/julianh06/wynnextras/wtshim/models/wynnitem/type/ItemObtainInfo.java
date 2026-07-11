// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ItemObtainInfo (SLIM stand-in / type token).
 * Wynntils' real record is (ItemObtainType sourceType, Optional<String> name). The fork only uses
 * this as the element type of `List<ItemObtainInfo>` and always passes an empty list — it never
 * constructs or reads one. Kept as an inert record so the type resolves; ItemObtainType is not
 * ported. Restore the faithful shape if a caller ever needs obtain-source data.
 */
package julianh06.wynnextras.wtshim.models.wynnitem.type;

public record ItemObtainInfo() {}
