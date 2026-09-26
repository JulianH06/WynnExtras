package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.config.SavedBuild;
import julianh06.wynnextras.features.buildplanner.config.SavedBuildManager;
import julianh06.wynnextras.features.buildplanner.config.ToolWorkspaceManager;
import julianh06.wynnextras.features.buildplanner.data.BuildCalculator;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeClass;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeDatabase;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeDefinition;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeState;
import julianh06.wynnextras.features.buildplanner.data.AspectDatabase;
import julianh06.wynnextras.features.buildplanner.data.AspectSelection;
import julianh06.wynnextras.features.buildplanner.data.DamageCalculator;
import julianh06.wynnextras.features.buildplanner.data.ItemDatabase;
import julianh06.wynnextras.features.buildplanner.data.IdentificationUnits;
import julianh06.wynnextras.features.buildplanner.data.SkillPointWarning;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import julianh06.wynnextras.features.buildplanner.data.WynnTome;
import julianh06.wynnextras.features.buildplanner.data.WynnAspect;
import julianh06.wynnextras.features.buildplanner.data.TomeDatabase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class WynnBuilderScreen extends Screen {
    private static final Identifier EQUIPMENT_TEXTURE = Identifier.of("wynnextras", "textures/gui/buildplanner/equipment.png");
    private static final int MARGIN = 8;
    private static final int GAP = 8;
    private static final int ITEM_ROW_HEIGHT = 48;
    private static final int ITEM_INFO_WIDTH = 106;
    private static final int SKILL_CARD_HEIGHT = 97;
    private static final int POSITIVE = 0xFF00FF40;
    private static final int NEGATIVE = 0xFFFF3030;
    private static final int EARTH = 0xFF20C020;
    private static final int THUNDER = 0xFFFFFF20;
    private static final int WATER = 0xFF20FFFF;
    private static final int FIRE = 0xFFFF4040;
    private static final int AIR = 0xFFFFFFFF;
    private static final int RESULT_LIMIT = 10;
    private static final String[] SKILL_NAMES = {"Strength", "Dexterity", "Intelligence", "Defense", "Agility"};
    private static final String[] SKILL_EFFECTS = {"damage", "crit", "cost red.", "resist", "dodge"};
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
    private String notifiedWorkspaceError = "";
    private int workspaceTicks;
    private final Map<BuildSlot, WynnItem> equipped = new EnumMap<>(BuildSlot.class);
    private final Map<BuildSlot, String> powderCodes = new EnumMap<>(BuildSlot.class);
    private final Map<String, WynnTome> selectedTomes = new LinkedHashMap<>();
    private final Map<String, AspectSelection> selectedAspects = new LinkedHashMap<>();
    private final Map<WynnItem, ItemStack> iconCache = new IdentityHashMap<>();
    private final int[] assignedSkills = new int[5];
    private final AbilityTreeState abilityTreeState = new AbilityTreeState();
    private final List<ThemedButton> dynamicButtons = new ArrayList<>();
    private final List<ThemedButton> resultButtons = new ArrayList<>();
    private final List<TextFieldWidget> dynamicFields = new ArrayList<>();
    private final List<TextWidget> dynamicLabels = new ArrayList<>();
    private BuildSlot pickerSlot;
    private WynnItem detailItem;
    private final SmoothScroll detailScroll = new SmoothScroll(16.0F);
    private int detailMaxScroll;
    private final SmoothScroll summaryScroll = new SmoothScroll(16.0F);
    private int summaryMaxScroll;
    private final SmoothScroll damageScroll = new SmoothScroll(16.0F);
    private int damageMaxScroll;
    private int expandedSpell = -1;
    private final List<DamageCardBounds> damageCards = new ArrayList<>();
    private int damageCacheKey = Integer.MIN_VALUE;
    private List<DamageCalculator.SpellResult> damageCache = List.of();
    private TextFieldWidget searchField;
    private ThemedButton abilityTreeButton;
    private BuildCalculator.SkillPointPlan skillPointPlan =
            BuildCalculator.optimizeSkillPoints(121, List.of());
    private int resultOffset;
    private int level = 121;
    private boolean detailed;
    private String boostSection = "Ability Boosts";

    public WynnBuilderScreen(Screen parent) {
        this(parent, WorkspaceTabs.builderId());
    }

    WynnBuilderScreen(Screen parent, String toolTabId) {
        super(Text.literal("WynnBuilder"));
        this.parent = parent;
        this.toolTabId = toolTabId;
    }

    @Override
    protected void init() {
        super.init();
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
    }

    private void rebuildWidgets() {
        for (ThemedButton button : dynamicButtons) {
            this.remove(button);
        }
        dynamicButtons.clear();
        resultButtons.clear();
        for (TextFieldWidget field : dynamicFields) {
            this.remove(field);
        }
        dynamicFields.clear();
        for (TextWidget label : dynamicLabels) {
            this.remove(label);
        }
        dynamicLabels.clear();
        searchField = null;
        abilityTreeButton = null;

        if (pendingBuild != null) {
            buildTabWidgets();
            addDynamic(new ThemedButton(
                    this.width - 74, this.height - 28, 64, 20, Text.literal("Done"), this::close));
            return;
        }
        if (pickerSlot != null) {
            buildPickerWidgets();
            return;
        }

        Layout layout = layout();
        buildTabWidgets();
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
            TextFieldWidget powderField = new TextFieldWidget(
                    this.textRenderer, position.x + ITEM_INFO_WIDTH + 5, position.y + 25,
                    position.width - ITEM_INFO_WIDTH - 9, 18,
                    Text.literal("Powders"));
            int powderSlots = item == null ? 0 : item.stat("powderSlots");
            powderField.setMaxLength(Math.max(1, powderSlots * 2));
            powderField.setText(powderCodes.getOrDefault(slot, ""));
            powderField.setPlaceholder(Text.literal(powderSlots + " powder slots"));
            powderField.setEditable(item != null && powderSlots > 0);
            powderField.setTextPredicate(text -> validPowderText(text, powderSlots));
            powderField.setChangedListener(text -> powderCodes.put(slot, text.toLowerCase()));
            addDynamicField(powderField);
            if (powderSlots == 0 && item != null) {
                powderField.setText("0 slots");
            }
        }

        int skillY = layout.skillTop + 26;
        int skillCardWidth = (layout.leftWidth - 8) / 5;
        for (int i = 0; i < SKILL_NAMES.length; i++) {
            int index = i;
            int cardX = MARGIN + 4 + i * skillCardWidth;
            TextFieldWidget skillField = new TextFieldWidget(
                    this.textRenderer, cardX + 8, skillY + 11, skillCardWidth - 16, 20,
                    Text.literal(SKILL_NAMES[i]));
            int itemBonus = skillPointPlan.finalSkills()[i] - skillPointPlan.assigned()[i];
            skillField.setMaxLength(4);
            skillField.setCentered(true);
            skillField.setText(Integer.toString(assignedSkills[i] + itemBonus));
            skillField.setTextPredicate(text -> text.matches("-?\\d{0,3}"));
            skillField.setChangedListener(text -> {
                if (!text.isEmpty() && !"-".equals(text)) {
                    assignedSkills[index] = Math.max(0,
                            Math.min(999, Integer.parseInt(text) - itemBonus));
                }
            });
            addDynamicField(skillField);
        }

        int levelControlX = layout.middleX + (layout.middleWidth - 148) / 2;
        int levelControlY = this.height - MARGIN - 24;
        TextWidget levelLabel = new TextWidget(Text.literal("Lv."), this.textRenderer);
        levelLabel.setPosition(levelControlX, levelControlY + 6);
        dynamicLabels.add(this.addDrawableChild(levelLabel));
        TextFieldWidget levelField = new TextFieldWidget(
                this.textRenderer, levelControlX + 22, levelControlY, 48, 20,
                Text.literal("Level"));
        levelField.setMaxLength(3);
        levelField.setCentered(true);
        levelField.setText(Integer.toString(level));
        levelField.setTextPredicate(text -> text.matches("\\d{0,3}"));
        levelField.setChangedListener(text -> {
            if (!text.isEmpty()) {
                int newLevel = Math.max(1, Math.min(121, Integer.parseInt(text)));
                if (newLevel < level) {
                    resetAbilityTrees();
                }
                level = newLevel;
                skillPointPlan = BuildCalculator.optimizeSkillPoints(
                        level, equippedItems(), new ArrayList<>(selectedTomes.values()));
            }
        });
        addDynamicField(levelField);
        addDynamic(new ThemedButton(
                levelControlX + 76, levelControlY, 64, 20, Text.literal("Reset"), () -> {
                    equipped.clear();
                    powderCodes.clear();
                    selectedTomes.clear();
                    selectedAspects.clear();
                    resetAbilityTrees();
                    for (int i = 0; i < assignedSkills.length; i++) assignedSkills[i] = 0;
                    skillPointPlan = BuildCalculator.optimizeSkillPoints(level, List.of());
                    tabName = workspace.defaultBuildName(toolTabId);
                    saveWorkspace();
                    rebuildWidgets();
                }));

        addDynamic(new ThemedButton(
                layout.middleX + 4, MARGIN + 4, (layout.middleWidth - 8) / 2, 20,
                Text.literal("Summary"), () -> {
                    detailed = false;
                    summaryScroll.jump(0.0F);
                }));
        addDynamic(new ThemedButton(
                layout.middleX + 4 + (layout.middleWidth - 8) / 2, MARGIN + 4,
                (layout.middleWidth - 8) / 2, 20, Text.literal("Detailed"), () -> {
                    detailed = true;
                    summaryScroll.jump(0.0F);
                }));
        int footerX = layout.rightX + 8;
        int footerY = this.height - MARGIN - 24;
        int footerGap = 4;
        int footerButtonWidth = (layout.rightWidth - 16 - footerGap * 2) / 3;
        addDynamic(new ThemedButton(
                footerX, footerY, footerButtonWidth, 20,
                Text.literal("Saved"), this::openSavedBuilds));
        addDynamic(new ThemedButton(
                footerX + footerButtonWidth + footerGap, footerY, footerButtonWidth, 20,
                Text.literal("Save"), this::openSaveBuild));
        addDynamic(new ThemedButton(
                footerX + (footerButtonWidth + footerGap) * 2, footerY, footerButtonWidth, 20,
                Text.literal("Done"), this::close));

        int boostButtonY = layout.boostTop + 5;
        int boostButtonWidth = (layout.leftWidth - 20) / 3;
        addDynamic(new ThemedButton(MARGIN + 5, boostButtonY, boostButtonWidth, 20,
                Text.literal("Ability Boosts"), () -> { }));
        addDynamic(new ThemedButton(MARGIN + 10 + boostButtonWidth, boostButtonY, boostButtonWidth, 20,
                Text.literal("Powder Specials"), () -> boostSection = "Powder Specials"));
        addDynamic(new ThemedButton(MARGIN + 15 + boostButtonWidth * 2, boostButtonY, boostButtonWidth, 20,
                Text.literal("Raid Buffs"), () -> boostSection = "Raid Buffs"));
        int secondRowY = boostButtonY + 24;
        addDynamic(new ThemedButton(MARGIN + 5, secondRowY, boostButtonWidth, 20,
                Text.literal("Tomes & Aspects"), () -> client.setScreen(new TomeScreen(this))));
        abilityTreeButton = new ThemedButton(
                MARGIN + 10 + boostButtonWidth, secondRowY, boostButtonWidth, 20,
                Text.literal("Ability Tree"), this::openAbilityTree);
        abilityTreeButton.active = equipped.containsKey(BuildSlot.WEAPON);
        addDynamic(abilityTreeButton);
    }

    private void buildPickerWidgets() {
        int pickerWidth = Math.min(520, this.width - 60);
        int pickerHeight = Math.min(326, this.height - 50);
        int left = (this.width - pickerWidth) / 2;
        int top = (this.height - pickerHeight) / 2;

        searchField = new TextFieldWidget(
                this.textRenderer, left + 14, top + 34, pickerWidth - 28, 20, Text.literal("Search"));
        searchField.setPlaceholder(Text.literal("Search " + pickerSlot.label.toLowerCase() + "..."));
        searchField.setMaxLength(100);
        searchField.setChangedListener(query -> {
            resultOffset = 0;
            rebuildPickerResults(left, top, pickerWidth);
        });
        addDynamicField(searchField);
        this.setInitialFocus(searchField);

        addDynamic(new ThemedButton(
                left + pickerWidth - 78, top + pickerHeight - 28, 64, 20, Text.literal("Cancel"), () -> {
                    pickerSlot = null;
                    resultOffset = 0;
                    rebuildWidgets();
                }));
        if (equipped.containsKey(pickerSlot)) {
            addDynamic(new ThemedButton(
                    left + 14, top + pickerHeight - 28, 64, 20, Text.literal("Remove"), () -> {
                            if (pickerSlot == BuildSlot.WEAPON) {
                                selectedAspects.clear();
                            }
                            equipped.remove(pickerSlot);
                        powderCodes.remove(pickerSlot);
                        autoAllocateSkillPoints();
                        pickerSlot = null;
                        rebuildWidgets();
                    }));
        }
        addDynamic(new ThemedButton(
                left + 88, top + pickerHeight - 28, 78, 20, Text.literal("Crafted"), () -> {
                    BuildSlot slot = pickerSlot;
                    client.setScreen(new CraftedItemScreen(this, slot.searchType, slot.name(), equipped.get(slot),
                            item -> equipItem(slot, item)));
                }));
        rebuildPickerResults(left, top, pickerWidth);
    }

    private void rebuildPickerResults(int left, int top, int pickerWidth) {
        for (ThemedButton button : resultButtons) {
            this.remove(button);
            dynamicButtons.remove(button);
        }
        resultButtons.clear();
        if (pickerSlot == null) return;

        List<WynnItem> results = ItemDatabase.getInstance()
                .searchItems(searchField == null ? "" : searchField.getText(), pickerSlot.searchType);
        int visible = Math.min(pickerVisibleCount(), Math.max(0, results.size() - resultOffset));
        for (int i = 0; i < visible; i++) {
            WynnItem item = results.get(i + resultOffset);
            Text label = Text.literal(item.displayName()).formatted(tierColor(item.tier()))
                    .append(Text.literal("   Lv. " + item.stat("lvl")).formatted(Formatting.GRAY));
            ThemedButton button = new ThemedButton(
                    left + 14, top + 62 + i * 22, pickerWidth - 28, 20, label,
                    () -> equipItem(pickerSlot, item));
            addDynamic(button);
            resultButtons.add(button);
        }
    }

    private void equipItem(BuildSlot slot, WynnItem item) {
        if (slot == BuildSlot.WEAPON) {
            WynnItem previous = equipped.get(BuildSlot.WEAPON);
            if (previous != null && AbilityTreeClass.fromWeaponSubtype(previous.subType())
                    != AbilityTreeClass.fromWeaponSubtype(item.subType())) {
                selectedAspects.clear();
            }
        }
        equipped.put(slot, item);
        String powders = powderCodes.getOrDefault(slot, "");
        powderCodes.put(slot, powders.substring(0, Math.min(powders.length(), item.stat("powderSlots") * 2)));
        autoAllocateSkillPoints();
        pickerSlot = null;
        resultOffset = 0;
        rebuildWidgets();
        saveWorkspace();
    }

    private void openPicker(BuildSlot slot) {
        pickerSlot = slot;
        resultOffset = 0;
        rebuildWidgets();
    }

    private void openAbilityTree() {
        WynnItem weapon = equipped.get(BuildSlot.WEAPON);
        if (this.client == null || weapon == null) {
            return;
        }
        AbilityTreeClass abilityClass = AbilityTreeClass.fromWeaponSubtype(
                weapon.subType());
        this.client.setScreen(new AbilityTreeScreen(this, abilityTreeState, abilityClass, level));
    }

    AbilityTreeClass selectedAbilityClass() {
        WynnItem weapon = equipped.get(BuildSlot.WEAPON);
        return weapon == null
                ? null
                : AbilityTreeClass.fromWeaponSubtype(weapon.subType());
    }

    private void openSaveBuild() {
        if (client == null) {
            return;
        }
        WynnItem weapon = equipped.get(BuildSlot.WEAPON);
        AbilityTreeClass abilityClass = AbilityTreeClass.fromWeaponSubtype(
                weapon == null ? "" : weapon.subType());
        client.setScreen(new SaveBuildScreen(this, abilityClass));
    }

    private void openSavedBuilds() {
        if (client != null) {
            client.setScreen(new SavedBuildsScreen(this));
        }
    }

    boolean saveBuild(String name, AbilityTreeClass abilityClass) {
        boolean saved = SavedBuildManager.getInstance().save(snapshotBuild(name, abilityClass, false));
        if (saved) {
            tabName = name;
            saveWorkspace();
        }
        return saved;
    }

    private SavedBuild snapshotBuild(String name, AbilityTreeClass abilityClass, boolean draft) {
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
        AbilityTreeClass loadedClass = loadedWeapon == null
                ? classForName(build.characterClass())
                : AbilityTreeClass.fromWeaponSubtype(loadedWeapon.subType());
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
        pickerSlot = null;
        detailItem = null;
        summaryScroll.jump(0.0F);
        damageScroll.jump(0.0F);
        expandedSpell = -1;
        damageCacheKey = Integer.MIN_VALUE;
        iconCache.clear();
        damageCards.clear();
        detailed = false;
        boostSection = "Ability Boosts";
    }

    private void restorePendingBuild() {
        SavedBuild build = pendingBuild;
        if (build.equipment().values().stream().anyMatch(value -> !CraftedItemCodec.isReference(value))
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
            if (Arrays.stream(AbilityTreeClass.values()).noneMatch(value -> value.apiName().equals(abilityClass))) {
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
        if (workspaceInitialized && pendingBuild == null) {
            AbilityTreeClass abilityClass = selectedAbilityClass();
            workspace.updateBuild(toolTabId, snapshotBuild(
                    tabName, abilityClass == null ? AbilityTreeClass.ARCHER : abilityClass, true));
        }
        String error = workspace.error();
        if (!error.isEmpty() && !error.equals(notifiedWorkspaceError) && client != null && client.player != null) {
            client.player.sendMessage(Text.literal(error).formatted(Formatting.RED), false);
        }
        notifiedWorkspaceError = error;
    }

    private void buildTabWidgets() {
        WorkspaceTabs.add(this, parent, toolTabId, MARGIN + 6, MARGIN + 4, layout().leftWidth - 12, this::addDynamic);
    }

    String toolTabId() { return toolTabId; }
    Screen workspaceParent() { return parent; }
    void equipFromTool(String slot, WynnItem item) {
        if (pendingBuild != null) throw new IllegalStateException("Restore the destination build's missing data before equipping.");
        equipItem(BuildSlot.valueOf(slot), item);
    }

    @Override
    public void tick() {
        super.tick();
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
        for (AbilityTreeClass abilityClass : AbilityTreeClass.values()) {
            abilityTreeState.reset(abilityClass);
        }
    }

    private void addDynamic(ThemedButton button) {
        dynamicButtons.add(this.addDrawableChild(button));
    }

    private void addDynamicField(TextFieldWidget field) {
        dynamicFields.add(this.addDrawableChild(field));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
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
        if (pickerSlot != null) {
            List<WynnItem> results = ItemDatabase.getInstance()
                    .searchItems(searchField == null ? "" : searchField.getText(), pickerSlot.searchType);
            int max = Math.max(0, results.size() - pickerVisibleCount());
            resultOffset = Math.max(0, Math.min(max, resultOffset - (int) Math.signum(verticalAmount)));
            int pickerWidth = Math.min(520, this.width - 60);
            int pickerHeight = Math.min(326, this.height - 50);
            rebuildPickerResults((this.width - pickerWidth) / 2, (this.height - pickerHeight) / 2, pickerWidth);
            return true;
        }
        Layout layout = layout();
        if (WorkspaceTabs.scroll(parent, toolTabId, MARGIN + 6, MARGIN + 4, layout.leftWidth - 12,
                mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
        if (mouseX >= layout.middleX && mouseX < layout.middleX + layout.middleWidth
                && mouseY >= MARGIN + 30 && mouseY < this.height - MARGIN - 28) {
            summaryScroll.move(
                    (float) -Math.signum(verticalAmount) * 18.0F,
                    0.0F,
                    summaryMaxScroll);
            return true;
        }
        if (mouseX >= layout.rightX && mouseX < layout.rightX + layout.rightWidth
                && mouseY >= MARGIN && mouseY < this.height - MARGIN - 28) {
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
        if (pendingBuild != null) {
            return super.mouseClicked(click, doubled);
        }
        if (detailItem != null) {
            detailItem = null;
            detailScroll.jump(0.0F);
            return true;
        }
        if (pickerSlot == null && click.button() == 0) {
            Layout layout = layout();
            for (BuildSlot slot : BuildSlot.values()) {
                WynnItem item = equipped.get(slot);
                if (item == null) {
                    continue;
                }
                SlotPosition position = slotPosition(layout, slot);
                if (click.x() >= position.x + 4 && click.x() < position.x + 46
                        && click.y() >= position.y + 3 && click.y() < position.y + 44) {
                    detailItem = item;
                    detailScroll.jump(0.0F);
                    detailMaxScroll = 0;
                    return true;
                }
            }
            for (DamageCardBounds card : damageCards) {
                if (click.x() >= card.left() && click.x() < card.right()
                        && click.y() >= card.top() && click.y() < card.bottom()) {
                    expandedSpell = expandedSpell == card.spell() ? -1 : card.spell();
                    damageScroll.jump(0.0F);
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, UiTheme.BACKGROUND);
        if (pendingBuild != null) {
            UiTheme.drawPanel(context, MARGIN, MARGIN, this.width - MARGIN * 2, this.height - MARGIN * 2);
            context.drawCenteredTextWithShadow(textRenderer,
                    fitText(restoreError, this.width - 40), this.width / 2, this.height / 2, 0xFFFFAA55);
            super.render(context, mouseX, mouseY, delta);
            renderWorkspaceError(context);
            return;
        }
        Layout layout = layout();
        BuildCalculator.Result stats = calculateStats();
        renderPanels(context, layout);
        renderEquipment(context, layout);
        renderSkills(context, layout, stats);
        renderSummary(context, layout, stats);
        renderDamage(context, layout, stats);
        renderBoostArea(context, layout, stats);
        if (pickerSlot != null) {
            renderPicker(context);
        }
        super.render(context, mouseX, mouseY, delta);
        if (pickerSlot == null && detailItem == null) {
            renderSkillDetails(context, layout, stats);
        }
        if (detailItem != null) {
            renderItemDetails(context);
        }
        renderWorkspaceError(context);
    }

    private void renderWorkspaceError(DrawContext context) {
        if (!workspace.error().isEmpty()) {
            context.fill(0, this.height - 12, this.width, this.height, 0xEE201010);
            context.drawCenteredTextWithShadow(textRenderer, fitText(workspace.error(), this.width - 12),
                    this.width / 2, this.height - 10, 0xFFFF5555);
        }
    }

    private void renderPanels(DrawContext context, Layout layout) {
        UiTheme.drawPanel(context, MARGIN, layout.equipmentTop, layout.leftWidth, layout.equipmentHeight);
        UiTheme.drawPanel(context, MARGIN, layout.skillTop, layout.leftWidth, layout.skillHeight);
        UiTheme.drawPanel(context, MARGIN, layout.boostTop, layout.leftWidth, layout.boostHeight);
        UiTheme.drawPanel(context, layout.middleX, MARGIN, layout.middleWidth, this.height - MARGIN * 2);
        UiTheme.drawPanel(context, layout.rightX, MARGIN, layout.rightWidth, this.height - MARGIN * 2);

    }

    private void renderEquipment(DrawContext context, Layout layout) {
        for (BuildSlot slot : BuildSlot.values()) {
            SlotPosition position = slotPosition(layout, slot);
            WynnItem item = equipped.get(slot);
            int border = item == null ? UiTheme.BORDER : tierRgb(item.tier());
            UiTheme.drawRoundedBox(
                    context, position.x + 4, position.y + 3, 42, 41, UiTheme.SURFACE, border);
            if (item == null) {
                context.drawCenteredTextWithShadow(this.textRenderer,
                        Text.literal(slot.shortLabel), position.x + 25, position.y + 19, 0xAAAAAA);
            }
            if (item != null) {
                drawEquipmentIcon(context, slot, item, position.x + 11, position.y + 9);
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
                context.drawTextWithShadow(
                        this.textRenderer, primary, primaryX, position.y + 10, primaryColor);
                context.drawTextWithShadow(this.textRenderer, "Lv. " + item.stat("lvl"),
                        position.x + 51, position.y + 28, 0xFFFFFFFF);
            }
        }
    }

    private void drawPixelHeart(DrawContext context, int x, int y) {
        int red = 0xFFFF3030;
        context.fill(x + 1, y, x + 3, y + 1, red);
        context.fill(x + 4, y, x + 6, y + 1, red);
        context.fill(x, y + 1, x + 7, y + 3, red);
        context.fill(x + 1, y + 3, x + 6, y + 4, red);
        context.fill(x + 2, y + 4, x + 5, y + 5, red);
        context.fill(x + 3, y + 5, x + 4, y + 6, red);
    }

    private void drawPixelWeapon(DrawContext context, int x, int y) {
        int blade = 0xFFE8E8E8;
        int shadow = 0xFF888888;
        int gold = 0xFFFFAA00;
        context.fill(x + 7, y, x + 9, y + 2, blade);
        context.fill(x + 6, y + 1, x + 8, y + 3, blade);
        context.fill(x + 5, y + 2, x + 7, y + 4, blade);
        context.fill(x + 4, y + 3, x + 6, y + 5, blade);
        context.fill(x + 3, y + 4, x + 5, y + 6, blade);
        context.fill(x + 7, y + 2, x + 8, y + 3, shadow);
        context.fill(x + 2, y + 5, x + 5, y + 7, gold);
        context.fill(x + 1, y + 7, x + 3, y + 9, gold);
        context.fill(x, y + 8, x + 2, y + 10, 0xFFAA5500);
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
            context.drawTexture(RenderPipelines.GUI_TEXTURED, EQUIPMENT_TEXTURE,
                    x, y, sprite * 120, 0, size, size, 120, 120, 1440, 120);
            return;
        }
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(size / 16.0F, size / 16.0F);
        context.drawItem(stack, 0, 0);
        context.getMatrices().popMatrix();
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
        if (item.customModelData() > 0) {
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                    List.of((float) item.customModelData()), List.of(), List.of(), List.of()));
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
                    context, x + 2, layout.skillTop + 5, cardWidth - 4, SKILL_CARD_HEIGHT,
                    UiTheme.SURFACE, UiTheme.BORDER);
        }
        int remaining = availableSkillPoints() - totalAssigned();
        int overCapSkill = firstOverCapSkill();
        int assignedLineY = layout.skillTop + layout.skillHeight - (overCapSkill >= 0 ? 26 : 14);
        String remainingPrefix = "Assigned " + totalAssigned() + " skillpoints. Remaining: ";
        String remainingValue = Integer.toString(remaining);
        int remainingWidth = this.textRenderer.getWidth(remainingPrefix + remainingValue);
        int remainingX = MARGIN + (layout.leftWidth - remainingWidth) / 2;
        context.drawTextWithShadow(this.textRenderer,
                remainingPrefix, remainingX, assignedLineY, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer,
                remainingValue, remainingX + this.textRenderer.getWidth(remainingPrefix), assignedLineY,
                remaining < 0 ? NEGATIVE : POSITIVE);
        if (overCapSkill >= 0) {
            SkillPointWarning warning = SkillPointWarning.forBuild(
                    level, assignedSkills, new ArrayList<>(selectedTomes.values())).orElseThrow();
            float scale = Math.min(1.0F, (layout.leftWidth - 12.0F) / textRenderer.getWidth(warning.message()));
            drawScaledCenteredText(context, Text.literal(warning.message()),
                    MARGIN + layout.leftWidth / 2, assignedLineY + 12,
                    warning.tomeSuggestion() ? 0xFFFFAA00 : 0xFFFF5555, scale);
        }
    }

    private void renderSkillDetails(
            DrawContext context,
            Layout layout,
            BuildCalculator.Result stats
    ) {
        int cardWidth = (layout.leftWidth - 8) / 5;
        int top = layout.skillTop + 26;
        for (int i = 0; i < SKILL_NAMES.length; i++) {
            int x = MARGIN + 4 + i * cardWidth;
            int centerX = x + cardWidth / 2;
            drawScaledCenteredText(context,
                    Text.literal(SKILL_NAMES[i]).formatted(SKILL_COLORS[i]),
                    centerX, layout.skillTop + 10, 0xFFFFFFFF, 0.9F);
            drawScaledCenteredText(context,
                    Text.literal("Assign: " + assignedSkills[i]).formatted(Formatting.WHITE),
                    centerX, top + 36, 0xFFFFFFFF, 0.8F);
            drawScaledCenteredText(context,
                    Text.literal("Original: " + skillPointPlan.finalSkills()[i])
                            .formatted(Formatting.WHITE),
                    centerX, top + 48, 0xFFFFFFFF, 0.8F);
            drawScaledCenteredText(context,
                    Text.literal(String.format("%.1f%% %s",
                                    BuildCalculator.skillEffectPercentage(i, stats.skills()[i]), SKILL_EFFECTS[i]))
                            .formatted(SKILL_COLORS[i]),
                    centerX, top + 61, 0xFFFFFFFF, 0.8F);
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
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX, y);
        context.getMatrices().scale(scale, scale);
        context.drawCenteredTextWithShadow(this.textRenderer, text, 0, 0, color);
        context.getMatrices().popMatrix();
    }

    private int firstOverCapSkill() {
        for (int i = 0; i < assignedSkills.length; i++) {
            if (assignedSkills[i] > 100) {
                return i;
            }
        }
        return -1;
    }

    private void renderSummary(DrawContext context, Layout layout, BuildCalculator.Result stats) {
        int x = layout.middleX + 12;
        int viewportTop = MARGIN + 32;
        int viewportBottom = this.height - MARGIN - 28;
        int visibleScroll = Math.round(summaryScroll.update());
        int y = MARGIN + 38 - visibleScroll;
        int valueX = layout.middleX + layout.middleWidth - 12;
        Map<String, Integer> ids = stats.ids();

        context.enableScissor(layout.middleX + 4, viewportTop,
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
            context.disableScissor();
        }

        int contentBottom = y + visibleScroll;
        summaryMaxScroll = Math.max(0, contentBottom - viewportBottom + 4);
        summaryScroll.clamp(0.0F, summaryMaxScroll);
        if (summaryMaxScroll > 0) {
            int trackTop = viewportTop + 2;
            int trackHeight = viewportBottom - viewportTop - 4;
            int thumbHeight = Math.max(18, trackHeight * trackHeight / (trackHeight + summaryMaxScroll));
            int thumbY = trackTop
                    + Math.round((trackHeight - thumbHeight)
                            * summaryScroll.position() / summaryMaxScroll);
            context.fill(layout.middleX + layout.middleWidth - 5, trackTop,
                    layout.middleX + layout.middleWidth - 3, trackTop + trackHeight, UiTheme.BORDER);
            context.fill(layout.middleX + layout.middleWidth - 5, thumbY,
                    layout.middleX + layout.middleWidth - 3, thumbY + thumbHeight, 0xFFAAAAAA);
        }
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
                AbilityTreeClass abilityClass = AbilityTreeClass.fromWeaponSubtype(
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
        context.drawTextWithShadow(this.textRenderer, label, x, y, labelColor);
        context.drawTextWithShadow(this.textRenderer, value, valueX - this.textRenderer.getWidth(value), y, valueColor);
        return y + 14;
    }

    private int summarySubLine(DrawContext context, int x, int valueX, int y, String label, String value) {
        int labelX = Math.max(x, valueX - this.textRenderer.getWidth(label + " " + value));
        context.drawTextWithShadow(this.textRenderer, label, labelX, y, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, value,
                valueX - this.textRenderer.getWidth(value), y, numericColor(value));
        return y + 14;
    }

    private int summaryDivider(DrawContext context, Layout layout, int y) {
        context.fill(layout.middleX + 4, y + 3,
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

    private void renderDamage(DrawContext context, Layout layout, BuildCalculator.Result stats) {
        WynnItem weapon = equipped.get(BuildSlot.WEAPON);
        AbilityTreeClass abilityClass = AbilityTreeClass.fromWeaponSubtype(
                weapon == null ? "" : weapon.subType());
        AbilityTreeDefinition tree = AbilityTreeDatabase.getInstance().get(abilityClass);
        Set<String> selectedAbilities = abilityTreeState.selected(abilityClass);
        int cacheKey = Objects.hash(
                weapon,
                tree,
                selectedAbilities,
                stats.ids(),
                Arrays.hashCode(stats.skills()),
                Arrays.hashCode(stats.weaponDamage()),
                stats.baseAttackTier(),
                stats.effectiveAttackTier());
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
        int viewportTop = MARGIN + 4;
        int viewportBottom = this.height - MARGIN - 28;
        int visibleScroll = Math.round(damageScroll.update());
        int y = viewportTop + 4 - visibleScroll;
        damageCards.clear();
        context.enableScissor(
                layout.rightX + 3, viewportTop,
                layout.rightX + layout.rightWidth - 3, viewportBottom);
        try {
            if (weapon == null) {
                context.drawCenteredTextWithShadow(
                        this.textRenderer, "Select a weapon",
                        layout.rightX + layout.rightWidth / 2, MARGIN + 42, 0xFFAAAAAA);
                damageMaxScroll = 0;
                return;
            }
            double criticalChance = BuildCalculator.skillPercentage(stats.skills()[1]);
            for (DamageCalculator.SpellResult spell : spells) {
                boolean expanded = expandedSpell == spell.index();
                int cardHeight = damageCardHeight(spell, expanded);
                UiTheme.drawRoundedBox(
                        context, cardX, y, cardWidth, cardHeight, UiTheme.SURFACE, UiTheme.BORDER);
                damageCards.add(new DamageCardBounds(
                        spell.index(), cardX, Math.max(viewportTop, y),
                        cardX + cardWidth, Math.min(viewportBottom, y + Math.min(cardHeight, 80))));
                int lineY = y + 9;
                drawSpellTitle(context, spell, cardX + cardWidth / 2, lineY);
                lineY += 16;
                DamageCalculator.PartResult summary = spell.summaryPart();
                if (summary == null) {
                    context.drawCenteredTextWithShadow(
                            this.textRenderer, "Ability Tree required",
                            cardX + cardWidth / 2, lineY, 0xFF777777);
                } else if (spell.index() == 0) {
                    double perAttack = summary.average(criticalChance);
                    drawDamageValue(context, cardX, cardWidth, lineY,
                            "Average DPS: ", perAttack * BuildCalculator.attacksPerSecond(stats.effectiveAttackTier()));
                    lineY += 12;
                    context.drawCenteredTextWithShadow(
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
            context.disableScissor();
        }
        int contentBottom = y + visibleScroll;
        damageMaxScroll = Math.max(0, contentBottom - viewportBottom + 4);
        damageScroll.clamp(0.0F, damageMaxScroll);
        if (damageMaxScroll > 0) {
            int trackHeight = viewportBottom - viewportTop - 4;
            int thumbHeight = Math.max(18, trackHeight * trackHeight / (trackHeight + damageMaxScroll));
            int thumbY = viewportTop + 2
                    + Math.round((trackHeight - thumbHeight)
                            * damageScroll.position() / damageMaxScroll);
            context.fill(layout.rightX + layout.rightWidth - 5, viewportTop + 2,
                    layout.rightX + layout.rightWidth - 3, viewportTop + 2 + trackHeight, UiTheme.BORDER);
            context.fill(layout.rightX + layout.rightWidth - 5, thumbY,
                    layout.rightX + layout.rightWidth - 3, thumbY + thumbHeight, 0xFFAAAAAA);
        }
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
            context.drawCenteredTextWithShadow(textRenderer, name, centerX, y, 0xFFFFFFFF);
            return;
        }
        String cost = " (" + format(spell.cost());
        String close = ")";
        int iconWidth = 7;
        int totalWidth = textRenderer.getWidth(name)
                + textRenderer.getWidth(cost) + iconWidth + textRenderer.getWidth(close);
        int x = centerX - totalWidth / 2;
        context.drawTextWithShadow(textRenderer, name, x, y, 0xFFFFFFFF);
        x += textRenderer.getWidth(name);
        context.drawTextWithShadow(textRenderer, cost, x, y, 0xFF55FFFF);
        x += textRenderer.getWidth(cost) + 1;
        drawManaDrop(context, x, y + 1);
        x += iconWidth - 1;
        context.drawTextWithShadow(textRenderer, close, x, y, 0xFF55FFFF);
    }

    private static void drawManaDrop(DrawContext context, int x, int y) {
        context.fill(x + 2, y, x + 4, y + 1, 0xFFBFFFFF);
        context.fill(x + 1, y + 1, x + 5, y + 3, 0xFF55FFFF);
        context.fill(x, y + 3, x + 6, y + 6, 0xFF20BFFF);
        context.fill(x + 1, y + 6, x + 5, y + 7, 0xFF0088CC);
        context.fill(x + 1, y + 3, x + 2, y + 5, 0xFFFFFFFF);
    }

    private static void drawExpandArrow(
            DrawContext context,
            int centerX,
            int y,
            boolean expanded
    ) {
        int color = 0xFF55FFFF;
        if (expanded) {
            context.fill(centerX - 1, y, centerX + 2, y + 1, color);
            context.fill(centerX - 3, y + 1, centerX + 4, y + 2, color);
            context.fill(centerX - 5, y + 2, centerX + 6, y + 4, color);
        } else {
            context.fill(centerX - 5, y, centerX + 6, y + 2, color);
            context.fill(centerX - 3, y + 2, centerX + 4, y + 3, color);
            context.fill(centerX - 1, y + 3, centerX + 2, y + 5, color);
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
        context.drawCenteredTextWithShadow(
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
        context.drawCenteredTextWithShadow(
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
            context.drawCenteredTextWithShadow(
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
        context.drawCenteredTextWithShadow(
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
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(boostSection + " options will appear here.").formatted(Formatting.DARK_GRAY),
                    MARGIN + layout.leftWidth / 2, layout.boostTop + 63, 0x777777);
        }

    }

    private void renderPicker(DrawContext context) {
        int pickerWidth = Math.min(520, this.width - 60);
        int pickerHeight = Math.min(326, this.height - 50);
        int left = (this.width - pickerWidth) / 2;
        int top = (this.height - pickerHeight) / 2;
        context.fill(0, 0, this.width, this.height, 0x99000000);
        UiTheme.drawPanel(context, left, top, pickerWidth, pickerHeight);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Choose " + pickerSlot.label).styled(style -> style.withColor(UiTheme.accentRgb())),
                left + 14, top + 13, 0xFFFFFF);
    }

    private void renderItemDetails(DrawContext context) {
        BuildSlot detailSlot = slotForItem(detailItem);
        detailMaxScroll = ItemInspectionCard.render(context, textRenderer, detailItem,
                powderCodes.getOrDefault(detailSlot, ""), width, height, tierRgb(detailItem.tier()), detailScroll,
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

    private BuildCalculator.Result calculateStats() {
        return BuildCalculator.calculate(
                level, assignedSkills, equippedItems(), new ArrayList<>(selectedTomes.values()));
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
        autoAllocateSkillPoints();
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

    private static AbilityTreeClass classForName(String name) {
        for (AbilityTreeClass abilityClass : AbilityTreeClass.values()) {
            if (abilityClass.apiName().equalsIgnoreCase(name)
                    || abilityClass.displayName().equalsIgnoreCase(name)) {
                return abilityClass;
            }
        }
        return AbilityTreeClass.ARCHER;
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

    private void autoAllocateSkillPoints() {
        skillPointPlan = BuildCalculator.optimizeSkillPoints(
                level, equippedItems(), new ArrayList<>(selectedTomes.values()));
        System.arraycopy(skillPointPlan.assigned(), 0, assignedSkills, 0, assignedSkills.length);
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
        int leftWidth = Math.max(390, (int) (total * 0.49D));
        int middleWidth = Math.max(210, (int) (total * 0.24D));
        if (leftWidth + middleWidth > total - 220) {
            leftWidth = Math.max(320, total - 430);
            middleWidth = 200;
        }

        int rightWidth = total - leftWidth - middleWidth;
        int middleX = MARGIN + leftWidth + GAP;
        int rightX = middleX + middleWidth + GAP;
        int equipmentTop = MARGIN;
        int equipmentHeight = 280;
        int skillTop = equipmentTop + equipmentHeight + GAP;
        int skillHeight = 136;
        int boostTop = skillTop + skillHeight + GAP;
        int boostHeight = Math.max(58, this.height - MARGIN - boostTop);
        return new Layout(
                leftWidth, middleX, middleWidth, rightX, rightWidth,
                equipmentTop, equipmentHeight, skillTop, skillHeight, boostTop, boostHeight);
    }

    private int pickerVisibleCount() {
        int pickerHeight = Math.min(326, this.height - 50);
        return Math.max(1, Math.min(RESULT_LIMIT, (pickerHeight - 100) / 22));
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
}
