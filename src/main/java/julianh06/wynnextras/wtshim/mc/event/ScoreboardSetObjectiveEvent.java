// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings.
 */
package julianh06.wynnextras.wtshim.mc.event;

import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.text.Text;
import net.neoforged.bus.api.Event;

public class ScoreboardSetObjectiveEvent extends Event {
    public static final int METHOD_ADD = 0;
    public static final int METHOD_REMOVE = 1;
    public static final int METHOD_CHANGE = 2;

    private final String objectiveName;
    private final Text displayName;
    private final ScoreboardCriterion.RenderType renderType;
    private final int method;

    public ScoreboardSetObjectiveEvent(
            String objectiveName, Text displayName, ScoreboardCriterion.RenderType renderType, int method) {
        this.objectiveName = objectiveName;
        this.displayName = displayName;
        this.renderType = renderType;
        this.method = method;
    }

    public String getObjectiveName() {
        return objectiveName;
    }

    public Text getDisplayName() {
        return displayName;
    }

    public ScoreboardCriterion.RenderType getRenderType() {
        return renderType;
    }

    public int getMethod() {
        return method;
    }
}
