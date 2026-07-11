// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — GearInfo (slim gear-database record).
 *
 * One entry per gear item in Wynncraft's gear.json (cdn.wynntils.com/static/Reference/gear.json),
 * loaded by GearModel. Slim vs Wynntils' GearInfo: only the fields WynnExtras callers read are kept
 * (type/tier for weapon detection + bank-search filters, requirements for the skill-point
 * auto-selector, variableStats for the identification-range display). Icon/set/obtain/base-damage
 * metadata is NOT parsed — no caller reads it.
 */
package julianh06.wynnextras.wtshim.models.gear.type;

import julianh06.wynnextras.wtshim.models.stats.type.StatPossibleValues;
import julianh06.wynnextras.wtshim.models.stats.type.StatType;
import julianh06.wynnextras.wtshim.utils.type.Pair;
import java.util.List;

public record GearInfo(
        String name,
        GearType type,
        GearTier tier,
        GearRequirements requirements,
        List<Pair<StatType, StatPossibleValues>> variableStats) {}
