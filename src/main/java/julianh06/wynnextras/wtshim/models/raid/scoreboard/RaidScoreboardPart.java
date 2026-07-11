// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Verbatim port for the WynnExtras standalone compat shim (wtshim); only the package changed.
 */
package julianh06.wynnextras.wtshim.models.raid.scoreboard;

import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.core.text.type.StyleType;
import julianh06.wynnextras.wtshim.handlers.scoreboard.ScoreboardPart;
import julianh06.wynnextras.wtshim.handlers.scoreboard.ScoreboardSegment;
import julianh06.wynnextras.wtshim.handlers.scoreboard.type.SegmentMatcher;
import julianh06.wynnextras.wtshim.utils.type.CappedValue;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RaidScoreboardPart extends ScoreboardPart {
    private static final SegmentMatcher RAID_MATCHER = SegmentMatcher.fromPattern("Raid:");
    // Text is split onto two lines, 2nd line says "to the exit"
    private static final Pattern BUFF_PATTERN = Pattern.compile("^Choose a buff or go$");
    private static final Pattern CHALLENGE_COMPLETED_PATTERN = Pattern.compile("^Challenge Completed!$");
    private static final Pattern CHALLENGES_PATTERN = Pattern.compile("^[-—] Challenges: (\\d+)/(\\d+)$");
    private static final Pattern EXIT_PATTERN = Pattern.compile("^Go to the exit$");
    // Split onto two lines, 2nd says "died"
    private static final Pattern PLAYERS_DIED_PATTERN = Pattern.compile("^Too many players have$");
    private static final Pattern OUT_OF_TIME_PATTERN = Pattern.compile("^You ran out of time!$");
    private static final Pattern TIMER_PATTERN = Pattern.compile(
            "^[-—] Time Left: (?<hours>\\d+:)?(?<minutes>\\d+):(?<seconds>\\d+)(?: \\[\\+\\d+[msMS]\\])?$");

    @Override
    public SegmentMatcher getSegmentMatcher() {
        return RAID_MATCHER;
    }

    @Override
    public void onSegmentChange(ScoreboardSegment newValue) {
        List<StyledText> content = newValue.getContent();

        if (content.isEmpty()) {
            WynntilsMod.warn("Raid scoreboard segment content is empty");
            return;
        }

        Models.Raid.notifyRaidScoreboardShown();

        StyledText currentStateLine = content.getFirst();

        if (currentStateLine.matches(EXIT_PATTERN, StyleType.NONE)) {
            Models.Raid.tryEnterChallengeIntermission();
        } else if (currentStateLine.matches(CHALLENGE_COMPLETED_PATTERN, StyleType.NONE)) {
            Models.Raid.completeChallenge();
        } else if (currentStateLine.matches(BUFF_PATTERN, StyleType.NONE)) {
            Models.Raid.enterBuffRoom();
        } else if (currentStateLine.matches(PLAYERS_DIED_PATTERN, StyleType.NONE)
                || (currentStateLine.matches(OUT_OF_TIME_PATTERN, StyleType.NONE))) {
            Models.Raid.failedRaid();
        } else {
            Models.Raid.tryStartChallenge(currentStateLine);
        }

        // Some challenges have instructions that take up more than 1 line so we need to loop through the remaining
        // content to find the time and challenges lines
        if (content.size() < 3) {
            WynntilsMod.warn("Raid scoreboard segment content is too short; less than 3");
            return;
        }

        for (StyledText line : content.subList(1, content.size())) {
            Matcher matcher = line.getMatcher(TIMER_PATTERN, StyleType.NONE);
            if (matcher.matches()) {
                int minutes = Integer.parseInt(matcher.group("minutes"));
                int seconds = Integer.parseInt(matcher.group("seconds"));

                if (matcher.group("hours") != null) {
                    int hours = Integer.parseInt(matcher.group("hours"));
                    minutes += hours * 60;
                }

                Models.Raid.setTimeLeft(minutes * 60 + seconds);
                continue;
            }

            // Challenges line should be last so no need to break
            matcher = line.getMatcher(CHALLENGES_PATTERN, StyleType.NONE);
            if (matcher.matches()) {
                Models.Raid.setChallenges(
                        new CappedValue(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
            }
        }
    }

    @Override
    public void onSegmentRemove(ScoreboardSegment segment) {
        // Do nothing here, we will know when the raid is over from patterns
    }

    @Override
    public void reset() {
        // Do nothing here, we will know when the raid is over from patterns
    }

    @Override
    public String toString() {
        return "RaidScoreboardPart{}";
    }
}
