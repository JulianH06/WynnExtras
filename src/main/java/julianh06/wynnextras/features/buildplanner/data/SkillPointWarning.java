package julianh06.wynnextras.features.buildplanner.data;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record SkillPointWarning(String message, boolean tomeSuggestion) {
    private static final String[] SKILLS = {"str", "dex", "int", "def", "agi"};
    private static final String[] LABELS = {"Str", "Dex", "Int", "Def", "Agi"};
    private static final String[] FULL_NAMES = {"Strength", "Dexterity", "Intelligence", "Defense", "Agility"};

    public static Optional<SkillPointWarning> forBuild(int level, int[] assigned, List<WynnTome> selected) {
        if (Arrays.stream(assigned).noneMatch(value -> value > 100)) {
            return Optional.empty();
        }
        List<WynnTome> guildTomes = selected.stream().filter(tome -> tome.type().equals("guildTome")).toList();
        int[] current = new int[5];
        for (WynnTome tome : guildTomes) {
            for (int i = 0; i < 5; i++) {
                current[i] += tome.identifications().getOrDefault(SKILLS[i], 0);
            }
        }
        List<WynnTome> candidates = TomeDatabase.getInstance().search("guildTome", "").stream()
                .filter(tome -> tome.level() <= level)
                .sorted(Comparator.comparingInt(tome -> isRainbow(tome) ? 1 : 0))
                .toList();
        for (WynnTome tome : candidates) {
            int total = 0;
            boolean fits = true;
            for (int i = 0; i < 5; i++) {
                int required = Math.max(0, assigned[i] + current[i] - tome.identifications().getOrDefault(SKILLS[i], 0));
                fits &= required <= 100;
                total += required;
            }
            if (fits && total <= BuildCalculator.levelToSkillPoints(level)) {
                String name = "Rainbow";
                if (!isRainbow(tome)) {
                    for (int i = 0; i < 5; i++) {
                        if (tome.identifications().getOrDefault(SKILLS[i], 0) > 0) {
                            name = LABELS[i];
                            break;
                        }
                    }
                }
                return Optional.of(new SkillPointWarning("Warning: Build requires a " + name + " Tome"
                        + (guildTomes.isEmpty() ? "" : " (replace guild tome)"), true));
            }
        }
        int first = 0;
        while (assigned[first] <= 100) {
            first++;
        }
        return Optional.of(new SkillPointWarning("Cannot assign " + assigned[first] + " skillpoints in "
                + FULL_NAMES[first] + " manually.", false));
    }

    private static boolean isRainbow(WynnTome tome) {
        return Arrays.stream(SKILLS).allMatch(key -> tome.identifications().getOrDefault(key, 0) > 0);
    }
}
