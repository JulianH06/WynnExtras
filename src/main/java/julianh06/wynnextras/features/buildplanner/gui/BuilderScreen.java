package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.compat.wynntils.WynntilsCustomModelAdapter;
import julianh06.wynnextras.features.buildplanner.PlannerLog;
import julianh06.wynnextras.features.buildplanner.config.SavedAbilityTree;
import julianh06.wynnextras.features.buildplanner.config.SavedAbilityTreeManager;
import julianh06.wynnextras.features.buildplanner.config.SavedBuild;
import julianh06.wynnextras.features.buildplanner.config.SavedBuildManager;
import julianh06.wynnextras.features.buildplanner.config.ToolWorkspaceManager;
import julianh06.wynnextras.features.buildplanner.data.BuildCalculator;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.wynncraft.state.CharacterClass;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeDatabase;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeDefinition;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeState;
import julianh06.wynnextras.features.buildplanner.data.AspectDatabase;
import julianh06.wynnextras.features.buildplanner.data.AspectSelection;
import julianh06.wynnextras.features.buildplanner.data.DamageCalculator;
import julianh06.wynnextras.features.buildplanner.data.ItemDatabase;
import julianh06.wynnextras.features.buildplanner.data.IdentificationUnits;
import julianh06.wynnextras.features.buildplanner.data.ItemInspection;
import julianh06.wynnextras.features.buildplanner.data.SkillPointWarning;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import julianh06.wynnextras.features.buildplanner.data.WynnTome;
import julianh06.wynnextras.features.buildplanner.data.WynnAspect;
import julianh06.wynnextras.features.buildplanner.data.WynnBuilderLinkImporter;
import julianh06.wynnextras.features.buildplanner.data.WynnBuilderItemIndex;
import julianh06.wynnextras.features.buildplanner.data.WynnBuilderTreeCodec;
import julianh06.wynnextras.features.buildplanner.data.TomeDatabase;
import julianh06.wynnextras.features.profileviewer.data.AbilityMapData;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeCache;
import julianh06.wynnextras.utils.LinkUtils;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import julianh06.wynnextras.utils.UI.AbilityTreeWidget;
import julianh06.wynnextras.utils.UI.SmoothScroll;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderBuild;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderBuildCodec;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderCraft;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderLegacyBuildCodec;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderVersions;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class BuilderScreen extends PlannerScreen {
    private static boolean abilityTreeVisibleSession;
    private static final SmoothScroll ABILITY_TREE_SCROLL = new SmoothScroll();
    private static final SmoothScroll ABILITY_SIDEBAR_SCROLL = new SmoothScroll();
    private static final Identifier EQUIPMENT_TEXTURE = Identifier.of("wynnextras", "textures/gui/buildplanner/equipment.png");
    private static final Identifier TREE_BACKGROUND = Identifier.of(
            "wynnextras", "textures/gui/profileviewer/treetabbackground.png");
    private static final Identifier ARCHETYPE_ICONS = Identifier.of(
            "wynnextras", "textures/gui/buildplanner/archetypes.png");
    private static final int MARGIN = 8;
    private static final int GAP = 8;
    private static final int CONTENT_TOP = 58;
    private static final int PANEL_TOP_INSET = 6;
    private static final int VIEW_SWITCH_OFFSET = 24;
    private static final int ITEM_ROW_HEIGHT = 48;
    private static final int ITEM_INFO_WIDTH = 106;
    private static final int SKILL_CARD_HEIGHT = 82;
    private static final int AUTO_ASSIGN_WIDTH = 92;
    private static final int AUTO_ASSIGN_MAIN_WIDTH = 150;
    private static final int AUTO_ASSIGN_ALL_WIDTH = 150;
    private static final int POSITIVE = 0xFF00FF40;
    private static final int NEGATIVE = 0xFFFF3030;
    private static final int EARTH = 0xFF20C020;
    private static final int THUNDER = 0xFFFFFF20;
    private static final int WATER = 0xFF20FFFF;
    private static final int FIRE = 0xFFFF4040;
    private static final int AIR = 0xFFFFFFFF;
    private static final int PICKER_ROW_HEIGHT = 24;
    private static final int PICKER_HEADING_HEIGHT = 18;
    private static final String[] SKILL_NAMES = {"Strength", "Dexterity", "Intelligence", "Defense", "Agility"};
    private static final String[] SKILL_EFFECTS = {"damage", "crit", "cost red.", "resist", "dodge"};
    private static final Identifier[] SKILL_ICONS = {
            Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/strength.png"),
            Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/dexterity.png"),
            Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/intelligence.png"),
            Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/defence.png"),
            Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/agility.png")
    };
    private static final Formatting[] SKILL_COLORS = {
            Formatting.GREEN, Formatting.YELLOW, Formatting.AQUA, Formatting.RED, Formatting.WHITE
    };
    private static final StatDisplay[] SUMMARY_STATS = {
            stat("manaRegen", "Mana Regen:", "/5s"),
            stat("manaSteal", "Mana Steal:", "/3s"),
            stat("lifeSteal", "Life Steal:", "/3s"),
            stat("poison", "Poison:", "/3s"),
            stat("reflection", "Reflection:", "%"),
            stat("thorns", "Thorns:", "%"),
            stat("exploding", "Exploding:", "%"),
            stat("rawAttackSpeed", "Attack Speed Bonus:", " tier"),
            stat("walkSpeed", "Walk Speed Bonus:", "%"),
            stat("sprint", "Sprint Bonus:", "%"),
            stat("sprintRegen", "Sprint Regen Bonus:", "%"),
            stat("jumpHeight", "Jump Height:", ""),
            stat("combatExperience", "Combat XP Bonus:", "%"),
            stat("lootBonus", "Loot Bonus:", "%"),
            stat("lootQuality", "Loot Quality:", "%"),
            stat("stealing", "Stealing:", "%"),
            stat("gatherXpBonus", "Gathering XP Bonus:", "%"),
            stat("gatherSpeed", "Gathering Speed Bonus:", "%"),
            stat("knockback", "Knockback:", "%"),
            stat("weakenEnemy", "Weaken Enemy:", "%"),
            stat("slowEnemy", "Slow Enemy:", "%"),
            stat("mainAttackRange", "Melee Range %:", "%")
    };
    private static final StatDisplay[] DETAILED_SUSTAIN_STATS = {
            stat("manaRegen", "Mana Regen:", "/5s"),
            stat("manaSteal", "Mana Steal:", "/3s"),
            stat("rawMaxMana", "Max Mana:", ""),
            stat("healthRegenRaw", "Raw Health Regen:", ""),
            stat("healthRegen", "Health Regen %:", "%"),
            stat("healingEfficiency", "Heal Effectiveness %:", "%"),
            stat("lifeSteal", "Life Steal:", "/3s")
    };
    private static final StatDisplay[] DETAILED_DAMAGE_STATS = {
            stat("rawSpellDamage", "Spell Damage Raw:", ""),
            stat("rawNeutralSpellDamage", "Neut. Spell Damage Raw:", ""),
            stat("rawElementalSpellDamage", "Elem. Spell Damage Raw:", ""),
            stat("spellDamage", "Spell Damage %:", "%"),
            stat("neutralSpellDamage", "Neut. Spell Damage %:", "%"),
            stat("elementalSpellDamage", "Elem. Spell Damage %:", "%"),
            stat("rawMainAttackDamage", "Melee Damage Raw:", ""),
            stat("rawNeutralMainAttackDamage", "Neut. Melee Damage Raw:", ""),
            stat("rawElementalMainAttackDamage", "Elem. Melee Damage Raw:", ""),
            stat("mainAttackDamage", "Melee Damage %:", "%"),
            stat("neutralMainAttackDamage", "Neut. Melee Damage %:", "%"),
            stat("elementalMainAttackDamage", "Elem. Melee Damage %:", "%"),
            stat("rawDamage", "Damage Raw:", ""),
            stat("rawNeutralDamage", "Neutral Damage Raw:", ""),
            stat("rawElementalDamage", "Elemental Damage Raw:", ""),
            stat("damage", "Damage %:", "%"),
            stat("neutralDamage", "Neutral Damage %:", "%"),
            stat("elementalDamage", "Elemental Damage %:", "%")
    };
    private static final StatDisplay[] DETAILED_ELEMENTAL_STATS = elementalStats();
    private static final StatDisplay[] DETAILED_UTILITY_STATS = {
            stat("elementalDefence", "Elemental Defense %:", "%"),
            reversedStat("1stSpellCost", "1st Spell Cost %:", "%"),
            reversedStat("raw1stSpellCost", "1st Spell Cost Raw:", ""),
            reversedStat("2ndSpellCost", "2nd Spell Cost %:", "%"),
            reversedStat("raw2ndSpellCost", "2nd Spell Cost Raw:", ""),
            reversedStat("3rdSpellCost", "3rd Spell Cost %:", "%"),
            reversedStat("raw3rdSpellCost", "3rd Spell Cost Raw:", ""),
            reversedStat("4thSpellCost", "4th Spell Cost %:", "%"),
            reversedStat("raw4thSpellCost", "4th Spell Cost Raw:", ""),
            stat("rawAttackSpeed", "Attack Speed Bonus:", " tier"),
            stat("poison", "Poison:", "/3s"),
            stat("reflection", "Reflection:", "%"),
            stat("thorns", "Thorns:", "%"),
            stat("exploding", "Exploding:", "%"),
            stat("walkSpeed", "Walk Speed Bonus:", "%"),
            stat("sprint", "Sprint Bonus:", "%"),
            stat("sprintRegen", "Sprint Regen Bonus:", "%"),
            stat("jumpHeight", "Jump Height:", ""),
            stat("combatExperience", "Combat XP Bonus:", "%"),
            stat("lootBonus", "Loot Bonus:", "%"),
            stat("lootQuality", "Loot Quality:", "%"),
            stat("stealing", "Stealing:", "%"),
            stat("gatherXpBonus", "Gathering XP Bonus:", "%"),
            stat("gatherSpeed", "Gathering Speed Bonus:", "%"),
            stat("knockback", "Knockback:", "%"),
            stat("weakenEnemy", "Weaken Enemy:", "%"),
            stat("slowEnemy", "Slow Enemy:", "%"),
            stat("mainAttackRange", "Melee Range %:", "%")
    };

    private final Screen parent;
    private final ToolWorkspaceManager workspace = ToolWorkspaceManager.getInstance();
    private final String toolTabId;
    private boolean workspaceInitialized;
    private SavedBuild pendingBuild;
    private String tabName = "Build 1";
    private String restoreError = "";
    private String notice = "";
    private List<String> abilityTreeNotices = List.of();
    private Set<String> abilityTreeNoticeNodeIds = Set.of();
    private String abilityTreeSearch = "";
    private NoticeSource noticeSource = NoticeSource.IMPORT;
    private String notifiedWorkspaceError = "";
    private int workspaceTicks;
    private final Map<BuildSlot, WynnItem> equipped = new EnumMap<>(BuildSlot.class);
    private final Map<BuildSlot, String> powderCodes = new EnumMap<>(BuildSlot.class);
    private final List<Offhand> offhands = new ArrayList<>();
    private final Map<String, WynnTome> selectedTomes = new LinkedHashMap<>();
    private final Map<String, AspectSelection> selectedAspects = new LinkedHashMap<>();
    private final Map<WynnItem, ItemStack> iconCache = new IdentityHashMap<>();
    private final Map<WynnItem, BuildCalculator.Result> offhandStatsCache = new IdentityHashMap<>();
    private final int[] assignedSkills = new int[5];
    private final AbilityTreeState abilityTreeState = new AbilityTreeState();
    private final List<ThemedButton> dynamicButtons = new ArrayList<>();
    private final List<ThemedButton> pickerButtons = new ArrayList<>();
    private final List<TextInputWidget> dynamicFields = new ArrayList<>();
    private final List<TextInputWidget> skillFields = new ArrayList<>();
    private final List<TextWidget> dynamicLabels = new ArrayList<>();
    private BuildSlot pickerSlot;
    private int pickerOffhandIndex = -1;
    private WynnItem detailItem;
    private final SmoothScroll detailScroll = new SmoothScroll();
    private int detailMaxScroll;
    private final SmoothScroll summaryScroll = new SmoothScroll();
    private final PlannerScrollbar summaryScrollbar = new PlannerScrollbar(summaryScroll, 18);
    private int summaryMaxScroll;
    private final SmoothScroll damageScroll = new SmoothScroll();
    private final PlannerScrollbar damageScrollbar = new PlannerScrollbar(damageScroll, 18);
    private int damageMaxScroll;
    private int expandedSpell = -1;
    private final List<DamageCardBounds> damageCards = new ArrayList<>();
    private int damageCacheKey = Integer.MIN_VALUE;
    private List<DamageCalculator.SpellResult> damageCache = List.of();
    private BuildCalculator.Result statsCache;
    private TextInputWidget searchField;
    private Widget pickerOverlay;
    private PlannerPickerList<WynnItem> pickerList;
    private ClassSelectorWidget classSelector;
    private ThemedButton abilityTreeWarningDismissButton;
    private AbilityTreeWidget abilityTreeWidget;
    private Widget abilityPresetOverlayWidget;
    private final List<ThemedButton> abilityPresetButtons = new ArrayList<>();
    private List<SavedAbilityTree> visibleAbilityPresets = List.of();
    private AbilityPresetOverlay abilityPresetOverlay = AbilityPresetOverlay.NONE;
    private int abilityPresetScroll;
    private final SmoothScroll abilityTreeScroll = ABILITY_TREE_SCROLL;
    private final PlannerScrollbar abilityTreeScrollbar = new PlannerScrollbar(abilityTreeScroll, 32);
    private int abilityTreeMaxScroll;
    private final SmoothScroll abilitySidebarScroll = ABILITY_SIDEBAR_SCROLL;
    private final PlannerScrollbar abilitySidebarScrollbar = new PlannerScrollbar(abilitySidebarScroll, 24);
    private int abilitySidebarMaxScroll;
    private int abilitySidebarEntriesCacheKey = Integer.MIN_VALUE;
    private List<AbilitySidebarEntry> abilitySidebarEntriesCache = List.of();
    private boolean abilityTreeVisible;
    private String abilityTreeStatus = "";
    private long abilityTreeStatusUntil;
    private AbilityTreeDefinition.Archetype hoveredArchetype;
    private final Deque<SavedBuild> undoHistory = new ArrayDeque<>();
    private final Deque<SavedBuild> redoHistory = new ArrayDeque<>();
    private SavedBuild lastHistoryState;
    private BuildCalculator.SkillPointPlan skillPointPlan =
            BuildCalculator.optimizeSkillPoints(121, List.of());
    private final SmoothScroll pickerScroll = new SmoothScroll();
    private CharacterClass selectedClass = CharacterClass.ARCHER;
    private String skillPointError = "";
    private String skillPointSuccess = "";
    private int level = 121;
    private boolean detailed;
    private String boostSection = "Ability Boosts";

    public BuilderScreen(Screen parent) {
        this(parent, WorkspaceTabs.builderId());
    }

    BuilderScreen(Screen parent, String toolTabId) {
        super(Text.literal("Builder"));
        this.parent = parent;
        this.toolTabId = toolTabId;
        this.abilityTreeVisible = abilityTreeVisibleSession;
    }

    @Override
    protected boolean shouldRenderBlur() {
        return false;
    }

    @Override
    protected int getMinLogicalHeight() {
        return (int) Math.round(720 * scaleFactor);
    }

    @Override
    protected boolean shouldRenderBackground() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        damageCacheKey = Integer.MIN_VALUE;
        if (!workspaceInitialized) {
            workspaceInitialized = true;
            pendingBuild = workspace.find(toolTabId).build();
            tabName = pendingBuild.name();
        } else {
            saveWorkspace();
        }
        if (pendingBuild != null) {
            restorePendingBuild();
        }
        rebuildWidgets();
        if (lastHistoryState == null && pendingBuild == null) {
            lastHistoryState = historySnapshot();
        }
    }

    private void rebuildWidgets() {
        for (ThemedButton button : dynamicButtons) {
            this.remove(button);
        }
        dynamicButtons.clear();
        pickerButtons.clear();
        for (TextInputWidget field : dynamicFields) removeRootWidget(field);
        dynamicFields.clear();
        skillFields.clear();
        removeRootWidget(pickerOverlay);
        removeRootWidget(pickerList);
        removeRootWidget(classSelector);
        removeRootWidget(abilityTreeWidget);
        removeRootWidget(abilityPresetOverlayWidget);
        pickerOverlay = null;
        pickerList = null;
        classSelector = null;
        abilityPresetOverlayWidget = null;
        abilityTreeWarningDismissButton = null;
        abilityPresetButtons.clear();
        for (TextWidget label : dynamicLabels) {
            this.remove(label);
        }
        dynamicLabels.clear();
        searchField = null;

        if (pendingBuild != null) {
            buildTabWidgets(layout());
            addDynamic(new ThemedButton(
                    this.width - 74, this.height - 28, 64, 20, Text.literal("Done"), this::close));
            return;
        }
        Layout layout = layout();
        buildTabWidgets(layout);
        for (BuildSlot slot : BuildSlot.values()) {
            SlotPosition position = slotPosition(layout, slot);
            WynnItem item = equipped.get(slot);
            int controlWidth = position.width - ITEM_INFO_WIDTH - 9;
            String displayName = item == null ? slot.label + ": Empty" : item.displayName();
            String fittedName = fitText(displayName, controlWidth - 10);
            Text name = Text.literal(fittedName).formatted(
                    item == null ? Formatting.GRAY : tierColor(item.tier()));
            addDynamic(new ThemedButton(
                    position.x + ITEM_INFO_WIDTH + 5, position.y + 3,
                    controlWidth, 20, name, () -> openPicker(slot)));

            if (!slot.powderable) {
                powderCodes.remove(slot);
                continue;
            }
            TextInputWidget powderField = textInput(
                    position.x + ITEM_INFO_WIDTH + 5, position.y + 25,
                    position.width - ITEM_INFO_WIDTH - 9, 18, "Powders");
            int powderSlots = item == null ? 0 : item.stat("powderSlots");
            powderField.setMaxLength(Math.max(1, powderSlots * 2));
            powderField.setInput(powderCodes.getOrDefault(slot, ""));
            powderField.setPlaceholder(powderSlots + " powder slots");
            powderField.setEnabled(item != null && powderSlots > 0);
            powderField.setCharacterFilter(character -> "etwfaETWFA1234567".indexOf(character) >= 0);
            powderField.setInputPredicate(text -> validPowderText(text, powderSlots));
            powderField.setOnChange(text -> {
                powderCodes.put(slot, text.toLowerCase());
                invalidateStats();
            });
            addDynamicField(powderField);
        }
        for (int i = 0; i < offhands.size(); i++) {
            int index = i;
            Offhand offhand = offhands.get(i);
            SlotPosition position = offhandPosition(layout, i);
            int controlWidth = position.width - ITEM_INFO_WIDTH - 9;
            int removeWidth = 20;
            int itemButtonWidth = controlWidth - removeWidth - 4;
            String fittedName = fitText(offhand.item().displayName(), itemButtonWidth - 10);
            addDynamic(new ThemedButton(
                    position.x + ITEM_INFO_WIDTH + 5, position.y + 3,
                    itemButtonWidth, 20, Text.literal(fittedName).formatted(tierColor(offhand.item().tier())),
                    () -> openOffhandPicker(index)));
            addDynamic(new ThemedButton(
                    position.x + ITEM_INFO_WIDTH + 9 + itemButtonWidth, position.y + 3,
                    removeWidth, 20, Text.literal("X"), () -> removeOffhand(index))
                    .setHoverText(Text.literal("Remove this offhand from the build.")));

            TextInputWidget powderField = textInput(
                    position.x + ITEM_INFO_WIDTH + 5, position.y + 25,
                    controlWidth, 18, "Powders");
            int powderSlots = offhand.item().stat("powderSlots");
            powderField.setMaxLength(Math.max(1, powderSlots * 2));
            powderField.setInput(offhand.powders());
            powderField.setPlaceholder(powderSlots + " powder slots");
            powderField.setEnabled(powderSlots > 0);
            powderField.setCharacterFilter(character -> "etwfaETWFA1234567".indexOf(character) >= 0);
            powderField.setInputPredicate(text -> validPowderText(text, powderSlots));
            powderField.setOnChange(text -> {
                Offhand current = offhands.get(index);
                offhands.set(index, new Offhand(current.item(), text.toLowerCase(Locale.ROOT)));
            });
            addDynamicField(powderField);
        }

        int skillY = layout.skillTop + 11;
        int skillCardWidth = (layout.leftWidth - 8) / 5;
        int[] currentSkills = calculateStats().skills();
        for (int i = 0; i < SKILL_NAMES.length; i++) {
            int index = i;
            int cardX = MARGIN + 4 + i * skillCardWidth;
            TextInputWidget skillField = textInput(
                    cardX + 8, skillY + 11, skillCardWidth - 16, 20, SKILL_NAMES[i]);
            int itemBonus = currentSkills[i] - assignedSkills[i];
            skillField.setMaxLength(5);
            skillField.setCentered(true);
            skillField.setTextScale(2.5F);
            skillField.setCharacterFilter(character -> Character.isDigit(character) || character == '-');
            skillField.setInputPredicate(text -> text.matches("-?\\d{0,4}"));
            skillField.setInput(Integer.toString(assignedSkills[i] + itemBonus));
            skillField.setOnChange(text -> {
                int activeItemBonus = calculateStats().skills()[index] - assignedSkills[index];
                assignedSkills[index] = text.isEmpty() || "-".equals(text)
                        ? 0
                        : Math.clamp(Integer.parseInt(text) - activeItemBonus, 0, 999);
                invalidateStats();
                skillPointError = "";
                skillPointSuccess = "";
                refreshSkillFields(false);
            });
            skillField.setOnBlur(ignored -> refreshSkillFields(true));
            skillFields.add(skillField);
            addDynamicField(skillField);
        }
        if (offhands.isEmpty()) {
            addDynamic(new ThemedButton(
                    MARGIN + 8, layout.skillTop + layout.skillHeight - 25,
                    AUTO_ASSIGN_WIDTH, 20, Text.literal("Auto Assign"),
                    () -> autoAssignSkillPoints(false)));
        } else {
            addDynamic(new ThemedButton(
                    MARGIN + 8, layout.skillTop + layout.skillHeight - 25,
                    AUTO_ASSIGN_MAIN_WIDTH, 20, Text.literal("Auto SP: Main Weapon Only"),
                    () -> autoAssignSkillPoints(false))
                    .setHoverText(Text.literal("Assign skill points for the main weapon only.")));
            addDynamic(new ThemedButton(
                    MARGIN + 12 + AUTO_ASSIGN_MAIN_WIDTH, layout.skillTop + layout.skillHeight - 25,
                    AUTO_ASSIGN_ALL_WIDTH, 20, Text.literal("Auto SP: Main + Offhand" + (offhands.size() > 1 ? "s" : "")),
                    () -> autoAssignSkillPoints(true))
                    .setHoverText(Text.literal(
                            "Assign enough skill points for the main weapon and every offhand.")));
        }

        BuilderFooterLayout footerLayout = builderFooterLayout(layout);
        if (offhands.size() < 3) {
            SlotPosition position = offhandPosition(layout, offhands.size());
            int buttonWidth = 140;
            addDynamic(new ThemedButton(
                    position.x + (position.width - buttonWidth) / 2,
                    position.y + (ITEM_ROW_HEIGHT - 20) / 2,
                    buttonWidth, 20, Text.literal("Add Offhand"),
                    () -> openOffhandPicker(offhands.size()))
                    .setHoverLines(List.of(
                            Text.literal("§lOffhand info:"),
                            Text.literal("Offhands are supported only in the WynnExtras builder."),
                            Text.literal("They are excluded from stat and damage calculations."),
                            Text.literal("They are not available when opening the build in WynnBuilder"),
                            Text.literal("using the Browser button in the bottom right."))));
        }

        AbilityTreeLayout treeLayout = abilityTreeLayout(layout);
        int viewSwitchX = abilityTreeVisible ? treeLayout.treeLeft() + 4 : layout.middleX + 4;
        int viewSwitchWidth = abilityTreeVisible ? treeLayout.treeWidth() - 8 : layout.middleWidth - 8;
        int viewSwitchY = layout.equipmentTop + PANEL_TOP_INSET;
        addDynamic(new ThemedButton(
                viewSwitchX, viewSwitchY, viewSwitchWidth, 20,
                Text.literal(abilityTreeVisible
                        ? "Switch to Stats & Damage"
                        : "Switch to Ability Tree"),
                () -> setAbilityTreeVisible(!abilityTreeVisible)));

        if (!abilityTreeVisible) {
            int summaryButtonGap = 4;
            int summaryButtonWidth = (layout.middleWidth - 8 - summaryButtonGap) / 2;
            addDynamic(new ThemedButton(
                    layout.middleX + 4, layout.equipmentTop + PANEL_TOP_INSET + VIEW_SWITCH_OFFSET,
                    summaryButtonWidth, 20,
                    Text.literal("Summary"), () -> {
                        detailed = false;
                        summaryScroll.jump(0.0F);
                    }));
            addDynamic(new ThemedButton(
                    layout.middleX + 4 + summaryButtonWidth + summaryButtonGap,
                    layout.equipmentTop + PANEL_TOP_INSET + VIEW_SWITCH_OFFSET,
                    summaryButtonWidth, 20, Text.literal("Detailed"), () -> {
                        detailed = true;
                        summaryScroll.jump(0.0F);
                    }));
        }
        int footerY = this.height - MARGIN - 24;
        int footerGap = 4;
        int footerX = footerLayout.buttonX();
        int footerButtonWidth = footerLayout.buttonWidth();
        addDynamic(new ThemedButton(
                footerX, footerY, footerButtonWidth, 20,
                Text.literal("Import"), this::importWynnBuilderLink));
        addDynamic(new ThemedButton(
                footerX + footerButtonWidth + footerGap, footerY, footerButtonWidth, 20,
                Text.literal("Browser"), this::openInWynnBuilder));
        addDynamic(new ThemedButton(
                footerX + (footerButtonWidth + footerGap) * 2, footerY, footerButtonWidth, 20,
                Text.literal("Saved"), this::openSavedBuilds));
        addDynamic(new ThemedButton(
                footerX + (footerButtonWidth + footerGap) * 3, footerY, footerButtonWidth, 20,
                Text.literal("Save"), this::openSaveBuild));
        addDynamic(new ThemedButton(
                footerX + (footerButtonWidth + footerGap) * 4, footerY, footerButtonWidth, 20,
                Text.literal("Done"), this::close));

        int boostButtonY = layout.boostTop + 5;
        int boostButtonGap = 4;
        int boostButtonWidth = (layout.leftWidth - 10 - boostButtonGap * 3) / 4;
        addBoostButton(0, boostButtonY, boostButtonWidth, boostButtonGap,
                "Tomes & Aspects", () -> client.setScreen(new TomeScreen(this)));
        addBoostButton(1, boostButtonY, boostButtonWidth, boostButtonGap,
                "Ability Boosts", () -> boostSection = "Ability Boosts");
        addBoostButton(2, boostButtonY, boostButtonWidth, boostButtonGap,
                "Powder Specials", () -> boostSection = "Powder Specials");
        addBoostButton(3, boostButtonY, boostButtonWidth, boostButtonGap,
                "Raid Buffs", () -> boostSection = "Raid Buffs");
        if (abilityTreeVisible) {
            buildAbilityTreeWidgets(layout);
        } else {
            abilityPresetOverlay = AbilityPresetOverlay.NONE;
        }
        if (pickerOpen()) {
            for (ThemedButton button : dynamicButtons) button.active = false;
            for (TextInputWidget field : dynamicFields) {
                field.setEnabled(false);
                field.setDisabledTooltip(null);
            }
            if (abilityTreeWidget != null) abilityTreeWidget.setVisible(false);
            pickerOverlay = new PickerOverlay();
            addRootWidget(pickerOverlay);
            buildPickerWidgets();
        }
    }

    private void buildPickerWidgets() {
        int pickerWidth = Math.min(520, this.width - 60);
        int pickerHeight = Math.min(326, this.height - 50);
        int left = (this.width - pickerWidth) / 2;
        int top = (this.height - pickerHeight) / 2;

        searchField = textInput(
                left + 14, top + 34, pickerWidth - 28, 20,
                "Search " + pickerLabel().toLowerCase(Locale.ROOT) + "...");
        searchField.setMaxLength(100);
        searchField.setOnChange(query -> {
            pickerScroll.jump(0.0F);
            rebuildPickerResults(left, top, pickerWidth);
        });
        addDynamicField(searchField);
        setFocusedWidget(searchField);

        addPicker(new ThemedButton(
                left + pickerWidth - 78, top + pickerHeight - 28, 64, 20, Text.literal("Cancel"), () -> {
                    closePicker();
                    rebuildWidgets();
                }));
        if (currentPickerItem() != null) {
            addPicker(new ThemedButton(
                    left + 14, top + pickerHeight - 28, 64, 20, Text.literal("Remove"), () -> {
                        removePickerItem();
                        rebuildWidgets();
                    }));
        }
        addPicker(new ThemedButton(
                left + 88, top + pickerHeight - 28, 78, 20, Text.literal("Crafted"), () -> {
                    BuildSlot slot = pickerSlot;
                    int offhandIndex = pickerOffhandIndex;
                    client.setScreen(new CraftedItemScreen(
                            this, pickerSearchType(), pickerSlotId(), currentPickerItem(),
                            item -> equipPickerItem(slot, offhandIndex, item)));
                }));
        rebuildPickerResults(left, top, pickerWidth);
    }

    private void rebuildPickerResults(int left, int top, int pickerWidth) {
        if (!pickerOpen()) return;
        List<WynnItem> results = ItemDatabase.getInstance()
                .searchItems(searchField == null ? "" : searchField.getInput(), pickerSearchType());
        List<PlannerPickerList.Row<WynnItem>> rows = new ArrayList<>();
        if (pickerOffhandIndex >= 0) {
            results.stream()
                    .filter(item -> CharacterClass.fromWeaponSubtype(item.subType()) == selectedClass)
                    .filter(item -> offhands.stream().noneMatch(offhand -> offhand.item().reference().equals(item.reference()))
                            || currentPickerItem() != null
                            && currentPickerItem().reference().equals(item.reference()))
                    .forEach(item -> rows.add(PlannerPickerList.Row.value(item, PICKER_ROW_HEIGHT)));
        } else if (pickerSlot == BuildSlot.WEAPON) {
            List<WynnItem> active = results.stream()
                    .filter(item -> CharacterClass.fromWeaponSubtype(item.subType()) == selectedClass).toList();
            List<WynnItem> others = results.stream()
                    .filter(item -> CharacterClass.fromWeaponSubtype(item.subType()) != selectedClass).toList();
            if (!active.isEmpty()) {
                rows.add(PlannerPickerList.Row.heading(selectedClass.displayName() + " weapons", PICKER_HEADING_HEIGHT));
                active.forEach(item -> rows.add(PlannerPickerList.Row.value(item, PICKER_ROW_HEIGHT)));
            }
            if (!others.isEmpty()) {
                rows.add(PlannerPickerList.Row.heading("Other classes", PICKER_HEADING_HEIGHT));
                others.forEach(item -> rows.add(PlannerPickerList.Row.value(item, PICKER_ROW_HEIGHT)));
            }
        } else {
            results.forEach(item -> rows.add(PlannerPickerList.Row.value(item, PICKER_ROW_HEIGHT)));
        }
        int listHeight = Math.max(24, Math.min(326, this.height - 50) - 100);
        if (pickerList == null) {
            pickerList = new PlannerPickerList<>(left + 14, top + 62, pickerWidth - 28, listHeight, rows,
                    pickerScroll, this::renderPickerRow,
                    (context, item, mouseX, mouseY) -> renderItemTooltip(
                            context, item, mouseX, mouseY, false),
                    this::equipSelectedPickerItem);
            addRootWidget(pickerList);
        } else {
            pickerList.setRows(rows);
        }
    }

    private void equipItem(BuildSlot slot, WynnItem item) {
        if (slot == BuildSlot.WEAPON) {
            CharacterClass weaponClass = CharacterClass.fromWeaponSubtype(item.subType());
            if (selectedClass != weaponClass) {
                selectedAspects.clear();
                offhands.clear();
            }
            selectedClass = weaponClass;
        }
        equipped.put(slot, item);
        String powders = powderCodes.getOrDefault(slot, "");
        powderCodes.put(slot, powders.substring(0, Math.min(powders.length(), item.stat("powderSlots") * 2)));
        recalculateSkillPointPlan();
        closePicker();
        rebuildWidgets();
        saveWorkspace();
    }

    private void openPicker(BuildSlot slot) {
        pickerSlot = slot;
        pickerOffhandIndex = -1;
        pickerScroll.jump(0.0F);
        rebuildWidgets();
    }

    private void openOffhandPicker(int index) {
        closePicker();
        pickerOffhandIndex = index;
        pickerScroll.jump(0.0F);
        rebuildWidgets();
    }

    private void equipSelectedPickerItem(WynnItem item) {
        equipPickerItem(pickerSlot, pickerOffhandIndex, item);
    }

    private void equipPickerItem(BuildSlot slot, int offhandIndex, WynnItem item) {
        if (offhandIndex >= 0) {
            equipOffhand(offhandIndex, item);
        } else {
            equipItem(slot, item);
        }
    }

    private void equipOffhand(int index, WynnItem item) {
        if (index < 0 || index > offhands.size() || index >= 3
                || !"weapon".equals(item.type())
                || CharacterClass.fromWeaponSubtype(item.subType()) != selectedClass
                || hasDuplicateOffhand(item, index)) {
            return;
        }
        if (index == offhands.size()) {
            offhands.add(new Offhand(item, ""));
        } else {
            Offhand current = offhands.get(index);
            String powders = current.powders().substring(
                    0, Math.min(current.powders().length(), item.stat("powderSlots") * 2));
            offhands.set(index, new Offhand(item, powders));
        }
        recalculateSkillPointPlan();
        closePicker();
        rebuildWidgets();
        saveWorkspace();
    }

    private boolean hasDuplicateOffhand(WynnItem item, int replacedIndex) {
        for (int i = 0; i < offhands.size(); i++) {
            if (i != replacedIndex && offhands.get(i).item().reference().equals(item.reference())) return true;
        }
        return false;
    }

    private void removeOffhand(int index) {
        if (index < 0 || index >= offhands.size()) return;
        offhands.remove(index);
        recalculateSkillPointPlan();
        saveWorkspace();
        rebuildWidgets();
    }

    private void removePickerItem() {
        if (pickerOffhandIndex >= 0) {
            int index = pickerOffhandIndex;
            closePicker();
            removeOffhand(index);
            return;
        }
        if (pickerSlot == BuildSlot.WEAPON) {
            selectedAspects.clear();
        }
        equipped.remove(pickerSlot);
        powderCodes.remove(pickerSlot);
        recalculateSkillPointPlan();
        closePicker();
    }

    private boolean pickerOpen() {
        return pickerSlot != null || pickerOffhandIndex >= 0;
    }

    private void closePicker() {
        pickerSlot = null;
        pickerOffhandIndex = -1;
        pickerScroll.jump(0.0F);
    }

    private String pickerLabel() {
        return pickerOffhandIndex >= 0 ? "Offhand" : pickerSlot.label;
    }

    private String pickerSearchType() {
        return pickerOffhandIndex >= 0 ? "weapon" : pickerSlot.searchType;
    }

    private String pickerSlotId() {
        return pickerOffhandIndex >= 0 ? "OFFHAND_" + (pickerOffhandIndex + 1) : pickerSlot.name();
    }

    private WynnItem currentPickerItem() {
        if (pickerOffhandIndex >= 0) {
            return pickerOffhandIndex < offhands.size() ? offhands.get(pickerOffhandIndex).item() : null;
        }
        return equipped.get(pickerSlot);
    }

    private void addBoostButton(
            int index,
            int y,
            int width,
            int gap,
            String label,
            Runnable action
    ) {
        addDynamic(new ThemedButton(
                MARGIN + 5 + index * (width + gap), y, width, 20,
                Text.literal(fitText(label, width - 8)), action)
                .setHoverText(Text.literal(label)));
    }

    private void setAbilityTreeVisible(boolean visible) {
        if (abilityTreeVisible == visible) return;
        abilityTreeVisible = visible;
        abilityTreeVisibleSession = abilityTreeVisible;
        abilityPresetOverlay = AbilityPresetOverlay.NONE;
        if (abilityTreeVisible) {
            AbilityTreeDatabase.getInstance().loadAsync(selectedClass);
        }
        rebuildWidgets();
    }

    private void buildAbilityTreeWidgets(Layout layout) {
        abilityTreeWidget = new AbilityTreeWidget(selectedClass.apiName(), 0, 0, 0, 0, 0);
        abilityTreeWidget.setNodeClickListener(this::setAbilityTreeNode);
        addRootWidget(abilityTreeWidget);

        AbilityTreeLayout treeLayout = abilityTreeLayout(layout);
        int toolbarX = treeLayout.treeLeft() + 4;
        int toolbarY = layout.equipmentTop + PANEL_TOP_INSET + VIEW_SWITCH_OFFSET;
        int toolbarWidth = treeLayout.treeWidth() - 8;
        int gap = 4;
        int buttonWidth = (toolbarWidth - gap * 2) / 3;
        addDynamic(new ThemedButton(
                toolbarX, toolbarY, buttonWidth, 20, Text.literal("Reset Tree"), () -> {
                    abilityTreeState.reset(selectedClass);
                    abilityTreeNotices = List.of();
                    abilityTreeNoticeNodeIds = Set.of();
                    abilityTreeWidget.setSelectedIds(abilityTreeState.selected(selectedClass));
                    invalidateStats();
                    setAbilityTreeStatus("Tree reset");
                    saveWorkspace();
                }));
        addDynamic(new ThemedButton(
                toolbarX + buttonWidth + gap, toolbarY, buttonWidth, 20,
                Text.literal("Copy Tree"), this::copyAbilityTree));
        addDynamic(new ThemedButton(
                toolbarX + (buttonWidth + gap) * 2, toolbarY, buttonWidth, 20,
                Text.literal("Paste Tree"), this::pasteAbilityTree));
        TextInputWidget treeSearchField = textInput(
                toolbarX, toolbarY + 24, toolbarWidth, 18, "Search abilities...");
        treeSearchField.setMaxLength(80);
        treeSearchField.setInput(abilityTreeSearch);
        treeSearchField.setOnChange(value -> {
            abilityTreeSearch = value;
            AbilityTreeDefinition definition = AbilityTreeDatabase.getInstance().get(selectedClass);
            abilityTreeWidget.setSearchIds(abilityTreeSearchNodeIds(definition));
            abilityTreeWidget.setSearchActive(!value.isBlank());
        });
        abilityTreeWidget.setSearchInput("");
        abilityTreeWidget.setSearchIds(abilityTreeSearchNodeIds(
                AbilityTreeDatabase.getInstance().get(selectedClass)));
        abilityTreeWidget.setSearchActive(!abilityTreeSearch.isBlank());
        addDynamicField(treeSearchField);
        abilityTreeWarningDismissButton = new ThemedButton(
                0, 0, 14, 12, Text.literal("x"), this::dismissAbilityTreeWarnings);
        abilityTreeWarningDismissButton.visible = false;
        abilityTreeWarningDismissButton.setHoverText(Text.literal("Ignore warnings"));
        addDynamic(abilityTreeWarningDismissButton);

        int sidebarButtonGap = 4;
        int sidebarButtonWidth = (treeLayout.sidebarWidth() - 12 - sidebarButtonGap) / 2;
        int sidebarButtonY = abilityTreeBottom(layout) - 25;
        addDynamic(new ThemedButton(
                treeLayout.sidebarX() + 6, sidebarButtonY, sidebarButtonWidth, 20,
                Text.literal("Save Tree"), () -> openAbilityPresetOverlay(AbilityPresetOverlay.SAVE)));
        addDynamic(new ThemedButton(
                treeLayout.sidebarX() + 6 + sidebarButtonWidth + sidebarButtonGap,
                sidebarButtonY, sidebarButtonWidth, 20,
                Text.literal("Saved Trees"), () -> openAbilityPresetOverlay(AbilityPresetOverlay.SAVED)));
        if (abilityPresetOverlay != AbilityPresetOverlay.NONE) {
            buildAbilityPresetOverlay();
        }
    }

    private void setAbilityTreeNode(String nodeId, boolean active) {
        AbilityTreeDefinition definition = AbilityTreeDatabase.getInstance().get(selectedClass);
        if (definition == null) return;
        AbilityTreeState.SelectionResult result = abilityTreeState.setSelected(
                definition, nodeId, active, level);
        if (!result.changed()) {
            setAbilityTreeWarning(result.message());
            abilityTreeNoticeNodeIds = Set.of(nodeId);
            return;
        }
        abilityTreeNotices = List.of();
        abilityTreeNoticeNodeIds = Set.of();
        abilityTreeWidget.setSelectedIds(abilityTreeState.selected(selectedClass));
        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        invalidateStats();
        abilityTreeStatus = "";
        saveWorkspace();
    }

    private void copyAbilityTree() {
        if (client == null) return;
        AbilityTreeDefinition definition = AbilityTreeDatabase.getInstance().get(selectedClass);
        if (definition == null) {
            setAbilityTreeWarning("Ability tree is still loading");
            return;
        }
        try {
            WynnBuilderTreeCodec.EncodeResult encoded = WynnBuilderTreeCodec.encodeBestEffort(
                    definition, abilityTreeState.selected(selectedClass));
            client.keyboard.setClipboard(encoded.hash());
            abilityTreeNotices = encoded.skippedAbilities().isEmpty()
                    ? List.of()
                    : List.of("Copy skipped incompatible abilities: "
                            + String.join(", ", encoded.skippedAbilities()));
            abilityTreeNoticeNodeIds = skippedAbilityIds(encoded);
            setAbilityTreeStatus("WynnBuilder tree copied");
        } catch (IllegalArgumentException exception) {
            setAbilityTreeWarning(exception.getMessage());
        } catch (IllegalStateException exception) {
            PlannerLog.LOGGER.error("Failed to copy WynnBuilder ability tree.", exception);
            setAbilityTreeWarning("Could not copy ability tree. See the log.");
        }
    }

    private void pasteAbilityTree() {
        if (client == null) return;
        AbilityTreeDefinition definition = AbilityTreeDatabase.getInstance().get(selectedClass);
        if (definition == null) {
            setAbilityTreeWarning("Ability tree is still loading");
            return;
        }
        try {
            AbilityTreeState.SelectionResult result = abilityTreeState.importTree(
                    definition, client.keyboard.getClipboard(), level);
            if (result.changed()) {
                abilityTreeNotices = List.of();
                abilityTreeNoticeNodeIds = Set.of();
                setAbilityTreeStatus(selectedClass.displayName() + " tree imported");
                abilityTreeWidget.setSelectedIds(abilityTreeState.selected(selectedClass));
                invalidateStats();
                saveWorkspace();
            } else {
                setAbilityTreeWarning(result.message());
            }
        } catch (IllegalStateException exception) {
            PlannerLog.LOGGER.error("Failed to paste WynnBuilder ability tree.", exception);
            setAbilityTreeWarning("Could not paste ability tree. See the log.");
        }
    }

    private void setAbilityTreeStatus(String message) {
        abilityTreeStatus = message == null ? "" : message;
        abilityTreeStatusUntil = System.currentTimeMillis() + 3500;
    }

    private void setAbilityTreeWarning(String message) {
        abilityTreeNotices = message == null || message.isBlank()
                ? List.of() : List.of(message);
        AbilityTreeDefinition definition = AbilityTreeDatabase.getInstance().get(selectedClass);
        abilityTreeNoticeNodeIds = definition == null
                ? Set.of() : nodeIdsMentionedInWarnings(definition, abilityTreeNotices);
        abilityTreeStatus = "";
    }

    private void dismissAbilityTreeWarnings() {
        abilityTreeNotices = List.of();
        abilityTreeNoticeNodeIds = Set.of();
        if (abilityTreeWarningDismissButton != null) {
            abilityTreeWarningDismissButton.visible = false;
        }
    }

    private void openAbilityPresetOverlay(AbilityPresetOverlay overlay) {
        abilityPresetOverlay = overlay;
        abilityPresetScroll = 0;
        rebuildWidgets();
    }

    private void closeAbilityPresetOverlay() {
        abilityPresetOverlay = AbilityPresetOverlay.NONE;
        abilityPresetScroll = 0;
        rebuildWidgets();
    }

    private void buildAbilityPresetOverlay() {
        for (ThemedButton button : dynamicButtons) button.active = false;
        for (TextInputWidget field : dynamicFields) field.setEnabled(false);
        abilityTreeWidget.setVisible(false);
        abilityPresetOverlayWidget = new AbilityPresetOverlayWidget();
        addRootWidget(abilityPresetOverlayWidget);

        AbilityPresetPanel panel = abilityPresetPanel();
        addAbilityPresetButton(new ThemedButton(
                panel.x() + 16, panel.y() + panel.height() - 32, 80, 20,
                Text.literal("Back"), this::closeAbilityPresetOverlay));
        if (abilityPresetOverlay == AbilityPresetOverlay.SAVE) {
            TextInputWidget nameField = textInput(
                    panel.x() + 16, panel.y() + 38, panel.width() - 32, 20,
                    "Tree preset name");
            nameField.setMaxLength(64);
            nameField.setPlaceholder("Name (&a and §a colors supported)...");
            addDynamicField(nameField);
            setFocusedWidget(nameField);
            addAbilityPresetButton(new ThemedButton(
                    panel.x() + panel.width() - 96,
                    panel.y() + panel.height() - 32,
                    80, 20, Text.literal("Save"), () -> saveAbilityPreset(nameField)));
            return;
        }

        visibleAbilityPresets = SavedAbilityTreeManager.getInstance().getAll(selectedClass);
        int rowTop = panel.y() + 38;
        int rowHeight = 24;
        int visibleRows = Math.max(1, (panel.height() - 82) / rowHeight);
        int maxScroll = Math.max(0, visibleAbilityPresets.size() - visibleRows);
        abilityPresetScroll = Math.min(abilityPresetScroll, maxScroll);
        int count = Math.min(visibleRows, visibleAbilityPresets.size() - abilityPresetScroll);
        for (int index = 0; index < count; index++) {
            SavedAbilityTree preset = visibleAbilityPresets.get(index + abilityPresetScroll);
            int y = rowTop + index * rowHeight;
            Text label = MinecraftColorText.parse(preset.name()).copy()
                    .append(Text.literal("  (" + preset.selectedNodes().size() + ")")
                            .formatted(Formatting.GRAY));
            addAbilityPresetButton(new ThemedButton(
                    panel.x() + 16, y, panel.width() - 66, rowHeight - 3,
                    label, () -> equipAbilityPreset(preset)));
            addAbilityPresetButton(new ThemedButton(
                    panel.x() + panel.width() - 42, y, 26, rowHeight - 3,
                    Text.literal("X").formatted(Formatting.RED),
                    () -> removeAbilityPreset(preset)));
        }
    }

    private void addAbilityPresetButton(ThemedButton button) {
        abilityPresetButtons.add(button);
        addDynamic(button);
    }

    private void saveAbilityPreset(TextInputWidget nameField) {
        String name = nameField.getInput().trim();
        if (MinecraftColorText.plain(name).isBlank()) {
            setAbilityTreeStatus("Enter a preset name");
            return;
        }
        boolean saved = SavedAbilityTreeManager.getInstance().save(new SavedAbilityTree(
                name,
                selectedClass.apiName(),
                abilityTreeState.selected(selectedClass),
                System.currentTimeMillis()));
        if (saved) {
            closeAbilityPresetOverlay();
            setAbilityTreeStatus("Tree preset saved");
        } else {
            setAbilityTreeStatus("Could not save tree preset");
        }
    }

    private void equipAbilityPreset(SavedAbilityTree preset) {
        AbilityTreeDefinition definition = AbilityTreeDatabase.getInstance().get(selectedClass);
        if (definition == null) {
            setAbilityTreeStatus("Ability tree is still loading");
            return;
        }
        String encoded = "wq-atree:1:" + selectedClass.apiName() + ":"
                + preset.selectedNodes().stream().sorted().collect(Collectors.joining(","));
        AbilityTreeState.SelectionResult result = abilityTreeState.importTree(
                definition, encoded, level);
        if (result.changed()) {
            invalidateStats();
            saveWorkspace();
            closeAbilityPresetOverlay();
            setAbilityTreeStatus("Tree preset equipped");
        } else {
            setAbilityTreeStatus(result.message());
        }
    }

    private void removeAbilityPreset(SavedAbilityTree preset) {
        if (!SavedAbilityTreeManager.getInstance().remove(preset)) {
            setAbilityTreeStatus("Could not remove tree preset");
        }
        rebuildWidgets();
    }

    private AbilityPresetPanel abilityPresetPanel() {
        Layout layout = layout();
        int areaLeft = layout.middleX;
        int areaRight = layout.rightX + layout.rightWidth;
        int areaTop = layout.equipmentTop;
        int areaBottom = this.height - MARGIN;
        int panelWidth = Math.min(390, areaRight - areaLeft - 24);
        int panelHeight = Math.min(270, areaBottom - areaTop - 24);
        return new AbilityPresetPanel(
                areaLeft + (areaRight - areaLeft - panelWidth) / 2,
                areaTop + (areaBottom - areaTop - panelHeight) / 2,
                panelWidth,
                panelHeight);
    }

    CharacterClass selectedAbilityClass() {
        return selectedClass;
    }

    private void openSaveBuild() {
        if (client == null) {
            return;
        }
        client.setScreen(new SaveBuildScreen(this, selectedClass));
    }

    private void openSavedBuilds() {
        if (client != null) {
            client.setScreen(new SavedBuildsScreen(this));
        }
    }

    private void importWynnBuilderLink() {
        if (client == null) return;
        noticeSource = NoticeSource.IMPORT;
        try {
            WynnBuilderLinkImporter.Result imported = WynnBuilderLinkImporter.importLink(client.keyboard.getClipboard());
            loadBuild(imported.build());
            abilityTreeNotices = imported.warnings();
            AbilityTreeDefinition definition = AbilityTreeDatabase.getInstance().get(selectedClass);
            abilityTreeNoticeNodeIds = definition == null
                    ? Set.of() : nodeIdsMentionedInWarnings(definition, abilityTreeNotices);
            notice = "";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            setAbilityTreeWarning("Import failed: " + exception.getMessage());
            notice = "";
        }
    }

    private void openInWynnBuilder() {
        noticeSource = NoticeSource.BROWSER;
        try {
            EncodedBuild encoded = encodeCurrentBuild();
            LinkUtils.openLink("https://wynnbuilder.github.io/builder/#" + encoded.hash());
            abilityTreeNotices = encoded.warnings();
            abilityTreeNoticeNodeIds = encoded.warningNodeIds();
            notice = "";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            setAbilityTreeWarning("Could not open build: " + exception.getMessage());
            notice = "";
        }
    }

    private EncodedBuild encodeCurrentBuild() {
        var itemIndex = WynnBuilderItemIndex.getInstance();
        if (!itemIndex.isReady()) throw new IllegalStateException("WynnBuilder item IDs are still loading");
        Map<WynnBuilderBuild.EquipmentSlot, WynnBuilderBuild.Equipment> equipment =
                new EnumMap<>(WynnBuilderBuild.EquipmentSlot.class);
        Map<WynnBuilderBuild.EquipmentSlot, List<WynnBuilderBuild.Powder>> powders =
                new EnumMap<>(WynnBuilderBuild.EquipmentSlot.class);
        for (BuildSlot slot : BuildSlot.values()) {
            WynnBuilderBuild.EquipmentSlot encodedSlot = WynnBuilderBuild.EquipmentSlot.valueOf(slot.name());
            WynnItem item = equipped.get(slot);
            if (item == null) {
                equipment.put(encodedSlot, new WynnBuilderBuild.Equipment.None());
            } else if (item.isCrafted()) {
                var craft = CraftedItemCodec.readCraft(item.craftedCode());
                equipment.put(encodedSlot, new WynnBuilderBuild.Equipment.Crafted(new WynnBuilderCraft(
                        craft.ingredients().stream().mapToInt(Integer::intValue).toArray(), craft.recipeId(),
                        craft.material1(), craft.material2(), craft.speed())));
            } else {
                Integer itemId = itemIndex.id(item.displayName());
                if (itemId == null) throw new IllegalStateException("No WynnBuilder ID for " + item.displayName());
                equipment.put(encodedSlot, new WynnBuilderBuild.Equipment.Normal(itemId));
            }
            powders.put(encodedSlot, parsePowders(powderCodes.getOrDefault(slot, "")));
        }
        List<String> tomeSlots = List.of("weaponTome1", "weaponTome2", "armorTome1", "armorTome2",
                "armorTome3", "armorTome4", "gatherXpTome1", "gatherXpTome2", "dungeonXpTome1",
                "dungeonXpTome2", "mobXpTome1", "mobXpTome2", "guildTome1", "lootrunTome1");
        List<Integer> tomes = tomeSlots.stream()
                .map(slot -> selectedTomes.containsKey(slot) ? selectedTomes.get(slot).id() : null).toList();
        List<WynnBuilderBuild.Aspect> aspects = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            AspectSelection selection = selectedAspects.get("aspect" + (i + 1));
            aspects.add(selection == null ? null
                    : new WynnBuilderBuild.Aspect(selection.aspect().id(), selection.tier()));
        }
        CharacterClass abilityClass = selectedAbilityClass();
        String tree = "";
        List<String> warnings = new ArrayList<>();
        Set<String> warningNodeIds = new LinkedHashSet<>();
        if (abilityClass != null && !abilityTreeState.selected(abilityClass).isEmpty()) {
            AbilityTreeDefinition definition = AbilityTreeDatabase.getInstance().get(abilityClass);
            if (definition == null) throw new IllegalStateException("Ability tree is still loading");
            WynnBuilderTreeCodec.EncodeResult encodedTree = WynnBuilderTreeCodec.encodeBestEffort(
                    definition, abilityTreeState.selected(abilityClass));
            tree = encodedTree.hash();
            if (!encodedTree.skippedAbilities().isEmpty()) {
                warnings.add("Skipped incompatible abilities: "
                        + String.join(", ", encodedTree.skippedAbilities()) + ".");
                warningNodeIds.addAll(skippedAbilityIds(encodedTree));
            }
        }
        Integer[] skills = Arrays.stream(assignedSkills).boxed().toArray(Integer[]::new);
        WynnBuilderBuild build = new WynnBuilderBuild(
                WynnBuilderVersions.latestIndex(), equipment, powders, tomes, skills, level, aspects,
                new WynnBuilderBuild.BitPayload(tree, tree.length() * 6));
        String hash = equipment.get(WynnBuilderBuild.EquipmentSlot.WEAPON) instanceof WynnBuilderBuild.Equipment.None
                ? WynnBuilderLegacyBuildCodec.encodeWithoutWeapon(build)
                : WynnBuilderBuildCodec.encode(build);
        return new EncodedBuild(hash, List.copyOf(warnings), Set.copyOf(warningNodeIds));
    }

    private static List<WynnBuilderBuild.Powder> parsePowders(String code) {
        if (code == null || code.isBlank()) return List.of();
        if (!code.matches("(?i)([etwfa][1-7])+")) throw new IllegalArgumentException("Invalid powder code");
        List<WynnBuilderBuild.Powder> result = new ArrayList<>();
        for (int i = 0; i < code.length(); i += 2) {
            int element = "etwfa".indexOf(Character.toLowerCase(code.charAt(i)));
            result.add(new WynnBuilderBuild.Powder(element, code.charAt(i + 1) - '0'));
        }
        return List.copyOf(result);
    }

    boolean saveBuild(String name, CharacterClass abilityClass) {
        boolean saved = SavedBuildManager.getInstance().save(snapshotBuild(name, abilityClass, false));
        if (saved) {
            tabName = name;
            saveWorkspace();
        }
        return saved;
    }

    private SavedBuild snapshotBuild(String name, CharacterClass abilityClass, boolean draft) {
        Map<String, String> items = new LinkedHashMap<>();
        for (Map.Entry<BuildSlot, WynnItem> entry : equipped.entrySet()) {
            items.put(entry.getKey().name(), entry.getValue().reference());
        }
        Map<String, String> powders = new LinkedHashMap<>();
        for (Map.Entry<BuildSlot, String> entry : powderCodes.entrySet()) {
            if (entry.getValue().matches(draft ? "(?i)([etwfa][1-7])*[etwfa]?" : "(?i)([etwfa][1-7])*")) {
                powders.put(entry.getKey().name(), entry.getValue().toLowerCase(Locale.ROOT));
            }
        }
        return new SavedBuild(
                name,
                abilityClass.apiName(),
                level,
                items,
                powders,
                offhands.stream().map(offhand -> new SavedBuild.SavedOffhand(
                        offhand.item().reference(),
                        offhand.powders().matches(draft ? "(?i)([etwfa][1-7])*[etwfa]?" : "(?i)([etwfa][1-7])*")
                                ? offhand.powders() : "")).toList(),
                assignedSkills,
                selectedTomes.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().displayName())),
                selectedAspects.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new SavedBuild.SavedAspect(
                                entry.getValue().aspect().displayName(), entry.getValue().tier()))),
                abilityTreeState.snapshot(),
                draft ? 0 : System.currentTimeMillis());
    }

    void loadBuild(SavedBuild build) {
        pendingBuild = build;
        tabName = build.name();
        restorePendingBuild();
        saveWorkspace();
        rebuildWidgets();
    }

    private void applyBuild(SavedBuild build) {
        abilityTreeNotices = List.of();
        abilityTreeNoticeNodeIds = Set.of();
        equipped.clear();
        powderCodes.clear();
        for (Map.Entry<String, String> entry : build.equipment().entrySet()) {
            try {
                BuildSlot slot = BuildSlot.valueOf(entry.getKey());
                WynnItem item = ItemDatabase.getInstance().getItem(entry.getValue());
                if (item != null) {
                    equipped.put(slot, item);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore slots removed by a future build-planner version.
            }
        }
        for (Map.Entry<String, String> entry : build.powders().entrySet()) {
            try {
                powderCodes.put(BuildSlot.valueOf(entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException ignored) {
                // Ignore slots removed by a future build-planner version.
            }
        }
        selectedTomes.clear();
        for (Map.Entry<String, String> entry : build.tomes().entrySet()) {
            WynnTome tome = TomeDatabase.getInstance().get(entry.getValue());
            if (tome != null && TomeScreen.accepts(entry.getKey(), tome)) {
                selectedTomes.put(entry.getKey(), tome);
            }
        }
        selectedAspects.clear();
        WynnItem loadedWeapon = equipped.get(BuildSlot.WEAPON);
        CharacterClass loadedClass = loadedWeapon == null
                ? classForName(build.characterClass())
                : CharacterClass.fromWeaponSubtype(loadedWeapon.subType());
        selectedClass = loadedClass;
        offhands.clear();
        for (SavedBuild.SavedOffhand savedOffhand : build.offhands()) {
            WynnItem item = ItemDatabase.getInstance().getItem(savedOffhand.item());
            if (item != null && "weapon".equals(item.type())
                    && CharacterClass.fromWeaponSubtype(item.subType()) == loadedClass
                    && !hasDuplicateOffhand(item, -1)) {
                int powderSlots = item.stat("powderSlots");
                String powders = validPowderText(savedOffhand.powders(), powderSlots)
                        ? savedOffhand.powders() : "";
                offhands.add(new Offhand(item, powders));
            }
        }
        AbilityTreeDatabase.getInstance().loadAsync(loadedClass);
        for (Map.Entry<String, SavedBuild.SavedAspect> entry : build.aspects().entrySet()) {
            WynnAspect aspect = AspectDatabase.getInstance().get(
                    loadedClass, entry.getValue().name());
            if (aspect != null && TomeScreen.validAspectSlot(entry.getKey())) {
                selectAspect(entry.getKey(), new AspectSelection(aspect, entry.getValue().tier()));
            }
        }
        level = build.level();
        skillPointPlan = BuildCalculator.optimizeSkillPoints(
                level, equippedItems(), new ArrayList<>(selectedTomes.values()));
        int[] savedSkills = build.assignedSkills();
        System.arraycopy(savedSkills, 0, assignedSkills, 0, assignedSkills.length);
        abilityTreeState.restore(build.abilityTrees());
        closePicker();
        detailItem = null;
        summaryScroll.jump(0.0F);
        damageScroll.jump(0.0F);
        expandedSpell = -1;
        damageCacheKey = Integer.MIN_VALUE;
        statsCache = null;
        iconCache.clear();
        offhandStatsCache.clear();
        damageCards.clear();
        detailed = false;
        boostSection = "Ability Boosts";
    }

    private void restorePendingBuild() {
        SavedBuild build = pendingBuild;
        if ((build.equipment().values().stream().anyMatch(value -> !CraftedItemCodec.isReference(value))
                || build.offhands().stream().anyMatch(
                        offhand -> !CraftedItemCodec.isReference(offhand.item())))
                && !ItemDatabase.getInstance().isReady()) {
            restoreError = ItemDatabase.getInstance().isLoading()
                    ? "Loading item data. Your tab is preserved."
                    : "Item data unavailable. Your tab is preserved; reopen after the database loads.";
            return;
        }
        for (Map.Entry<String, String> entry : build.equipment().entrySet()) {
            if (Arrays.stream(BuildSlot.values()).noneMatch(slot -> slot.name().equals(entry.getKey()))
                    || ItemDatabase.getInstance().getItem(entry.getValue()) == null) {
                restoreError = "Cannot restore item: " + entry.getValue() + ". Your tab is preserved.";
                return;
            }
        }
        for (String slot : build.powders().keySet()) {
            if (Arrays.stream(BuildSlot.values()).noneMatch(value -> value.name().equals(slot) && value.powderable)) {
                restoreError = "Cannot restore powder slot: " + slot + ". Your tab is preserved.";
                return;
            }
        }
        for (String abilityClass : build.abilityTrees().keySet()) {
            if (Arrays.stream(CharacterClass.playableValues()).noneMatch(value -> value.apiName().equals(abilityClass))) {
                restoreError = "Cannot restore ability class: " + abilityClass + ". Your tab is preserved.";
                return;
            }
        }
        for (Map.Entry<String, String> entry : build.tomes().entrySet()) {
            WynnTome tome = TomeDatabase.getInstance().get(entry.getValue());
            if (tome == null || !TomeScreen.accepts(entry.getKey(), tome)) {
                restoreError = "Cannot restore tome: " + entry.getValue() + ". Your tab is preserved.";
                return;
            }
        }
        for (Map.Entry<String, SavedBuild.SavedAspect> entry : build.aspects().entrySet()) {
            WynnAspect aspect = AspectDatabase.getInstance().get(
                    classForName(build.characterClass()), entry.getValue().name());
            if (!TomeScreen.validAspectSlot(entry.getKey())
                    || aspect == null || entry.getValue().tier() > aspect.tiers().size()) {
                restoreError = "Cannot restore aspect: " + entry.getValue().name() + ". Your tab is preserved.";
                return;
            }
        }
        applyBuild(build);
        pendingBuild = null;
        restoreError = "";
    }

    void saveWorkspace() {
        if (workspace.find(toolTabId) == null) return;
        if (workspaceInitialized && pendingBuild == null) {
            CharacterClass abilityClass = selectedAbilityClass();
            workspace.updateBuild(toolTabId, snapshotBuild(
                    tabName, abilityClass == null ? CharacterClass.ARCHER : abilityClass, true));
        }
        String error = workspace.error();
        if (!error.isEmpty() && !error.equals(notifiedWorkspaceError) && client != null && client.player != null) {
            client.player.sendMessage(Text.literal(error).formatted(Formatting.RED), false);
        }
        notifiedWorkspaceError = error;
    }

    private SavedBuild historySnapshot() {
        CharacterClass abilityClass = selectedAbilityClass();
        return snapshotBuild(tabName,
                abilityClass == null ? selectedClass : abilityClass, true);
    }

    private void captureHistoryChange() {
        SavedBuild current = historySnapshot();
        if (lastHistoryState == null) {
            lastHistoryState = current;
            return;
        }
        if (sameBuildState(lastHistoryState, current)) return;
        pushHistory(undoHistory, lastHistoryState);
        redoHistory.clear();
        lastHistoryState = current;
    }

    private void undoBuildChange() {
        captureHistoryChange();
        if (undoHistory.isEmpty()) return;
        pushHistory(redoHistory, historySnapshot());
        applyHistoryState(undoHistory.pop());
    }

    private void redoBuildChange() {
        captureHistoryChange();
        if (redoHistory.isEmpty()) return;
        pushHistory(undoHistory, historySnapshot());
        applyHistoryState(redoHistory.pop());
    }

    private void applyHistoryState(SavedBuild state) {
        tabName = state.name();
        applyBuild(state);
        lastHistoryState = historySnapshot();
        notice = "";
        saveWorkspace();
        rebuildWidgets();
    }

    private void pushHistory(Deque<SavedBuild> history, SavedBuild state) {
        history.push(state);
        if (history.size() > 100) history.removeLast();
    }

    private boolean sameBuildState(SavedBuild first, SavedBuild second) {
        return first.name().equals(second.name())
                && first.characterClass().equals(second.characterClass())
                && first.level() == second.level()
                && first.equipment().equals(second.equipment())
                && first.powders().equals(second.powders())
                && first.offhands().equals(second.offhands())
                && Arrays.equals(first.assignedSkills(), second.assignedSkills())
                && first.tomes().equals(second.tomes())
                && first.aspects().equals(second.aspects())
                && first.abilityTrees().equals(second.abilityTrees());
    }

    private void buildTabWidgets(Layout layout) {
        WorkspaceTabs.add(parent, toolTabId, 12, 8, width - 24, this::addDynamic);
        int classWidth = 116;
        int headerY = layout.equipmentTop + PANEL_TOP_INSET;
        int classX = MARGIN + layout.leftWidth - classWidth - 8;
        int resetWidth = 72;
        int resetX = classX - resetWidth - 6;
        int levelFieldWidth = 40;
        int levelFieldX = resetX - levelFieldWidth - 6;
        Text levelLabelText = Text.literal("Level");
        int levelLabelX = levelFieldX - textRenderer.getWidth(levelLabelText) - 6;
        int nameX = MARGIN + 10;
        TextInputWidget nameField = textInput(
                nameX, headerY, levelLabelX - nameX - 6, 20, "Tab name");
        nameField.setMaxLength(64);
        nameField.setInput(tabName);
        nameField.setOnChange(value -> {
            try {
                if (workspace.rename(toolTabId, value)) tabName = value.trim();
            } catch (IllegalArgumentException ignored) {
            }
        });
        addDynamicField(nameField);

        TextWidget levelLabel = new TextWidget(levelLabelText, this.textRenderer);
        levelLabel.setPosition(levelLabelX, headerY + 6);
        dynamicLabels.add(this.addDrawableChild(levelLabel));
        TextInputWidget levelField = textInput(levelFieldX, headerY, levelFieldWidth, 20, "Level");
        levelField.setMaxLength(3);
        levelField.setCharacterFilter(Character::isDigit);
        levelField.setInput(Integer.toString(level));
        levelField.setOnChange(text -> {
            if (!text.isEmpty()) {
                int newLevel = Math.clamp(Integer.parseInt(text), 1, 121);
                if (newLevel < level) resetAbilityTrees();
                level = newLevel;
                invalidateStats();
                skillPointPlan = BuildCalculator.optimizeSkillPoints(
                        level, equippedItems(), new ArrayList<>(selectedTomes.values()));
                skillPointError = "";
                skillPointSuccess = "";
            }
        });
        addDynamicField(levelField);
        addDynamic(new ThemedButton(
                resetX, headerY, resetWidth, 20, Text.literal("Reset Build"), this::resetBuild)
                .setHoverText(Text.of("You can always use ctrl + z to undo, in case you clicked this accidentally")));

        classSelector = new ClassSelectorWidget(
                classX, headerY, classWidth, 20);
        addRootWidget(classSelector);
    }

    private void resetBuild() {
        equipped.clear();
        powderCodes.clear();
        offhands.clear();
        selectedTomes.clear();
        selectedAspects.clear();
        resetAbilityTrees();
        for (int i = 0; i < assignedSkills.length; i++) assignedSkills[i] = 0;
        invalidateStats();
        skillPointPlan = BuildCalculator.optimizeSkillPoints(level, List.of());
        skillPointError = "";
        skillPointSuccess = "";
        tabName = workspace.defaultBuildName(toolTabId);
        saveWorkspace();
        rebuildWidgets();
    }

    String toolTabId() { return toolTabId; }
    Screen workspaceParent() { return parent; }
    String crafterType(String slot, WynnItem current) {
        if (slot.startsWith("RING_")) return "ring";
        if (slot.equals("WEAPON") || slot.startsWith("OFFHAND_")) {
            return current == null ? selectedClass.weaponSubtype() : current.subType();
        }
        return slot.toLowerCase(Locale.ROOT);
    }
    void equipFromTool(String slot, WynnItem item) {
        if (pendingBuild != null) throw new IllegalStateException("Restore the destination build's missing data before equipping.");
        if (slot.startsWith("OFFHAND_")) {
            equipOffhand(Integer.parseInt(slot.substring("OFFHAND_".length())) - 1, item);
        } else {
            equipItem(BuildSlot.valueOf(slot), item);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (pendingBuild == null) captureHistoryChange();
        if (++workspaceTicks % 40 == 0) {
            if (pendingBuild != null) {
                restorePendingBuild();
                if (pendingBuild == null) {
                    rebuildWidgets();
                }
            }
            saveWorkspace();
        }
    }

    @Override
    public void removed() {
        saveWorkspace();
        super.removed();
    }

    private void resetAbilityTrees() {
        for (CharacterClass abilityClass : CharacterClass.playableValues()) {
            abilityTreeState.reset(abilityClass);
        }
    }

    private void addDynamic(ThemedButton button) {
        dynamicButtons.add(this.addDrawableChild(button));
    }

    private void addPicker(ThemedButton button) {
        pickerButtons.add(button);
        addDynamic(button);
    }

    private void addDynamicField(TextInputWidget field) {
        dynamicFields.add(field);
        addRootWidget(field);
    }

    private TextInputWidget textInput(int x, int y, int width, int height, String placeholder) {
        TextInputWidget field = new TextInputWidget(
                x * 2, y * 2, width * 2, height * 2, 8, 10, 2.0F);
        field.setPlaceholder(placeholder);
        field.setBackgroundColor(CustomColor.fromInt(UiTheme.SURFACE));
        field.setFocusedColor(CustomColor.fromInt(UiTheme.hoverBackground()));
        field.setTextColor(CustomColor.fromInt(UiTheme.TEXT));
        field.setPlaceholderColor(CustomColor.fromInt(UiTheme.MUTED));
        field.setCursorColor(CustomColor.fromInt(UiTheme.accent()));
        return field;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double logicalX = logicalCoordinate(mouseX);
        double logicalY = logicalCoordinate(mouseY);
        if (pendingBuild != null) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (detailItem != null) {
            detailScroll.move(
                    (float) -Math.signum(verticalAmount) * 18.0F,
                    0.0F,
                    detailMaxScroll);
            return true;
        }
        if (pickerOpen()) {
            if (pickerList != null && pickerList.containsLogical(logicalX, logicalY)) {
                pickerList.scroll(verticalAmount);
            }
            return true;
        }
        Layout layout = layout();
        if (WorkspaceTabs.scroll(parent, toolTabId, 12, 8, width - 24,
                logicalX, logicalY, horizontalAmount, verticalAmount)) return true;
        if (abilityPresetOverlay != AbilityPresetOverlay.NONE) {
            if (abilityPresetOverlay == AbilityPresetOverlay.SAVED) {
                AbilityPresetPanel panel = abilityPresetPanel();
                if (logicalX >= panel.x() && logicalX < panel.x() + panel.width()
                        && logicalY >= panel.y() && logicalY < panel.y() + panel.height()) {
                    int visibleRows = Math.max(1, (panel.height() - 82) / 24);
                    int maxScroll = Math.max(0, visibleAbilityPresets.size() - visibleRows);
                    int next = Math.clamp(
                            abilityPresetScroll - (int) Math.signum(verticalAmount),
                            0, maxScroll);
                    if (next != abilityPresetScroll) {
                        abilityPresetScroll = next;
                        rebuildWidgets();
                    }
                }
            }
            return true;
        }
        if (abilityTreeVisible && isInAbilityTreeSidebar(layout, logicalX, logicalY)) {
            abilitySidebarScroll.move(
                    (float) -Math.signum(verticalAmount) * 24.0F,
                    0.0F,
                    abilitySidebarMaxScroll);
            return true;
        }
        if (abilityTreeVisible && isInAbilityTreeArea(layout, logicalX, logicalY)) {
            AbilityTreeDefinition definition = AbilityTreeDatabase.getInstance().get(selectedClass);
            if (definition != null) {
                int viewportHeight = abilityTreeBottom(layout) - abilityTreeTop(layout);
                int contentHeight = definition.maxY() * 40 + 24;
                int maxScroll = Math.max(0, contentHeight - viewportHeight);
                abilityTreeScroll.move(
                        (float) -Math.signum(verticalAmount) * 36.0F,
                        0.0F,
                        maxScroll);
            }
            return true;
        }
        if (logicalX >= layout.middleX && logicalX < layout.middleX + layout.middleWidth
                && logicalY >= layout.equipmentTop + 30 + VIEW_SWITCH_OFFSET
                && logicalY < this.height - MARGIN - 28) {
            summaryScroll.move(
                    (float) -Math.signum(verticalAmount) * 18.0F,
                    0.0F,
                    summaryMaxScroll);
            return true;
        }
        if (logicalX >= layout.rightX && logicalX < layout.rightX + layout.rightWidth
                && logicalY >= MARGIN
                && logicalY < this.height - MARGIN - 28) {
            damageScroll.move(
                    (float) -Math.signum(verticalAmount) * 24.0F,
                    0.0F,
                    damageMaxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Click logical = logicalClick(click);
        clearUiFocus();
        if (pendingBuild != null) {
            return super.mouseClicked(click, doubled);
        }
        if (detailItem != null) {
            detailItem = null;
            detailScroll.jump(0.0F);
            return true;
        }
        if (abilityPresetOverlay != AbilityPresetOverlay.NONE) {
            return super.mouseClicked(click, doubled);
        }
        if (pickerOpen() && logical.button() == 0) {
            int pickerWidth = Math.min(520, this.width - 60);
            int pickerHeight = Math.min(326, this.height - 50);
            int left = (this.width - pickerWidth) / 2;
            int top = (this.height - pickerHeight) / 2;
            if (logical.x() < left || logical.x() >= left + pickerWidth
                    || logical.y() < top || logical.y() >= top + pickerHeight) {
                closePicker();
                rebuildWidgets();
                return true;
            }
        }
        if (!pickerOpen() && logical.button() == 0) {
            if (abilityTreeVisible
                    && (abilityTreeScrollbar.mouseClicked(logical.x(), logical.y(), logical.button())
                    || abilitySidebarScrollbar.mouseClicked(logical.x(), logical.y(), logical.button()))) return true;
            if (!abilityTreeVisible
                    && (summaryScrollbar.mouseClicked(logical.x(), logical.y(), logical.button())
                    || damageScrollbar.mouseClicked(logical.x(), logical.y(), logical.button()))) return true;
            Layout layout = layout();
            for (BuildSlot slot : BuildSlot.values()) {
                SlotPosition position = slotPosition(layout, slot);
                if (logical.x() >= position.x + 4 && logical.x() < position.x + 46
                        && logical.y() >= position.y + 3 && logical.y() < position.y + 44) {
                    openPicker(slot);
                    return true;
                }
            }
            for (int i = 0; i < offhands.size(); i++) {
                SlotPosition position = offhandPosition(layout, i);
                if (logical.x() >= position.x + 4 && logical.x() < position.x + 46
                        && logical.y() >= position.y + 3 && logical.y() < position.y + 44) {
                    openOffhandPicker(i);
                    return true;
                }
            }
            if (!abilityTreeVisible) {
                for (DamageCardBounds card : damageCards) {
                    if (logical.x() >= card.left() && logical.x() < card.right()
                            && logical.y() >= card.top() && logical.y() < card.bottom()) {
                        expandedSpell = expandedSpell == card.spell() ? -1 : card.spell();
                        damageScroll.jump(0.0F);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        Click logical = logicalClick(click);
        if (abilityTreeVisible && (abilityTreeScrollbar.mouseDragged(
                logical.x(), logical.y(), logical.button(), deltaX / matrixScale, deltaY / matrixScale)
                || abilitySidebarScrollbar.mouseDragged(
                logical.x(), logical.y(), logical.button(), deltaX / matrixScale, deltaY / matrixScale))) return true;
        if (!abilityTreeVisible && (summaryScrollbar.mouseDragged(logical.x(), logical.y(), logical.button(),
                deltaX / matrixScale, deltaY / matrixScale)
                || damageScrollbar.mouseDragged(logical.x(), logical.y(), logical.button(),
                deltaX / matrixScale, deltaY / matrixScale))) return true;
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        Click logical = logicalClick(click);
        if (abilityTreeVisible
                && (abilityTreeScrollbar.mouseReleased(logical.x(), logical.y(), logical.button())
                || abilitySidebarScrollbar.mouseReleased(logical.x(), logical.y(), logical.button()))) return true;
        if (!abilityTreeVisible
                && (summaryScrollbar.mouseReleased(logical.x(), logical.y(), logical.button())
                || damageScrollbar.mouseReleased(logical.x(), logical.y(), logical.button()))) return true;
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        boolean control = (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
        if (control && matchesPrintableKey(input, GLFW.GLFW_KEY_Z, "z")) {
            undoBuildChange();
            return true;
        }
        if (control && matchesPrintableKey(input, GLFW.GLFW_KEY_Y, "y")) {
            redoBuildChange();
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
            for (TextInputWidget skillField : skillFields) {
                if (!skillField.isFocused()) continue;
                skillField.setFocused(false);
                return true;
            }
        }
        if (abilityPresetOverlay != AbilityPresetOverlay.NONE
                && input.key() == GLFW.GLFW_KEY_ESCAPE) {
            closeAbilityPresetOverlay();
            return true;
        }
        if (pickerOpen()) {
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                if (pickerList != null) pickerList.chooseFirst();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                closePicker();
                rebuildWidgets();
                return true;
            }
        }
        return super.keyPressed(input);
    }

    private static boolean matchesPrintableKey(KeyInput input, int fallbackKey, String name) {
        String keyName = GLFW.glfwGetKeyName(input.key(), input.scancode());
        return keyName == null ? input.key() == fallbackKey : keyName.equalsIgnoreCase(name);
    }

    @Override
    protected void drawPlannerContent(DrawContext context, int mouseX, int mouseY, float delta) {
        for (TextInputWidget field : dynamicFields) field.setVisible(detailItem == null);
        ui.fill(0, 0, width, height, UiTheme.BACKGROUND);
        if (pendingBuild != null) {
            UiTheme.drawPanel(ui, MARGIN, MARGIN, this.width - MARGIN * 2, this.height - MARGIN * 2);
            ui.drawCenteredTextWithShadow(textRenderer,
                    fitText(restoreError, this.width - 40), this.width / 2, this.height / 2, 0xFFFFAA55);
            renderVanillaChildren(context, mouseX, mouseY, delta);
            renderWorkspaceError(context);
            return;
        }
        Layout layout = layout();
        BuildCalculator.Result stats = calculateStats();
        renderPanels(context, layout);
        renderEquipment(context, layout);
        renderSkills(context, layout, stats);
        if (abilityTreeVisible) {
            renderAbilityTree(context, layout, mouseX, mouseY, delta);
        } else {
            renderSummary(context, layout, stats, mouseX, mouseY, delta);
            renderDamage(context, layout, stats, mouseX, mouseY, delta);
        }
        renderBoostArea(context, layout, stats);
        renderVanillaChildren(context, mouseX, mouseY, delta);
        if (detailItem == null) {
            renderSkillDetails(context, layout, stats);
            renderEquipmentTooltip(context, layout, mouseX, mouseY);
        }
        if (detailItem != null) {
            renderItemDetails(context, delta);
        }
        renderWorkspaceError(context);
        renderNotice(layout);
    }

    @Override
    protected void drawPlannerForeground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (pickerOpen()) {
            for (ThemedButton button : pickerButtons) button.render(context, mouseX, mouseY, delta);
            return;
        }
        if (abilityPresetOverlay != AbilityPresetOverlay.NONE) {
            for (ThemedButton button : abilityPresetButtons) {
                button.render(context, mouseX, mouseY, delta);
            }
            return;
        }
        if (abilityTreeVisible && abilityTreeWidget != null) {
            renderAbilityTreeStatus();
            if (hoveredArchetype != null) renderArchetypeTooltip(mouseX, mouseY);
            else renderAbilityTreeTooltip(mouseX, mouseY);
        }
    }

    private void renderArchetypeTooltip(int mouseX, int mouseY) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal(hoveredArchetype.name() + " Archetype")
                .formatted(Formatting.BOLD)
                .styled(style -> style.withColor(hoveredArchetype.color())));
        if (!hoveredArchetype.description().isBlank()) {
            lines.add(Text.empty());
            String description = hoveredArchetype.description().trim();
            int qualifierStart = description.lastIndexOf(" (");
            String main = qualifierStart < 0 ? description : description.substring(0, qualifierStart).trim();
            String qualifier = qualifierStart < 0 ? "" : description.substring(qualifierStart + 1).trim();
            lines.addAll(wrapTooltipText(main, 220, 0xFFDDDDDD));
            lines.addAll(wrapTooltipText(qualifier, 220, 0xFF666666));
        }
        renderPlannerTooltip(drawContext, lines, mouseX, mouseY);
    }

    private List<Text> wrapTooltipText(String value, int width, int color) {
        if (value.isBlank()) return List.of();
        List<Text> result = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && textRenderer.getWidth(candidate) > width) {
                result.add(Text.literal(line.toString()).styled(style -> style.withColor(color)));
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) {
            result.add(Text.literal(line.toString()).styled(style -> style.withColor(color)));
        }
        return List.copyOf(result);
    }

    private void renderAbilityTreeStatus() {
        if (abilityTreeStatus.isEmpty()
                || System.currentTimeMillis() >= abilityTreeStatusUntil) return;
        Layout layout = layout();
        int left = layout.middleX + 4;
        int right = layout.rightX + layout.rightWidth - 4;
        ui.drawCenteredTextWithShadow(
                textRenderer,
                abilityTreeStatus,
                left + (right - left) / 2,
                abilityTreeBottom(layout) - 13,
                abilityTreeStatus.contains("copied")
                        || abilityTreeStatus.contains("imported")
                        || abilityTreeStatus.contains("reset")
                        ? 0xFF55FF55 : 0xFFFF5555);
    }

    private void renderAbilityTreeTooltip(int mouseX, int mouseY) {
        AbilityMapData.Node hovered = abilityTreeWidget.getCurrentHoveredNode();
        AbilityTreeDefinition definition = AbilityTreeDatabase.getInstance().get(selectedClass);
        if (hovered == null || hovered.meta == null || definition == null) return;
        AbilityTreeDefinition.Node node = definition.nodes().get(hovered.meta.id);
        if (node == null) return;

        List<Text> lines = abilityTooltipLines(definition, node);
        renderPlannerTooltip(drawContext, lines, mouseX, mouseY);
    }

    private List<Text> abilityTooltipLines(
            AbilityTreeDefinition definition,
            AbilityTreeDefinition.Node node
    ) {
        List<Text> lines = new ArrayList<>();
        List<Text> name = WynncraftApiHandler.parseStyledHtml(List.of(node.formattedName()));
        lines.add(name.isEmpty()
                ? Text.literal(node.name()).formatted(Formatting.BOLD)
                        .styled(style -> style.withColor(node.color()))
                : name.getFirst());
        lines.addAll(WynncraftApiHandler.parseStyledHtml(abilityDescriptionLines(node)));

        AbilityTreeDefinition.Archetype archetype = definition.archetypes().get(node.archetype());
        if (archetype != null) {
            lines.add(Text.empty());
            lines.add(Text.literal(archetype.name() + " Archetype")
                    .formatted(Formatting.BOLD)
                    .styled(style -> style.withColor(archetype.color())));
        }

        lines.add(Text.empty());
        Set<String> selected = abilityTreeState.selected(selectedClass);
        int remaining = abilityTreeState.remaining(definition, level);
        boolean alreadySelected = selected.contains(node.id());
        lines.add(abilityRequirementLine(
                alreadySelected || remaining >= node.abilityPointCost(),
                "Ability Points: ", Integer.toString(node.abilityPointCost())));
        for (String requiredId : node.requiredNodes()) {
            AbilityTreeDefinition.Node required = definition.nodes().get(requiredId);
            lines.add(abilityRequirementLine(
                    selected.contains(requiredId),
                    "Required Ability: ",
                    required == null ? requiredId : required.name()));
        }
        if (node.archetypeRequirement() != null) {
            AbilityTreeDefinition.ArchetypeRequirement requirement = node.archetypeRequirement();
            AbilityTreeDefinition.Archetype requiredArchetype =
                    definition.archetypes().get(requirement.archetype());
            int current = abilityTreeState.archetypeCounts(definition)
                    .getOrDefault(requirement.archetype(), 0);
            lines.add(abilityArchetypeRequirementLine(
                    current >= requirement.amount(),
                    "Min " + (requiredArchetype == null
                            ? requirement.archetype() : requiredArchetype.name()) + " Archetype: ",
                    current, requirement.amount()));
        }
        if (node.combatLevel() > 0) {
            lines.add(abilityRequirementLine(
                    level >= node.combatLevel(),
                    "Combat Lv. min: ", Integer.toString(node.combatLevel())));
        }
        for (String lockedId : node.locks()) {
            AbilityTreeDefinition.Node locked = definition.nodes().get(lockedId);
            lines.add(Text.literal("\u2716 ").formatted(Formatting.RED)
                    .append(Text.literal("Blocks Ability: ").formatted(Formatting.GRAY))
                    .append(Text.literal(locked == null ? lockedId : locked.name())
                            .formatted(Formatting.WHITE)));
        }
        return lines;
    }

    private List<String> abilityDescriptionLines(AbilityTreeDefinition.Node node) {
        List<String> result = new ArrayList<>();
        for (String formattedLine : node.formattedDescription()) {
            String plain = WynncraftApiHandler.stripHtml(formattedLine).trim();
            if (plain.startsWith("Ability Points:")
                    || plain.startsWith("Required Ability:")
                    || plain.startsWith("Archetype Requirement:")
                    || (plain.startsWith("Min ") && plain.contains(" Archetype:"))
                    || plain.startsWith("Combat Lv")
                    || plain.startsWith("Unlocking will block:")
                    || (!node.locks().isEmpty() && plain.startsWith("- "))
                    || plain.endsWith(" Archetype")) {
                continue;
            }
            if (plain.isEmpty()) {
                if (!result.isEmpty() && !result.getLast().isEmpty()) result.add("");
            } else {
                result.add(formattedLine);
            }
        }
        while (!result.isEmpty() && result.getLast().isEmpty()) result.removeLast();
        return result;
    }

    private MutableText abilityRequirementLine(boolean fulfilled, String label, String value) {
        return Text.literal(fulfilled ? "\u2714 " : "\u2716 ")
                .formatted(fulfilled ? Formatting.GREEN : Formatting.RED)
                .append(Text.literal(label).formatted(Formatting.GRAY))
                .append(Text.literal(value).formatted(Formatting.WHITE));
    }

    private MutableText abilityArchetypeRequirementLine(
            boolean fulfilled,
            String label,
            int current,
            int required
    ) {
        return Text.literal(fulfilled ? "\u2714 " : "\u2716 ")
                .formatted(fulfilled ? Formatting.GREEN : Formatting.RED)
                .append(Text.literal(label).formatted(Formatting.GRAY))
                .append(Text.literal(Integer.toString(current)).formatted(Formatting.WHITE))
                .append(Text.literal("/" + required).formatted(Formatting.GRAY));
    }

    private void renderWorkspaceError(DrawContext context) {
        if (!workspace.error().isEmpty()) {
            ui.fill(0, this.height - 12, this.width, this.height, 0xEE201010);
            ui.drawCenteredTextWithShadow(textRenderer, fitText(workspace.error(), this.width - 12),
                    this.width / 2, this.height - 10, 0xFFFF5555);
        }
    }

    private void renderNotice(Layout layout) {
        if (!notice.isBlank()) {
            int left = layout.middleX + 8;
            int width = layout.rightX + layout.rightWidth - left - 8;
            List<net.minecraft.text.OrderedText> lines = textRenderer.wrapLines(
                    Text.literal(notice), width - 12);
            int visibleLines = Math.min(4, lines.size());
            int height = visibleLines * 11 + 8;
            int top = this.height - MARGIN - 29 - height;
            UiTheme.drawSquarePanel(ui, left, top, width, height);
            for (int i = 0; i < visibleLines; i++) {
                ui.drawText(textRenderer, lines.get(i), left + 6, top + 5 + i * 11,
                        0xFFFFAA55, true);
            }
        }
    }

    private enum NoticeSource {
        IMPORT,
        BROWSER
    }

    private void renderPanels(DrawContext context, Layout layout) {
        UiTheme.drawPanel(ui, MARGIN, layout.equipmentTop, layout.leftWidth, layout.equipmentHeight);
        UiTheme.drawPanel(ui, MARGIN, layout.skillTop, layout.leftWidth, layout.skillHeight);
        UiTheme.drawPanel(ui, MARGIN, layout.boostTop, layout.leftWidth, layout.boostHeight);
        if (abilityTreeVisible) {
            AbilityTreeLayout treeLayout = abilityTreeLayout(layout);
            UiTheme.drawPanel(ui, treeLayout.treeLeft(), layout.equipmentTop,
                    treeLayout.treeWidth(),
                    this.height - MARGIN - layout.equipmentTop);
            UiTheme.drawPanel(ui, treeLayout.sidebarX(), layout.equipmentTop,
                    treeLayout.sidebarWidth(),
                    this.height - MARGIN - layout.equipmentTop);
        } else {
            UiTheme.drawPanel(ui, layout.middleX, layout.equipmentTop, layout.middleWidth,
                    this.height - MARGIN - layout.equipmentTop);
            UiTheme.drawPanel(ui, layout.rightX, layout.equipmentTop, layout.rightWidth,
                    this.height - MARGIN - layout.equipmentTop);
        }
    }

    private void renderAbilityTree(
            DrawContext context,
            Layout layout,
            int mouseX,
            int mouseY,
            float delta
    ) {
        AbilityTreeLayout treeLayout = abilityTreeLayout(layout);
        int left = treeLayout.treeLeft() + 4;
        int right = treeLayout.treeLeft() + treeLayout.treeWidth() - 10;
        int top = abilityTreeTop(layout);
        int bottom = abilityTreeBottom(layout);
        int width = right - left;
        AbilityTreeDefinition definition = AbilityTreeDatabase.getInstance().get(selectedClass);
        AbilityMapData classMap = AbilityTreeCache.getClassMap(selectedClass.apiName());

        ui.fill(left, top, right, bottom, 0xFF383A45);
        ui.enableScissor(left, top, right, bottom);
        ui.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                TREE_BACKGROUND,
                left - 24, top - 24,
                315, 23,
                width + 48, bottom - top + 48,
                274, 174,
                590, 220);
        ui.disableScissor();

        renderAbilityTreeSidebar(context, layout, treeLayout, definition, mouseX, mouseY, delta);

        if (abilityPresetOverlay != AbilityPresetOverlay.NONE) {
            abilityTreeWidget.setVisible(false);
            return;
        }

        if (definition == null || classMap == null) {
            String error = AbilityTreeDatabase.getInstance().error(selectedClass);
            ui.drawCenteredTextWithShadow(
                    textRenderer,
                    error.isEmpty() ? "Loading live Wynncraft ability tree..." : error,
                    left + width / 2,
                    (top + bottom) / 2,
                    error.isEmpty() ? 0xFFAAAAAA : 0xFFFF5555);
            abilityTreeWidget.setVisible(false);
            return;
        }

        int treeTop = top;
        int viewportHeight = bottom - treeTop;
        int contentHeight = definition.maxY() * 40 + 24;
        abilityTreeMaxScroll = Math.max(0, contentHeight - viewportHeight);
        abilityTreeScroll.clamp(0.0F, abilityTreeMaxScroll);
        int minNodeX = definition.nodes().values().stream()
                .mapToInt(AbilityTreeDefinition.Node::x).min().orElse(5);
        int maxNodeX = definition.nodes().values().stream()
                .mapToInt(AbilityTreeDefinition.Node::x).max().orElse(5);
        int contentCenter = Math.round((minNodeX + maxNodeX) * 37.5F + 987.5F);
        int widgetX = left + right - contentCenter;
        int widgetTopOffset = 30;
        int widgetY = treeTop * 2 - widgetTopOffset;
        int widgetBotLimit = viewportHeight * 2 + widgetTopOffset;
        abilityTreeWidget.setVisible(true);
        abilityTreeWidget.setClassName(selectedClass.apiName());
        abilityTreeWidget.setViewport(
                widgetX, widgetY, width * 2, viewportHeight * 2, widgetBotLimit);
        abilityTreeWidget.setClipBounds(
                left * 2, treeTop * 2, width * 2, viewportHeight * 2);
        if (abilityTreeWidget.classTree != classMap) {
            abilityTreeWidget.setClassTree(classMap);
        }
        abilityTreeWidget.setScrollOffset(Math.round(abilityTreeScroll.update(delta) * 2));
        abilityTreeWidget.setSelectedIds(abilityTreeState.selected(selectedClass));
        abilityTreeWidget.setSearchIds(abilityTreeSearchNodeIds(definition));
        abilityTreeWidget.setSearchActive(!abilityTreeSearch.isBlank());
        Set<String> warningNodeIds = new LinkedHashSet<>(abilityTreeNoticeNodeIds);
        warningNodeIds.addAll(nodeIdsMentionedInWarnings(definition, abilityTreeNotices));
        abilityTreeWidget.setHighlightedIds(
                warningNodeIds,
                abilityTreeErrorNodeIds(
                        definition,
                        abilityTreeState.archetypeCounts(definition),
                        AbilityTreeState.abilityPointsForLevel(level)));
        abilityTreeScrollbar.configure(
                treeLayout.treeLeft() + treeLayout.treeWidth() - 8,
                treeTop - 2,
                viewportHeight + 2,
                abilityTreeMaxScroll);
        abilityTreeScrollbar.draw(context, mouseX, mouseY, delta, ui);

    }

    private void renderAbilityTreeSidebar(
            DrawContext context,
            Layout layout,
            AbilityTreeLayout treeLayout,
            AbilityTreeDefinition definition,
            int mouseX,
            int mouseY,
            float delta
    ) {
        int x = treeLayout.sidebarX() + 8;
        int width = treeLayout.sidebarWidth() - 22;
        int y = layout.equipmentTop + 8;
        hoveredArchetype = null;
        if (definition == null) {
            ui.drawCenteredTextWithShadow(
                    textRenderer, "Loading...", x + width / 2, y, 0xFFAAAAAA);
            return;
        }
        Map<String, Integer> counts = abilityTreeState.archetypeCounts(definition);
        int spent = abilityTreeState.spent(definition);
        int capacity = AbilityTreeState.abilityPointsForLevel(level);
        drawLargeCenteredText(
                Text.literal(spent + "/" + capacity + " AP").formatted(Formatting.BOLD),
                x + width / 2, y, 0xFFFFFFFF);
        y += 18;
        List<AbilityTreeDefinition.Archetype> archetypes = new ArrayList<>(
                definition.archetypes().values());
        int columnWidth = width / Math.max(1, archetypes.size());
        int iconSize = 52;
        for (int i = 0; i < archetypes.size(); i++) {
            AbilityTreeDefinition.Archetype archetype = archetypes.get(i);
            int centerX = x + columnWidth * i + columnWidth / 2;
            int iconX = centerX - iconSize / 2;
            int count = counts.getOrDefault(archetype.id(), 0);
            ArchetypeIcon icon = archetypeIcon(archetype.name());
            ui.drawTexture(RenderPipelines.GUI_TEXTURED, ARCHETYPE_ICONS, iconX, y,
                    icon.u(), icon.v(), iconSize, iconSize, 32, 32, 96, 160);
            if (mouseX >= iconX && mouseX < iconX + iconSize
                    && mouseY >= y && mouseY < y + iconSize) {
                hoveredArchetype = archetype;
            }
            ui.drawCenteredTextWithShadow(textRenderer, count + " pts",
                    centerX, y + iconSize + 1, archetype.color());
            ui.drawCenteredTextWithShadow(textRenderer,
                    fitText(archetype.name(), columnWidth - 4),
                    centerX, y + iconSize + 12, archetype.color());
        }
        y += iconSize + 30;
        ui.drawTextWithShadow(
                textRenderer,
                Text.literal("Active Abilities").formatted(Formatting.BOLD),
                x, y, 0xFFFFFFFF);

        int listTop = y + 14;
        int listBottom = abilityTreeBottom(layout) - 29;
        List<String> warnings = abilityTreeWarnings(definition, counts, capacity);
        List<net.minecraft.text.OrderedText> importWarningLines = new ArrayList<>();
        for (String warning : abilityTreeNotices) {
            importWarningLines.addAll(textRenderer.wrapLines(Text.literal(warning), width));
        }
        List<net.minecraft.text.OrderedText> warningLines = new ArrayList<>();
        for (String warning : warnings) {
            warningLines.addAll(textRenderer.wrapLines(Text.literal(warning), width));
        }
        List<AbilitySidebarEntry> entries = abilitySidebarEntries(definition, width - 10);
        int contentHeight = abilityTreeNotices.isEmpty()
                ? 0 : 15 + importWarningLines.size() * 11 + 5;
        if (!warnings.isEmpty()) contentHeight += 15 + warningLines.size() * 11 + 5;
        if (abilityTreeNotices.isEmpty() && warnings.isEmpty() && entries.isEmpty()) contentHeight = 11;
        for (AbilitySidebarEntry entry : entries) contentHeight += entry.height() + 4;
        abilitySidebarMaxScroll = Math.max(0, contentHeight - (listBottom - listTop));
        abilitySidebarScroll.clamp(0.0F, abilitySidebarMaxScroll);
        int contentY = listTop - Math.round(abilitySidebarScroll.update(delta));
        if (abilityTreeWarningDismissButton != null) {
            abilityTreeWarningDismissButton.setPosition(x + width - 14, contentY - 2);
            abilityTreeWarningDismissButton.visible = !abilityTreeNotices.isEmpty()
                    && abilityPresetOverlay == AbilityPresetOverlay.NONE
                    && !pickerOpen()
                    && contentY >= listTop && contentY + 12 <= listBottom;
        }

        ui.enableScissor(x - 2, listTop, x + width + 2, listBottom);
        if (!abilityTreeNotices.isEmpty()) {
            ui.drawTextWithShadow(textRenderer,
                    Text.literal("ATree Warning!").formatted(Formatting.BOLD),
                    x, contentY, 0xFFFFAA00);
            contentY += 13;
            for (net.minecraft.text.OrderedText warning : importWarningLines) {
                ui.drawText(textRenderer, warning, x, contentY, 0xFFFFCC55, true);
                contentY += 11;
            }
            contentY += 5;
        }
        if (!warnings.isEmpty()) {
            ui.drawTextWithShadow(textRenderer,
                    Text.literal("ATree Error!").formatted(Formatting.BOLD),
                    x, contentY, 0xFFFF5555);
            contentY += 13;
            for (net.minecraft.text.OrderedText warning : warningLines) {
                ui.drawText(textRenderer, warning, x, contentY, 0xFFFF7777, true);
                contentY += 11;
            }
            contentY += 5;
        } else if (entries.isEmpty()) {
            ui.drawTextWithShadow(textRenderer, "No abilities selected", x, contentY, 0xFF777777);
        }
        for (AbilitySidebarEntry entry : entries) {
            int cardY = contentY;
            if (cardY + entry.height() >= listTop && cardY < listBottom) {
                UiTheme.drawRoundedBox(ui, x, cardY, width, entry.height(),
                        UiTheme.SURFACE, entry.node().color());
                ui.drawTextWithShadow(textRenderer,
                        Text.literal(entry.node().name()).formatted(Formatting.BOLD),
                        x + 5, cardY + 4, entry.node().color());
                int textY = cardY + 16;
                for (net.minecraft.text.OrderedText line : entry.lines()) {
                    ui.drawText(textRenderer, line, x + 5, textY, 0xFFAAAAAA, false);
                    textY += 10;
                }
            }
            contentY += entry.height() + 4;
        }
        ui.disableScissor();
        abilitySidebarScrollbar.configure(
                treeLayout.sidebarX() + treeLayout.sidebarWidth() - 8,
                listTop,
                listBottom - listTop,
                abilitySidebarMaxScroll);
        abilitySidebarScrollbar.draw(context, mouseX, mouseY, delta, ui);
    }

    private List<AbilitySidebarEntry> abilitySidebarEntries(
            AbilityTreeDefinition definition,
            int textWidth
    ) {
        Set<String> selected = abilityTreeState.selected(selectedClass);
        int cacheKey = 31 * (31 * System.identityHashCode(definition) + selected.hashCode())
                + textWidth;
        if (cacheKey == abilitySidebarEntriesCacheKey) return abilitySidebarEntriesCache;
        List<AbilitySidebarEntry> result = new ArrayList<>();
        for (AbilityTreeDefinition.Node node : definition.nodes().values()) {
            if (!selected.contains(node.id()) || !node.sidebarAbility()) continue;
            List<net.minecraft.text.OrderedText> wrapped = new ArrayList<>();
            for (Text line : WynncraftApiHandler.parseStyledHtml(abilityDescriptionLines(node))) {
                if (line.getString().isEmpty()) {
                    wrapped.add(Text.empty().asOrderedText());
                } else {
                    wrapped.addAll(textRenderer.wrapLines(line, textWidth));
                }
            }
            result.add(new AbilitySidebarEntry(node, List.copyOf(wrapped),
                    21 + wrapped.size() * 10));
        }
        abilitySidebarEntriesCacheKey = cacheKey;
        abilitySidebarEntriesCache = List.copyOf(result);
        return abilitySidebarEntriesCache;
    }

    private void drawLargeCenteredText(Text text, int x, int y, int color) {
        ui.pushMatrix();
        ui.translate(x, y);
        ui.scale(1.25F, 1.25F);
        ui.drawCenteredTextWithShadow(textRenderer, text, 0, 0, color);
        ui.popMatrix();
    }

    private ArchetypeIcon archetypeIcon(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "boltslinger" -> new ArchetypeIcon(0, 0);
            case "sharpshooter" -> new ArchetypeIcon(32, 0);
            case "trapper" -> new ArchetypeIcon(64, 0);
            case "acrobat" -> new ArchetypeIcon(0, 32);
            case "shadestepper" -> new ArchetypeIcon(32, 32);
            case "trickster" -> new ArchetypeIcon(64, 32);
            case "arcanist" -> new ArchetypeIcon(0, 64);
            case "light bender" -> new ArchetypeIcon(32, 64);
            case "riftwalker" -> new ArchetypeIcon(64, 64);
            case "acolyte" -> new ArchetypeIcon(0, 96);
            case "ritualist" -> new ArchetypeIcon(32, 96);
            case "summoner" -> new ArchetypeIcon(64, 96);
            case "battle monk" -> new ArchetypeIcon(0, 128);
            case "fallen" -> new ArchetypeIcon(32, 128);
            case "paladin" -> new ArchetypeIcon(64, 128);
            default -> new ArchetypeIcon(0, 0);
        };
    }

    private List<String> abilityTreeWarnings(
            AbilityTreeDefinition definition,
            Map<String, Integer> archetypeCounts,
            int capacity
    ) {
        List<String> warnings = new ArrayList<>();
        Set<String> selected = abilityTreeState.selected(selectedClass);
        if (abilityTreeState.spent(definition) > capacity) warnings.add("Too many ability points assigned");
        for (AbilityTreeDefinition.Node node : definition.nodes().values()) {
            if (!selected.contains(node.id())) continue;
            if (node.combatLevel() > level) warnings.add(node.name() + ": requires combat level " + node.combatLevel());
            for (String required : node.requiredNodes()) {
                if (!selected.contains(required)) {
                    AbilityTreeDefinition.Node requiredNode = definition.nodes().get(required);
                    warnings.add(node.name() + ": requires "
                            + (requiredNode == null ? required : requiredNode.name()));
                }
            }
            if (node.archetypeRequirement() != null) {
                AbilityTreeDefinition.ArchetypeRequirement requirement = node.archetypeRequirement();
                int current = archetypeCounts.getOrDefault(requirement.archetype(), 0);
                if (requirement.archetype().equals(node.archetype())) current--;
                if (current < requirement.amount()) {
                    AbilityTreeDefinition.Archetype archetype = definition.archetypes()
                            .get(requirement.archetype());
                    warnings.add(node.name() + ": requires " + requirement.amount() + " "
                            + (archetype == null ? requirement.archetype() : archetype.name())
                            + " points");
                }
            }
            for (String locked : node.locks()) {
                if (selected.contains(locked) && node.id().compareTo(locked) < 0) {
                    AbilityTreeDefinition.Node lockedNode = definition.nodes().get(locked);
                    warnings.add(node.name() + " blocks "
                            + (lockedNode == null ? locked : lockedNode.name()));
                }
            }
        }
        return warnings;
    }

    private Set<String> abilityTreeErrorNodeIds(
            AbilityTreeDefinition definition,
            Map<String, Integer> archetypeCounts,
            int capacity
    ) {
        Set<String> result = new LinkedHashSet<>();
        Set<String> selected = abilityTreeState.selected(selectedClass);
        if (abilityTreeState.spent(definition) > capacity) result.addAll(selected);
        for (AbilityTreeDefinition.Node node : definition.nodes().values()) {
            if (!selected.contains(node.id())) continue;
            if (node.combatLevel() > level) result.add(node.id());
            for (String required : node.requiredNodes()) {
                if (!selected.contains(required)) {
                    result.add(node.id());
                    if (definition.nodes().containsKey(required)) result.add(required);
                }
            }
            if (node.archetypeRequirement() != null) {
                AbilityTreeDefinition.ArchetypeRequirement requirement = node.archetypeRequirement();
                int current = archetypeCounts.getOrDefault(requirement.archetype(), 0);
                if (requirement.archetype().equals(node.archetype())) current--;
                if (current < requirement.amount()) result.add(node.id());
            }
            for (String locked : node.locks()) {
                if (selected.contains(locked)) {
                    result.add(node.id());
                    result.add(locked);
                }
            }
        }
        return Set.copyOf(result);
    }

    private Set<String> skippedAbilityIds(WynnBuilderTreeCodec.EncodeResult encoded) {
        Set<String> result = new LinkedHashSet<>(abilityTreeState.selected(selectedClass));
        result.removeAll(encoded.encoded());
        return Set.copyOf(result);
    }

    private Set<String> abilityTreeSearchNodeIds(AbilityTreeDefinition definition) {
        if (definition == null || abilityTreeSearch.isBlank()) return Set.of();
        String search = abilityTreeSearch.strip().toLowerCase(Locale.ROOT);
        Set<String> result = new LinkedHashSet<>();
        for (AbilityTreeDefinition.Node node : definition.nodes().values()) {
            if (node.name().toLowerCase(Locale.ROOT).contains(search)) result.add(node.id());
        }
        return Set.copyOf(result);
    }

    private Set<String> nodeIdsMentionedInWarnings(
            AbilityTreeDefinition definition,
            List<String> warnings
    ) {
        Set<String> result = new LinkedHashSet<>();
        for (AbilityTreeDefinition.Node node : definition.nodes().values()) {
            for (String warning : warnings) {
                if (warning.contains(node.name())) {
                    result.add(node.id());
                    break;
                }
            }
        }
        return Set.copyOf(result);
    }

    private int abilityTreeTop(Layout layout) {
        return layout.equipmentTop + PANEL_TOP_INSET + VIEW_SWITCH_OFFSET + 47;
    }

    private int abilityTreeBottom(Layout layout) {
        return this.height - MARGIN - 28;
    }

    private boolean isInAbilityTreeArea(Layout layout, double mouseX, double mouseY) {
        AbilityTreeLayout treeLayout = abilityTreeLayout(layout);
        return mouseX >= treeLayout.treeLeft()
                && mouseX < treeLayout.treeLeft() + treeLayout.treeWidth()
                && mouseY >= abilityTreeTop(layout)
                && mouseY < abilityTreeBottom(layout);
    }

    private boolean isInAbilityTreeSidebar(Layout layout, double mouseX, double mouseY) {
        AbilityTreeLayout treeLayout = abilityTreeLayout(layout);
        return mouseX >= treeLayout.sidebarX()
                && mouseX < treeLayout.sidebarX() + treeLayout.sidebarWidth()
                && mouseY >= layout.equipmentTop
                && mouseY < abilityTreeBottom(layout) - 29;
    }

    private AbilityTreeLayout abilityTreeLayout(Layout layout) {
        int areaLeft = layout.middleX;
        int areaRight = layout.rightX + layout.rightWidth;
        int areaWidth = areaRight - areaLeft;
        int sidebarWidth = Math.clamp(areaWidth / 3, 180, 220);
        int gap = GAP;
        int treeWidth = areaWidth - sidebarWidth - gap;
        return new AbilityTreeLayout(
                areaLeft, treeWidth, areaLeft + treeWidth + gap, sidebarWidth);
    }

    private BuilderFooterLayout builderFooterLayout(Layout layout) {
        int buttonX = layout.middleX + 4;
        int buttonRight = layout.rightX + layout.rightWidth - 8;
        int buttonWidth = (buttonRight - buttonX - 16) / 5;
        return new BuilderFooterLayout(buttonX, buttonWidth);
    }

    private void renderEquipment(DrawContext context, Layout layout) {
        for (BuildSlot slot : BuildSlot.values()) {
            SlotPosition position = slotPosition(layout, slot);
            WynnItem item = equipped.get(slot);
            int border = item == null ? UiTheme.BORDER : tierRgb(item.tier());
            UiTheme.drawRoundedBox(
                    ui, position.x + 4, position.y + 3, 42, 41, UiTheme.SURFACE, border);
            if (item == null) {
                ui.drawCenteredTextWithShadow(this.textRenderer,
                        Text.literal(slot.shortLabel), position.x + 25, position.y + 19, 0xAAAAAA);
            }
            if (item != null) {
                drawEquipmentIcon(context, slot, item, position.x + 11, position.y + 9);
                renderSkillRequirementWarning(position, item);
                String powders = powderCodes.getOrDefault(slot, "");
                String primary = slot == BuildSlot.WEAPON
                        ? format(BuildCalculator.weaponBaseDps(item, powders))
                        : format(BuildCalculator.armorHealthWithPowders(item, powders));
                int primaryX = position.x + 51;
                int primaryColor = slot == BuildSlot.WEAPON ? 0xFFFFAA00 : 0xFFFF5555;
                if (slot == BuildSlot.WEAPON) {
                    drawPixelWeapon(context, primaryX, position.y + 9);
                    primaryX += 11;
                } else {
                    drawPixelHeart(context, primaryX, position.y + 11);
                    primaryX += 11;
                }
                ui.drawTextWithShadow(
                        this.textRenderer, primary, primaryX, position.y + 10, primaryColor);
                ui.drawTextWithShadow(this.textRenderer, "Lv. " + item.stat("lvl"),
                        position.x + 51, position.y + 28, 0xFFFFFFFF);
            }
        }
        for (int i = 0; i < offhands.size(); i++) {
            Offhand offhand = offhands.get(i);
            SlotPosition position = offhandPosition(layout, i);
            WynnItem item = offhand.item();
            UiTheme.drawRoundedBox(
                    ui, position.x + 4, position.y + 3, 42, 41,
                    UiTheme.SURFACE, tierRgb(item.tier()));
            drawEquipmentIcon(context, BuildSlot.WEAPON, item, position.x + 11, position.y + 9);
            renderSkillRequirementWarning(position, item);
            int primaryX = position.x + 51;
            drawPixelWeapon(context, primaryX, position.y + 9);
            primaryX += 11;
            ui.drawTextWithShadow(this.textRenderer,
                    format(BuildCalculator.weaponBaseDps(item, offhand.powders())),
                    primaryX, position.y + 10, 0xFFFFAA00);
            ui.drawTextWithShadow(this.textRenderer, "Lv. " + item.stat("lvl"),
                    position.x + 51, position.y + 28, 0xFFFFFFFF);
        }
    }

    private void renderEquipmentTooltip(DrawContext context, Layout layout, int mouseX, int mouseY) {
        if (pickerOpen()) return;
        for (BuildSlot slot : BuildSlot.values()) {
            WynnItem item = equipped.get(slot);
            if (item == null) continue;
            SlotPosition position = slotPosition(layout, slot);
            if (mouseX >= position.x + 11 && mouseX < position.x + 39
                    && mouseY >= position.y + 9 && mouseY < position.y + 37) {
                renderItemTooltip(context, item, mouseX, mouseY, true);
                return;
            }
        }
        for (int i = 0; i < offhands.size(); i++) {
            SlotPosition position = offhandPosition(layout, i);
            if (mouseX >= position.x + 11 && mouseX < position.x + 39
                    && mouseY >= position.y + 9 && mouseY < position.y + 37) {
                renderItemTooltip(context, offhands.get(i).item(), mouseX, mouseY, true);
                return;
            }
        }
    }

    private void renderSkillRequirementWarning(SlotPosition position, WynnItem item) {
        if (meetsCurrentSkillRequirements(item)) return;
        ui.pushMatrix();
        ui.translate(position.x + 25, position.y + 12);
        ui.scale(3.0F, 3.0F);
        ui.drawCenteredTextWithShadow(
                textRenderer, Text.literal("!").formatted(Formatting.BOLD), 0, 0, NEGATIVE);
        ui.popMatrix();
    }

    private void drawPixelHeart(DrawContext context, int x, int y) {
        int red = 0xFFFF3030;
        ui.fill(x + 1, y, x + 3, y + 1, red);
        ui.fill(x + 4, y, x + 6, y + 1, red);
        ui.fill(x, y + 1, x + 7, y + 3, red);
        ui.fill(x + 1, y + 3, x + 6, y + 4, red);
        ui.fill(x + 2, y + 4, x + 5, y + 5, red);
        ui.fill(x + 3, y + 5, x + 4, y + 6, red);
    }

    private void drawPixelWeapon(DrawContext context, int x, int y) {
        int blade = 0xFFE8E8E8;
        int shadow = 0xFF888888;
        int gold = 0xFFFFAA00;
        ui.fill(x + 7, y, x + 9, y + 2, blade);
        ui.fill(x + 6, y + 1, x + 8, y + 3, blade);
        ui.fill(x + 5, y + 2, x + 7, y + 4, blade);
        ui.fill(x + 4, y + 3, x + 6, y + 5, blade);
        ui.fill(x + 3, y + 4, x + 5, y + 6, blade);
        ui.fill(x + 7, y + 2, x + 8, y + 3, shadow);
        ui.fill(x + 2, y + 5, x + 5, y + 7, gold);
        ui.fill(x + 1, y + 7, x + 3, y + 9, gold);
        ui.fill(x, y + 8, x + 2, y + 10, 0xFFAA5500);
    }

    private void drawEquipmentIcon(
            DrawContext context,
            BuildSlot slot,
            WynnItem item,
            int x,
            int y
    ) {
        drawEquipmentIcon(context, slot, item, x, y, 28);
    }

    private void drawEquipmentIcon(DrawContext context, BuildSlot slot, WynnItem item, int x, int y, int size) {
        ItemStack stack = equipmentIcon(slot, item);
        int sprite = switch (slot) {
            case RING_1, RING_2 -> 9;
            case BRACELET -> 10;
            case NECKLACE -> 11;
            default -> -1;
        };
        if (sprite >= 0 && (stack.isEmpty() || item.customModelData() <= 0 || item.isCrafted())) {
            ui.drawTexture(RenderPipelines.GUI_TEXTURED, EQUIPMENT_TEXTURE,
                    x, y, sprite * 120, 0, size, size, 120, 120, 1440, 120);
            return;
        }
        ui.pushMatrix();
        ui.translate(x, y);
        ui.scale(size / 16.0F, size / 16.0F);
        ui.drawItem(stack, 0, 0);
        ui.popMatrix();
    }

    private ItemStack equipmentIcon(BuildSlot slot, WynnItem item) {
        return iconCache.computeIfAbsent(item, ignored -> createEquipmentIcon(slot, item));
    }

    private static ItemStack createEquipmentIcon(BuildSlot slot, WynnItem item) {
        ItemStack fallback = new ItemStack(switch (slot) {
            case HELMET -> Items.IRON_HELMET;
            case CHESTPLATE -> Items.IRON_CHESTPLATE;
            case LEGGINGS -> Items.IRON_LEGGINGS;
            case BOOTS -> Items.IRON_BOOTS;
            case RING_1, RING_2, BRACELET, NECKLACE -> Items.AIR;
            case WEAPON -> switch (item.subType().toLowerCase(Locale.ROOT)) {
                case "bow" -> Items.BOW;
                case "wand" -> Items.BLAZE_ROD;
                case "dagger" -> Items.SHEARS;
                case "relik" -> Items.TOTEM_OF_UNDYING;
                default -> Items.IRON_SWORD;
            };
        });
        if (item.iconId().isBlank()) {
            return fallback;
        }
        var iconItem = Registries.ITEM.get(Identifier.of(item.iconId()));
        ItemStack stack = new ItemStack(iconItem);
        if (stack.isEmpty()) {
            return fallback;
        }
        float modelData = WynntilsCustomModelAdapter.getFloat(item.iconModel())
                .orElse((float) item.customModelData());
                // wynntils has the current IDs saved and is more reliable than the API (especially after a resource pack update)
                // thats why were using it here if its available
        if (modelData > 0) {
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                    List.of(modelData), List.of(), List.of(), List.of()));
        }
        return stack;
    }

    private String fitText(String value, int width) {
        if (this.textRenderer.getWidth(value) <= width) {
            return value;
        }
        String ellipsis = "...";
        return this.textRenderer.trimToWidth(
                value, Math.max(0, width - this.textRenderer.getWidth(ellipsis))) + ellipsis;
    }

    private void renderSkills(DrawContext context, Layout layout, BuildCalculator.Result stats) {
        int cardWidth = (layout.leftWidth - 8) / 5;
        int top = layout.skillTop + 26;
        for (int i = 0; i < SKILL_NAMES.length; i++) {
            int x = MARGIN + 4 + i * cardWidth;
            UiTheme.drawRoundedBox(
                    ui, x + 2, layout.skillTop + 5, cardWidth - 4, SKILL_CARD_HEIGHT,
                    UiTheme.SURFACE, UiTheme.BORDER);
        }
        int remaining = availableSkillPoints() - totalAssigned();
        int overCapSkill = firstOverCapSkill();
        boolean hasStatus = overCapSkill >= 0 || !skillPointError.isBlank() || !skillPointSuccess.isBlank();
        int assignedLineY = layout.skillTop + layout.skillHeight - (hasStatus ? 26 : 14);
        String remainingPrefix = "Assigned " + totalAssigned() + " skillpoints. Remaining: ";
        String remainingValue = Integer.toString(remaining);
        int remainingWidth = this.textRenderer.getWidth(remainingPrefix + remainingValue);
        int statusLeft = offhands.isEmpty()
                ? MARGIN + 8 + AUTO_ASSIGN_WIDTH + 8
                : MARGIN + 16 + AUTO_ASSIGN_MAIN_WIDTH + AUTO_ASSIGN_ALL_WIDTH;
        int statusWidth = layout.leftWidth - (statusLeft - MARGIN) - 8;
        int remainingX = statusLeft + (statusWidth - remainingWidth) / 2;
        ui.drawTextWithShadow(this.textRenderer,
                remainingPrefix, remainingX, assignedLineY, 0xFFFFFFFF);
        ui.drawTextWithShadow(this.textRenderer,
                remainingValue, remainingX + this.textRenderer.getWidth(remainingPrefix), assignedLineY,
                remaining < 0 ? NEGATIVE : POSITIVE);
        if (hasStatus) {
            SkillPointWarning warning = overCapSkill < 0 ? null : SkillPointWarning.forBuild(
                    level, assignedSkills, new ArrayList<>(selectedTomes.values())).orElseThrow();
            boolean successStatus = warning == null && skillPointError.isBlank();
            String message = !skillPointError.isBlank()
                    ? skillPointError
                    : warning != null ? warning.message() : skillPointSuccess;
            float scale = Math.min(1.0F, statusWidth / (float) textRenderer.getWidth(message));
            drawScaledCenteredText(context, Text.literal(message),
                    statusLeft + statusWidth / 2, assignedLineY + 12,
                    successStatus ? POSITIVE
                            : warning != null && warning.tomeSuggestion() ? 0xFFFFAA00 : 0xFFFF5555,
                    scale);
        }
    }

    private void renderSkillDetails(
            DrawContext context,
            Layout layout,
            BuildCalculator.Result stats
    ) {
        int cardWidth = (layout.leftWidth - 8) / 5;
        int top = layout.skillTop + 11;
        for (int i = 0; i < SKILL_NAMES.length; i++) {
            int x = MARGIN + 4 + i * cardWidth;
            int centerX = x + cardWidth / 2;
            int iconSize = 12;
            int iconGap = 4;
            int nameWidth = Math.round(this.textRenderer.getWidth(SKILL_NAMES[i]));
            int nameGroupWidth = iconSize + iconGap + nameWidth;
            int iconX = centerX - nameGroupWidth / 2;
            ui.drawTexture(RenderPipelines.GUI_TEXTURED, SKILL_ICONS[i], iconX, layout.skillTop + 8,
                    0, 0, iconSize, iconSize, 16, 16, 16, 16);
            drawScaledCenteredText(context,
                    Text.literal(SKILL_NAMES[i]).formatted(SKILL_COLORS[i]),
                    iconX + iconSize + iconGap + nameWidth / 2,
                    layout.skillTop + 10, 0xFFFFFFFF, 1);
            drawScaledCenteredText(context,
                    Text.literal("Assign: " + assignedSkills[i]).formatted(Formatting.WHITE),
                    centerX, top + 36, 0xFFFFFFFF, 1);
            drawScaledCenteredText(context,
                    Text.literal("Original: " + skillPointPlan.finalSkills()[i])
                            .formatted(Formatting.WHITE),
                    centerX, top + 48, 0xFFFFFFFF, 1);
            drawScaledCenteredText(context,
                    Text.literal(String.format("%.1f%% %s",
                                    BuildCalculator.skillEffectPercentage(i, stats.skills()[i]), SKILL_EFFECTS[i]))
                            .formatted(SKILL_COLORS[i]),
                    centerX, top + 61, 0xFFFFFFFF, 1);
        }
    }

    private void drawScaledCenteredText(
            DrawContext context,
            Text text,
            int centerX,
            int y,
            int color,
            float scale
    ) {
        ui.pushMatrix();
        ui.translate(centerX, y);
        ui.scale(scale, scale);
        ui.drawCenteredTextWithShadow(this.textRenderer, text, 0, 0, color);
        ui.popMatrix();
    }

    private int firstOverCapSkill() {
        for (int i = 0; i < assignedSkills.length; i++) {
            if (assignedSkills[i] > 100) {
                return i;
            }
        }
        return -1;
    }

    private void renderSummary(DrawContext context, Layout layout, BuildCalculator.Result stats,
            int mouseX, int mouseY, float delta) {
        int x = layout.middleX + 12;
        int viewportTop = layout.equipmentTop + 32 + VIEW_SWITCH_OFFSET;
        int viewportBottom = this.height - MARGIN - 28;
        int visibleScroll = Math.round(summaryScroll.update(delta));
        int y = layout.equipmentTop + 38 + VIEW_SWITCH_OFFSET - visibleScroll;
        int valueX = layout.middleX + layout.middleWidth - 12;
        Map<String, Integer> ids = stats.ids();

        ui.enableScissor(layout.middleX + 4, viewportTop,
                layout.middleX + layout.middleWidth - 4, viewportBottom);
        try {
            y = summaryLine(context, x, valueX, y, "\u2665 Total HP:",
                    format(stats.health()), FIRE, 0xFFFFFFFF);
            y = summaryLine(context, x, valueX, y, "Effective HP:",
                    format(stats.effectiveHp()), 0xFFFFFFFF, 0xFFFFFFFF);
            y = summaryLine(context, x, valueX, y, "Effective HP (no agi):",
                    format(stats.effectiveHpNoAgility()), 0xFFFFFFFF, 0xFFFFFFFF);
            y = summaryLine(context, x, valueX, y, "\u2665 HP Regen (Total):",
                    format(stats.healthRegen()), FIRE, 0xFFFFFFFF);
            if (stats.healthRegen() != 0) {
                y = summaryLine(context, x, valueX, y, "Effective HP Regen:",
                        format(stats.healthRegen() * stats.effectiveHp() / Math.max(1, stats.health())),
                        0xFFFFFFFF);
            }
            String[] elements = {"\u2618 Earth Def (Total):", "\u2726 Thunder Def (Total):",
                    "\u2749 Water Def (Total):", "\u2739 Fire Def (Total):", "\u2733 Air Def (Total):"};
            int[] colors = {EARTH, THUNDER, WATER, FIRE, AIR};
            for (int i = 0; i < elements.length; i++) {
                if (stats.defenses()[i] != 0) {
                    y = summaryLine(context, x, valueX, y,
                            elements[i], format(stats.defenses()[i]), colors[i]);
                }
            }
            if (detailed) {
                y = summaryLine(context, x, valueX, y, "Damage Absorbed:",
                        format(BuildCalculator.skillEffectPercentage(3, stats.skills()[3])) + "%",
                        0xFFFFFFFF);
                y = summaryLine(context, x, valueX, y, "Dodge Chance:",
                        format(BuildCalculator.skillEffectPercentage(4, stats.skills()[4])) + "%",
                        0xFFFFFFFF);
            }

            StatDisplay[] firstGroup = detailed ? DETAILED_SUSTAIN_STATS : SUMMARY_STATS;
            if (hasAny(ids, firstGroup)) {
                y = summaryDivider(context, layout, y);
                y = renderStatGroup(context, x, valueX, y, ids, firstGroup, stats);
            }
            if (detailed && hasAny(ids, DETAILED_DAMAGE_STATS)) {
                y = summaryDivider(context, layout, y);
                y = renderStatGroup(context, x, valueX, y, ids, DETAILED_DAMAGE_STATS, stats);
            }
            if (detailed && hasAny(ids, DETAILED_ELEMENTAL_STATS)) {
                y = summaryDivider(context, layout, y);
                y = renderStatGroup(context, x, valueX, y, ids, DETAILED_ELEMENTAL_STATS, stats);
            }
            if (detailed && hasAny(ids, DETAILED_UTILITY_STATS)) {
                y = summaryDivider(context, layout, y);
                y = renderStatGroup(context, x, valueX, y, ids, DETAILED_UTILITY_STATS, stats);
            }
        } finally {
            ui.disableScissor();
        }

        int contentBottom = y + visibleScroll;
        summaryMaxScroll = Math.max(0, contentBottom - viewportBottom + 4);
        summaryScroll.clamp(0.0F, summaryMaxScroll);
        summaryScrollbar.configure(layout.middleX + layout.middleWidth - 7,
                viewportTop + 2, viewportBottom - viewportTop - 4, summaryMaxScroll);
        summaryScrollbar.draw(context, mouseX, mouseY, delta, ui);
    }

    private int renderStatGroup(
            DrawContext context, int x, int valueX, int y, Map<String, Integer> ids,
            StatDisplay[] displays, BuildCalculator.Result stats
    ) {
        for (StatDisplay display : displays) {
            int value = ids.getOrDefault(display.key(), 0);
            if (value == 0) {
                continue;
            }
            y = summaryLine(context, x, valueX, y, display.label(), value + display.suffix(),
                    display.color(), display.reversed() ? numericColor(-value) : numericColor(value));
            if ("manaSteal".equals(display.key())) {
                double hitsPerThreeSeconds = BuildCalculator.attacksPerSecond(stats.effectiveAttackTier()) * 3.0D;
                y = summarySubLine(context, x, valueX, y, "\u279c Mana per hit:",
                        format(value / hitsPerThreeSeconds));
            } else if ("lifeSteal".equals(display.key())) {
                double hitsPerThreeSeconds = BuildCalculator.attacksPerSecond(stats.effectiveAttackTier()) * 3.0D;
                y = summarySubLine(context, x, valueX, y, "\u279c Effective LS:",
                        format(value * stats.effectiveHp() / Math.max(1, stats.health())) + "/3s");
                y = summarySubLine(context, x, valueX, y, "\u279c Life per hit:",
                        format(value / hitsPerThreeSeconds));
            } else if ("rawMaxMana".equals(display.key())) {
                y = summarySubLine(context, x, valueX, y, "\u279c Total Mana:",
                        Integer.toString(100 + value));
            } else if ("manaRegen".equals(display.key())) {
                y = summarySubLine(context, x, valueX, y, "\u279c Total with base:",
                        (value + 25) + "/5s");
            } else if ("mainAttackRange".equals(display.key())) {
                WynnItem weapon = equipped.get(BuildSlot.WEAPON);
                CharacterClass abilityClass = CharacterClass.fromWeaponSubtype(
                        weapon == null ? "" : weapon.subType());
                AbilityTreeDefinition tree = AbilityTreeDatabase.getInstance().get(abilityClass);
                double baseRange = DamageCalculator.mainAttackRange(
                        weapon == null ? "" : weapon.subType(),
                        tree,
                        abilityTreeState.selected(abilityClass));
                double totalRange = Math.round(baseRange * (1 + value / 100.0) * 10) / 10.0;
                y = summarySubLine(context, x, valueX, y, "\u279c Total Range:",
                        format(totalRange) + " Blocks");
            }
        }
        return y;
    }

    private int summaryLine(DrawContext context, int x, int valueX, int y, String label, String value, int color) {
        return summaryLine(context, x, valueX, y, label, value, color, numericColor(value));
    }

    private int summaryLine(
            DrawContext context, int x, int valueX, int y,
            String label, String value, int labelColor, int valueColor
    ) {
        ui.drawTextWithShadow(this.textRenderer, label, x, y, labelColor);
        ui.drawTextWithShadow(this.textRenderer, value, valueX - this.textRenderer.getWidth(value), y, valueColor);
        return y + 14;
    }

    private int summarySubLine(DrawContext context, int x, int valueX, int y, String label, String value) {
        int labelX = Math.max(x, valueX - this.textRenderer.getWidth(label + " " + value));
        ui.drawTextWithShadow(this.textRenderer, label, labelX, y, 0xFFFFFFFF);
        ui.drawTextWithShadow(this.textRenderer, value,
                valueX - this.textRenderer.getWidth(value), y, numericColor(value));
        return y + 14;
    }

    private int summaryDivider(DrawContext context, Layout layout, int y) {
        ui.fill(layout.middleX + 4, y + 3,
                layout.middleX + layout.middleWidth - 4, y + 4, 0xFF555555);
        return y + 12;
    }

    private static int numericColor(String value) {
        return value.stripLeading().startsWith("-") ? NEGATIVE : POSITIVE;
    }

    private static int numericColor(int value) {
        return value < 0 ? NEGATIVE : POSITIVE;
    }

    private static boolean hasAny(Map<String, Integer> ids, StatDisplay[] displays) {
        for (StatDisplay display : displays) {
            if (ids.getOrDefault(display.key(), 0) != 0) {
                return true;
            }
        }
        return false;
    }

    private static StatDisplay stat(String key, String label, String suffix) {
        return new StatDisplay(key, label, suffix, 0xFFFFFFFF, false);
    }

    private static StatDisplay stat(String key, String label, String suffix, int color) {
        return new StatDisplay(key, label, suffix, color, false);
    }

    private static StatDisplay reversedStat(String key, String label, String suffix) {
        return new StatDisplay(key, label, suffix, 0xFFFFFFFF, true);
    }

    private static StatDisplay[] elementalStats() {
        List<StatDisplay> stats = new ArrayList<>();
        String[] keys = {"Earth", "Thunder", "Water", "Fire", "Air"};
        String[] names = {"Earth", "Thunder", "Water", "Fire", "Air"};
        String[] icons = {"\u2618 ", "\u2726 ", "\u2749 ", "\u2739 ", "\u2733 "};
        int[] colors = {EARTH, THUNDER, WATER, FIRE, AIR};
        String[][] groups = {
                {"raw%sSpellDamage", "%s Spell Damage Raw:", ""},
                {"%sSpellDamage", "%s Spell Damage %%:", "%"},
                {"raw%sMainAttackDamage", "%s Melee Damage Raw:", ""},
                {"%sMainAttackDamage", "%s Melee Damage %%:", "%"},
                {"raw%sDamage", "%s Damage Raw:", ""},
                {"%sDamage", "%s Damage %%:", "%"},
                {"%sDefence", "%s Defense %%:", "%"}
        };
        for (String[] group : groups) {
            for (int i = 0; i < keys.length; i++) {
                String keyElement = group[0].startsWith("raw")
                        ? keys[i]
                        : keys[i].toLowerCase(Locale.ROOT);
                stats.add(stat(
                        String.format(group[0], keyElement),
                        icons[i] + String.format(group[1], names[i]).replace("%%", "%"),
                        group[2],
                        colors[i]));
            }
        }
        stats.add(stat("criticalDamageBonus", "Crit Damage Bonus %:", "%"));
        return stats.toArray(StatDisplay[]::new);
    }

    private void renderDamage(DrawContext context, Layout layout, BuildCalculator.Result stats,
            int mouseX, int mouseY, float delta) {
        WynnItem weapon = equipped.get(BuildSlot.WEAPON);
        CharacterClass abilityClass = CharacterClass.fromWeaponSubtype(
                weapon == null ? "" : weapon.subType());
        AbilityTreeDefinition tree = AbilityTreeDatabase.getInstance().get(abilityClass);
        Set<String> selectedAbilities = abilityTreeState.selected(abilityClass);
        int cacheKey = System.identityHashCode(weapon);
        cacheKey = 31 * cacheKey + System.identityHashCode(tree);
        cacheKey = 31 * cacheKey + System.identityHashCode(stats);
        cacheKey = 31 * cacheKey + selectedAbilities.hashCode();
        if (cacheKey != damageCacheKey) {
            damageCache = weapon == null
                    ? List.of()
                    : DamageCalculator.calculate(
                            stats, weapon.subType(), tree, selectedAbilities,
                            new ArrayList<>(selectedAspects.values()));
            damageCacheKey = cacheKey;
        }
        List<DamageCalculator.SpellResult> spells = damageCache;
        int cardX = layout.rightX + 8;
        int cardWidth = layout.rightWidth - 16;
        int viewportTop = layout.equipmentTop + 4;
        int viewportBottom = this.height - MARGIN - 28;
        int visibleScroll = Math.round(damageScroll.update(delta));
        int y = layout.equipmentTop + PANEL_TOP_INSET - visibleScroll;
        damageCards.clear();
        ui.enableScissor(
                layout.rightX + 3, viewportTop,
                layout.rightX + layout.rightWidth - 3, viewportBottom);
        try {
            if (weapon == null) {
                ui.drawCenteredTextWithShadow(
                        this.textRenderer, "Select a weapon",
                        layout.rightX + layout.rightWidth / 2, layout.equipmentTop + 42, 0xFFAAAAAA);
                damageMaxScroll = 0;
                damageScrollbar.configure(layout.rightX + layout.rightWidth - 7,
                        viewportTop + 2, viewportBottom - viewportTop - 4, 0);
                return;
            }
            double criticalChance = BuildCalculator.skillPercentage(stats.skills()[1]);
            for (DamageCalculator.SpellResult spell : spells) {
                boolean expanded = expandedSpell == spell.index();
                int cardHeight = damageCardHeight(spell, expanded);
                UiTheme.drawRoundedBox(
                        ui, cardX, y, cardWidth, cardHeight, UiTheme.SURFACE, UiTheme.BORDER);
                damageCards.add(new DamageCardBounds(
                        spell.index(), cardX, Math.max(viewportTop, y),
                        cardX + cardWidth, Math.min(viewportBottom, y + Math.min(cardHeight, 80))));
                int lineY = y + 9;
                drawSpellTitle(context, spell, cardX + cardWidth / 2, lineY);
                lineY += 16;
                DamageCalculator.PartResult summary = spell.summaryPart();
                if (summary == null) {
                    ui.drawCenteredTextWithShadow(
                            this.textRenderer, "Ability Tree required",
                            cardX + cardWidth / 2, lineY, 0xFF777777);
                } else if (spell.index() == 0) {
                    double perAttack = summary.average(criticalChance);
                    drawDamageValue(context, cardX, cardWidth, lineY,
                            "Average DPS: ", perAttack * BuildCalculator.attacksPerSecond(stats.effectiveAttackTier()));
                    lineY += 12;
                    ui.drawCenteredTextWithShadow(
                            this.textRenderer,
                            "Attack Speed: " + attackSpeedName(stats.effectiveAttackTier()),
                            cardX + cardWidth / 2, lineY, 0xFFFFFFFF);
                    lineY += 12;
                    drawDamageValue(context, cardX, cardWidth, lineY, "Per Attack: ", perAttack);
                } else {
                    drawDamageValue(context, cardX, cardWidth, lineY,
                            summary.name().toLowerCase(Locale.ROOT).contains("dps")
                                    ? "DPS: " : "Total Damage: ",
                            summary.average(criticalChance));
                }
                drawExpandArrow(
                        context, cardX + cardWidth / 2, y + cardHeight - 9, expanded);
                if (expanded) {
                    lineY = y + 70;
                    for (DamageCalculator.PartResult part : spell.parts()) {
                        if (!part.display()) {
                            continue;
                        }
                        lineY = renderDamagePart(
                                context, cardX, cardWidth, lineY, part, criticalChance);
                    }
                }
                y += cardHeight + 8;
            }
        } finally {
            ui.disableScissor();
        }
        int contentBottom = y + visibleScroll;
        damageMaxScroll = Math.max(0, contentBottom - viewportBottom + 4);
        damageScroll.clamp(0.0F, damageMaxScroll);
        damageScrollbar.configure(layout.rightX + layout.rightWidth - 7,
                viewportTop + 2, viewportBottom - viewportTop - 4, damageMaxScroll);
        damageScrollbar.draw(context, mouseX, mouseY, delta, ui);
    }

    private int damageCardHeight(DamageCalculator.SpellResult spell, boolean expanded) {
        if (!expanded) {
            return spell.index() == 0 ? 80 : 64;
        }

        int height = 76;
        for (DamageCalculator.PartResult part : spell.parts()) {
            if (!part.display()) {
                continue;
            }
            if (part.healing()) {
                height += 38;
                continue;
            }
            int normalElements = 0;
            int criticalElements = 0;
            for (int i = 0; i < 6; i++) {
                if (part.normal()[i][1] != 0) normalElements++;
                if (part.critical()[i][1] != 0) criticalElements++;
            }
            height += 71 + (normalElements + criticalElements) * 11;
        }
        return height;
    }

    private void drawSpellTitle(
            DrawContext context,
            DamageCalculator.SpellResult spell,
            int centerX,
            int y
    ) {
        Text name = Text.literal(spell.name()).formatted(Formatting.BOLD);
        if (spell.index() == 0 || spell.cost() <= 0) {
            ui.drawCenteredTextWithShadow(textRenderer, name, centerX, y, 0xFFFFFFFF);
            return;
        }
        String cost = " (" + format(spell.cost());
        String close = ")";
        int iconWidth = 7;
        int totalWidth = textRenderer.getWidth(name)
                + textRenderer.getWidth(cost) + iconWidth + textRenderer.getWidth(close);
        int x = centerX - totalWidth / 2;
        ui.drawTextWithShadow(textRenderer, name, x, y, 0xFFFFFFFF);
        x += textRenderer.getWidth(name);
        ui.drawTextWithShadow(textRenderer, cost, x, y, 0xFF55FFFF);
        x += textRenderer.getWidth(cost) + 1;
        drawManaDrop(context, x, y + 1);
        x += iconWidth - 1;
        ui.drawTextWithShadow(textRenderer, close, x, y, 0xFF55FFFF);
    }

    private void drawManaDrop(DrawContext context, int x, int y) {
        ui.fill(x + 2, y, x + 4, y + 1, 0xFFBFFFFF);
        ui.fill(x + 1, y + 1, x + 5, y + 3, 0xFF55FFFF);
        ui.fill(x, y + 3, x + 6, y + 6, 0xFF20BFFF);
        ui.fill(x + 1, y + 6, x + 5, y + 7, 0xFF0088CC);
        ui.fill(x + 1, y + 3, x + 2, y + 5, 0xFFFFFFFF);
    }

    private void drawExpandArrow(
            DrawContext context,
            int centerX,
            int y,
            boolean expanded
    ) {
        int color = 0xFF55FFFF;
        if (expanded) {
            ui.fill(centerX - 1, y, centerX + 2, y + 1, color);
            ui.fill(centerX - 3, y + 1, centerX + 4, y + 2, color);
            ui.fill(centerX - 5, y + 2, centerX + 6, y + 4, color);
        } else {
            ui.fill(centerX - 5, y, centerX + 6, y + 2, color);
            ui.fill(centerX - 3, y + 2, centerX + 4, y + 3, color);
            ui.fill(centerX - 1, y + 3, centerX + 2, y + 5, color);
        }
    }

    private int renderDamagePart(
            DrawContext context,
            int cardX,
            int cardWidth,
            int y,
            DamageCalculator.PartResult part,
            double criticalChance
    ) {
        ui.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(part.name()).formatted(Formatting.BOLD),
                cardX + cardWidth / 2, y, 0xFFFFFFFF);
        y += 13;
        if (part.healing()) {
            drawDamageValue(context, cardX, cardWidth, y, "Healing: ", part.healingAmount());
            return y + 22;
        }
        var multipliers = Text.literal("");
        Formatting[] colors = {
                Formatting.GRAY, Formatting.GREEN, Formatting.YELLOW,
                Formatting.AQUA, Formatting.RED, Formatting.WHITE
        };
        String[] icons = {"\u2748", "\u2618", "\u2726", "\u2749", "\u2739", "\u2733"};
        for (int i = 0; i < 6; i++) {
            if (part.multipliers()[i] != 0) {
                multipliers.append(Text.literal(
                        (multipliers.getString().isEmpty() ? "" : " + ")
                                + format(part.multipliers()[i]) + "% " + icons[i])
                        .formatted(colors[i]));
            }
        }
        ui.drawCenteredTextWithShadow(
                this.textRenderer, multipliers, cardX + cardWidth / 2, y, 0xFFFFFFFF);
        y += 12;
        drawDamageValue(context, cardX, cardWidth, y, "Average: ", part.average(criticalChance));
        y += 12;
        drawDamageValue(context, cardX, cardWidth, y, "Non-Crit Average: ", part.normalAverage());
        y += 12;
        y = renderElementRanges(context, cardX, cardWidth, y, part.normal());
        drawDamageValue(context, cardX, cardWidth, y, "Crit Average: ", part.criticalAverage());
        y += 12;
        y = renderElementRanges(context, cardX, cardWidth, y, part.critical());
        return y + 10;
    }

    private int renderElementRanges(
            DrawContext context,
            int cardX,
            int cardWidth,
            int y,
            double[][] ranges
    ) {
        String[] icons = {"\u2748", "\u2618", "\u2726", "\u2749", "\u2739", "\u2733"};
        int[] colors = {0xFFDDDDDD, EARTH, THUNDER, WATER, FIRE, AIR};
        for (int i = 0; i < ranges.length; i++) {
            if (ranges[i][1] == 0) {
                continue;
            }
            ui.drawCenteredTextWithShadow(
                    this.textRenderer,
                    icons[i] + " " + format(ranges[i][0]) + " - " + format(ranges[i][1]),
                    cardX + cardWidth / 2, y, colors[i]);
            y += 11;
        }
        return y;
    }

    private void drawDamageValue(
            DrawContext context,
            int cardX,
            int cardWidth,
            int y,
            String label,
            double value
    ) {
        Text text = Text.literal(label).formatted(Formatting.WHITE)
                .append(Text.literal(format(value)).formatted(Formatting.GOLD));
        ui.drawCenteredTextWithShadow(
                this.textRenderer, text, cardX + cardWidth / 2, y, 0xFFFFFFFF);
    }

    private static String attackSpeedName(int tier) {
        return switch (tier) {
            case 0 -> "Super Slow";
            case 1 -> "Very Slow";
            case 2 -> "Slow";
            case 3 -> "Normal";
            case 4 -> "Fast";
            case 5 -> "Very Fast";
            default -> "Super Fast";
        };
    }

    private void renderBoostArea(DrawContext context, Layout layout, BuildCalculator.Result stats) {
        if (!"Ability Boosts".equals(boostSection)) {
            ui.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(boostSection + " options will appear here.").formatted(Formatting.DARK_GRAY),
                    MARGIN + layout.leftWidth / 2, layout.boostTop + 63, 0x777777);
        }

    }

    private void renderPicker(DrawContext context) {
        int pickerWidth = Math.min(520, this.width - 60);
        int pickerHeight = Math.min(326, this.height - 50);
        int left = (this.width - pickerWidth) / 2;
        int top = (this.height - pickerHeight) / 2;
        ui.fill(0, 0, this.width, this.height, 0x99000000);
        UiTheme.drawPanel(ui, left, top, pickerWidth, pickerHeight);
        ui.drawTextWithShadow(this.textRenderer,
                Text.literal("Choose " + pickerLabel()).styled(style -> style.withColor(UiTheme.accentRgb())),
                left + 14, top + 13, 0xFFFFFF);
    }

    private void renderPickerRow(DrawContext context, julianh06.wynnextras.utils.UI.UIUtils ui,
            net.minecraft.client.font.TextRenderer textRenderer, WynnItem item,
            int x, int y, int width, int height, boolean hovered) {
        UiTheme.drawRoundedBox(ui, x, y, width, height - 2,
                hovered ? UiTheme.hoverBackground() : UiTheme.SURFACE,
                hovered ? UiTheme.accent() : UiTheme.BORDER);
        drawEquipmentIcon(context, pickerOffhandIndex >= 0 ? BuildSlot.WEAPON : pickerSlot,
                item, x + 3, y + 1, 18);
        Text name = Text.literal(fitText(item.displayName(), width - 94)).formatted(tierColor(item.tier()));
        ui.drawTextWithShadow(textRenderer, name, x + 25, y + 7, 0xFFFFFFFF);
        String levelText = "Lv. " + item.stat("lvl");
        ui.drawTextWithShadow(textRenderer, levelText,
                x + width - 6 - textRenderer.getWidth(levelText), y + 7, UiTheme.MUTED);
    }

    private List<Text> itemTooltip(WynnItem item) {
        int tooltipWidth = tooltipWidth(item);
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.empty());
        tooltip.add(tooltipNameLine(item));
        tooltip.add(tooltipTypeLine(item));
        Text tags = tooltipTagsLine(item);
        if (!tags.getString().isBlank()) tooltip.add(tags);
        tooltip.add(Text.empty());

        int dividerColor = tooltipDividerColor(item.tier());
        if ("weapon".equals(item.type())) {
            String dps = String.format(Locale.US, "%,d", Math.round(BuildCalculator.weaponBaseDps(item, "")));
            tooltip.add(tooltipText(dps, "offset/wynncraft_quad/12", dividerColor)
                    .append(tooltipText(" DPS", "language/wynncraft", 0xFFFFFFFF)));
        } else {
            tooltip.add(tooltipText(signed(item.stat("hp")), "offset/wynncraft_quad/12", dividerColor)
                    .append(tooltipText(" Health", "language/wynncraft", 0xFFFFFFFF)));
        }

        if (!item.attackSpeed().isBlank()) {
            String speed = readableTooltipValue(item.attackSpeed());
            tooltip.add(tooltipText("\uE007", "tooltip/attribute/sprite", 0xFFFFFFFF)
                    .append(tooltipText(" " + speed + " ", "language/wynncraft", 0xFFAAAAAA))
                    .append(tooltipText("(" + attackSpeed(item.attackSpeed()) + " hits/s)",
                            "language/wynncraft", 0xFF555555)));
        }

        String[] damagePrefixes = {"n", "e", "t", "w", "f", "a"};
        String[] damageSprites = {"\uE005", "\uE000", "\uE001", "\uE002", "\uE003", "\uE004"};
        MutableText damages = Text.empty();
        for (int i = 0; i < damagePrefixes.length; i++) {
            int minimum = item.stat(damagePrefixes[i] + "DamMin");
            int maximum = item.stat(damagePrefixes[i] + "DamMax");
            if (minimum == 0 && maximum == 0) continue;
            damages.append(tooltipSprite(damageSprites[i]));
            damages.append(tooltipText(" " + minimum + "-" + maximum + " ",
                    "language/wynncraft", 0xFFAAAAAA));
        }
        if (!damages.getString().isBlank()) tooltip.add(damages);

        String[] defensePrefixes = {"e", "t", "w", "f", "a"};
        String[] defenseSprites = {"\uE000", "\uE001", "\uE002", "\uE003", "\uE004"};
        MutableText defenses = Text.empty();
        for (int i = 0; i < defensePrefixes.length; i++) {
            int value = item.stat(defensePrefixes[i] + "Def");
            if (value == 0) continue;
            defenses.append(tooltipSprite(defenseSprites[i]));
            defenses.append(tooltipText(" " + signed(value) + " ", "language/wynncraft", 0xFFAAAAAA));
        }
        if (!defenses.getString().isBlank()) tooltip.add(defenses);

        tooltip.add(tooltipDivider(item.tier()));

        String[] skillKeys = {"strReq", "dexReq", "intReq", "defReq", "agiReq"};
        boolean hasSkillRequirements = Arrays.stream(skillKeys).anyMatch(key -> item.stat(key) != 0);
        if (hasSkillRequirements) {
            tooltip.add(Text.empty());
            tooltip.add(centerTooltipLine(skillRequirementIcons(item, skillKeys), tooltipWidth));
            tooltip.add(Text.empty());
            tooltip.add(centerTooltipLine(skillRequirementValues(item, skillKeys), tooltipWidth));
            tooltip.add(Text.empty());
        }

        if ("weapon".equals(item.type())) {
            CharacterClass requiredClass = CharacterClass.fromWeaponSubtype(item.subType());
            tooltip.add(alignedTooltipLine(
                    requirementLabel(" Class Type", requiredClass == selectedClass),
                    tooltipText(requiredClass.displayName(), "language/wynncraft", 0xFFAAAAAA), tooltipWidth));
        }
        int requiredLevel = item.stat(item.isCrafted() ? "lvlLow" : "lvl");
        if (requiredLevel != 0) {
            tooltip.add(alignedTooltipLine(requirementLabel(" Combat Level", level >= requiredLevel),
                    tooltipText(Integer.toString(requiredLevel), "language/wynncraft", 0xFFAAAAAA), tooltipWidth));
        }

        tooltip.add(tooltipDivider(item.tier()));

        for (ItemInspection.Row row : ItemInspection.rows(item, "", true)) {
            if (row.kind() != ItemInspection.Kind.SKILL
                    && row.kind() != ItemInspection.Kind.IDENTIFICATION) continue;
            String value = row.value();
            if (row.beneficial() && !value.startsWith("+") && !value.startsWith("-")) value = "+" + value;
            String label = row.label().replaceFirst(" %$", "");
            tooltip.add(alignedTooltipLine(
                    tooltipText(label + " ", "language/wynncraft", 0xFFFFFFFF),
                    identificationValue(value, row.beneficial()),
                    tooltipWidth));
        }

        for (Map.Entry<String, String> majorId : item.majorIds().entrySet()) {
            tooltip.add(Text.empty());
            appendWrappedTooltip(tooltip, "◆ " + majorId.getKey() + ": " + majorId.getValue(),
                    tooltipDividerColor(item.tier()));
        }
        return tooltip;
    }

    private void renderItemTooltip(
            DrawContext context, WynnItem item, int mouseX, int mouseY, boolean equipped
    ) {
        int[] currentSkills = equipped ? currentSkillsForItem(item) : calculateStats().skills();
        int[] requirementSkills = equipped
                ? BuildCalculator.skillsAvailableForItemRequirements(item, currentSkills)
                : currentSkills;
        WynnItemTooltip.render(this, context, textRenderer, item, mouseX, mouseY,
                selectedClass, level, requirementSkills,
                equipped && !meetsCurrentSkillRequirements(item));
    }

    private int tooltipWidth(WynnItem item) {
        int width = Math.max(140, textRenderer.getWidth(item.displayName()) + 30);
        for (ItemInspection.Row row : ItemInspection.rows(item, "", true)) {
            if (row.kind() == ItemInspection.Kind.SKILL || row.kind() == ItemInspection.Kind.IDENTIFICATION) {
                width = Math.max(width, textRenderer.getWidth(row.label() + "  " + row.value()));
            }
        }
        return width;
    }

    private Text tooltipNameLine(WynnItem item) {
        MutableText line = tooltipTextWithWhiteShadow("\uDAFF\uDFF0", "language/wynncraft", 0xFFFFFFFF);
        line.append(tooltipText(emblemFrame(item.emblem()), "tooltip/emblem/frame", 0xFFFFFFFF));
        line.append(tooltipText("\uDAFF\uDFCF", "language/wynncraft", 0xFFFFFFFF));
        line.append(tooltipText(typeSprite(item), "tooltip/emblem/sprite", 0xFF00EB1C));
        line.append(tooltipSpace(5));
        line.append(tooltipText(item.displayName(), "language/wynncraft", tierRgb(item.tier())));
        return line;
    }

    private Text tooltipTypeLine(WynnItem item) {
        MutableText line = tooltipTextWithWhiteShadow("", "language/wynncraft",
                tooltipDividerColor(item.tier())).append(tooltipSpace(38));
        line.append(tooltipBanner(readableTooltipValue(item.tier()), tierRgb(item.tier()), 0xFF000000));
        line.append(tooltipSpace(1));
        line.append(tooltipBanner(itemTypeName(item), tooltipDividerColor(item.tier()), 0xFF000000));
        return line;
    }

    private Text tooltipTagsLine(WynnItem item) {
        String[] prefixes = {"e", "t", "w", "f", "a"};
        String[] base = {"\uE006", "\uE007", "\uE008", "\uE009", "\uE00A"};
        String[] overlay = {"\uF006", "\uF007", "\uF008", "\uF009", "\uF00A"};
        MutableText line = tooltipSpace(38);
        boolean any = false;
        for (int i = 0; i < prefixes.length; i++) {
            if (item.stat(prefixes[i] + "DamMin") == 0 && item.stat(prefixes[i] + "DamMax") == 0
                    && item.stat(prefixes[i] + "Def") == 0) continue;
            if (any) line.append(tooltipSpace(-1));
            line.append(tooltipTextWithWhiteShadow(base[i], "tooltip/banner", tooltipDividerColor(item.tier())));
            line.append(tooltipText("\uDAFF\uDFF6" + overlay[i], "tooltip/banner", 0xFFFFFFFF));
            any = true;
        }
        return any ? line : Text.empty();
    }

    private Text skillRequirementIcons(WynnItem item, String[] keys) {
        MutableText line = Text.empty();
        for (int i = 0; i < keys.length; i++) {
            int requirement = item.stat(keys[i]);
            String frame = requirement == 0 ? "\uE007" : String.valueOf((char) ('\uE000' + tierIndex(item.tier())));
            String sprite = String.valueOf((char) ((requirement == 0 ? '\uE010' : '\uE000') + i));
            line.append(tooltipTextWithWhiteShadow(frame, "tooltip/requirement/frame", 0xFFFFFFFF));
            line.append(tooltipTextWithWhiteShadow(
                    "\uDAFF\uDFE7" + sprite, "tooltip/requirement/sprite", 0xFFFFFFFF));
            line.append(tooltipSpace(2));
        }
        return line;
    }

    private Text skillRequirementValues(WynnItem item, String[] keys) {
        MutableText line = Text.empty();
        for (int i = 0; i < keys.length; i++) {
            int requirement = item.stat(keys[i]);
            boolean fulfilled = requirement == 0 || skillPointPlan.finalSkills()[i] >= requirement;
            String icon = requirement == 0 ? "\uE005" : fulfilled ? "\uE006" : "\uE007";
            line.append(tooltipTextWithWhiteShadow(
                    icon + "\uDAFF\uDFFF", "tooltip/requirement/sprite", 0xFFFFFFFF));
            line.append(tooltipSpace(3));
            line.append(tooltipText(Integer.toString(requirement), "language/wynncraft",
                    requirement == 0 ? 0xFF555555 : fulfilled ? 0xFFACFAC6 : 0xFFFAACAC));
            line.append(tooltipSpace(10));
        }
        return line;
    }

    private Text requirementLabel(String label, boolean fulfilled) {
        return tooltipTextWithWhiteShadow((fulfilled ? "\uE006" : "\uE007") + "\uDAFF\uDFFF",
                "tooltip/requirement/sprite", 0xFFFFFFFF)
                .append(tooltipText(label, "language/wynncraft", 0xFFFFFFFF));
    }

    private Text tooltipDivider(String tier) {
        return tooltipTextWithWhiteShadow("\uE000", "tooltip/divider", tooltipDividerColor(tier));
    }

    private Text tooltipSprite(String sprite) {
        return tooltipTextWithWhiteShadow(sprite, "tooltip/attribute/sprite", 0xFFFFFFFF);
    }

    private Text identificationValue(String value, boolean beneficial) {
        int color = beneficial ? 0xFFACFAC6 : 0xFFFAACAC;
        int darkColor = beneficial ? 0xFF6D9E7E : 0xFFA87474;
        int separator = value.indexOf(" to ");
        if (separator < 0) return tooltipText(value, "language/wynncraft", color);
        return tooltipText(value.substring(0, separator), "language/wynncraft", color)
                .append(tooltipText(" to ", "language/wynncraft", darkColor))
                .append(tooltipText(value.substring(separator + 4), "language/wynncraft", color));
    }

    private MutableText alignedTooltipLine(Text left, Text right, int width) {
        MutableText line = Text.empty().append(left);
        line.append(tooltipSpace(width - textRenderer.getWidth(left) - textRenderer.getWidth(right)));
        return line.append(right);
    }

    private MutableText centerTooltipLine(Text value, int width) {
        return tooltipSpace(Math.max(0, (width - textRenderer.getWidth(value)) / 2)).append(value);
    }

    private static MutableText tooltipText(String value, String font, int color) {
        return Text.literal(value).fillStyle(Style.EMPTY
                .withFont(new StyleSpriteSource.Font(Identifier.ofVanilla(font)))
                .withColor(color));
    }

    private static MutableText tooltipTextWithWhiteShadow(String value, String font, int color) {
        return Text.literal(value).fillStyle(Style.EMPTY
                .withFont(new StyleSpriteSource.Font(Identifier.ofVanilla(font)))
                .withColor(color)
                .withShadowColor(0xFFFFFF));
    }

    private static MutableText tooltipSpace(int pixels) {
        if (pixels == 0) return Text.empty();
        int codePoint = 0xD0000 + pixels;
        return tooltipText(new String(Character.toChars(codePoint)), "space", 0xFFFFFFFF);
    }

    private MutableText tooltipBanner(String value, int backgroundColor, int foregroundColor) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder background = new StringBuilder("\uE060\uDAFF\uDFFF");
        StringBuilder foreground = new StringBuilder();
        for (char character : lower.toCharArray()) {
            if (character == ' ') {
                background.append('\uE061');
                foreground.append(' ');
            } else {
                background.append(bannerGlyph(character, true));
                foreground.append(bannerGlyph(character, false));
            }
            background.append("\uDAFF\uDFFF");
        }
        background.append('\uE062');
        MutableText line = tooltipText(background.toString(), "banner/box", backgroundColor);
        line.append(tooltipSpace(2 - textRenderer.getWidth(line)));
        line.append(tooltipText(foreground.toString(), "banner/box", foregroundColor));
        return line;
    }

    private static char bannerGlyph(char value, boolean background) {
        int base;
        if (value >= 'a' && value <= 'z') base = value - 'a';
        else if (value >= '0' && value <= '9') base = 0x20 + value - '0';
        else base = switch (value) {
            case '?' -> 0x1A;
            case '[' -> 0x1B;
            case ']' -> 0x1C;
            case '/' -> 0x1D;
            case '%' -> 0x1E;
            case '&' -> 0x1F;
            case '!' -> 0x2A;
            case '(' -> 0x2B;
            case ')' -> 0x2C;
            case '<' -> 0x2D;
            case '=' -> 0x2E;
            case '>' -> 0x2F;
            case '+' -> 0x70;
            case '-' -> 0x71;
            case '.' -> 0x72;
            default -> 0x1A;
        };
        if (background) base += base >= 0x70 ? 0x10 : 0x30;
        return (char) ('\uE000' + base);
    }

    private void appendWrappedTooltip(List<Text> tooltip, String value, int color) {
        StringBuilder line = new StringBuilder();
        for (String word : value.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && textRenderer.getWidth(candidate) > 150) {
                tooltip.add(tooltipText(line.toString(), "language/wynncraft", color));
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) tooltip.add(tooltipText(line.toString(), "language/wynncraft", color));
    }

    private static int tooltipDividerColor(String tier) {
        if (tier == null) return 0xFFE0E0E0;
        return switch (tier.toLowerCase(Locale.ROOT)) {
            case "unique" -> 0xFFFFF2B3;
            case "rare", "set" -> 0xFFF2C2F2;
            case "legendary" -> 0xFFC2F2F2;
            case "fabled" -> 0xFFF2C2C2;
            case "mythic" -> 0xFFE0B3E6;
            default -> 0xFFE0E0E0;
        };
    }

    private static int tierIndex(String tier) {
        if (tier == null) return 0;
        return switch (tier.toLowerCase(Locale.ROOT)) {
            case "unique" -> 1;
            case "rare", "set" -> 2;
            case "legendary" -> 3;
            case "fabled" -> 4;
            case "mythic" -> 5;
            case "crafted" -> 6;
            default -> 0;
        };
    }

    private static String emblemFrame(String emblem) {
        String[] parts = emblem == null ? new String[0] : emblem.toLowerCase(Locale.ROOT).split("_");
        int shape = parts.length == 0 ? 0 : switch (parts[0]) {
            case "square" -> 1;
            case "hexagon" -> 2;
            case "shield" -> 3;
            case "sticker" -> 4;
            case "circle" -> 5;
            default -> 0;
        };
        int variant = 1;
        if (parts.length > 1) {
            try {
                variant = Math.clamp(Integer.parseInt(parts[1]), 1, 6);
            } catch (NumberFormatException ignored) {}
        }
        return String.valueOf((char) ('\uE000' + (variant - 1) * 0x10 + shape));
    }

    private static String typeSprite(WynnItem item) {
        return switch (item.subType().toLowerCase(Locale.ROOT)) {
            case "helmet" -> "\uE000";
            case "chestplate" -> "\uE001";
            case "leggings" -> "\uE002";
            case "boots" -> "\uE003";
            case "bow" -> "\uE004";
            case "dagger" -> "\uE005";
            case "wand" -> "\uE006";
            case "relik" -> "\uE007";
            case "spear" -> "\uE008";
            case "ring" -> "\uE014";
            case "bracelet" -> "\uE015";
            case "necklace" -> "\uE016";
            default -> "\uE000";
        };
    }

    private static String itemTypeName(WynnItem item) {
        return item.subType().isBlank() ? item.type().toUpperCase(Locale.ROOT)
                : item.subType().toUpperCase(Locale.ROOT);
    }

    private static String attackSpeed(String speed) {
        return switch (speed.toLowerCase(Locale.ROOT).replace("_", "")) {
            case "superfast" -> "4.3";
            case "veryfast" -> "3.1";
            case "fast" -> "2.5";
            case "normal" -> "2.05";
            case "slow" -> "1.5";
            case "veryslow" -> "0.83";
            case "superslow" -> "0.51";
            default -> "?";
        };
    }

    private static String readableTooltipValue(String value) {
        String readable = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return readable.isEmpty() ? "" : Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
    }

    private final class ClassSelectorWidget extends Widget {
        private final int left;
        private final int top;
        private final int controlWidth;
        private boolean open;

        private ClassSelectorWidget(int left, int top, int width, int height) {
            super(left * 2, top * 2, width * 2, height * 2);
            setUiScaleFactor(1.0);
            this.left = left;
            this.top = top;
            this.controlWidth = width;
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            drawClassOption(context, selectedClass, left, top, mouseX, mouseY);
            ui.drawTextWithShadow(textRenderer, open ? "▲" : "▼", left + controlWidth - 12, top + 6,
                    UiTheme.MUTED);
            if (!open) return;
            UiTheme.drawPanel(ui, left, top + 22, controlWidth, CharacterClass.playableValues().length * 22 + 4);
            for (int i = 0; i < CharacterClass.playableValues().length; i++) {
                drawClassOption(context, CharacterClass.playableValues()[i], left + 2, top + 24 + i * 22,
                        mouseX, mouseY);
            }
        }

        private void drawClassOption(DrawContext context, CharacterClass abilityClass, int x, int y,
                int mouseX, int mouseY) {
            boolean hovered = mouseX >= x && mouseX < x + controlWidth - (x == left ? 0 : 4)
                    && mouseY >= y && mouseY < y + 20;
            UiTheme.drawRoundedBox(ui, x, y, controlWidth - (x == left ? 0 : 4), 20,
                    hovered ? UiTheme.hoverBackground() : UiTheme.SURFACE,
                    abilityClass == selectedClass ? UiTheme.accent() : UiTheme.BORDER);
            Identifier texture = Identifier.of("wynnextras", "textures/gui/profileviewer/classes/"
                    + abilityClass.apiName() + ".png");
            ui.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x + 4, y + 2,
                    0, 0, 14, 16, 283, 324, 283, 324);
            ui.drawTextWithShadow(textRenderer, abilityClass.displayName(), x + 23, y + 6,
                    abilityClass.color());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return false;
            if (mouseX >= left && mouseX < left + controlWidth && mouseY >= top && mouseY < top + 20) {
                open = !open;
                return true;
            }
            if (open && mouseX >= left && mouseX < left + controlWidth
                    && mouseY >= top + 22 && mouseY < top + 26 + CharacterClass.playableValues().length * 22) {
                int index = (int) ((mouseY - top - 24) / 22);
                if (index >= 0 && index < CharacterClass.playableValues().length) {
                    selectClass(CharacterClass.playableValues()[index]);
                }
                return true;
            }
            if (open) {
                open = false;
                return true;
            }
            return false;
        }
    }

    private void selectClass(CharacterClass abilityClass) {
        if (selectedClass != abilityClass) {
            selectedClass = abilityClass;
            selectedAspects.clear();
            offhands.clear();
            WynnItem weapon = equipped.get(BuildSlot.WEAPON);
            if (weapon != null && CharacterClass.fromWeaponSubtype(weapon.subType()) != abilityClass) {
                equipped.remove(BuildSlot.WEAPON);
                powderCodes.remove(BuildSlot.WEAPON);
                recalculateSkillPointPlan();
            }
            AbilityTreeDatabase.getInstance().loadAsync(abilityClass);
            saveWorkspace();
        }
        rebuildWidgets();
    }

    private final class PickerOverlay extends Widget {
        private PickerOverlay() {
            super(0, 0, BuilderScreen.this.width * 2, BuilderScreen.this.height * 2);
            setUiScaleFactor(1.0);
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            renderPicker(context);
        }
    }

    private void renderItemDetails(DrawContext context, float delta) {
        BuildSlot detailSlot = slotForItem(detailItem);
        detailMaxScroll = ItemInspectionCard.render(context, textRenderer, detailItem,
                powderCodeForItem(detailItem, detailSlot), width, height,
                tierRgb(detailItem.tier()), detailScroll, delta,
                (x, y, size) -> drawEquipmentIcon(context, detailSlot, detailItem, x, y, size));
    }

    private BuildSlot slotForItem(WynnItem item) {
        for (Map.Entry<BuildSlot, WynnItem> entry : equipped.entrySet()) {
            if (entry.getValue() == item) {
                return entry.getKey();
            }
        }
        return BuildSlot.WEAPON;
    }

    private String powderCodeForItem(WynnItem item, BuildSlot slot) {
        for (Offhand offhand : offhands) {
            if (offhand.item() == item) return offhand.powders();
        }
        return powderCodes.getOrDefault(slot, "");
    }

    private BuildCalculator.Result calculateStats() {
        if (statsCache == null) {
            statsCache = BuildCalculator.calculate(
                    level, assignedSkills, equippedItems(), new ArrayList<>(selectedTomes.values()));
        }
        return statsCache;
    }

    private void refreshSkillFields(boolean includeFocused) {
        int[] skills = calculateStats().skills();
        for (int i = 0; i < skillFields.size() && i < skills.length; i++) {
            TextInputWidget field = skillFields.get(i);
            if (includeFocused || !field.isFocused()) field.setInput(Integer.toString(skills[i]));
        }
    }

    private boolean meetsCurrentSkillRequirements(WynnItem item) {
        BuildCalculator.Result stats = currentStatsForItem(item);
        return stats.activeItems().contains(item)
                && BuildCalculator.meetsCurrentSkillRequirements(item, stats.skills());
    }

    private int[] currentSkillsForItem(WynnItem item) {
        return currentStatsForItem(item).skills();
    }

    private BuildCalculator.Result currentStatsForItem(WynnItem item) {
        for (Offhand offhand : offhands) {
            if (offhand.item() == item) {
                return offhandStatsCache.computeIfAbsent(item, ignored -> calculateOffhandStats(offhand));
            }
        }
        return calculateStats();
    }

    private BuildCalculator.Result calculateOffhandStats(Offhand offhand) {
        List<BuildCalculator.EquippedItem> items = new ArrayList<>();
        for (Map.Entry<BuildSlot, WynnItem> entry : equipped.entrySet()) {
            if (entry.getKey() == BuildSlot.WEAPON) continue;
            items.add(new BuildCalculator.EquippedItem(
                    entry.getValue(), powderCodes.getOrDefault(entry.getKey(), ""), false));
        }
        items.add(new BuildCalculator.EquippedItem(offhand.item(), offhand.powders(), true));
        return BuildCalculator.calculate(
                level, assignedSkills, items, new ArrayList<>(selectedTomes.values()));
    }

    private void invalidateStats() {
        statsCache = null;
        offhandStatsCache.clear();
        damageCacheKey = Integer.MIN_VALUE;
    }

    WynnTome selectedTome(String slot) {
        return selectedTomes.get(slot);
    }

    void selectTome(String slot, WynnTome tome) {
        if (tome == null) {
            selectedTomes.remove(slot);
        } else {
            selectedTomes.put(slot, tome);
        }
        recalculateSkillPointPlan();
    }

    AspectSelection selectedAspect(String slot) {
        return selectedAspects.get(slot);
    }

    String selectAspect(String slot, AspectSelection selection) {
        if (selection == null) {
            selectedAspects.remove(slot);
            damageCacheKey = Integer.MIN_VALUE;
            return "";
        }
        boolean duplicate = selectedAspects.entrySet().stream()
                .anyMatch(entry -> !entry.getKey().equals(slot)
                        && entry.getValue().aspect().id() == selection.aspect().id()
                        && entry.getValue().aspect().abilityClass()
                                == selection.aspect().abilityClass());
        if (duplicate) {
            return "That aspect is already selected.";
        }
        boolean anotherMythic = "Mythic".equalsIgnoreCase(selection.aspect().rarity())
                && selectedAspects.entrySet().stream()
                        .anyMatch(entry -> !entry.getKey().equals(slot)
                                && "Mythic".equalsIgnoreCase(
                                        entry.getValue().aspect().rarity()));
        if (anotherMythic) {
            return "Only one Mythic aspect can be selected.";
        }
        selectedAspects.put(slot, selection);
        damageCacheKey = Integer.MIN_VALUE;
        return "";
    }

    private static CharacterClass classForName(String name) {
        for (CharacterClass abilityClass : CharacterClass.playableValues()) {
            if (abilityClass.apiName().equalsIgnoreCase(name)
                    || abilityClass.displayName().equalsIgnoreCase(name)) {
                return abilityClass;
            }
        }
        return CharacterClass.ARCHER;
    }

    private List<BuildCalculator.EquippedItem> equippedItems() {
        List<BuildCalculator.EquippedItem> items = new ArrayList<>();
        for (Map.Entry<BuildSlot, WynnItem> entry : equipped.entrySet()) {
            items.add(new BuildCalculator.EquippedItem(
                    entry.getValue(),
                    powderCodes.getOrDefault(entry.getKey(), ""),
                    entry.getKey() == BuildSlot.WEAPON));
        }
        return items;
    }

    private void recalculateSkillPointPlan() {
        invalidateStats();
        skillPointPlan = BuildCalculator.optimizeSkillPoints(
                level, equippedItems(), new ArrayList<>(selectedTomes.values()));
        skillPointError = "";
        skillPointSuccess = "";
    }

    private void autoAssignSkillPoints(boolean includeOffhands) {
        invalidateStats();
        skillPointPlan = includeOffhands
                ? BuildCalculator.optimizeSkillPointsForAllWeapons(
                        level, equippedItems(), offhandItems(), new ArrayList<>(selectedTomes.values()))
                : BuildCalculator.optimizeSkillPoints(
                        level, equippedItems(), new ArrayList<>(selectedTomes.values()));
        skillPointError = "";
        skillPointSuccess = "";
        if (!skillPointPlan.underPerSkillCap() || !skillPointPlan.withinAvailablePoints()) {
            notice = "";
            skillPointError = "Cannot auto assign: " + skillPointPlan.problem();
            skillPointSuccess = "";
            return;
        }
        System.arraycopy(skillPointPlan.assigned(), 0, assignedSkills, 0, assignedSkills.length);
        int remaining = availableSkillPoints() - totalAssigned();
        notice = "";
        skillPointError = "";
        skillPointSuccess = "Assigned the required skill points for "
                + (includeOffhands ? "all weapons. " : "the main weapon. ")
                + remaining + " remain freely assignable.";
        saveWorkspace();
        rebuildWidgets();
    }

    private List<WynnItem> offhandItems() {
        return offhands.stream().map(Offhand::item).toList();
    }

    private String[] spellNames() {
        WynnItem weapon = equipped.get(BuildSlot.WEAPON);
        String type = weapon == null ? "" : weapon.subType().toLowerCase();
        return switch (type) {
            case "dagger" -> new String[]{"Main Attack", "Spin Attack", "Vanish", "Multihit", "Smoke Bomb"};
            case "spear" -> new String[]{"Main Attack", "Bash", "Charge", "Uppercut", "War Scream"};
            case "wand" -> new String[]{"Main Attack", "Heal", "Teleport", "Meteor", "Ice Snake"};
            case "relik" -> new String[]{"Main Attack", "Totem", "Haul", "Aura", "Uproot"};
            default -> new String[]{"Bow Shot", "Arrow Storm", "Escape", "Arrow Bomb", "Arrow Shield"};
        };
    }

    private int rootSpellIndex(WynnItem weapon) {
        if (weapon == null || weapon.subType() == null) {
            return -1;
        }
        return switch (weapon.subType().toLowerCase()) {
            case "bow", "wand" -> 3;
            case "dagger", "spear", "relik" -> 1;
            default -> -1;
        };
    }

    private Layout layout() {
        int total = this.width - MARGIN * 2 - GAP * 2;
        int leftWidth = Math.max(320, (int) Math.round(total * 0.49));
        int middleWidth = Math.max(200, (int) Math.round(total * 0.24));
        if (leftWidth + middleWidth > total - 220) {
            leftWidth = Math.max(320, total - 430);
            middleWidth = 200;
        }

        int rightWidth = total - leftWidth - middleWidth;
        int middleX = MARGIN + leftWidth + GAP;
        int rightX = middleX + middleWidth + GAP;
        int equipmentTop = CONTENT_TOP;
        int equipmentHeight = offhands.isEmpty() ? 280 : 328;
        int skillTop = equipmentTop + equipmentHeight + GAP;
        int skillHeight = 121;
        int boostTop = skillTop + skillHeight + GAP;
        int boostHeight = Math.max(58, this.height - MARGIN - boostTop);
        return new Layout(
                leftWidth, middleX, middleWidth, rightX, rightWidth,
                equipmentTop, equipmentHeight, skillTop, skillHeight, boostTop, boostHeight);
    }

    private SlotPosition slotPosition(Layout layout, BuildSlot slot) {
        int columnGap = 6;
        int columnWidth = (layout.leftWidth - 12 - columnGap) / 2;
        boolean rightColumn = slot.ordinal() >= 5;
        int row = rightColumn ? slot.ordinal() - 5 : slot.ordinal();
        int x = MARGIN + 6 + (rightColumn ? columnWidth + columnGap : 0);
        int y = layout.equipmentTop + 32 + row * ITEM_ROW_HEIGHT;
        return new SlotPosition(x, y, columnWidth);
    }

    private SlotPosition offhandPosition(Layout layout, int index) {
        SlotPosition weapon = slotPosition(layout, BuildSlot.WEAPON);
        boolean rightColumn = index == 0 || index == 2;
        int row = index == 0 ? 0 : 1;
        return new SlotPosition(
                weapon.x + (rightColumn ? weapon.width + 6 : 0),
                weapon.y + row * ITEM_ROW_HEIGHT,
                weapon.width);
    }

    private int totalAssigned() {
        int total = 0;
        for (int value : assignedSkills) total += value;
        return total;
    }

    private int availableSkillPoints() {
        return BuildCalculator.levelToSkillPoints(level);
    }

    private String weaponDamage(WynnItem weapon) {
        int min = 0;
        int max = 0;
        for (String element : List.of("nDam", "eDam", "tDam", "wDam", "fDam", "aDam")) {
            min += weapon.stat(element + "Min");
            max += weapon.stat(element + "Max");
        }
        return min == 0 && max == 0 ? "Unavailable" : min + "-" + max;
    }

    private boolean validPowderText(String text, int slots) {
        if (text.length() > slots * 2) return false;
        if (text.isEmpty()) return true;
        return text.matches("(?i)([etwfa][1-7])*[etwfa]?");
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.005D) {
            return String.format(Locale.US, "%,d", Math.round(value));
        }
        return String.format(Locale.US, "%,.2f", value);
    }

    private static String formatRange(BuildCalculator.DamageRange range) {
        return range == null ? "0" : format(range.min()) + "-" + format(range.max());
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private static Formatting tierColor(String tier) {
        if (tier == null) return Formatting.WHITE;
        return switch (tier.toLowerCase()) {
            case "crafted" -> Formatting.DARK_AQUA;
            case "mythic" -> Formatting.DARK_PURPLE;
            case "fabled" -> Formatting.RED;
            case "legendary" -> Formatting.AQUA;
            case "rare" -> Formatting.LIGHT_PURPLE;
            case "unique" -> Formatting.YELLOW;
            case "set" -> Formatting.GREEN;
            default -> Formatting.WHITE;
        };
    }

    private static int tierRgb(String tier) {
        if (tier == null) return 0xFFFFFFFF;
        return switch (tier.toLowerCase()) {
            case "crafted" -> 0xFF00AAAA;
            case "mythic" -> 0xFFAA00AA;
            case "fabled" -> 0xFFFF5555;
            case "legendary" -> 0xFF55FFFF;
            case "rare" -> 0xFFFF55FF;
            case "unique" -> 0xFFFFFF55;
            case "set" -> 0xFF55FF55;
            default -> 0xFFFFFFFF;
        };
    }

    @Override
    public void close() {
        if (detailItem != null) {
            detailItem = null;
            detailScroll.jump(0.0F);
            return;
        }
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    private enum BuildSlot {
        HELMET("Helmet", "H", "helmet", true),
        CHESTPLATE("Chestplate", "C", "chestplate", true),
        LEGGINGS("Leggings", "L", "leggings", true),
        BOOTS("Boots", "B", "boots", true),
        WEAPON("Weapon", "W", "weapon", true),
        RING_1("Ring 1", "R1", "ring", false),
        RING_2("Ring 2", "R2", "ring", false),
        BRACELET("Bracelet", "Br", "bracelet", false),
        NECKLACE("Necklace", "N", "necklace", false);

        final String label;
        final String shortLabel;
        final String searchType;
        final boolean powderable;

        BuildSlot(String label, String shortLabel, String searchType, boolean powderable) {
            this.label = label;
            this.shortLabel = shortLabel;
            this.searchType = searchType;
            this.powderable = powderable;
        }
    }

    private record StatDisplay(String key, String label, String suffix, int color, boolean reversed) {
    }

    private record DamageCardBounds(int spell, int left, int top, int right, int bottom) {
    }

    private final class AbilityPresetOverlayWidget extends Widget {
        private AbilityPresetOverlayWidget() {
            super(0, 0, BuilderScreen.this.width, BuilderScreen.this.height);
            setUiScaleFactor(1.0);
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            Layout layout = layout();
            int areaLeft = layout.middleX;
            int areaRight = layout.rightX + layout.rightWidth;
            int areaTop = layout.equipmentTop;
            int areaBottom = BuilderScreen.this.height - MARGIN;
            ui.fill(areaLeft, areaTop, areaRight, areaBottom, 0xBB000000);
            AbilityPresetPanel panel = abilityPresetPanel();
            UiTheme.drawPanel(ui, panel.x(), panel.y(), panel.width(), panel.height());
            String title = abilityPresetOverlay == AbilityPresetOverlay.SAVE
                    ? "Save Ability Tree" : "Saved Ability Trees";
            ui.drawCenteredTextWithShadow(
                    textRenderer,
                    title,
                    panel.x() + panel.width() / 2,
                    panel.y() + 14,
                    UiTheme.accentRgb());
            if (abilityPresetOverlay == AbilityPresetOverlay.SAVE) {
                ui.drawCenteredTextWithShadow(
                        textRenderer,
                        abilityTreeState.selected(selectedClass).size() + " selected abilities",
                        panel.x() + panel.width() / 2,
                        panel.y() + 70,
                        selectedClass.color());
            } else if (visibleAbilityPresets.isEmpty()) {
                ui.drawCenteredTextWithShadow(
                        textRenderer,
                        "No saved trees yet.",
                        panel.x() + panel.width() / 2,
                        panel.y() + 70,
                        0xFFAAAAAA);
            }
            if (!abilityTreeStatus.isEmpty()
                    && System.currentTimeMillis() < abilityTreeStatusUntil) {
                ui.drawCenteredTextWithShadow(
                        textRenderer,
                        abilityTreeStatus,
                        panel.x() + panel.width() / 2,
                        panel.y() + panel.height() - 48,
                        0xFFFF5555);
            }
        }
    }

    private enum AbilityPresetOverlay {
        NONE,
        SAVE,
        SAVED
    }

    private record AbilityPresetPanel(int x, int y, int width, int height) {
    }

    private record AbilityTreeLayout(
            int treeLeft,
            int treeWidth,
            int sidebarX,
            int sidebarWidth
    ) {
    }

    private record AbilitySidebarEntry(
            AbilityTreeDefinition.Node node,
            List<net.minecraft.text.OrderedText> lines,
            int height
    ) {
    }

    private record ArchetypeIcon(int u, int v) {
    }

    private record BuilderFooterLayout(int buttonX, int buttonWidth) {
    }

    private record EncodedBuild(String hash, List<String> warnings, Set<String> warningNodeIds) {
    }

    private record Layout(
            int leftWidth,
            int middleX,
            int middleWidth,
            int rightX,
            int rightWidth,
            int equipmentTop,
            int equipmentHeight,
            int skillTop,
            int skillHeight,
            int boostTop,
            int boostHeight) {}

    private record SlotPosition(int x, int y, int width) {}

    private record Offhand(WynnItem item, String powders) {}
}
