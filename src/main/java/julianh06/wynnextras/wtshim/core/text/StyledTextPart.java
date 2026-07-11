// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings.
 *
 * Yarn mapping deviations (documented inline):
 *  - Component -> Text, MutableComponent -> MutableText, Style unchanged package (net.minecraft.text).
 *  - ChatFormatting -> Formatting (getByCode->byCode, PREFIX_CODE->FORMATTING_CODE_PREFIX).
 *  - FontDescription -> StyleSpriteSource.
 *  - Style#applyTo -> Style#withParent, Style#applyFormat -> Style#withFormatting.
 *  - Component.literal(t).withStyle(s) -> Text.literal(t).setStyle(s) (literal starts EMPTY, so
 *    setStyle is equivalent to Mojmap's merge-onto-empty).
 * Dropped from the original (documented):
 *  - fromJson(JsonArray): only used by Wynntils' HTML-API parser; unused in the shim and it pulls in
 *    Gson + sprite-source subtypes. Removed.
 *  - Font §{fr:/fas:/fps:} special-code PARSING: needs FontLookup / sprite-source subtypes /
 *    profile resolution. Removed; §{sc:} (shadow color) parsing is kept. Fonts still flow through
 *    from real components via Style#getFont(), preserving equals/hashCode fidelity.
 */
package julianh06.wynnextras.wtshim.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import julianh06.wynnextras.wtshim.core.text.type.StyleType;
import julianh06.wynnextras.wtshim.utils.colors.CustomColor;
import julianh06.wynnextras.wtshim.utils.wynn.WynnUtils;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class StyledTextPart {
    private final String text;
    private final PartStyle style;

    private final StyledText parent;

    public StyledTextPart(String text, Style style, StyledText parent, Style parentStyle) {
        this.parent = parent;
        this.text = text;

        // Must be done last
        this.style = PartStyle.fromStyle(style, this, parentStyle);
    }

    StyledTextPart(StyledTextPart part, StyledText parent) {
        this.text = part.text;
        this.style = new PartStyle(part.style, this);
        this.parent = parent;
    }

    private StyledTextPart(StyledTextPart part, PartStyle style, StyledText parent) {
        this.text = part.text;
        this.style = style;
        this.parent = parent;
    }

    // This factory is used to create a StyledTextPart from a component that has formatting codes
    // It is separate from the constructor because this only needs to be applied in cases there the text could have
    // formatting codes
    static List<StyledTextPart> fromCodedString(String codedString, Style style, StyledText parent, Style parentStyle) {
        // When we have a style, but the text has formatting codes,
        // we need to apply the formatting codes to the style
        // This means that the actual style applies first; then the formatting codes
        List<StyledTextPart> parts = new ArrayList<>();

        Style currentStyle = style;
        // Preserve inherited font across color code resets
        Style inheritedStyle = parentStyle == null ? style : style.withParent(parentStyle);
        StyleSpriteSource inheritedFont = inheritedStyle.getFont();
        StringBuilder currentString = new StringBuilder();

        boolean nextIsFormatting = false;
        StringBuilder hexColorFormatting = new StringBuilder();

        // []
        boolean clickEventPrefix = false;
        // <>
        boolean hoverEventPrefix = false;
        // {}
        boolean specialPrefix = false;
        StringBuilder specialString = new StringBuilder();

        String eventIndexString = "";

        for (char current : codedString.toCharArray()) {
            if (nextIsFormatting) {
                nextIsFormatting = false;

                // Only parse events, if we have a parent
                if (parent != null) {
                    if (current == '[') {
                        clickEventPrefix = true;
                        continue;
                    }
                    if (current == '<') {
                        hoverEventPrefix = true;
                        continue;
                    }
                }

                if (current == '{') {
                    specialPrefix = true;
                    continue;
                }
                // It looks like we have a hex color code
                if (current == '#') {
                    hexColorFormatting.append(current);
                    continue;
                }

                Formatting formatting = Formatting.byCode(current);

                if (formatting == null) {
                    currentString.append(Formatting.FORMATTING_CODE_PREFIX);
                    currentString.append(current);
                    continue;
                }

                // If we already had some text with the current style
                // Append it before modifying the style
                if (!currentString.isEmpty()) {
                    if (style != Style.EMPTY) {
                        // We might have lost an event, so we need to add it back
                        currentStyle = currentStyle
                                .withClickEvent(style.getClickEvent())
                                .withHoverEvent(style.getHoverEvent());
                    }
                    // But if the style is empty, we might have parsed events from the string itself

                    parts.add(new StyledTextPart(currentString.toString(), currentStyle, null, parentStyle));

                    // reset string
                    // style is not reset, because we want to keep the formatting
                    currentString = new StringBuilder();
                }

                // Color formatting resets the style
                if (formatting.isColor()) {
                    currentStyle = Style.EMPTY.withColor(formatting);

                    // But we keep the inherited font
                    if (inheritedFont != null) {
                        currentStyle = currentStyle.withFont(inheritedFont);
                    }
                } else {
                    currentStyle = currentStyle.withFormatting(formatting);
                }
                continue;
            }

            if (specialPrefix) {
                if (current != '}') {
                    // Keep appending until we find the closing bracket
                    specialString.append(current);
                    continue;
                } else {
                    // We currently do not have any special formatting
                    // But this is a placeholder for future features
                    specialPrefix = false;
                    String special = specialString.toString();
                    specialString = new StringBuilder();
                    if (special.startsWith("sc:")) {
                        // If we already had some text with the current style
                        // Append it before modifying the style
                        if (!currentString.isEmpty()) {
                            if (style != Style.EMPTY) {
                                // We might have lost an event, so we need to add it back
                                currentStyle = currentStyle
                                        .withClickEvent(style.getClickEvent())
                                        .withHoverEvent(style.getHoverEvent());
                            }
                            // But if the style is empty, we might have parsed events from the string itself

                            parts.add(new StyledTextPart(currentString.toString(), currentStyle, null, parentStyle));

                            // reset string
                            // style is not reset, because we want to keep the formatting
                            currentString = new StringBuilder();
                        }

                        CustomColor shadowColor = CustomColor.fromHexString(special.substring(3));
                        currentStyle = currentStyle.withShadowColor(shadowColor.asInt());
                    } else {
                        // Deviation: font specials (fr:/fas:/fps:) and unknown codes are ignored here.
                        // See class header — font parsing is dropped in the shim.
                    }
                    continue;
                }
            }

            // If we are parsing an event, handle it
            if (clickEventPrefix || hoverEventPrefix) {
                if (Character.isDigit(current)) {
                    eventIndexString += current;
                    continue;
                }

                // This is set to true if we have overwritten the current style's event
                Style oldStyle = null;

                if (clickEventPrefix && current == ']') {
                    ClickEvent clickEvent = parent.getClickEvent(Integer.parseInt(eventIndexString));

                    if (clickEvent != null) {
                        oldStyle = currentStyle;

                        currentStyle = currentStyle.withClickEvent(clickEvent);
                        clickEventPrefix = false;
                        eventIndexString = "";
                    }
                }

                if (hoverEventPrefix && current == '>') {
                    HoverEvent hoverEvent = parent.getHoverEvent(Integer.parseInt(eventIndexString));

                    if (hoverEvent != null) {
                        oldStyle = currentStyle;

                        currentStyle = currentStyle.withHoverEvent(hoverEvent);
                        hoverEventPrefix = false;
                        eventIndexString = "";
                    }
                }

                if (oldStyle != null) {
                    // If we already had some text with the current style
                    // Append it before modifying the style
                    if (!currentString.isEmpty()) {
                        if (style != Style.EMPTY) {
                            // We might have lost an event, so we need to add it back
                            // (theoretically this case can't happen at this location)
                            currentStyle = currentStyle
                                    .withClickEvent(style.getClickEvent())
                                    .withHoverEvent(style.getHoverEvent());
                        }
                        // But if the style is empty, we might have parsed events from the string itself

                        parts.add(new StyledTextPart(currentString.toString(), oldStyle, null, parentStyle));

                        // reset string
                        // style is not reset, because we want to keep the formatting
                        currentString = new StringBuilder();
                    }

                    // Even if we did not add a new part, we've parsed an event
                    continue;
                }

                // The event was not formatted properly, so add it as a string
                currentString.append(clickEventPrefix ? '[' : '<');
                currentString.append(eventIndexString);
                currentString.append(current);

                // Reset the related variables
                clickEventPrefix = false;
                hoverEventPrefix = false;
                eventIndexString = "";
                continue;
            }

            if (!hexColorFormatting.isEmpty()) {
                hexColorFormatting.append(current);

                // StyledText#getString() always uses full hex representation,
                // if the color is not a Formatting color (#rrggbbaa)
                if (hexColorFormatting.length() == 9) {
                    CustomColor customColor = CustomColor.fromHexString(hexColorFormatting.toString());

                    // If the color is invalid, we just append the hex formatting as text
                    if (customColor == CustomColor.NONE) {
                        currentString.append(hexColorFormatting);
                    } else if (!currentString.isEmpty()) {
                        // If we already had some text with the current style
                        // Append it before modifying the style
                        if (style != Style.EMPTY) {
                            // We might have lost an event, so we need to add it back
                            currentStyle = currentStyle
                                    .withClickEvent(style.getClickEvent())
                                    .withHoverEvent(style.getHoverEvent());
                        }
                        // But if the style is empty, we might have parsed events from the string itself

                        parts.add(new StyledTextPart(currentString.toString(), currentStyle, null, parentStyle));

                        // reset string
                        // style is not reset, because we want to keep the formatting
                        currentString = new StringBuilder();
                    }

                    currentStyle = currentStyle.withColor(customColor.asInt());
                    hexColorFormatting = new StringBuilder();
                }

                continue;
            }

            if (current == Formatting.FORMATTING_CODE_PREFIX) {
                nextIsFormatting = true;
                continue;
            }

            currentString.append(current);
        }

        // Check if we have some text left
        if (!currentString.isEmpty()) {
            if (style != Style.EMPTY) {
                // We might have lost an event, so we need to add it back
                currentStyle =
                        currentStyle.withClickEvent(style.getClickEvent()).withHoverEvent(style.getHoverEvent());
            }
            parts.add(new StyledTextPart(currentString.toString(), currentStyle, null, parentStyle));
        }

        return parts;
    }

    public String getString(PartStyle previousStyle, StyleType type) {
        return style.asString(previousStyle, type) + text;
    }

    public StyledText getParent() {
        return parent;
    }

    public PartStyle getPartStyle() {
        return style;
    }

    public StyledTextPart withStyle(PartStyle style) {
        return new StyledTextPart(this, style, parent);
    }

    public StyledTextPart withStyle(Function<PartStyle, PartStyle> function) {
        return withStyle(function.apply(style));
    }

    public MutableText getComponent() {
        // Yarn: Component.literal(t).withStyle(s) -> Text.literal(t).setStyle(s). The literal starts
        // with Style.EMPTY, so setStyle is equivalent to the original merge-onto-empty.
        return Text.literal(text).setStyle(style.getStyle());
    }

    StyledTextPart asNormalized() {
        return new StyledTextPart(WynnUtils.normalizeBadString(text), style.getStyle(), parent, null);
    }

    StyledTextPart stripLeading() {
        return new StyledTextPart(text.stripLeading(), style.getStyle(), parent, null);
    }

    StyledTextPart stripTrailing() {
        return new StyledTextPart(text.stripTrailing(), style.getStyle(), parent, null);
    }

    public boolean endsWith(String string) {
        return text.endsWith(string);
    }

    boolean isEmpty() {
        return text.isEmpty();
    }

    boolean isBlank() {
        return text.isBlank();
    }

    public int length() {
        return text.length();
    }

    @Override
    public String toString() {
        return "StyledTextPart[" + "text=" + text + ", " + "style=" + style + ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StyledTextPart that = (StyledTextPart) o;
        return Objects.equals(text, that.text) && Objects.equals(style, that.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, style);
    }
}
