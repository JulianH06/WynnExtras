// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings.
 */
package julianh06.wynnextras.wtshim.handlers.scoreboard;

import com.google.common.collect.ImmutableList;
import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.Handler;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.handlers.scoreboard.event.ScoreboardSegmentAdditionEvent;
import julianh06.wynnextras.wtshim.handlers.scoreboard.event.ScoreboardUpdatedEvent;
import julianh06.wynnextras.wtshim.handlers.scoreboard.type.ScoreboardLine;
import julianh06.wynnextras.wtshim.handlers.scoreboard.type.SegmentMatcher;
import julianh06.wynnextras.wtshim.mc.event.ScoreboardEvent;
import julianh06.wynnextras.wtshim.mc.event.ScoreboardSetDisplayObjectiveEvent;
import julianh06.wynnextras.wtshim.mc.event.ScoreboardSetObjectiveEvent;
import julianh06.wynnextras.wtshim.mc.event.TickEvent;
import julianh06.wynnextras.wtshim.models.worlds.event.WorldStateEvent;
import julianh06.wynnextras.wtshim.models.worlds.type.WorldState;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import julianh06.wynnextras.wtshim.utils.type.Pair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.commons.lang3.StringUtils;

public final class ScoreboardHandler extends Handler {
    private static final Pattern NEXT_LINE_PATTERN = Pattern.compile("À+");
    private static final String SCOREBOARD_KEY = "wynntilsSB";
    private static final MutableText SCOREBOARD_TITLE_COMPONENT = Text.literal("play.wynncraft.com")
            .formatted(Formatting.BOLD)
            .formatted(Formatting.GOLD);
    private static final int MAX_SCOREBOARD_LINE = 16;
    private static final ScoreboardPart FALLBACK_SCOREBOARD_PART = new FallbackScoreboardPart();

    private String currentScoreboardName = "";
    private List<Pair<ScoreboardPart, ScoreboardSegment>> scoreboardSegments = new ArrayList<>();

    private final List<ScoreboardPart> scoreboardParts = new ArrayList<>();

    private boolean scoreboardOutdated = false;
    private long lastScoreboardUpdateTick = -1;

    // Deviation: Yarn's client World exposes no monotonic getGameTime() equivalent (only
    // getTimeOfDay(), which Wynncraft freezes). We maintain our own tick counter, incremented
    // once per TickEvent, purely to reproduce Wynntils' "wait one tick after the last packet"
    // debounce. Behaviorally identical.
    private long currentTick = 0;

    public void addPart(ScoreboardPart scoreboardPart) {
        scoreboardParts.add(scoreboardPart);
    }

