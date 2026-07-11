// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Adapted for the WynnExtras standalone compat shim (wtshim).
 *
 * Minimal subset: only the two members RaidModel needs are ported — unwrap() (verbatim, removes
 * Wynn's post-2.1 soft-wrap so the buff/parasite chat patterns match) and extractNameAndNick()
 * (Yarn hover-event adaptation). extractNameAndNick feeds only RaidModel's party-buff attribution,
 * which WynnExtras does not read; the Yarn HoverEvent API (getAction()/ShowText.value()) is used
 * in place of Mojmap's action()/value().
 */
package julianh06.wynnextras.wtshim.utils.mc;

import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.core.text.StyledTextPart;
import julianh06.wynnextras.wtshim.core.text.type.StyleType;
import julianh06.wynnextras.wtshim.utils.type.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.text.HoverEvent;

public final class StyledTextUtils {
    // Note: Post Wynncraft 2.1, the hover text is inconsistent, sometimes "'s" is white, sometimes it's gray
    // And yes, the "user" needs to be optional
    public static final Pattern NICKNAME_PATTERN =
            Pattern.compile("§f(?<nick>.+?)(§7)?'s?(§7)? real (user)?name is §f(?<username>.+)");

    private static final String NEWLINE_PREPARATION = "\n";
    private static final Pattern NEWLINE_WRAP_PATTERN = Pattern.compile("\uDAFF\uDFFC\uE001\uDB00\uDC06");

    private StyledTextUtils() {}

    /**
     * Removes the soft-wrap Wynn gives to all messages post 2.1.
     *
     * @param styledText The styled text to unwrap
     * @return The unwrapped styled text
     */
    public static StyledText unwrap(StyledText styledText) {
        List<StyledTextPart> newParts = new ArrayList<>();

        StyledTextPart lastWrappedPart = null;
        boolean expectEmptySpaceAfterWrap = false;
        for (StyledTextPart part : styledText) {
            String partString = part.getString(null, StyleType.NONE);

            // After a wrap, Wynn injects an empty space, which we want to remove
            if (expectEmptySpaceAfterWrap) {
                // There is an edge-case where this space is the whole part, in which case we skip it
                if (partString.equals(" ")) {
                    expectEmptySpaceAfterWrap = false;
                    continue;
                }

                // If the part starts with a space, we remove it
                if (partString.startsWith(" ")) {
                    partString = partString.substring(1);
                    part = new StyledTextPart(partString, part.getPartStyle().getStyle(), null, null);
                    expectEmptySpaceAfterWrap = false;
                } else {
                    // Log the edge-case
                    WynntilsMod.warn("Unexpected edge-case in unwrap: " + part);
                }

                // Continue with execution, a part may have a wrap before and after it
            }

            // If the part ends with a newline, it may be a preparation for a wrap
            if (partString.endsWith(NEWLINE_PREPARATION)) {
                lastWrappedPart = part;
                continue;
            }

            // Confirm whether the last part was wrapped
            if (lastWrappedPart != null) {
                if (NEWLINE_WRAP_PATTERN.matcher(partString).matches()) {
                    // Skip the current part, add back the last part, without the newline
                    String lastPartWithoutNewline = lastWrappedPart.getString(null, StyleType.NONE);
                    lastPartWithoutNewline = lastPartWithoutNewline.substring(
                            0, lastPartWithoutNewline.length() - NEWLINE_PREPARATION.length());

                    // Check if the style of the current part matches with the one we added last time,
                    // if so, we merge them
                    if (!newParts.isEmpty()
                            && newParts.getLast().getPartStyle().equals(lastWrappedPart.getPartStyle())) {
                        StyledTextPart lastPart = newParts.removeLast();
                        lastPartWithoutNewline = lastPart.getString(null, StyleType.NONE) + lastPartWithoutNewline;

                        newParts.add(new StyledTextPart(
                                lastPartWithoutNewline,
                                lastWrappedPart.getPartStyle().getStyle(),
                                null,
                                null));
                    } else {
                        newParts.add(new StyledTextPart(
                                lastPartWithoutNewline,
                                lastWrappedPart.getPartStyle().getStyle(),
                                null,
                                null));
                    }

                    // After a soft wrap, we need to insert a space
                    if (!newParts.getLast().getString(null, StyleType.NONE).equals(" ")) {
                        newParts.add(new StyledTextPart(
                                " ", lastWrappedPart.getPartStyle().getStyle(), null, null));
                    }

                    expectEmptySpaceAfterWrap = true;
                } else {
                    // The last part had a newline, but it was not a wrap, so we add it to the new parts
                    newParts.add(lastWrappedPart);
                    newParts.add(part);
                }

                lastWrappedPart = null;
                continue;
            }

            // Check if the style of the current part matches with the one we added last time,
            // if so, we merge them
            if (!newParts.isEmpty() && newParts.getLast().getPartStyle().equals(part.getPartStyle())) {
                StyledTextPart lastPart = newParts.removeLast();
                partString = lastPart.getString(null, StyleType.NONE) + partString;

                newParts.add(new StyledTextPart(partString, part.getPartStyle().getStyle(), null, null));

                continue;
            }

            // If nothing special happened, we just add the part to the new parts
            newParts.add(part);
        }

        // If there is a part that turned out not to be wrapped, we add it to the new parts
        if (lastWrappedPart != null) {
            newParts.add(lastWrappedPart);
        }

        // If we inserted a space after a soft wrap but never saw a following part, drop that trailing space
        if (!newParts.isEmpty()
                && newParts.getLast().getString(null, StyleType.NONE).equals(" ")) {
            newParts.removeLast();
        }

        return StyledText.fromParts(newParts);
    }

    /**
     * @param styledText Entire StyledText containing the nickname segment
     * @return [username, nick] pair if a nickname is found, null otherwise
     */
    public static Pair<String, String> extractNameAndNick(Iterable<StyledTextPart> styledText) {
        for (StyledTextPart part : styledText) {
            // Yarn: PartStyle exposes the HoverEvent directly.
            HoverEvent hoverEvent = part.getPartStyle().getHoverEvent();

            if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) {
                continue;
            }

            HoverEvent.ShowText showTextHoverEvent = (HoverEvent.ShowText) hoverEvent;
            StyledText[] partTexts =
                    StyledText.fromComponent(showTextHoverEvent.value()).split("\n");

            for (StyledText partText : partTexts) {
                Matcher nicknameMatcher = partText.getMatcher(NICKNAME_PATTERN);

                if (nicknameMatcher.matches()) {
                    return new Pair<>(nicknameMatcher.group("username"), nicknameMatcher.group("nick"));
                }
            }
        }

        return null;
    }
}
