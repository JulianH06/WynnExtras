package julianh06.wynnextras.utils.text;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class StyledText {
    public static final StyledText EMPTY = fromString("");

    private final Text component;

    private StyledText(Text component) {
        this.component = component == null ? Text.empty() : component;
    }

    public static StyledText fromComponent(Text component) {
        return new StyledText(component);
    }

    public static StyledText fromString(String value) {
        return new StyledText(Text.literal(value == null ? "" : value));
    }

    public Text getComponent() {
        return component;
    }

    public String getString() {
        return component.getString();
    }

    public StyledText withoutFormatting() {
        return fromString(Formatting.strip(getString()));
    }

    @Override
    public String toString() {
        return getString();
    }
}
