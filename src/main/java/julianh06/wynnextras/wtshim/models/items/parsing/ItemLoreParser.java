// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ItemLoreParser.
 *
 * Pragmatic lore-line parser that extracts identifications, damages, and requirements.
 *
 * NOTE: Wynntils' own item-parse path uses encoded binary strings + id_keys.json to reconstruct
 * stats (exact roll, roll percentage, stat-type IDs). That requires the id_keys.json data file.
 * This implementation instead reads the human-readable lore lines — sufficient for bank search
 * filters, build-link class detection, rarity/level-based filters, etc. For precise roll
 * percentages and stat IDs, the encoded-item-string route is still needed (future work).
 */
package julianh06.wynnextras.wtshim.models.items.parsing;

import julianh06.wynnextras.wtshim.models.character.type.ClassType;
import julianh06.wynnextras.wtshim.models.elements.type.Skill;
import julianh06.wynnextras.wtshim.models.gear.type.GearRequirements;
import julianh06.wynnextras.wtshim.models.stats.type.DamageType;
import julianh06.wynnextras.wtshim.models.stats.type.StatActualValue;
import julianh06.wynnextras.wtshim.models.stats.type.StatPossibleValues;
import julianh06.wynnextras.wtshim.models.stats.type.StatType;
import julianh06.wynnextras.wtshim.utils.type.Pair;
import julianh06.wynnextras.wtshim.utils.type.RangedValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemLoreParser {
    private ItemLoreParser() {}

    // Rolled identification: "+5 Strength" / "-15% Walk Speed" / "+12/5s Mana Regen".
    private static final Pattern IDENT_ROLLED = Pattern.compile(
            "^(?<sign>[+-])(?<val>\\d+)(?<unit>%|/\\d+s)?\\s+(?<name>.+?)$");

    // Range identification (seen in unidentified gear boxes): "+5 to +15 Strength".
    private static final Pattern IDENT_RANGE = Pattern.compile(
            "^(?<sLow>[+-])(?<low>\\d+)\\s+to\\s+(?<sHigh>[+-])(?<high>\\d+)(?<unit>%|/\\d+s)?\\s+(?<name>.+?)$");

    // Damage line: "Neutral Damage: 50-100" / "Earth Damage: 10-25".
    private static final Pattern DAMAGE = Pattern.compile(
            "^(?<type>Neutral|Earth|Thunder|Water|Fire|Air)\\s+Damage:\\s*(?<low>\\d+)\\s*-\\s*(?<high>\\d+)$");

    // Requirement lines.
    private static final Pattern REQ_COMBAT_LEVEL = Pattern.compile("^Combat\\s+Lv\\.?\\s*Min:\\s*(?<lv>\\d+)$");
    private static final Pattern REQ_CLASS = Pattern.compile("^Class(?:es)? Req:?\\s*(?<cls>.+)$");
    private static final Pattern REQ_QUEST = Pattern.compile("^Quest Req:?\\s*(?<q>.+)$");
    private static final Pattern REQ_SKILL = Pattern.compile(
            "^(?<skill>Strength|Dexterity|Intelligence|Defence|Agility)\\s+Min:\\s*(?<val>\\d+)$");

    public record ParseResult(
            List<StatActualValue> identifications,
            List<StatPossibleValues> possibleValues,
            List<Pair<DamageType, RangedValue>> damages,
            GearRequirements requirements) {}

    public static ParseResult parse(List<String> plainLoreLines) {
        List<StatActualValue> idents = new ArrayList<>();
        List<StatPossibleValues> possibles = new ArrayList<>();
        List<Pair<DamageType, RangedValue>> damages = new ArrayList<>();

        int level = 0;
        Optional<ClassType> classReq = Optional.empty();
        Optional<String> questReq = Optional.empty();
        List<Pair<Skill, Integer>> skillReqs = new ArrayList<>();

        for (String raw : plainLoreLines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            // Requirements.
            Matcher mc = REQ_COMBAT_LEVEL.matcher(line);
            if (mc.matches()) { try { level = Integer.parseInt(mc.group("lv")); } catch (Exception e) {} continue; }

            Matcher cc = REQ_CLASS.matcher(line);
            if (cc.matches()) {
                String cls = cc.group("cls").split("[/,]")[0].trim();
                ClassType found = classFromName(cls);
                if (found != null) classReq = Optional.of(found);
                continue;
            }

            Matcher qc = REQ_QUEST.matcher(line);
            if (qc.matches()) { questReq = Optional.of(qc.group("q").trim()); continue; }

            Matcher sk = REQ_SKILL.matcher(line);
            if (sk.matches()) {
                Skill s = skillFromName(sk.group("skill"));
                if (s != null) {
                    try { skillReqs.add(new Pair<>(s, Integer.parseInt(sk.group("val")))); }
                    catch (NumberFormatException ignored) {}
                }
                continue;
            }

            // Damage lines.
            Matcher dm = DAMAGE.matcher(line);
            if (dm.matches()) {
                DamageType dt = damageTypeFromName(dm.group("type"));
                if (dt != null) {
                    try {
                        damages.add(new Pair<>(dt, new RangedValue(
                                Integer.parseInt(dm.group("low")),
                                Integer.parseInt(dm.group("high")))));
                    } catch (NumberFormatException ignored) {}
                }
                continue;
            }

            // Range identification (comes before rolled because it's more specific).
            Matcher rr = IDENT_RANGE.matcher(line);
            if (rr.matches()) {
                int low = signed(rr.group("sLow"), rr.group("low"));
                int high = signed(rr.group("sHigh"), rr.group("high"));
                String unit = rr.group("unit");
                String name = rr.group("name").trim();
                StatType st = new StatType(apiNameFor(name), name);
                possibles.add(new StatPossibleValues(st, new RangedValue(low, high), (low + high) / 2, false));
                continue;
            }

            // Single rolled identification.
            Matcher rf = IDENT_ROLLED.matcher(line);
            if (rf.matches()) {
                int val = signed(rf.group("sign"), rf.group("val"));
                String unit = rf.group("unit");
                String name = rf.group("name").trim();
                StatType st = new StatType(apiNameFor(name), name);
                idents.add(new StatActualValue(st, val, 0, null));
            }
        }

        GearRequirements reqs = new GearRequirements(level, classReq, skillReqs, questReq);
        return new ParseResult(idents, possibles, damages, reqs);
    }

    private static int signed(String sign, String val) {
        int n = Integer.parseInt(val);
        return "-".equals(sign) ? -n : n;
    }

    private static ClassType classFromName(String name) {
        if (name == null) return null;
        String upper = name.toUpperCase();
        for (ClassType t : ClassType.values()) {
            if (t == ClassType.NONE) continue;
            if (t.name().equals(upper)) return t;
            if (t.getActualName(true).equalsIgnoreCase(name)) return t;
            if (t.getActualName(false).equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    private static Skill skillFromName(String name) {
        for (Skill s : Skill.values()) if (s.name().equalsIgnoreCase(name)) return s;
        return null;
    }

    private static DamageType damageTypeFromName(String name) {
        for (DamageType d : DamageType.values()) if (d.name().equalsIgnoreCase(name)) return d;
        return null;
    }

    /** Rough camelCase API name — approximates Wynncraft's stat keys for display→key lookup. */
    private static String apiNameFor(String displayName) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String word : displayName.split("\\s+")) {
            if (word.isEmpty()) continue;
            if (first) { sb.append(Character.toLowerCase(word.charAt(0))).append(word.substring(1)); first = false; }
            else { sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()); }
        }
        return sb.toString();
    }
}
