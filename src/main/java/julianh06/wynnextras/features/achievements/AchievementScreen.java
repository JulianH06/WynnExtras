package julianh06.wynnextras.features.achievements;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.badges.BadgeCatalog;
import julianh06.wynnextras.features.badges.BadgeProfile;
import julianh06.wynnextras.features.badges.BadgeProfileData;
import julianh06.wynnextras.features.badges.BadgeService;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AchievementScreen extends WEScreen {
    private static final int BG = 0xF01A1410;
    private static final int BG_DARK = 0xFF120E0B;
    private static final int PANEL = 0xFF2E251C;
    private static final int PANEL_HOVER = 0xFF4D3C2D;
    private static final int PARCHMENT = 0xFF6C4F36;
    private static final int PARCHMENT_HOVER = 0xFF705030;
    private static final int BORDER = 0xFF3A2D24;
    private static final int BORDER_LIGHT = 0xFF876141;
    private static final int TEXT_MAIN = 0xFFE8DCC8;
    private static final int TEXT_DIM = 0xFF9A8B70;
    private static final int GOLD = 0xFFECC600;
    private static final int GREEN = 0xFF4A8C3A;
    private static final int RED = 0xFFA83232;
    private static final float SCROLL_SPEED = 0.3f;
    private static final float SCROLL_SNAP = 0.5f;
    private static final int BADGE_ICON_CELL = 88;
    private static final int BADGE_ICON_ROW_H = 92;
    private static final int BADGE_COLOR_CELL_W = BADGE_ICON_CELL;
    private static final int BADGE_COLOR_CELL_H = BADGE_ICON_CELL;
    private static final int BADGE_COLOR_ROW_H = BADGE_ICON_ROW_H;
    private static final int BADGE_CELL_GAP = 10;
    private static final int BADGE_SECTION_GAP = 48;
    private static final int BADGE_SEARCH_WIDTH = 260;
    private static final int BADGE_SEARCH_HEIGHT = 23;
    private static final int BADGE_FILTER_WIDTH = 92;
    private static final int BADGE_FILTER_GAP = 8;
    private static final int REWARD_PREVIEW_W = 38;
    private static final int REWARD_PREVIEW_H = 34;
    private static final int REWARD_PREVIEW_GAP = 8;
    private static final int ACHIEVEMENT_VIEWPORT_TOP = 126;
    private static final int ACHIEVEMENT_VIEWPORT_BOTTOM_PADDING = 20;
    private static final List<String> ASPECT_ACHIEVEMENT_ORDER = List.of(
            "aspect.max.all",
            "aspect.max.all.mythic",
            "aspect.max.all.fabled",
            "aspect.max.all.legendary",
            "aspect.max.all.warrior",
            "aspect.max.all.shaman",
            "aspect.max.all.mage",
            "aspect.max.all.archer",
            "aspect.max.all.assassin"
    );
    private static final List<String> WAR_ACHIEVEMENT_ORDER = List.of(
            "war.completion",
            "war.defence.very_low",
            "war.defence.low",
            "war.defence.medium",
            "war.defence.high",
            "war.defence.very_high"
    );

    private static final BadgeScreenSessionState SESSION_STATE = new BadgeScreenSessionState();
    private Tab tab = Tab.ACHIEVEMENTS;
    private float scroll = SESSION_STATE.achievementScroll;
    private float targetScroll = SESSION_STATE.targetAchievementScroll;
    private float iconScroll = SESSION_STATE.iconScroll;
    private float targetIconScroll = SESSION_STATE.targetIconScroll;
    private float colorScroll = SESSION_STATE.colorScroll;
    private float targetColorScroll = SESSION_STATE.targetColorScroll;
    private float maxAchievementScroll;
    private boolean achievementScrollbarDragging;
    private double achievementScrollbarDragOffset;
    private HorizontalScrollTarget horizontalScrollDragging;
    private double horizontalScrollDragOffset;
    private List<Text> hoveredBadgeTooltip = List.of();
    private TextInputWidget iconSearchBar;
    private TextInputWidget colorSearchBar;
    private TabButtonWidget achievementsTabButton;
    private TabButtonWidget badgesTabButton;
    private AchievementScrollBarWidget achievementScrollBarWidget;
    private HorizontalBadgeScrollBarWidget iconScrollBarWidget;
    private HorizontalBadgeScrollBarWidget colorScrollBarWidget;
    private final Map<String, CategoryHeaderWidget> categoryHeaderWidgets = new LinkedHashMap<>();
    private final Map<String, BadgeIconWidget> badgeIconWidgets = new LinkedHashMap<>();
    private final Map<String, BadgeColorWidget> badgeColorWidgets = new LinkedHashMap<>();
    private final Map<BadgeFilter, BadgeFilterButtonWidget> badgeFilterWidgets = new EnumMap<>(BadgeFilter.class);
    private UnlockFilterMode iconUnlockFilter = SESSION_STATE.iconUnlockFilter;
    private SpecialFilterMode imageIconFilter = SESSION_STATE.imageIconFilter;
    private UnlockFilterMode colorUnlockFilter = SESSION_STATE.colorUnlockFilter;
    private SpecialFilterMode fadeColorFilter = SESSION_STATE.fadeColorFilter;
    private String initialBadgeIconId;
    private String initialBadgeColorId;
    private boolean handledClose;
    private boolean scrollHandlerRegistered;
    private static final Map<String, Boolean> CATEGORY_EXPANDED = new LinkedHashMap<>();

    public AchievementScreen() {
        super(Text.literal("WynnExtras Achievements"));
    }

    @Override
    protected double getTargetScaleFactor() {
        return 2.5;
    }

    @Override
    protected int getMinLogicalWidth() {
        return 1300;
    }

    @Override
    protected int getMinLogicalHeight() {
        return 850;
    }

    @Override
    protected void init() {
        super.init();
        captureInitialBadgeProfile();
        initializeRootWidgets();
        if (!scrollHandlerRegistered) {
            ScreenMouseEvents.afterMouseScroll(this).register((screen, mouseX, mouseY, horizontalAmount, verticalAmount, consumed) -> {
                double mx = mouseX / matrixScale;
                double my = mouseY / matrixScale;
                if (tab == Tab.BADGES && scrollBadgeRow(mx, my, horizontalAmount, verticalAmount)) {
                    return true;
                }
                if (tab == Tab.ACHIEVEMENTS) {
                    targetScroll -= (float) verticalAmount * 34f;
                    clampScroll();
                }
                return true;
            });
            scrollHandlerRegistered = true;
        }
    }

    @Override
    public void removed() {
        saveSessionState();
        uploadBadgeProfileIfChanged();
        super.removed();
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        hoveredBadgeTooltip = List.of();
        hideDynamicWidgets();

        scroll = smoothScroll(scroll, targetScroll, tickDelta);
        iconScroll = smoothScroll(iconScroll, targetIconScroll, tickDelta);
        colorScroll = smoothScroll(colorScroll, targetColorScroll, tickDelta);

        drawHeader(ctx, mouseX, mouseY);

        if (tab == Tab.ACHIEVEMENTS) drawAchievements(ctx, mouseX, mouseY);
        else drawBadges(ctx, mouseX, mouseY);
    }

    private void initializeRootWidgets() {
        if (achievementsTabButton == null) {
            achievementsTabButton = new TabButtonWidget(Tab.ACHIEVEMENTS, 25, 58, 180, 34, "Achievements");
            badgesTabButton = new TabButtonWidget(Tab.BADGES, 215, 58, 180, 34, "Badges");
            achievementScrollBarWidget = new AchievementScrollBarWidget();
            iconScrollBarWidget = new HorizontalBadgeScrollBarWidget(HorizontalScrollTarget.ICONS);
            colorScrollBarWidget = new HorizontalBadgeScrollBarWidget(HorizontalScrollTarget.COLORS);

            for (AchievementCategory category : achievementCategories()) {
                categoryHeaderWidgets.put(category.key(), new CategoryHeaderWidget(category));
            }
            for (BadgeCatalog.BadgeIcon icon : BadgeCatalog.icons()) {
                badgeIconWidgets.put(icon.id(), new BadgeIconWidget(icon));
            }
            for (BadgeCatalog.BadgeColor color : BadgeCatalog.colors()) {
                badgeColorWidgets.put(color.id(), new BadgeColorWidget(color));
            }
            for (BadgeFilter filter : BadgeFilter.values()) {
                badgeFilterWidgets.put(filter, new BadgeFilterButtonWidget(filter));
            }
        }

        registerRootWidget(achievementsTabButton);
        registerRootWidget(badgesTabButton);
        for (CategoryHeaderWidget widget : categoryHeaderWidgets.values()) registerRootWidget(widget);
        registerRootWidget(achievementScrollBarWidget);
        registerRootWidget(iconSearchBar());
        registerRootWidget(colorSearchBar());
        for (BadgeFilterButtonWidget widget : badgeFilterWidgets.values()) registerRootWidget(widget);
        for (BadgeIconWidget widget : badgeIconWidgets.values()) registerRootWidget(widget);
        registerRootWidget(iconScrollBarWidget);
        for (BadgeColorWidget widget : badgeColorWidgets.values()) registerRootWidget(widget);
        registerRootWidget(colorScrollBarWidget);
    }

    private void registerRootWidget(Widget widget) {
        if (!rootWidgets.contains(widget)) addRootWidget(widget);
    }

    private void hideDynamicWidgets() {
        for (CategoryHeaderWidget widget : categoryHeaderWidgets.values()) widget.setVisible(false);
        achievementScrollBarWidget.setVisible(false);
        iconSearchBar.setVisible(false);
        colorSearchBar.setVisible(false);
        for (BadgeFilterButtonWidget widget : badgeFilterWidgets.values()) widget.setVisible(false);
        for (BadgeIconWidget widget : badgeIconWidgets.values()) widget.setVisible(false);
        iconScrollBarWidget.setVisible(false);
        for (BadgeColorWidget widget : badgeColorWidgets.values()) widget.setVisible(false);
        colorScrollBarWidget.setVisible(false);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if (tab == Tab.BADGES && !hoveredBadgeTooltip.isEmpty()) {
            TextInputWidget.drawFittingTooltip(ctx, hoveredBadgeTooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (achievementScrollbarDragging && tab == Tab.ACHIEVEMENTS) {
            setScrollFromScrollbar(click.y() / matrixScale - achievementScrollbarDragOffset);
            return true;
        }
        if (horizontalScrollDragging != null && tab == Tab.BADGES) {
            setHorizontalScrollFromThumb(horizontalScrollDragging, click.x() / matrixScale - horizontalScrollDragOffset);
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        achievementScrollbarDragging = false;
        horizontalScrollDragging = null;
        return super.mouseReleased(click);
    }

    private void drawHeader(DrawContext ctx, int mouseX, int mouseY) {
        int w = layoutWidth();
        drawRect(20, 20, w - 40, 88, BG_DARK);
        drawRect(20, 105, w - 40, 3, GOLD);
        drawText("WynnExtras Achievements (Experimental)", 25, 25, TEXT_MAIN);
        drawText("Track progress and choose the badge shown after your name.", 25, 40, TEXT_DIM);
        if (tab == Tab.BADGES) {
            drawBadgeHeaderPreview(ctx);
        }
    }

    private void drawTab(DrawContext ctx, int x, int y, String label, boolean selected, boolean hover) {
        drawRect(x, y, 180, 34, selected ? PARCHMENT : (hover ? PARCHMENT_HOVER : PANEL));
        drawRect(x, y, 180, 2, selected ? GOLD : BORDER_LIGHT);
        drawRect(x, y + 31, 180, 3, selected ? GOLD : BORDER);
        drawCenteredText(label, x + 90, y + 12, selected ? GOLD : TEXT_MAIN);
    }

    private void drawAchievements(DrawContext ctx, int mouseX, int mouseY) {
        List<AchievementCategory> categories = achievementCategories();
        int x = 24;
        int y = ACHIEVEMENT_VIEWPORT_TOP - Math.round(scroll);
        int width = layoutWidth() - 48;
        int rowH = 68;
        int headerH = 25;
        int viewportBottom = achievementViewportBottom();

        enableLogicalScissor(ctx, x, ACHIEVEMENT_VIEWPORT_TOP, width, achievementViewportHeight());
        for (AchievementCategory category : categories) {
            if (intersectsAchievementViewport(y, headerH)) {
                CategoryHeaderWidget widget = categoryHeaderWidgets.get(category.key());
                if (widget != null) {
                    widget.update(category, x, y, width, headerH);
                    widget.setVisible(true);
                }
            }
            y += headerH + 5;
            if (!category.expanded()) continue;

            for (Achievement achievement : category.achievements()) {
                if (y + rowH > ACHIEVEMENT_VIEWPORT_TOP && y < viewportBottom) {
                    drawAchievementRow(ctx, mouseX, mouseY, x + 8, y, width - 8, rowH, achievement);
                }
                y += rowH + 6;
            }
        }
        ctx.disableScissor();

        updateMaxScroll(y + Math.round(scroll) - ACHIEVEMENT_VIEWPORT_TOP);
        if (maxAchievementScroll > 0) {
            achievementScrollBarWidget.updateBounds();
            achievementScrollBarWidget.setVisible(true);
        }
    }

    private void drawBadges(DrawContext ctx, int mouseX, int mouseY) {
        int x = 24;
        int y = badgeIconLabelY();
        int w = layoutWidth() - 48;

        int searchX = x + textRenderer.getWidth("Icons") + 16;
        drawRect(x, y - 6, searchX - x - 7, BADGE_SEARCH_HEIGHT, PARCHMENT);
        drawText("Icons", x + 4, y + 2, TEXT_MAIN);
        drawBadgeSearchBar(iconSearchBar(), searchX, y - 6);
        drawBadgeFilterButtons(searchX + BADGE_SEARCH_WIDTH + BADGE_FILTER_GAP, y - 6,
                BadgeFilter.ICON_UNLOCK, BadgeFilter.ICON_IMAGE);
        drawIconRow(ctx, mouseX, mouseY, x, badgeIconRowY(), w);

        y = badgeColorLabelY();
        searchX = x + textRenderer.getWidth("Colors") + 16;
        drawRect(x, y - 6, searchX - x - 7, BADGE_SEARCH_HEIGHT, PARCHMENT);
        drawText("Colors", x + 4, y + 2, TEXT_MAIN);
        drawBadgeSearchBar(colorSearchBar(), searchX, y - 6);
        drawBadgeFilterButtons(searchX + BADGE_SEARCH_WIDTH + BADGE_FILTER_GAP, y - 6,
                BadgeFilter.COLOR_UNLOCK, BadgeFilter.COLOR_FADE);
        drawColorRow(ctx, mouseX, mouseY, x, badgeColorRowY(), w);
    }

    private void drawIconRow(DrawContext ctx, int mouseX, int mouseY, int x, int y, int width) {
        List<BadgeCatalog.BadgeIcon> icons = filteredBadgeIcons();
        int totalW = badgeRowWidth(icons.size(), BADGE_ICON_CELL);
        targetIconScroll = MathHelper.clamp(targetIconScroll, 0, Math.max(0, totalW - width));
        iconScroll = MathHelper.clamp(iconScroll, 0, Math.max(0, totalW - width));

        int i = 0;
        for (BadgeCatalog.BadgeIcon icon : icons) {
            int cx = x + i * (BADGE_ICON_CELL + BADGE_CELL_GAP) - Math.round(iconScroll);
            int cy = y;
            if (cx + BADGE_ICON_CELL < x || cx > x + width) {
                i++;
                continue;
            }
            if (inside(mouseX, mouseY, cx, cy, BADGE_ICON_CELL, BADGE_ICON_CELL) && inside(mouseX, mouseY, x, y, width, BADGE_ICON_CELL)) {
                hoveredBadgeTooltip = badgeIconTooltip(icon);
            }
            BadgeIconWidget widget = badgeIconWidgets.get(icon.id());
            if (widget != null) {
                widget.updateBounds(cx, cy, x, y, width, BADGE_ICON_CELL);
                widget.setVisible(true);
            }
            i++;
        }
        if (icons.isEmpty()) drawCenteredText("No matching icons", x + width / 2f, y + 38, TEXT_DIM);
        iconScrollBarWidget.update(x, y + BADGE_ICON_ROW_H, width, 5, totalW);
        iconScrollBarWidget.setVisible(totalW > width);
    }

    private void drawColorRow(DrawContext ctx, int mouseX, int mouseY, int x, int y, int width) {
        List<BadgeCatalog.BadgeColor> colors = filteredBadgeColors();
        int totalW = badgeRowWidth(colors.size(), BADGE_COLOR_CELL_W);
        targetColorScroll = MathHelper.clamp(targetColorScroll, 0, Math.max(0, totalW - width));
        colorScroll = MathHelper.clamp(colorScroll, 0, Math.max(0, totalW - width));

        int i = 0;
        for (BadgeCatalog.BadgeColor color : colors) {
            int cx = x + i * (BADGE_COLOR_CELL_W + BADGE_CELL_GAP) - Math.round(colorScroll);
            int cy = y;
            if (cx + BADGE_COLOR_CELL_W < x || cx > x + width) {
                i++;
                continue;
            }
            if (inside(mouseX, mouseY, cx, cy, BADGE_COLOR_CELL_W, BADGE_COLOR_CELL_H) && inside(mouseX, mouseY, x, y, width, BADGE_COLOR_CELL_H)) {
                hoveredBadgeTooltip = badgeColorTooltip(color);
            }
            BadgeColorWidget widget = badgeColorWidgets.get(color.id());
            if (widget != null) {
                widget.updateBounds(cx, cy, x, y, width, BADGE_COLOR_CELL_H);
                widget.setVisible(true);
            }
            i++;
        }
        if (colors.isEmpty()) drawCenteredText("No matching colors", x + width / 2f, y + 38, TEXT_DIM);
        colorScrollBarWidget.update(x, y + BADGE_COLOR_ROW_H, width, 5, totalW);
        colorScrollBarWidget.setVisible(totalW > width);
    }

    private TextInputWidget iconSearchBar() {
        if (iconSearchBar == null) {
            iconSearchBar = createBadgeSearchBar("Search icons...", HorizontalScrollTarget.ICONS);
            iconSearchBar.setInput(SESSION_STATE.iconSearch);
        }
        return iconSearchBar;
    }

    private TextInputWidget colorSearchBar() {
        if (colorSearchBar == null) {
            colorSearchBar = createBadgeSearchBar("Search colors...", HorizontalScrollTarget.COLORS);
            colorSearchBar.setInput(SESSION_STATE.colorSearch);
        }
        return colorSearchBar;
    }

    private TextInputWidget createBadgeSearchBar(String placeholder, HorizontalScrollTarget target) {
        TextInputWidget searchBar = new TextInputWidget(0, 0, 0, 0, 0, 0);
        searchBar.setPlaceholder(placeholder);
        searchBar.setBackgroundColor(CustomColor.fromInt(PANEL));
        searchBar.setFocusedColor(CustomColor.fromInt(PANEL_HOVER));
        searchBar.setTextColor(CustomColor.fromInt(TEXT_MAIN));
        searchBar.setPlaceholderColor(CustomColor.fromInt(TEXT_DIM));
        searchBar.setCursorColor(CustomColor.fromInt(TEXT_MAIN));
        searchBar.setSelectionColor(CustomColor.fromInt(0xAA876141));
        searchBar.setMaxLength(64);
        searchBar.setOnChange(value -> resetBadgeRowScroll(target));
        return searchBar;
    }

    private void drawBadgeSearchBar(TextInputWidget searchBar, int x, int y) {
        searchBar.setBounds(
                Math.round(uiX(x)),
                Math.round(uiY(y)),
                Math.round(uiX(BADGE_SEARCH_WIDTH)),
                Math.round(uiY(BADGE_SEARCH_HEIGHT)));
        searchBar.setTextOffset(Math.round(uiX(7)), Math.round(uiY(7)));
        searchBar.setTextScale(uiTextScale());
        searchBar.setVisible(true);
    }

    private void drawBadgeFilterButtons(int x, int y, BadgeFilter... filters) {
        for (BadgeFilter filter : filters) {
            BadgeFilterButtonWidget widget = badgeFilterWidgets.get(filter);
            widget.setBounds(x, y, BADGE_FILTER_WIDTH, BADGE_SEARCH_HEIGHT);
            widget.setVisible(true);
            x += BADGE_FILTER_WIDTH + BADGE_FILTER_GAP;
        }
    }

    private void resetBadgeRowScroll(HorizontalScrollTarget target) {
        setTargetHorizontalScroll(target, 0);
        if (target == HorizontalScrollTarget.ICONS) iconScroll = 0;
        else colorScroll = 0;
        if (horizontalScrollDragging == target) horizontalScrollDragging = null;
    }

    private List<BadgeCatalog.BadgeIcon> filteredBadgeIcons() {
        String query = badgeSearchQuery(iconSearchBar);
        return BadgeCatalog.icons().stream()
                .filter(icon -> {
                    boolean unlocked = BadgeCatalog.isUnlocked(icon);
                    if (!matchesUnlockFilter(unlocked, iconUnlockFilter)) return false;
                    if (!matchesSpecialFilter(icon.isCustom(), imageIconFilter)) return false;
                    return query.isEmpty() || matchesBadgeSearch(query, icon.id(), icon.displayName());
                })
                .toList();
    }

    private List<BadgeCatalog.BadgeColor> filteredBadgeColors() {
        String query = badgeSearchQuery(colorSearchBar);
        BadgeCatalog.BadgeIcon selectedIcon = BadgeCatalog.icon(BadgeProfileData.getLocalProfile().selectedIconId);
        return BadgeCatalog.colors().stream()
                .filter(color -> {
                    boolean unlocked = BadgeCatalog.isUnlocked(color) && BadgeCatalog.isCompatible(selectedIcon, color);
                    if (!matchesUnlockFilter(unlocked, colorUnlockFilter)) return false;
                    if (!matchesSpecialFilter(BadgeCatalog.isFade(color), fadeColorFilter)) return false;
                    return query.isEmpty() || matchesBadgeSearch(query, color.id(), color.displayName());
                })
                .toList();
    }

    private String badgeSearchQuery(TextInputWidget searchBar) {
        return searchBar == null ? "" : searchBar.getInput().trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesBadgeSearch(String query, String id, String displayName) {
        return id.toLowerCase(Locale.ROOT).contains(query)
                || displayName.toLowerCase(Locale.ROOT).contains(query);
    }

    private int badgeRowWidth(int entryCount, int cellWidth) {
        if (entryCount == 0) return 0;
        return entryCount * (cellWidth + BADGE_CELL_GAP) - BADGE_CELL_GAP;
    }

    private boolean matchesSpecialFilter(boolean special, SpecialFilterMode mode) {
        return switch (mode) {
            case ALL -> true;
            case HIDE -> !special;
            case ONLY -> special;
        };
    }

    private boolean matchesUnlockFilter(boolean unlocked, UnlockFilterMode mode) {
        return switch (mode) {
            case ALL -> true;
            case ONLY_UNLOCKED -> unlocked;
            case ONLY_LOCKED -> !unlocked;
        };
    }

    private String badgeFilterLabel(BadgeFilter filter) {
        return switch (filter) {
            case ICON_UNLOCK -> iconUnlockFilter.label;
            case ICON_IMAGE -> specialFilterLabel(filter.label, imageIconFilter);
            case COLOR_UNLOCK -> colorUnlockFilter.label;
            case COLOR_FADE -> specialFilterLabel(filter.label, fadeColorFilter);
        };
    }

    private String specialFilterLabel(String category, SpecialFilterMode mode) {
        return switch (mode) {
            case ALL -> "All";
            case HIDE -> "Hide " + category;
            case ONLY -> "Only " + category;
        };
    }

    private int badgeFilterAccent(BadgeFilter filter) {
        return switch (filter) {
            case ICON_UNLOCK -> iconUnlockFilter.accent;
            case ICON_IMAGE -> imageIconFilter.accent;
            case COLOR_UNLOCK -> colorUnlockFilter.accent;
            case COLOR_FADE -> fadeColorFilter.accent;
        };
    }

    private void toggleBadgeFilter(BadgeFilter filter, boolean backwards) {
        switch (filter) {
            case ICON_UNLOCK -> iconUnlockFilter = backwards ? iconUnlockFilter.previous() : iconUnlockFilter.next();
            case ICON_IMAGE -> imageIconFilter = backwards ? imageIconFilter.previous() : imageIconFilter.next();
            case COLOR_UNLOCK -> colorUnlockFilter = backwards ? colorUnlockFilter.previous() : colorUnlockFilter.next();
            case COLOR_FADE -> fadeColorFilter = backwards ? fadeColorFilter.previous() : fadeColorFilter.next();
        }
        resetBadgeRowScroll(filter.target);
    }

    private void saveSessionState() {
        SESSION_STATE.achievementScroll = scroll;
        SESSION_STATE.targetAchievementScroll = targetScroll;
        SESSION_STATE.iconScroll = iconScroll;
        SESSION_STATE.targetIconScroll = targetIconScroll;
        SESSION_STATE.colorScroll = colorScroll;
        SESSION_STATE.targetColorScroll = targetColorScroll;
        if (iconSearchBar != null) SESSION_STATE.iconSearch = iconSearchBar.getInput();
        if (colorSearchBar != null) SESSION_STATE.colorSearch = colorSearchBar.getInput();
        SESSION_STATE.iconUnlockFilter = iconUnlockFilter;
        SESSION_STATE.imageIconFilter = imageIconFilter;
        SESSION_STATE.colorUnlockFilter = colorUnlockFilter;
        SESSION_STATE.fadeColorFilter = fadeColorFilter;
    }

    private List<Achievement> allAchievements() {
        List<Achievement> list = new ArrayList<>();
        ensureAchievementsAvailable();
        list.addAll(AchievementTracking.achievements.allSimple());
        list.addAll(AchievementTracking.achievements.allProgress());
        list.addAll(AchievementTracking.achievements.allTiered());
        list.removeIf(achievement -> !Achievements.isKnownAchievement(achievement.getId()));
        list.sort(Comparator.comparing(Achievement::getId));
        return list;
    }

    private void ensureAchievementsAvailable() {
        if (AchievementTracking.achievements != null) return;
        AchievementTracking.achievements = new Achievements();
        AchievementTracking.achievements.populateAll();
    }

    private List<AchievementCategory> achievementCategories() {
        Map<String, List<Achievement>> grouped = new LinkedHashMap<>();
        grouped.put("General", new ArrayList<>());
        grouped.put("Raids", new ArrayList<>());
        grouped.put("Warring", new ArrayList<>());
        grouped.put("Aspects", new ArrayList<>());
        grouped.put("Gathering", new ArrayList<>());
        grouped.put("Crafting", new ArrayList<>());
        grouped.put("Testing", new ArrayList<>());

        for (Achievement achievement : allAchievements()) {
            grouped.computeIfAbsent(categoryName(achievement), ignored -> new ArrayList<>()).add(achievement);
        }

        List<AchievementCategory> categories = new ArrayList<>();
        for (Map.Entry<String, List<Achievement>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            String key = entry.getKey();
            if ("Aspects".equals(key)) {
                entry.getValue().sort(Comparator.comparingInt(this::aspectSortIndex).thenComparing(Achievement::getId));
            } else if ("Warring".equals(key)) {
                entry.getValue().sort(Comparator.comparingInt(this::warSortIndex).thenComparing(Achievement::getId));
            } else {
                entry.getValue().sort(Comparator.comparing(Achievement::getId));
            }
            CATEGORY_EXPANDED.putIfAbsent(key, true);
            categories.add(new AchievementCategory(key, entry.getValue()));
        }
        return categories;
    }

    private int aspectSortIndex(Achievement achievement) {
        int index = ASPECT_ACHIEVEMENT_ORDER.indexOf(achievement.getId());
        return index == -1 ? Integer.MAX_VALUE : index;
    }

    private int warSortIndex(Achievement achievement) {
        int index = WAR_ACHIEVEMENT_ORDER.indexOf(achievement.getId());
        return index == -1 ? Integer.MAX_VALUE : index;
    }

    private String categoryName(Achievement achievement) {
        String id = achievement.getId();
        if (id == null) return "General";
        if (id.startsWith("raid.")) return "Raids";
        if (id.startsWith("war.")) return "Warring";
        if (id.startsWith("aspect.")) return "Aspects";
        if (id.startsWith("prof.gather.")) return "Gathering";
        if (id.startsWith("prof.craft.")) return "Crafting";
        return "General";
    }

    private void drawCategoryHeader(DrawContext ctx, int x, int y, int w, int h, AchievementCategory category, boolean hover) {
        drawRect(x, y, w, h, hover ? PARCHMENT_HOVER : PARCHMENT);
        drawRect(x, y, 4, h, GOLD);
        drawRect(x, y + h - 1, w, 1, BORDER);
        drawText(category.expanded() ? "v" : ">", x + 12, y + 8, GOLD);
        drawText(category.key(), x + 30, y + 8, TEXT_MAIN);
        String count = completedCount(category.achievements()) + "/" + category.achievements().size();
        drawText(count, x + w - textRenderer.getWidth(count) - 12, y + 8, TEXT_DIM);
    }

    private int completedCount(List<Achievement> achievements) {
        int count = 0;
        for (Achievement achievement : achievements) {
            if (achievement.isUnlocked()) count++;
        }
        return count;
    }

    private void drawAchievementRow(DrawContext ctx, int mouseX, int mouseY, int x, int y, int width, int rowH, Achievement achievement) {
        boolean unlocked = achievement.isUnlocked();
        boolean hover = inside(mouseX, mouseY, x, y, width, rowH) && insideAchievementViewport(mouseX, mouseY);
        int accent = unlocked ? GREEN : BORDER_LIGHT;
        drawRect(x, y, width, rowH, hover ? PANEL_HOVER : PANEL);
        drawRect(x, y + rowH - 2, width, 2, BORDER);
        drawRect(x, y, 4, rowH, unlocked ? GREEN : RED);

        List<RewardPreview> rewards = rewardsFor(achievement.getId());
        int rewardW = rewards.isEmpty() ? 0 : rewards.size() * (REWARD_PREVIEW_W + REWARD_PREVIEW_GAP);
        int progressW = 136;
        int progressX = x + width - progressW - rewardW - 8;
        int progressY = y + 25;
        int textX = x + 14;
        int textY = y + rowH / 2 - 12;
        int textW = Math.max(60, progressX - textX - 18);
        drawText(trimText(achievement.getTitle(), textW), textX, textY, unlocked ? GREEN : TEXT_MAIN);
        drawText(trimText(achievement.getDescription(), textW), textX, textY + 14, TEXT_DIM);

        String progress = progressText(achievement);
        progress = trimText(progress, progressW);
        if(progress.equals("Locked")) progressY += 13;
        drawText(progress, progressX + progressW - textRenderer.getWidth(progress), progressY, unlocked ? GREEN : GOLD);
        if (achievement instanceof TieredAchievement tiered) {
            String tiers = trimText(tierMilestones(tiered), progressW);
            drawText(tiers, progressX + progressW - textRenderer.getWidth(tiers), progressY + 14, TEXT_DIM);
        }
        drawProgressBar(ctx, progressX, y + rowH - 14, progressW, 3, achievement.getProgress(), accent);

        if (!rewards.isEmpty()) {
            int rx = x + width - rewardW;
            drawText("Reward", rx, y + 5, TEXT_DIM);
            for (RewardPreview reward : rewards) {
                drawRewardPreview(ctx, rx, y + rowH / 2 - REWARD_PREVIEW_H / 2 + 6, reward,
                        isRewardUnlocked(achievement, reward));
                rx += REWARD_PREVIEW_W + REWARD_PREVIEW_GAP;
            }
        }
    }

    private void drawProgressBar(DrawContext ctx, int x, int y, int w, int h, float progress, int accent) {
        drawRect(x, y, w, h, BORDER);
        int fill = Math.round(w * MathHelper.clamp(progress, 0f, 1f));
        if (fill > 0) drawRect(x, y, fill, h, accent);
    }

    private List<RewardPreview> rewardsFor(String achievementId) {
        List<RewardPreview> rewards = new ArrayList<>();
        for (BadgeCatalog.BadgeIcon icon : BadgeCatalog.icons()) {
            if (icon.achievement() != null && achievementId != null && achievementId.equals(icon.achievement().id())) {
                rewards.add(new RewardPreview(RewardType.ICON, icon.id(), "white", icon.minTier()));
            }
        }
        for (BadgeCatalog.BadgeColor color : BadgeCatalog.colors()) {
            if (color.achievement() != null && achievementId != null && achievementId.equals(color.achievement().id())) {
                rewards.add(new RewardPreview(RewardType.COLOR, BadgeCatalog.DEFAULT_ICON_ID, color.id(), color.minTier()));
            }
        }
        rewards.sort(Comparator.comparingInt(reward -> reward.minTier() == null ? Integer.MAX_VALUE : reward.minTier()));
        return rewards;
    }

    private boolean isRewardUnlocked(Achievement achievement, RewardPreview reward) {
        if (reward.minTier() == null) return achievement.isUnlocked();
        return achievement instanceof TieredAchievement tiered
                && tiered.getCurrentLevel() >= reward.minTier();
    }

    private void drawRewardPreview(DrawContext ctx, int x, int y, RewardPreview reward, boolean unlocked) {
        drawRect(x, y, REWARD_PREVIEW_W, REWARD_PREVIEW_H, BG_DARK);
        drawRect(x, y, REWARD_PREVIEW_W, 2, unlocked ? GREEN : RED);
        drawRect(x, y + REWARD_PREVIEW_H - 2, REWARD_PREVIEW_W, 2, BORDER);
        Text preview = reward.type() == RewardType.COLOR
                ? BadgeCatalog.colorPreviewText(reward.colorId())
                : BadgeCatalog.badgeText(reward.iconId(), reward.colorId());
        if (reward.type() == RewardType.COLOR) {
            drawScaledBadgeText(ctx, preview, x + REWARD_PREVIEW_W / 2, y + 7, 2.45f, true);
        } else {
            drawScaledBadgeText(ctx, preview, x + REWARD_PREVIEW_W / 2, y + 8, 2.0f, true);
        }
        if (reward.minTier() != null) {
            String tier = "T" + reward.minTier();
            drawText(tier, x + REWARD_PREVIEW_W - textRenderer.getWidth(tier) - 1, y + 3, GOLD);
        }
    }

    private void drawBadgeHeaderPreview(DrawContext ctx) {
        BadgeProfile profile = BadgeProfileData.getLocalProfile();
        int w = layoutWidth();
        int x = Math.max(405, w - 284);
        int y = 58;
        int boxW = Math.max(120, w - x - 24);
        Text badge = BadgeCatalog.badgeText(profile.selectedIconId, profile.selectedColorId);
        int previewW = Math.max(12, boxW - 82);
        String playerName = "PlayerName";
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            playerName = client.player.getGameProfile().name();
        }
        int nameW = Math.max(0, previewW - textRenderer.getWidth(" ") - textRenderer.getWidth(badge));
        Text preview = Text.literal(trimText(playerName, nameW))
                .append(Text.literal(" "))
                .append(badge);
        drawRect(x, y, boxW, 34, PANEL);
        drawRect(x, y, boxW, 2, BORDER_LIGHT);
        drawRect(x, y + 32, boxW, 2, GOLD);
        drawText("Preview", x + 10, y + 12, TEXT_DIM);
        drawText(preview, x + 72, y + 12, TEXT_MAIN);
    }

    private boolean scrollBadgeRow(double mx, double my, double horizontalAmount, double verticalAmount) {
        int x = 24;
        int w = layoutWidth() - 48;
        double amount = horizontalAmount != 0 ? horizontalAmount : verticalAmount;
        if (amount == 0) return false;

        int iconY = badgeIconRowY();
        if (inside(mx, my, x, iconY, w, BADGE_ICON_ROW_H + 2)) {
            int totalW = badgeRowWidth(filteredBadgeIcons().size(), BADGE_ICON_CELL);
            targetIconScroll = MathHelper.clamp(targetIconScroll - (float) amount * 34f, 0, Math.max(0, totalW - w));
            return true;
        }

        int colorY = badgeColorRowY();
        if (inside(mx, my, x, colorY, w, BADGE_COLOR_ROW_H + 2)) {
            int totalW = badgeRowWidth(filteredBadgeColors().size(), BADGE_COLOR_CELL_W);
            targetColorScroll = MathHelper.clamp(targetColorScroll - (float) amount * 34f, 0, Math.max(0, totalW - w));
            return true;
        }
        return false;
    }

    private int badgeIconLabelY() {
        return 124;
    }

    private int badgeIconRowY() {
        return badgeIconLabelY() + 24;
    }

    private int badgeColorRowY() {
        return badgeIconRowY() + BADGE_ICON_ROW_H + BADGE_SECTION_GAP;
    }

    private int badgeColorLabelY() {
        return badgeColorRowY() - 24;
    }

    private float smoothScroll(float current, float target, float tickDelta) {
        float diff = target - current;
        if (Math.abs(diff) < SCROLL_SNAP || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) return target;
        return current + diff * SCROLL_SPEED * tickDelta;
    }

    private float getHorizontalScroll(HorizontalScrollTarget target) {
        return target == HorizontalScrollTarget.ICONS ? iconScroll : colorScroll;
    }

    private float getTargetHorizontalScroll(HorizontalScrollTarget target) {
        return target == HorizontalScrollTarget.ICONS ? targetIconScroll : targetColorScroll;
    }

    private void setTargetHorizontalScroll(HorizontalScrollTarget target, float value) {
        if (target == HorizontalScrollTarget.ICONS) {
            targetIconScroll = value;
        } else {
            targetColorScroll = value;
        }
    }

    private int horizontalScrollContentWidth(HorizontalScrollTarget target) {
        if (target == HorizontalScrollTarget.ICONS) {
            return badgeRowWidth(filteredBadgeIcons().size(), BADGE_ICON_CELL);
        }
        return badgeRowWidth(filteredBadgeColors().size(), BADGE_COLOR_CELL_W);
    }

    private void setHorizontalScrollFromThumb(HorizontalScrollTarget target, double thumbLeft) {
        int x = 24;
        int width = layoutWidth() - 48;
        int contentWidth = horizontalScrollContentWidth(target);
        int max = Math.max(0, contentWidth - width);
        if (max <= 0) {
            setTargetHorizontalScroll(target, 0);
            return;
        }
        int thumbW = horizontalScrollbarThumbWidth(width, contentWidth);
        double percent = (thumbLeft - x) / Math.max(1, width - thumbW);
        setTargetHorizontalScroll(target, MathHelper.clamp((float) (percent * max), 0, max));
    }

    private int horizontalScrollbarThumbWidth(int width, int contentWidth) {
        return MathHelper.clamp(Math.round(width * (width / (float) contentWidth)), 40, width);
    }

    private String progressText(Achievement achievement) {
        if (achievement instanceof TieredAchievement tiered) {
            Integer target = AchievementTracking.achievements == null ? null : AchievementTracking.achievements.getCurrentTierTarget(achievement.getId());
            List<Integer> targets = tiered.getLevelTargets();
            int maxTarget = targets.isEmpty() ? 0 : targets.getLast();
            if (achievement.isUnlocked() || target == null) {
                return tiered.getCurrent() + "/" + maxTarget + " Tier " + tiered.getCurrentLevel() + " (MAX)";
            }
            int maxTier = Math.max(1, targets.size());
            return tiered.getCurrent() + "/" + target + " Tier " + tiered.getCurrentLevel() + "/" + maxTier;
        }
        if (achievement.isUnlocked()) return "Unlocked";
        if (achievement instanceof ProgressAchievement progress) {
            return progress.getCurrent() + "/" + progress.getTarget();
        }
        return "Locked";
    }

    private String tierMilestones(TieredAchievement achievement) {
        List<Integer> targets = achievement.getLevelTargets();
        if (targets.isEmpty()) return "";

        StringBuilder tiers = new StringBuilder();
        for (int i = 0; i < targets.size(); i++) {
            if (i > 0) tiers.append("/");
            tiers.append(targets.get(i));
        }
        return tiers.toString();
    }

    private void updateMaxScroll(int contentHeight) {
        int viewport = achievementViewportHeight();
        float max = Math.max(0, contentHeight - viewport);
        maxAchievementScroll = max;
        targetScroll = MathHelper.clamp(targetScroll, 0, max);
        scroll = MathHelper.clamp(scroll, 0, max);
    }

    private void drawPanel(DrawContext ctx, int x, int y, int w, int h, boolean hover, int accent) {
        drawRect(x, y, w, h, hover ? PANEL_HOVER : PANEL);
        drawRect(x, y, w, 2, accent);
    }

    private void clampScroll() {
        targetScroll = Math.max(0, targetScroll);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private int layoutWidth() {
        return Math.max(1, (int) Math.round(getLogicalWidth() / scaleFactor));
    }

    private int layoutHeight() {
        return Math.max(1, (int) Math.round(getLogicalHeight() / scaleFactor));
    }

    private void enableLogicalScissor(DrawContext ctx, int x, int y, int w, int h) {
        ctx.enableScissor(x, y, x + w, y + h);
    }

    private int achievementViewportBottom() {
        return layoutHeight() - ACHIEVEMENT_VIEWPORT_BOTTOM_PADDING;
    }

    private int achievementViewportHeight() {
        return Math.max(1, achievementViewportBottom() - ACHIEVEMENT_VIEWPORT_TOP);
    }

    private boolean insideAchievementViewport(double mx, double my) {
        return my >= ACHIEVEMENT_VIEWPORT_TOP && my < achievementViewportBottom();
    }

    private boolean intersectsAchievementViewport(int y, int height) {
        return y + height > ACHIEVEMENT_VIEWPORT_TOP && y < achievementViewportBottom();
    }

    private void setScrollFromScrollbar(double thumbTop) {
        int barY = ACHIEVEMENT_VIEWPORT_TOP;
        int barH = achievementViewportHeight();
        int thumbH = scrollbarThumbHeight(barH);
        double percent = (thumbTop - barY) / Math.max(1, barH - thumbH);
        targetScroll = MathHelper.clamp((float) (percent * maxAchievementScroll), 0, maxAchievementScroll);
    }

    private int scrollbarThumbHeight(int barH) {
        int viewport = achievementViewportHeight();
        return MathHelper.clamp(Math.round(barH * (viewport / (viewport + maxAchievementScroll))), 24, barH);
    }

    private int scrollbarThumbY(int barY, int barH, int thumbH) {
        return barY + Math.round((barH - thumbH) * (scroll / maxAchievementScroll));
    }

    private void drawScaledBadgeText(DrawContext ctx, Text text, int x, int y, float scale, boolean centered) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);
        int drawX = centered ? -textRenderer.getWidth(text) / 2 : 0;
        drawText(text, drawX, 0, 0xFFFFFFFF);
        ctx.getMatrices().popMatrix();
    }

    private List<Text> badgeRequirementTooltip(AchievementId achievementId, Integer minTier) {
        if (achievementId == null) {
            return List.of(Text.of("§aUnlocked by default"));
        }

        Achievement achievement = AchievementTracking.achievements == null ? null : AchievementTracking.achievements.getById(achievementId.id());
        if (achievement == null) {
            return List.of(Text.of(BadgeCatalog.requirement(achievementId, minTier)));
        }

        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.of("§6" + achievement.getTitle()));
        tooltip.add(Text.of("§7" + achievement.getDescription()));
        tooltip.add(Text.of(""));
        tooltip.add(Text.of(minTier == null ? "§eRequired: Complete achievement" : "§eRequired Tier: " + minTier));
        tooltip.add(Text.of("§eProgress: " + progressText(achievement)));
        if (achievement instanceof TieredAchievement tiered) {
            tooltip.add(Text.of("§8Tiers: " + tierMilestones(tiered)));
        }
        return tooltip;
    }

    private List<Text> badgeIconTooltip(BadgeCatalog.BadgeIcon icon) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.of("§6" + icon.displayName() + (icon.isCustom() ? " (Image Icon)" : "")));
        tooltip.addAll(badgeRequirementTooltip(icon.achievement(), icon.minTier()));
        return tooltip;
    }

    private List<Text> badgeColorTooltip(BadgeCatalog.BadgeColor color) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.of("§6" + color.displayName()));
        if (color.preservesOriginal()) {
            tooltip.add(Text.of("§7Only available for image icons."));
        }
        tooltip.addAll(badgeRequirementTooltip(color.achievement(), color.minTier()));
        return tooltip;
    }

    private void drawRect(float x, float y, float width, float height, int color) {
        if (ui == null) return;
        ui.drawRect(uiX(x), uiY(y), uiX(width), uiY(height), CustomColor.fromInt(color));
    }

    private void drawText(String text, float x, float y, int color) {
        if (ui == null) return;
        ui.drawText(text, uiX(x), uiY(y), CustomColor.fromInt(color), uiTextScale());
    }

    private void drawText(Text text, float x, float y, int color) {
        if (ui == null) return;
        ui.drawText(text, uiX(x), uiY(y), CustomColor.fromInt(color), uiTextScale());
    }

    private void drawCenteredText(String text, float centerX, float y, int color) {
        drawText(text, centerX - textRenderer.getWidth(text) / 2f, y, color);
    }

    private float uiX(float x) {
        return (float) (x * scaleFactor);
    }

    private float uiY(float y) {
        return (float) (y * scaleFactor);
    }

    private float uiTextScale() {
        return (float) scaleFactor;
    }

    private void playClick() {
        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
    }

    private void captureInitialBadgeProfile() {
        if (initialBadgeIconId != null && initialBadgeColorId != null) return;
        BadgeProfile profile = BadgeProfileData.getLocalProfile();
        initialBadgeIconId = profile.selectedIconId;
        initialBadgeColorId = profile.selectedColorId;
    }

    private void uploadBadgeProfileIfChanged() {
        if (handledClose) return;
        handledClose = true;
        BadgeProfile profile = BadgeProfileData.getLocalProfile();
        if (!Objects.equals(initialBadgeIconId, profile.selectedIconId)
                || !Objects.equals(initialBadgeColorId, profile.selectedColorId)) {
            BadgeService.syncWithServerSoon();
        }
    }

    private String trimText(String text, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (textRenderer.getWidth(text) <= maxWidth) return text;
        if (maxWidth <= textRenderer.getWidth("..")) return textRenderer.trimToWidth(text, maxWidth);
        return textRenderer.trimToWidth(text, maxWidth - textRenderer.getWidth("..")) + "..";
    }

    private abstract class LogicalWidget extends Widget {
        private LogicalWidget(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        public boolean contains(int mx, int my) {
            return mx >= x && my >= y && mx < x + width && my < y + height;
        }
    }

    private class TabButtonWidget extends LogicalWidget {
        private final Tab buttonTab;
        private final String label;

        private TabButtonWidget(Tab buttonTab, int x, int y, int width, int height, String label) {
            super(x, y, width, height);
            this.buttonTab = buttonTab;
            this.label = label;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            drawTab(ctx, x, y, label, tab == buttonTab, hovered);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != 0) return false;
            tab = buttonTab;
            playClick();
            return true;
        }
    }

    private class BadgeFilterButtonWidget extends LogicalWidget {
        private final BadgeFilter filter;

        private BadgeFilterButtonWidget(BadgeFilter filter) {
            super(0, 0, 0, 0);
            this.filter = filter;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            int accent = badgeFilterAccent(filter);
            boolean active = accent != RED;
            drawRect(x, y, width, height, active ? (hovered ? PARCHMENT_HOVER : PARCHMENT) : (hovered ? PANEL_HOVER : PANEL));
            drawRect(x, y, width, 2, accent);
            drawCenteredText(badgeFilterLabel(filter), x + width / 2f, y + 8, active ? TEXT_MAIN : TEXT_DIM);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != 0 && button != 1) return false;
            toggleBadgeFilter(filter, button == 1);
            playClick();
            return true;
        }
    }

    private class CategoryHeaderWidget extends LogicalWidget {
        private AchievementCategory category;

        private CategoryHeaderWidget(AchievementCategory category) {
            super(0, 0, 0, 0);
            this.category = category;
        }

        private void update(AchievementCategory category, int x, int y, int width, int height) {
            this.category = category;
            setBounds(x, y, width, height);
        }

        @Override
        public boolean contains(int mx, int my) {
            return super.contains(mx, my) && insideAchievementViewport(mx, my);
        }

        @Override
        public void draw(DrawContext ctx, int mouseX, int mouseY, float tickDelta, julianh06.wynnextras.utils.UI.UIUtils ui) {
            enableLogicalScissor(ctx, 24, ACHIEVEMENT_VIEWPORT_TOP, layoutWidth() - 48, achievementViewportHeight());
            super.draw(ctx, mouseX, mouseY, tickDelta, ui);
            ctx.disableScissor();
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            drawCategoryHeader(ctx, x, y, width, height, category, hovered);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != 0) return false;
            CATEGORY_EXPANDED.put(category.key(), !category.expanded());
            playClick();
            return true;
        }
    }

    private abstract class BadgeCellWidget extends LogicalWidget {
        private int viewportX;
        private int viewportY;
        private int viewportWidth;
        private int viewportHeight;

        private BadgeCellWidget(int width, int height) {
            super(0, 0, width, height);
        }

        protected final void updateBounds(int x, int y, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
            setPosition(x, y);
            this.viewportX = viewportX;
            this.viewportY = viewportY;
            this.viewportWidth = viewportWidth;
            this.viewportHeight = viewportHeight;
        }

        @Override
        public boolean contains(int mx, int my) {
            return super.contains(mx, my) && inside(mx, my, viewportX, viewportY, viewportWidth, viewportHeight);
        }

        @Override
        public void draw(DrawContext ctx, int mouseX, int mouseY, float tickDelta, julianh06.wynnextras.utils.UI.UIUtils ui) {
            enableLogicalScissor(ctx, viewportX, viewportY, viewportWidth, viewportHeight);
            super.draw(ctx, mouseX, mouseY, tickDelta, ui);
            ctx.disableScissor();
        }
    }

    private class BadgeIconWidget extends BadgeCellWidget {
        private final BadgeCatalog.BadgeIcon icon;

        private BadgeIconWidget(BadgeCatalog.BadgeIcon icon) {
            super(BADGE_ICON_CELL, BADGE_ICON_CELL);
            this.icon = icon;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            BadgeProfile profile = BadgeProfileData.getLocalProfile();
            boolean unlocked = BadgeCatalog.isUnlocked(icon);
            boolean selected = icon.id().equals(profile.selectedIconId);
            drawPanel(ctx, x, y, width, height, hovered, selected ? GOLD : (unlocked ? BORDER_LIGHT : RED));
            drawScaledBadgeText(ctx, BadgeCatalog.badgeText(icon.id(), profile.selectedColorId), x + width / 2, y + 13, 1.8f, true);
            drawCenteredText(trimText(icon.displayName(), width - 8), x + width / 2, y + 52, unlocked ? TEXT_MAIN : TEXT_DIM);
            if (selected) drawCenteredText("Selected", x + width / 2, y + 68, GOLD);
            else if (!unlocked) drawCenteredText("Locked", x + width / 2, y + 68, RED);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != 0 || !BadgeCatalog.isUnlocked(icon)) return false;
            BadgeProfileData.setIcon(icon.id());
            playClick();
            return true;
        }
    }

    private class BadgeColorWidget extends BadgeCellWidget {
        private final BadgeCatalog.BadgeColor color;

        private BadgeColorWidget(BadgeCatalog.BadgeColor color) {
            super(BADGE_COLOR_CELL_W, BADGE_COLOR_CELL_H);
            this.color = color;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            BadgeProfile profile = BadgeProfileData.getLocalProfile();
            boolean unlocked = BadgeCatalog.isUnlocked(color)
                    && BadgeCatalog.isCompatible(BadgeCatalog.icon(profile.selectedIconId), color);
            boolean selected = color.id().equals(profile.selectedColorId);
            drawPanel(ctx, x, y, width, height, hovered, selected ? GOLD : (unlocked ? BORDER_LIGHT : RED));
            drawScaledBadgeText(ctx, BadgeCatalog.colorPreviewText(profile.selectedIconId, color.id()), x + width / 2, y + 12, 2.25f, true);
            if (BadgeCatalog.isFade(color)) {
                drawCenteredText("Fade color", x + width / 2, y + 37, TEXT_DIM);
            }
            drawCenteredText(trimText(color.displayName(), width - 8), x + width / 2, y + 52, unlocked ? TEXT_MAIN : TEXT_DIM);
            if (selected) drawCenteredText("Selected", x + width / 2, y + 68, GOLD);
            else if (!unlocked) drawCenteredText("Locked", x + width / 2, y + 68, RED);
        }

        @Override
        protected boolean onClick(int button) {
            BadgeProfile profile = BadgeProfileData.getLocalProfile();
            if (button != 0
                    || !BadgeCatalog.isUnlocked(color)
                    || !BadgeCatalog.isCompatible(BadgeCatalog.icon(profile.selectedIconId), color)) {
                return false;
            }
            BadgeProfileData.setColor(color.id());
            playClick();
            return true;
        }
    }

    private class AchievementScrollBarWidget extends LogicalWidget {
        private AchievementScrollBarWidget() {
            super(0, 0, 0, 0);
        }

        private void updateBounds() {
            setBounds(layoutWidth() - 16, ACHIEVEMENT_VIEWPORT_TOP, 6, achievementViewportHeight());
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if (maxAchievementScroll <= 0) return;
            int thumbH = scrollbarThumbHeight(height);
            int thumbY = scrollbarThumbY(y, height, thumbH);
            drawRect(x, y, width, height, BG_DARK);
            drawRect(x + 1, thumbY, width - 2, thumbH, GOLD);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0 || !contains((int) mx, (int) my) || maxAchievementScroll <= 0) return false;
            int thumbH = scrollbarThumbHeight(height);
            int thumbY = scrollbarThumbY(y, height, thumbH);
            achievementScrollbarDragging = true;
            achievementScrollbarDragOffset = my >= thumbY && my < thumbY + thumbH ? my - thumbY : thumbH / 2.0;
            setScrollFromScrollbar(my - achievementScrollbarDragOffset);
            playClick();
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (!achievementScrollbarDragging || button != 0 || tab != Tab.ACHIEVEMENTS) return false;
            setScrollFromScrollbar(mouseY - achievementScrollbarDragOffset);
            return true;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            if (!achievementScrollbarDragging) return false;
            achievementScrollbarDragging = false;
            return true;
        }
    }

    private class HorizontalBadgeScrollBarWidget extends LogicalWidget {
        private final HorizontalScrollTarget target;
        private int contentWidth;

        private HorizontalBadgeScrollBarWidget(HorizontalScrollTarget target) {
            super(0, 0, 0, 0);
            this.target = target;
        }

        private void update(int x, int y, int width, int height, int contentWidth) {
            setBounds(x, y, width, height);
            this.contentWidth = contentWidth;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            int max = Math.max(0, contentWidth - width);
            if (max <= 0) return;
            drawRect(x, y, width, height, BG_DARK);
            int thumbW = horizontalScrollbarThumbWidth(width, contentWidth);
            int thumbX = x + Math.round((width - thumbW) * (getHorizontalScroll(target) / max));
            drawRect(thumbX, y, thumbW, height, hovered || horizontalScrollDragging == target ? GOLD : BORDER_LIGHT);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0 || !contains((int) mx, (int) my)) return false;
            int max = Math.max(0, contentWidth - width);
            if (max <= 0) return false;
            int thumbW = horizontalScrollbarThumbWidth(width, contentWidth);
            int thumbX = x + Math.round((width - thumbW) * (getTargetHorizontalScroll(target) / max));
            horizontalScrollDragging = target;
            horizontalScrollDragOffset = mx >= thumbX && mx < thumbX + thumbW ? mx - thumbX : thumbW / 2.0;
            setHorizontalScrollFromThumb(target, mx - horizontalScrollDragOffset);
            playClick();
            return true;
        }
    }

    public enum Tab {
        ACHIEVEMENTS,
        BADGES
    }

    private enum HorizontalScrollTarget {
        ICONS,
        COLORS
    }

    private enum BadgeFilter {
        ICON_UNLOCK("Status", HorizontalScrollTarget.ICONS),
        ICON_IMAGE("Image", HorizontalScrollTarget.ICONS),
        COLOR_UNLOCK("Status", HorizontalScrollTarget.COLORS),
        COLOR_FADE("Fade", HorizontalScrollTarget.COLORS);

        private final String label;
        private final HorizontalScrollTarget target;

        BadgeFilter(String label, HorizontalScrollTarget target) {
            this.label = label;
            this.target = target;
        }
    }

    private enum UnlockFilterMode {
        ALL("All", GREEN),
        ONLY_UNLOCKED("Only Unlocked", GOLD),
        ONLY_LOCKED("Only Locked", RED);

        private final String label;
        private final int accent;

        UnlockFilterMode(String label, int accent) {
            this.label = label;
            this.accent = accent;
        }

        private UnlockFilterMode next() {
            return switch (this) {
                case ALL -> ONLY_UNLOCKED;
                case ONLY_UNLOCKED -> ONLY_LOCKED;
                case ONLY_LOCKED -> ALL;
            };
        }

        private UnlockFilterMode previous() {
            return switch (this) {
                case ALL -> ONLY_LOCKED;
                case ONLY_UNLOCKED -> ALL;
                case ONLY_LOCKED -> ONLY_UNLOCKED;
            };
        }
    }

    private enum SpecialFilterMode {
        ALL(GREEN),
        HIDE(RED),
        ONLY(GOLD);

        private final int accent;

        SpecialFilterMode(int accent) {
            this.accent = accent;
        }

        private SpecialFilterMode next() {
            return switch (this) {
                case ALL -> HIDE;
                case HIDE -> ONLY;
                case ONLY -> ALL;
            };
        }

        private SpecialFilterMode previous() {
            return switch (this) {
                case ALL -> ONLY;
                case HIDE -> ALL;
                case ONLY -> HIDE;
            };
        }
    }

    private static class BadgeScreenSessionState {
        private float achievementScroll;
        private float targetAchievementScroll;
        private float iconScroll;
        private float targetIconScroll;
        private float colorScroll;
        private float targetColorScroll;
        private String iconSearch = "";
        private String colorSearch = "";
        private UnlockFilterMode iconUnlockFilter = UnlockFilterMode.ALL;
        private SpecialFilterMode imageIconFilter = SpecialFilterMode.ALL;
        private UnlockFilterMode colorUnlockFilter = UnlockFilterMode.ALL;
        private SpecialFilterMode fadeColorFilter = SpecialFilterMode.ALL;
    }

    private record AchievementCategory(String key, List<Achievement> achievements) {
        private boolean expanded() {
            return CATEGORY_EXPANDED.getOrDefault(key, true);
        }
    }

    private enum RewardType {
        ICON,
        COLOR
    }

    private record RewardPreview(RewardType type, String iconId, String colorId, Integer minTier) {}

    public void setTab(Tab tab) {
        this.tab = tab;
    }
}