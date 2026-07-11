// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings.
 *
 * Yarn mapping deviations (all documented inline where they occur):
 *  - net.minecraft.network.chat.Style        -> net.minecraft.text.Style
 *  - net.minecraft.ChatFormatting            -> net.minecraft.util.Formatting
 *      (getByCode->byCode, getChar->getCode, getColor->getColorValue, PREFIX_CODE->FORMATTING_CODE_PREFIX)
 *  - net.minecraft.network.chat.TextColor    -> net.minecraft.text.TextColor (getValue->getRgb)
 *  - net.minecraft.network.chat.FontDescription -> net.minecraft.text.StyleSpriteSource
 *  - Style has no public all-args constructor in Yarn: getStyle() rebuilds via the builder.
 *  - Style#applyTo(parent) -> Style#withParent(parent).
 *  - Font §{fr:/fas:/fps:} code emission is dropped (no FontLookup / sprite-source subtypes in the
 *    shim). The font still participates in equals/hashCode and getStyle(); only INCLUDE_FONTS /
 *    COMPLETE textual reconstruction of the font is omitted. DEFAULT/NONE (all the shim matches
 *    against) never include fonts, so pattern matching is unaffected.
 */
package julianh06.wynnextras.wtshim.core.text;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Objects;
import julianh06.wynnextras.wtshim.core.text.type.StyleType;
import julianh06.wynnextras.wtshim.utils.colors.CustomColor;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public final class PartStyle {
    private static final String STYLE_PREFIX = "§";
    private static final Int2ObjectMap<Formatting> INTEGER_TO_CHATFORMATTING_MAP = Arrays.stream(Formatting.values())
            .filter(Formatting::isColor)
            .collect(
                    () -> new Int2ObjectOpenHashMap<>(Formatting.values().length),
                    (map, cf) -> map.put(cf.getColorValue() | 0xFF000000, cf),
                    Int2ObjectMap::putAll);

    private final StyledTextPart owner;

    private final CustomColor color;
    private final CustomColor shadowColor;
    private final boolean obfuscated;
    private final boolean bold;
    private final boolean strikethrough;
    private final boolean underlined;
    private final boolean italic;
    private final ClickEvent clickEvent;
    private final HoverEvent hoverEvent;
    private final StyleSpriteSource font;

    private PartStyle(
            StyledTextPart owner,
            CustomColor color,
            CustomColor shadowColor,
            boolean obfuscated,
            boolean bold,
            boolean strikethrough,
            boolean underlined,
            boolean italic,
            ClickEvent clickEvent,
            HoverEvent hoverEvent,
            StyleSpriteSource font) {
        this.owner = owner;
        this.color = color;
        this.shadowColor = shadowColor;
        this.obfuscated = obfuscated;
        this.bold = bold;
        this.strikethrough = strikethrough;
        this.underlined = underlined;
        this.italic = italic;
        this.clickEvent = clickEvent;
        this.hoverEvent = hoverEvent;
        this.font = font;
    }

    PartStyle(PartStyle partStyle, StyledTextPart owner) {
        this.owner = owner;
        this.color = partStyle.color;
        this.shadowColor = partStyle.shadowColor;
        this.obfuscated = partStyle.obfuscated;
        this.bold = partStyle.bold;
        this.strikethrough = partStyle.strikethrough;
        this.underlined = partStyle.underlined;
        this.italic = partStyle.italic;
        this.clickEvent = partStyle.clickEvent;
        this.hoverEvent = partStyle.hoverEvent;
        this.font = partStyle.font;
    }

    static PartStyle fromStyle(Style style, StyledTextPart owner, Style parentStyle) {
        Style inheritedStyle;

        if (parentStyle == null) {
            inheritedStyle = style;
        } else {
            // This changes properties that are null, as-in, inheriting from the previous style.
            // Yarn: Style#applyTo(parent) is Style#withParent(parent).
            inheritedStyle = style.withParent(parentStyle);
        }

        return new PartStyle(
                owner,
                inheritedStyle.getColor() == null
                        ? CustomColor.NONE
                        : CustomColor.fromInt(inheritedStyle.getColor().getRgb() | 0xFF000000),
                inheritedStyle.getShadowColor() == null
                        ? CustomColor.NONE
                        : CustomColor.fromARGBInt(inheritedStyle.getShadowColor()),
                inheritedStyle.isObfuscated(),
                inheritedStyle.isBold(),
                inheritedStyle.isStrikethrough(),
                inheritedStyle.isUnderlined(),
                inheritedStyle.isItalic(),
                inheritedStyle.getClickEvent(),
                inheritedStyle.getHoverEvent(),
                inheritedStyle.getFont());
    }

    public String asString(PartStyle previousStyle, StyleType type) {
        // Rules of converting a Style to a String:
        // Every style is prefixed with a §.
        // 0. Every style string is fully qualified, meaning that it contains all the formatting, and reset if needed.
        // 1. Style color is converted to a color segment.
        //    A color segment is the prefix and the chatFormatting char.
        //    If this is a custom color, a hex color code is used.
        //    Example: §#FF0000 or §1
        // 2. Formatting is converted the same way as in the Style class.
        // 3. Click events are wrapped in square brackets, and is represented as an id.
        // 4. Hover events are wrapped in angle brackets, and is represented as an id.
        // 5. Additional formatting support is expressed with §{...} (shadow color §{sc:X}).
        //    Font specials (§{fr:/fas:/fps:}) are NOT emitted by this shim (see class header).

        if (!type.includeBasicFormatting()) return "";

        StringBuilder styleString = new StringBuilder();

        boolean skipFormatting = false;

        // If the color is the same as the previous style, we can try to construct a difference.
        // If colors don't match, the inserted color will reset the formatting, thus we need to include all formatting.
        // If the current color is NONE, we NEED to try to construct a difference,
        // since there will be no color formatting resetting the formatting afterwards.
        if (previousStyle != null && (color == CustomColor.NONE || previousStyle.color.equals(color))) {
            String differenceString = this.tryConstructDifference(previousStyle, type);

            if (differenceString != null) {
                styleString.append(differenceString);
                skipFormatting = true;
            } else {
                styleString.append(STYLE_PREFIX).append(Formatting.RESET.getCode());
            }
        }

        if (!skipFormatting) {
            // 1. Color
            if (color != CustomColor.NONE) {
                Formatting chatFormatting = INTEGER_TO_CHATFORMATTING_MAP.get(color.asInt());

                if (chatFormatting != null) {
                    styleString.append(STYLE_PREFIX).append(chatFormatting.getCode());
                } else {
                    styleString.append(STYLE_PREFIX).append(color.toHexString());
                }
            }
            if (type.includeShadowColors() && shadowColor != CustomColor.NONE) {
                styleString
                        .append(STYLE_PREFIX)
                        .append("{sc:")
                        .append(shadowColor.toHexString())
                        .append("}");
            }

            // 2. Formatting
            if (obfuscated) {
                styleString.append(STYLE_PREFIX).append(Formatting.OBFUSCATED.getCode());
            }
            if (bold) {
                styleString.append(STYLE_PREFIX).append(Formatting.BOLD.getCode());
            }
            if (strikethrough) {
                styleString.append(STYLE_PREFIX).append(Formatting.STRIKETHROUGH.getCode());
            }
            if (underlined) {
                styleString.append(STYLE_PREFIX).append(Formatting.UNDERLINE.getCode());
            }
            if (italic) {
                styleString.append(STYLE_PREFIX).append(Formatting.ITALIC.getCode());
            }
            // Deviation: font §{fr:/fas:/fps:} emission dropped (no FontLookup in shim). See class header.

            if (type.includeEvents()) {
                // 3. Click event
                if (clickEvent != null) {
                    styleString
                            .append(STYLE_PREFIX)
                            .append("[")
                            .append(owner.getParent().getClickEventIndex(clickEvent))
                            .append("]");
                }

                // 4. Hover event
                if (hoverEvent != null) {
                    styleString
                            .append(STYLE_PREFIX)
                            .append("<")
                            .append(owner.getParent().getHoverEventIndex(hoverEvent))
                            .append(">");
                }
            }
        }

        return styleString.toString();
    }

    public Style getStyle() {
        // Yarn has no public all-args Style constructor (unlike Mojmap), so rebuild via the builder.
        // Style.EMPTY starts with all attributes unset; the with* boolean setters store explicit
        // Boolean.TRUE/FALSE just like the Mojmap raw constructor did.
        Style style = Style.EMPTY;
        if (color != CustomColor.NONE) {
            // Mask the color int to 0xRRGGBB (TextColor doesn't expect alpha).
            style = style.withColor(TextColor.fromRgb(color.asInt() & 0x00FFFFFF));
        }
        if (shadowColor != CustomColor.NONE) {
            style = style.withShadowColor(shadowColor.asInt());
        }
        style = style.withBold(bold)
                .withItalic(italic)
                .withUnderline(underlined)
                .withStrikethrough(strikethrough)
                .withObfuscated(obfuscated);
        if (clickEvent != null) {
            style = style.withClickEvent(clickEvent);
        }
        if (hoverEvent != null) {
            style = style.withHoverEvent(hoverEvent);
        }
        if (font != null) {
            style = style.withFont(font);
        }
        return style;
    }

    public PartStyle withColor(Formatting color) {
        if (!color.isColor()) {
            throw new IllegalArgumentException("Formatting " + color + " is not a color!");
        }

        CustomColor newColor = CustomColor.fromInt(color.getColorValue() | 0xFF000000);

        return new PartStyle(
                owner,
                newColor,
                shadowColor,
                obfuscated,
                bold,
                strikethrough,
                underlined,
                italic,
                clickEvent,
                hoverEvent,
                font);
    }

    public PartStyle withColor(CustomColor color) {
        return new PartStyle(
                owner,
                color,
                shadowColor,
                obfuscated,
                bold,
                strikethrough,
                underlined,
                italic,
                clickEvent,
                hoverEvent,
                font);
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isObfuscated() {
        return obfuscated;
    }

    public boolean isStrikethrough() {
        return strikethrough;
    }

    public boolean isUnderlined() {
        return underlined;
    }

    public boolean isItalic() {
        return italic;
    }

    public ClickEvent getClickEvent() {
        return clickEvent;
    }

    public HoverEvent getHoverEvent() {
        return hoverEvent;
    }

    public CustomColor getColor() {
        return color;
    }

    public CustomColor getShadowColor() {
        return shadowColor;
    }

    public StyleSpriteSource getFont() {
        return font;
    }

    public PartStyle withShadowColor(CustomColor shadowColor) {
        return new PartStyle(
                owner,
                color,
                shadowColor,
                obfuscated,
                bold,
                strikethrough,
                underlined,
                italic,
                clickEvent,
                hoverEvent,
                font);
    }

    public PartStyle withBold(boolean bold) {
        return new PartStyle(
                owner,
                color,
                shadowColor,
                obfuscated,
                bold,
                strikethrough,
                underlined,
                italic,
                clickEvent,
                hoverEvent,
                font);
    }

    public PartStyle withObfuscated(boolean obfuscated) {
        return new PartStyle(
                owner,
                color,
                shadowColor,
                obfuscated,
                bold,
                strikethrough,
                underlined,
                italic,
                clickEvent,
                hoverEvent,
                font);
    }

    public PartStyle withStrikethrough(boolean strikethrough) {
        return new PartStyle(
                owner,
                color,
                shadowColor,
                obfuscated,
                bold,
                strikethrough,
                underlined,
                italic,
                clickEvent,
                hoverEvent,
                font);
    }

    public PartStyle withUnderlined(boolean underlined) {
        return new PartStyle(
                owner,
                color,
                shadowColor,
                obfuscated,
                bold,
                strikethrough,
                underlined,
                italic,
                clickEvent,
                hoverEvent,
                font);
    }

    public PartStyle withItalic(boolean italic) {
        return new PartStyle(
                owner,
                color,
                shadowColor,
                obfuscated,
                bold,
                strikethrough,
                underlined,
                italic,
                clickEvent,
                hoverEvent,
                font);
    }

    public PartStyle withClickEvent(ClickEvent clickEvent) {
        return new PartStyle(
                owner,
                color,
                shadowColor,
                obfuscated,
                bold,
                strikethrough,
                underlined,
                italic,
                clickEvent,
                hoverEvent,
                font);
    }

    public PartStyle withHoverEvent(HoverEvent hoverEvent) {
        return new PartStyle(
                owner,
                color,
                shadowColor,
                obfuscated,
                bold,
                strikethrough,
                underlined,
                italic,
                clickEvent,
                hoverEvent,
                font);
    }

    public PartStyle withFont(StyleSpriteSource font) {
        return new PartStyle(
                owner,
                color,
                shadowColor,
                obfuscated,
                bold,
                strikethrough,
                underlined,
                italic,
                clickEvent,
                hoverEvent,
                font);
    }

    private String tryConstructDifference(PartStyle oldStyle, StyleType type) {
        StringBuilder add = new StringBuilder();

        int oldColorInt = oldStyle.color.asInt();
        int newColorInt = this.color.asInt();

        if (oldColorInt == -1) {
            if (newColorInt != -1) {
                Arrays.stream(Formatting.values())
                        .filter(c -> c.isColor() && newColorInt == (c.getColorValue() | 0xFF000000))
                        .findFirst()
                        .ifPresent(c -> add.append(STYLE_PREFIX).append(c.getCode()));
            }
        } else if (oldColorInt != newColorInt) {
            return null;
        }

        if (type.includeShadowColors()) {
            int oldShadowColorInt = oldStyle.shadowColor.asInt();
            int newShadowColorInt = this.shadowColor.asInt();

            if (oldShadowColorInt == -1) {
                if (newColorInt != -1) {
                    Arrays.stream(Formatting.values())
                            .filter(c -> c.isColor() && newShadowColorInt == (c.getColorValue()))
                            .findFirst()
                            .ifPresent(c -> add.append(STYLE_PREFIX).append(c.getCode()));
                }
            } else if (oldShadowColorInt != newShadowColorInt) {
                return null;
            }
        }

        if (oldStyle.obfuscated && !this.obfuscated) return null;
        if (!oldStyle.obfuscated && this.obfuscated) add.append(STYLE_PREFIX).append(Formatting.OBFUSCATED.getCode());

        if (oldStyle.bold && !this.bold) return null;
        if (!oldStyle.bold && this.bold) add.append(STYLE_PREFIX).append(Formatting.BOLD.getCode());

        if (oldStyle.strikethrough && !this.strikethrough) return null;
        if (!oldStyle.strikethrough && this.strikethrough) add.append(STYLE_PREFIX).append(Formatting.STRIKETHROUGH.getCode());

        if (oldStyle.underlined && !this.underlined) return null;
        if (!oldStyle.underlined && this.underlined) add.append(STYLE_PREFIX).append(Formatting.UNDERLINE.getCode());

        if (oldStyle.italic && !this.italic) return null;
        if (!oldStyle.italic && this.italic) add.append(STYLE_PREFIX).append(Formatting.ITALIC.getCode());

        if (type.includeFonts()) {
            // Correctness guard preserved from the original: losing a font cannot be expressed as a
            // difference. Font code emission itself is dropped (see class header) — DEFAULT/NONE
            // never set includeFonts, so this branch is never taken by the shim's own matching.
            if (oldStyle.font != null && this.font == null) return null;
        }

        if (type.includeEvents()) {
            // If there is a click event in the old style, but not in the new one, we can't construct a difference.
            if (oldStyle.clickEvent != null && this.clickEvent == null) return null;
            if (oldStyle.clickEvent != this.clickEvent) {
                add.append(STYLE_PREFIX)
                        .append("[")
                        .append(owner.getParent().getClickEventIndex(clickEvent))
                        .append("]");
            }

            if (oldStyle.hoverEvent != null && this.hoverEvent == null) return null;
            if (oldStyle.hoverEvent != this.hoverEvent) {
                add.append(STYLE_PREFIX)
                        .append("<")
                        .append(owner.getParent().getHoverEventIndex(hoverEvent))
                        .append(">");
            }
        }

        return add.toString();
    }

    @Override
    public String toString() {
        return "PartStyle{" + "color="
                + color + ", obfuscated="
                + obfuscated + ", bold="
                + bold + ", strikethrough="
                + strikethrough + ", underlined="
                + underlined + ", italic="
                + italic + ", clickEvent="
                + clickEvent + ", hoverEvent="
                + hoverEvent + ", font="
                + font + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartStyle partStyle = (PartStyle) o;
        return obfuscated == partStyle.obfuscated
                && bold == partStyle.bold
                && strikethrough == partStyle.strikethrough
                && underlined == partStyle.underlined
                && italic == partStyle.italic
                && Objects.equals(color, partStyle.color)
                && Objects.equals(clickEvent, partStyle.clickEvent)
                && Objects.equals(hoverEvent, partStyle.hoverEvent)
                && Objects.equals(font, partStyle.font);
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, obfuscated, bold, strikethrough, underlined, italic, clickEvent, hoverEvent, font);
    }
}
