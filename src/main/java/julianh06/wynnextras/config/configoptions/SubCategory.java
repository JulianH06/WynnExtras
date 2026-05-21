package julianh06.wynnextras.config.configoptions;

import java.util.ArrayList;
import java.util.List;

public class SubCategory {
    public final String name;
    public final List<ConfigOption> options = new ArrayList<>();
    private boolean expanded = false;

    public SubCategory(String name) { this.name = name; }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void toggleExpanded() {
        expanded = !expanded;
    }
}