    private boolean isValidScoreboardName(String scoreboardName) {
        // If the name is longer than 14 characters, we need to trim it (16 chars max, 2 reversed for sb/bf)
        String name = McUtils.player().getNameForScoreboard();
        name = name.length() > 14 ? name.substring(0, 14) : name;

        return (scoreboardName.startsWith("sb") || scoreboardName.startsWith("bf")) && scoreboardName.endsWith(name);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSetScore(ScoreboardEvent.Set event) {
        if (!currentScoreboardName.equals(event.getObjectiveName())) return;

        updateNextTick();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSetScore(ScoreboardEvent.Reset event) {
        updateNextTick();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSetObjective(ScoreboardSetObjectiveEvent event) {
        if (!currentScoreboardName.equals(event.getObjectiveName())) return;

        updateNextTick();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSetObjectiveDisplay(ScoreboardSetDisplayObjectiveEvent event) {
        if (!isValidScoreboardName(event.getObjectiveName())) return;

        currentScoreboardName = event.getObjectiveName();
        updateNextTick();

        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        currentTick++;

        if (!scoreboardOutdated) return;
        if (McUtils.mc().world == null) return;

        // Wait until packets have stopped arriving
        if (currentTick - lastScoreboardUpdateTick < 1) {
            return;
        }

        scoreboardOutdated = false;
        handleUpdate();
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() == WorldState.WORLD) return;

        scoreboardSegments.forEach(pair -> pair.key().reset());

        scoreboardSegments = new ArrayList<>();
        currentScoreboardName = "";

        scoreboardOutdated = false;
        lastScoreboardUpdateTick = -1;
    }

    private void updateNextTick() {
        scoreboardOutdated = true;
        lastScoreboardUpdateTick = currentTick;
    }

    private void handleUpdate() {
        // 1. Get a reconstructed scoreboard from the current scoreboard state
        List<ScoreboardLine> reconstructedScoreboard = getCurrentScoreboardState(currentScoreboardName);

        // 2. Verify that the scoreboard is in a semi-valid state
        // (in a state where we can make sense of it, even if the actual data is still being updated)
        List<ScoreboardPart> validParts = getValidScoreboardParts(reconstructedScoreboard);

        // 3. Calculate the scoreboard segments, do segment updates
        calculateScoreboardSegments(reconstructedScoreboard, validParts);

        // 4. Create our own scoreboard to hide specific segments
        createScoreboardFromSegments();
    }

    private List<ScoreboardLine> getCurrentScoreboardState(String currentScoreboardName) {
        Scoreboard scoreboard = McUtils.mc().world.getScoreboard();
        ScoreboardObjective currentObjective = scoreboard.getNullableObjective(currentScoreboardName);

        if (currentObjective == null) {
            WynntilsMod.warn("Could not find the current scoreboard objective: " + currentScoreboardName);
            return List.of();
        }

        return scoreboard.getScoreboardEntries(currentObjective).stream()
                .map(entry -> new ScoreboardLine(StyledText.fromString(entry.owner()), entry.value()))
                .sorted(Comparator.comparing(ScoreboardLine::score).reversed())
                .toList();
    }

    private List<ScoreboardPart> getValidScoreboardParts(List<ScoreboardLine> reconstructedScoreboard) {
        // The scoreboard is valid if:
        // 1. There are no duplicate lines
        // 2. There are no gaps in the scores, and they are decreasing (there are no duplicate scores)

        // We can also check for validness by checking scoreboard parts:
        // 3. A valid scoreboard always starts with a newline (À)
        // 4. A scoreboard is valid if it consists of valid segments:
        //    - A valid segment is a part that starts with a header, then one or more lines, then a footer which is a
        // newline (À+).
        //    - The footer is not present if the segment is the last one displayed.

        // 0. An empty scoreboard is valid
        if (reconstructedScoreboard.isEmpty()) {
            return List.of();
        }

        // 1. Check for duplicate lines
        List<StyledText> lines = new ArrayList<>();
        for (ScoreboardLine line : reconstructedScoreboard) {
            if (lines.contains(line.line())) {
                // We found a duplicate line, so the scoreboard is invalid
                return List.of();
            }

            lines.add(line.line());
        }

        // 2. Check for gaps in the scores
        int lastScore = reconstructedScoreboard.stream()
                .map(ScoreboardLine::score)
                .findFirst()
                .orElse(0);
        for (ScoreboardLine line : reconstructedScoreboard.stream().skip(1).toList()) {
            if (line.score() >= lastScore) {
                // We found a non strictly decreasing score, so the scoreboard is invalid
                // Note: lastScore - line.score() should always be 1,
                //       but during very specific cases during lootruns there can be a gap of 2
                return List.of();
            }

            lastScore = line.score();
        }

        // 3. Check for a new line at the start
        if (!reconstructedScoreboard.stream()
                .findFirst()
                .map(ScoreboardLine::line)
                .orElse(StyledText.EMPTY)
                .equals(StyledText.fromString("À"))) {
            // We did not find a new line at the start, so the scoreboard is invalid
            return List.of();
        }

        // 4. Check for segment correctness
        //    There are 2 error cases here:
        //       - Fatal error: We find info that makes the current scoreboard invalid
        //       - "Valid" error: We find an error, but it only makes the current segment invalid, not the scoreboard
        //                        If we find a segment that is not valid,
        //                        we return the list of valid segments up to that point.
        //                        This is a valid case because the scoreboard cannot fit all segments,
        //                        so it will only display the x lines.
        int currentIndex = 1;
        List<ScoreboardLine> scoreboardLines = reconstructedScoreboard.stream().toList();

        List<ScoreboardPart> scoreboardParts = new ArrayList<>();
        while (currentIndex < scoreboardLines.size()) {
            ScoreboardPart part = getScoreboardPartForHeader(scoreboardLines.get(currentIndex));

            // We could not find a suitable part for the header
            if (part == null) {
                return scoreboardParts;
            }

            // A part cannot be duplicated unless the scoreboard is invalid (or the part is the fallback part)
            if (part != FALLBACK_SCOREBOARD_PART && scoreboardParts.contains(part)) {
                return List.of();
            }

            // The header can be the last line, but that makes that segment invalid
            if (currentIndex + 1 == scoreboardLines.size()) {
                return scoreboardParts;
            }

            // The next line cannot be the end of this segment
            // (As it would mean the header has no content)
            if (scoreboardLines
                    .get(currentIndex + 1)
                    .line()
                    .getMatcher(NEXT_LINE_PATTERN)
                    .matches()) {
                return List.of();
            }

            scoreboardParts.add(part);

            // Find the next segment end
            for (currentIndex = currentIndex + 1; currentIndex < scoreboardLines.size(); currentIndex++) {
                ScoreboardLine line = scoreboardLines.get(currentIndex);

                if (line.line().getMatcher(NEXT_LINE_PATTERN).matches()) {
                    currentIndex++;
                    break;
                }
            }
        }

        // All checks passed, so the scoreboard is valid
        // (In theory, this can happen while the scoreboard is still being updated, but it's very unlikely, and we
        // cannot do anything about it)
        return scoreboardParts;
    }

    private void calculateScoreboardSegments(
            List<ScoreboardLine> reconstructedScoreboard, List<ScoreboardPart> validParts) {
        int currentIndex = 1;
        List<ScoreboardLine> scoreboardLines = reconstructedScoreboard.stream().toList();

        List<Pair<ScoreboardPart, ScoreboardSegment>> oldSegments = ImmutableList.copyOf(scoreboardSegments);
        scoreboardSegments = new ArrayList<>();

        int validPartIndex = 0;
        while (currentIndex < scoreboardLines.size() && validPartIndex < validParts.size()) {
            ScoreboardLine headerLine = scoreboardLines.get(currentIndex);
            ScoreboardPart calculatedPart = getScoreboardPartForHeader(headerLine);

            // We could not find a suitable part for the header
            if (calculatedPart == null) {
                WynntilsMod.error(
                        "Scoreboard passed validness check, but we could not find a scoreboard part for the line: "
                                + scoreboardLines.get(currentIndex).line());
                return;
            }

            // Check if we calculate the same part as during the validation
            if (calculatedPart != validParts.get(validPartIndex)) {
                WynntilsMod.error("Scoreboard passed validness check, but the scoreboard part for the line: "
                        + scoreboardLines.get(currentIndex).line()
                        + " does not match the valid part: "
                        + validParts.get(validPartIndex));
                return;
            }

            validPartIndex++;

            List<StyledText> contentLines = new ArrayList<>();
            for (currentIndex = currentIndex + 1; currentIndex < scoreboardLines.size(); currentIndex++) {
                ScoreboardLine line = scoreboardLines.get(currentIndex);

                if (line.line().getMatcher(NEXT_LINE_PATTERN).matches()) {
                    currentIndex++;
                    break;
                }

                contentLines.add(line.line());
            }

            ScoreboardSegment segment = new ScoreboardSegment(calculatedPart, headerLine.line(), contentLines);
            boolean eventCanceled = WynntilsMod.postEvent(new ScoreboardSegmentAdditionEvent(segment));

            segment.setVisibility(!eventCanceled);
            scoreboardSegments.add(new Pair<>(calculatedPart, segment));
        }

        // Handle segment removals
        for (Pair<ScoreboardPart, ScoreboardSegment> oldPair : oldSegments) {
            // Special case for the fallback part, don't call onSegmentRemove
            if (oldPair.key() == FALLBACK_SCOREBOARD_PART) continue;

            Optional<Pair<ScoreboardPart, ScoreboardSegment>> segmentOpt = scoreboardSegments.stream()
                    .filter(pair -> pair.key() == oldPair.key())
                    .findFirst();
            if (segmentOpt.isEmpty()) {
                oldPair.key().onSegmentRemove(oldPair.value());
            }
        }

        // Handle segment changes
        for (Pair<ScoreboardPart, ScoreboardSegment> pair : scoreboardSegments) {
            // Special case for the fallback part, don't call onSegmentChange
            if (pair.key() == FALLBACK_SCOREBOARD_PART) continue;

            Optional<Pair<ScoreboardPart, ScoreboardSegment>> oldSegmentOpt = oldSegments.stream()
                    .filter(oldPair -> oldPair.key() == pair.key())
                    .findFirst();
            if (oldSegmentOpt.isEmpty() || !oldSegmentOpt.get().value().equals(pair.value())) {
                pair.key().onSegmentChange(pair.value());
            }
        }
    }

    private void createScoreboardFromSegments() {
        // Wynntils uses McUtils.player().level(); Yarn's ClientPlayerEntity exposes no world getter,
        // so we read the identical scoreboard via the client world.
        Scoreboard scoreboard = McUtils.mc().world.getScoreboard();

        ScoreboardObjective oldObjective = scoreboard.getNullableObjective(SCOREBOARD_KEY);
        if (oldObjective != null) {
            scoreboard.removeObjective(oldObjective);
        }

        ScoreboardObjective wynntilsObjective = scoreboard.addObjective(
                SCOREBOARD_KEY,
                ScoreboardCriterion.DUMMY,
                SCOREBOARD_TITLE_COMPONENT,
                ScoreboardCriterion.RenderType.INTEGER,
                true,
                BlankNumberFormat.INSTANCE);

        if (scoreboardSegments.stream().map(Pair::value).noneMatch(ScoreboardSegment::isVisible)) {
            WynntilsMod.postEvent(new ScoreboardUpdatedEvent(new ArrayList<>()));
            return;
        }

        // Only display the scoreboard if there is at least one visible segment
        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, wynntilsObjective);

        int currentScoreboardLine = MAX_SCOREBOARD_LINE;

        // Insert the first line at the top
        scoreboard
                .getOrCreateScore(ScoreHolder.fromName("À"), wynntilsObjective)
                .setScore(currentScoreboardLine);
        currentScoreboardLine--;

        int separatorCount = 2;

        // Insert the visible segments
        List<ScoreboardSegment> segments =
                scoreboardSegments.stream().map(Pair::value).toList();
        for (int i = 0; i < segments.size(); i++) {
            ScoreboardSegment scoreboardSegment = segments.get(i);
            if (!scoreboardSegment.isVisible()) continue;

            scoreboard
                    .getOrCreateScore(
                            ScoreHolder.fromName(
                                    scoreboardSegment.getHeader().getString()),
                            wynntilsObjective)
                    .setScore(currentScoreboardLine);
            currentScoreboardLine--;

            for (StyledText line : scoreboardSegment.getContent()) {
                scoreboard
                        .getOrCreateScore(ScoreHolder.fromName(line.getString()), wynntilsObjective)
                        .setScore(currentScoreboardLine);
                currentScoreboardLine--;
            }

            if (i != segments.size() - 1) {
                scoreboard
                        .getOrCreateScore(
                                ScoreHolder.fromName(StringUtils.repeat('À', separatorCount)), wynntilsObjective)
                        .setScore(currentScoreboardLine);
                currentScoreboardLine--;
                separatorCount++;
            }
        }

        WynntilsMod.postEvent(new ScoreboardUpdatedEvent(scoreboardSegments));
    }

    private ScoreboardPart getScoreboardPartForHeader(ScoreboardLine scoreboardLine) {
        String unformattedLine = scoreboardLine.line().getStringWithoutFormatting();

        for (ScoreboardPart part : scoreboardParts) {
            if (part.getSegmentMatcher()
                    .headerPattern()
                    .matcher(unformattedLine)
                    .matches()) {
                return part;
            }
        }

        return FALLBACK_SCOREBOARD_PART;
    }

    private static final class FallbackScoreboardPart extends ScoreboardPart {
        private static final SegmentMatcher FALLBACK_MATCHER = SegmentMatcher.fromPattern(".*");

        @Override
        public SegmentMatcher getSegmentMatcher() {
            return FALLBACK_MATCHER;
        }

        @Override
        public void onSegmentChange(ScoreboardSegment newValue) {}

        @Override
        public void onSegmentRemove(ScoreboardSegment segment) {}

        @Override
        public void reset() {}

        @Override
        public String toString() {
            return "FallbackScoreboardPart{}";
        }
    }
}
