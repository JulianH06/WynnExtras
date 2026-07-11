// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — base Container type.
 * Faithful port of Wynntils' Container: a container is recognised by a title Pattern
 * (matched against the OpenScreen packet title via MenuEvent) or by a Screen predicate
 * (matched at ScreenInit). ContainerModel iterates the registered types and picks the match.
 *
 * Yarn adaptation: Component -> Text, Screen import from net.minecraft.client.gui.screen.
 */
package julianh06.wynnextras.wtshim.models.containers;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class Container {
    private final Predicate<Screen> screenPredicate;
    private final Pattern titlePattern;

    private int containerId;

    protected Container(Pattern titlePattern) {
        this.titlePattern = titlePattern;
        this.screenPredicate =
                screen -> titlePattern.matcher(screen.getTitle().getString()).matches();
    }

    protected Container(Predicate<Screen> screenPredicate) {
        this.titlePattern = null;
        this.screenPredicate = screenPredicate;
    }

    public void setContainerId(int containerId) {
        this.containerId = containerId;
    }

    public int getContainerId() {
        return containerId;
    }

    public boolean isScreen(Screen screen) {
        return screenPredicate.test(screen);
    }

    public boolean matchesTitle(Text title) {
        if (titlePattern != null) {
            return titlePattern.matcher(title.getString()).matches();
        }
        // For custom predicates, fallback to false
        return false;
    }

    public String getContainerName() {
        return this.getClass().getSimpleName().replace("Container", "");
    }
}
