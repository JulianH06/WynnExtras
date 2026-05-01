package julianh06.wynnextras.config.configoptions;

import com.wynntils.utils.colors.CustomColor;

public class DescLine {
    public enum Align { LEFT, CENTER, RIGHT }

    public final String text;
    public final Align align;
    public final boolean bold, italic, underline;
    public final float scale;
    public final CustomColor color;

    private DescLine(String text, Align align, boolean bold, boolean italic, boolean underline, float scale, CustomColor color) {
        this.text = text;
        this.align = align;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.scale = scale;
        this.color = color;
    }

    public static DescLine of(String text) {
        return new DescLine(text, Align.LEFT, false, false, false, 1.0f, null);
    }

    public DescLine left()      { return new DescLine(text, Align.LEFT,   bold, italic, underline, scale, color); }
    public DescLine center()    { return new DescLine(text, Align.CENTER, bold, italic, underline, scale, color); }
    public DescLine right()     { return new DescLine(text, Align.RIGHT,  bold, italic, underline, scale, color); }
    public DescLine bold()      { return new DescLine(text, align, true,  italic, underline, scale, color); }
    public DescLine italic()    { return new DescLine(text, align, bold,  true,  underline, scale, color); }
    public DescLine underline() { return new DescLine(text, align, bold,  italic, true,  scale, color); }
    public DescLine scale(float s) { return new DescLine(text, align, bold, italic, underline, s, color); }
    public DescLine color(CustomColor c) { return new DescLine(text, align, bold, italic, underline, scale, c); }
    public DescLine color(int c) { return new DescLine(text, align, bold, italic, underline, scale, CustomColor.fromInt(c)); }
}
