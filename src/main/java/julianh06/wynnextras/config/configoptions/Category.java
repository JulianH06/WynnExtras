package julianh06.wynnextras.config.configoptions;

import java.util.ArrayList;
import java.util.List;

import static julianh06.wynnextras.config.ConfigTheme.*;

public class Category {
    public final String name;
    public final int color;
    public final List<Object> items = new ArrayList<>();
    private SubCategory currentSub = null;
    private final ConfigScreenContext ctx;

    public Category(String name, int color, ConfigScreenContext ctx) {
        this.name = name;
        this.color = color;
        this.ctx = ctx;
    }

    public Category add(ConfigOption opt) {
        if (currentSub != null) currentSub.options.add(opt);
        else items.add(opt);
        return this;
    }

    public Category sub(String name) {
        currentSub = new SubCategory(name);
        items.add(currentSub);
        return this;
    }

    public Category endSub() {
        currentSub = null;
        return this;
    }

    public int getTotalHeight() {
        int contentW = ctx.getContentWidth();
        int h = 0;
        for (Object item : items) {
            if (item instanceof SubCategory s && ctx.subHasMatches(s)) {
                h += SUBCATEGORY_HEADER_HEIGHT + 5;
                if (s.isExpanded()) {
                    for (ConfigOption opt : s.options) {
                        if (ctx.matchesSearch(opt)) h += opt.getHeight(contentW - 8) + OPTION_SPACING;
                    }
                }
            } else if (item instanceof ConfigOption opt && ctx.matchesSearch(opt)) {
                h += opt.getHeight(contentW) + OPTION_SPACING;
            }
        }
        return h + 20;
    }
}
