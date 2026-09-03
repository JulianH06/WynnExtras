package julianh06.wynnextras.wynncraft.item;

import julianh06.wynnextras.wynncraft.state.SkillPoint;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WynnTooltipParser {
    private static final String REQUIREMENT_SPRITE_FONT = "minecraft:tooltip/requirement/sprite";
    private static final String ATTRIBUTE_SPRITE_FONT = "minecraft:tooltip/attribute/sprite";
    private static final Pattern INTEGER = Pattern.compile("^[+-]?\\d{1,3}(?:,\\d{3})*$");
    private static final Pattern TEXT_SKILL_BONUS = Pattern.compile(
            "^\\s*(strength|dexterity|intelligence|defen[cs]e|agility)[^+\\-\\r\\n]*([+-]\\d{1,3}(?:,\\d{3})*)(?![\\d,%])",
            Pattern.CASE_INSENSITIVE
    );

    record Segment(String font, String text) {}

    record SkillStats(Map<SkillPoint, Integer> requirements, Map<SkillPoint, Integer> bonuses) {}

    private record SkillBonus(SkillPoint skill, int value) {}

    private WynnTooltipParser() {}

    static SkillStats parseSkillStats(List<List<Segment>> lore) {
        Map<SkillPoint, Integer> requirements = new EnumMap<>(SkillPoint.class);
        Map<SkillPoint, Integer> bonuses = new EnumMap<>(SkillPoint.class);
        int requirementHeader = -1;

        for (int lineIndex = 0; lineIndex < lore.size(); lineIndex++) {
            List<Segment> line = lore.get(lineIndex);
            if (isSkillRequirementHeader(line)) requirementHeader = lineIndex;

            SkillBonus textBonus = textSkillBonus(line);
            if (textBonus != null) {
                bonuses.put(textBonus.skill(), textBonus.value());
                continue;
            }

            Integer bonusSkill = attributeSkill(line);
            Integer bonusValue = firstInteger(line);
            if (bonusSkill != null && bonusValue != null) bonuses.put(skillPoint(bonusSkill), bonusValue);
        }

        if (requirementHeader >= 0) {
            int lastValueLine = Math.min(lore.size(), requirementHeader + 4);
            for (int lineIndex = requirementHeader + 1; lineIndex < lastValueLine; lineIndex++) {
                List<Integer> values = integers(lore.get(lineIndex));
                if (values.size() != 5) continue;
                for (int skill = 0; skill < values.size(); skill++) {
                    requirements.put(skillPoint(skill), values.get(skill));
                }
                break;
            }
        }

        return new SkillStats(Map.copyOf(requirements), Map.copyOf(bonuses));
    }

    static boolean hasWeaponStats(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        return lower.contains(" dps") || lower.contains("hits/s");
    }

    private static boolean isSkillRequirementHeader(List<Segment> line) {
        int expectedSkill = 0;
        for (Segment segment : line) {
            if (!REQUIREMENT_SPRITE_FONT.equals(segment.font()) || segment.text() == null) continue;
            for (int offset = 0; offset < segment.text().length();) {
                int codePoint = segment.text().codePointAt(offset);
                offset += Character.charCount(codePoint);
                int skill = requirementSkill(codePoint);
                if (skill < 0) continue;
                if (skill != expectedSkill) return false;
                expectedSkill++;
            }
        }
        return expectedSkill == 5;
    }

    private static SkillBonus textSkillBonus(List<Segment> line) {
        StringBuilder text = new StringBuilder();
        for (Segment segment : line) {
            if (segment.text() != null) text.append(segment.text());
        }

        Matcher matcher = TEXT_SKILL_BONUS.matcher(text);
        if (!matcher.find()) return null;

        SkillPoint skill = switch (matcher.group(1).toLowerCase(Locale.ROOT)) {
            case "strength" -> SkillPoint.STRENGTH;
            case "dexterity" -> SkillPoint.DEXTERITY;
            case "intelligence" -> SkillPoint.INTELLIGENCE;
            case "defense", "defence" -> SkillPoint.DEFENCE;
            case "agility" -> SkillPoint.AGILITY;
            default -> null;
        };
        if (skill == null) return null;

        try {
            return new SkillBonus(skill, Integer.parseInt(matcher.group(2).replace(",", "")));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer attributeSkill(List<Segment> line) {
        for (Segment segment : line) {
            if (!ATTRIBUTE_SPRITE_FONT.equals(segment.font()) || segment.text() == null) continue;
            for (int offset = 0; offset < segment.text().length();) {
                int codePoint = segment.text().codePointAt(offset);
                offset += Character.charCount(codePoint);
                if (codePoint >= 0xE010 && codePoint <= 0xE014) return codePoint - 0xE010;
            }
        }
        return null;
    }

    private static int requirementSkill(int codePoint) {
        if (codePoint >= 0xE000 && codePoint <= 0xE004) return codePoint - 0xE000;
        if (codePoint >= 0xE010 && codePoint <= 0xE014) return codePoint - 0xE010;
        return -1;
    }

    private static SkillPoint skillPoint(int index) {
        return SkillPoint.values()[index];
    }

    private static Integer firstInteger(List<Segment> line) {
        List<Integer> values = integers(line);
        return values.isEmpty() ? null : values.getFirst();
    }

    private static List<Integer> integers(List<Segment> line) {
        java.util.ArrayList<Integer> values = new java.util.ArrayList<>();
        for (Segment segment : line) {
            if (segment.text() == null) continue;
            Matcher matcher = INTEGER.matcher(segment.text().trim());
            if (!matcher.matches()) continue;
            try {
                values.add(Integer.parseInt(matcher.group().replace(",", "")));
            } catch (NumberFormatException ignored) {}
        }
        return values;
    }
}
