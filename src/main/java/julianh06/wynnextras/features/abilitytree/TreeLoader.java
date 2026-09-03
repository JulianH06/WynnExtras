package julianh06.wynnextras.features.abilitytree;

import com.google.gson.*;
import julianh06.wynnextras.wynncraft.state.StatusEffectState;
import julianh06.wynnextras.utils.ContainerUtils;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.features.loader.SkillPointLoader;
import julianh06.wynnextras.utils.WynncraftAuthManager;
import julianh06.wynnextras.features.profileviewer.data.*;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@WEModule
public class TreeLoader {
    private static final String ABILITY_TREE_TITLE = "\uDAFF\uDFEA\uE000";
    private static final String ABILITY_TREE_ULTIMATE_TITLE = "\uDAFF\uDFEA\uE057";
    private static final String ABILITY_TREE_RESET_TITLE = "\uDAFF\uDFEA\uE001";
    static final int GUI_SETTLE_TICKS = 4;
    static int ticksSinceLastAction = 0;
    static long lastAbilityTreeMenuClick = 0;

    static boolean inCompassMenu = false;
    static boolean inTreeMenu = false;
    static boolean inResetMenu = false;
    static boolean wasStarted = false;
    static boolean treeMenuWasOpened = false;
    static boolean resetMenuWasOpened = false;
    static boolean wasReset = false;
    static HandledScreen<?> screen = null;
    static boolean resetTree = false;
    static List<AbilityMapData.Node> abilitiesToClick2 = null;
    static AbilityTreeData classTree = null;

    static Gson gson = new GsonBuilder()
            .registerTypeAdapter(AbilityMapData.class, new AbilityMapDataDeserializer())
            .registerTypeAdapter(AbilityTreeData.class, new AbilityTreeDataDeserializer())
            .registerTypeAdapter(AbilityMapData.Icon.class, new IconDeserializer())
            .registerTypeAdapter(AbilityMapData.Node.class, new NodeDeserializer())
            .registerTypeAdapter(AbilityTreeData.Icon.class, new IconDeserializer())
            .setPrettyPrinting()
            .create();

    private static Command openTreeScreen = new Command(
            "tree",
            "",
            (ctx) -> {
                WEScreen.open(TreeScreen::new);
                return 1;
            },
            null, null
    );


    static public void resetAll() {
        treeMenuWasOpened = false;
        resetMenuWasOpened = false;
        wasReset = false;
        resetTree = false;
        abilitiesToClick2 = null;
        ticksSinceLastAction = 0;
        lastAbilityTreeMenuClick = 0;
        pendingReset = null;
    }

    private static class PendingClick {
        String abilityName;
        int slot;
        int attempts;
        int sentRevision;
        int revisionRetries;
        int ticksWaiting;
        PendingClick(String abilityName, int slot, int sentRevision) {
            this.abilityName = abilityName;
            this.slot = slot;
            this.attempts = 0;
            this.sentRevision = sentRevision;
            this.revisionRetries = 0;
            this.ticksWaiting = 0;
        }
    }

    public static boolean loadSkillpoints = false;
    public static int[] skillPointSet;

    private static final int ABILITY_CLICK_RETRY_TICKS = 10;
    private static final int MAX_ATTEMPTS_PER_ABILITY = 15;
    private static final int ABILITY_MENU_SETTLE_TICKS = 2;
    private static final int PAGE_SWITCH_RETRY_TICKS = 10;
    private static final int MAX_PAGE_SWITCH_ATTEMPTS = 15;
    private static final int ABILITY_TREE_CONTENT_SLOTS = 54;
    private static PendingClick pendingClick = null;

    private static class PendingResetClick {
        String stage;
        int slot;
        int ticksWaiting;
        int attempts;
        PendingResetClick(String stage, int slot) {
            this.stage = stage;
            this.slot = slot;
            this.ticksWaiting = 0;
            this.attempts = 0;
        }
    }
    private static PendingResetClick pendingReset = null;
    private static final int RESET_CLICK_TIMEOUT = 5;
    private static final int MAX_RESET_ATTEMPTS = 15;

    private static boolean scrolledUp = false;
    private static ItemStack firstNode = null;
    private static long lastResetTryClick = 0;

    public static void init() {
        TreeData.loadAll();

        // Main tick and automation logic (unchanged)
        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            Screen currScreen = client.currentScreen;
            if (currScreen == null) return;

            if (currScreen instanceof HandledScreen) screen = (HandledScreen<?>) currScreen;
            else screen = null;

            String InventoryTitle = currScreen.getTitle().getString();
            boolean oneTrue = inTreeMenu || inResetMenu;

            inCompassMenu = InventoryTitle.equals("\uDAFF\uDFDC\uE003");
            if(inCompassMenu && resetTree) {
                long now = System.currentTimeMillis();
                if (now - lastAbilityTreeMenuClick >= 500
                        && TreeLoader.clickOnNameInInventory("Ability Tree", screen, MinecraftClient.getInstance())) {
                    lastAbilityTreeMenuClick = now;
                }
                return;
            }

            inTreeMenu = InventoryTitle.equals(ABILITY_TREE_TITLE)
                    || InventoryTitle.equals(ABILITY_TREE_ULTIMATE_TITLE);
            inResetMenu = InventoryTitle.equals(ABILITY_TREE_RESET_TITLE);
            if (!inTreeMenu && !inResetMenu && !wasStarted && oneTrue) resetAll();
            if (wasStarted && inTreeMenu) wasStarted = false;
        });

        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
            if (!resetTree) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            ClientPlayerEntity player = client.player;

            ticksSinceLastAction++;
            boolean hasTreeManipulation = StatusEffectState.hasEffect("Tree Manipulation");
            //hasTreeManipulation = false;
            if (ticksSinceLastAction < GUI_SETTLE_TICKS) return;

            if (pendingReset != null && screen != null) {
                pendingReset.ticksWaiting++;

                if (pendingReset.stage.equals("socket")) {
                    if (isSlotNamed(screen, pendingReset.slot, "Ability Shard")) {
                        pendingReset = null;
                        ticksSinceLastAction = 0;
                        return;
                    }

                    if (pendingReset.ticksWaiting >= RESET_CLICK_TIMEOUT) {
                        pendingReset.attempts++;
                        if (pendingReset.attempts > MAX_RESET_ATTEMPTS) {
                            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Couldn't insert an Ability Shard. Make sure you have 3 Ability Shards in your inventory.")));
                            resetAll();
                            return;
                        }
                        clickSlotHelper(pendingReset.slot, screen, client);
                        pendingReset.ticksWaiting = 0;
                    }
                    return;
                }

                if (pendingReset.stage.equals("confirm")) {
                    if (inTreeMenu && !inResetMenu) {
                        pendingReset = null;
                        resetTree = false;
                        return;
                    }

                    if (pendingReset.ticksWaiting >= RESET_CLICK_TIMEOUT) {
                        pendingReset.attempts++;
                        if (pendingReset.attempts > MAX_RESET_ATTEMPTS) {
                            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Reset failed due to lag, please try again.")));
                            resetAll();
                            return;
                        }
                        confirmReset(client, player, screen);
                        pendingReset.ticksWaiting = 0;
                    }
                }

                return;
            }

            if (!treeMenuWasOpened) {
                openTreeMenu(client, player);
                return;
            }

            if (hasTreeManipulation && inTreeMenu && !resetMenuWasOpened) {
                shiftClickSlotHelper(54 + 4, 1, screen);
                lastResetTryClick = System.currentTimeMillis();
                resetMenuWasOpened = true;
                wasReset = true;
                return;
            }

            if (inTreeMenu && !resetMenuWasOpened) {
                openTreeResetMenu(client, player, screen);
                return;
            }

            if (inResetMenu && !wasReset) {
                int emptySocket = findNamedSlot(screen, "Empty Socket");
                if (emptySocket >= 0) {
                    clickSlotHelper(emptySocket, screen, client);
                    pendingReset = new PendingResetClick("socket", emptySocket);
                    return;
                }

                if (countFilledAbilityShardSockets(screen) >= 3) {
                    confirmReset(client, player, screen);
                    pendingReset = new PendingResetClick("confirm", -1);
                    return;
                }
            }

            if (inTreeMenu && wasReset) {
                resetTree = false;
            }

        });

        int[] abilityClickTicks = {0};
        int[] currentPage = {1};
        AtomicInteger failCycles = new AtomicInteger();
        final int MAX_FAIL_CYCLES = 80;


        AtomicBoolean pendingPageSwitch = new AtomicBoolean(false);
        AtomicInteger pageSwitchTicks = new AtomicInteger();
        int[] pendingPageDirection = {0};
        int[] pendingPageTargetPage = {1};
        int[] pendingPageTargetSlot = {-1};
        int[] pageSwitchAttempts = {0};
        String[] pendingPageTargetName = {null};


        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
            if(System.currentTimeMillis() - lastResetTryClick < 1000) return;

            if (abilitiesToClick2 == null || abilitiesToClick2.isEmpty()) {
                abilityClickTicks[0] = 0;
                failCycles.set(0);
                pendingClick = null;
                return;
            }
            if (resetTree) {
                abilityClickTicks[0] = 0;
                currentPage[0] = 1;
                failCycles.set(0);
                pendingClick = null;
                pendingPageSwitch.set(false);
                pageSwitchTicks.set(0);
                pageSwitchAttempts[0] = 0;
                pendingPageTargetName[0] = null;
                pendingPageTargetSlot[0] = -1;
                return;
            }
            if (!inTreeMenu) {
                abilityClickTicks[0] = 0;
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null || client.currentScreen == null) {
                abilityClickTicks[0] = 0;
                return;
            }
            ClientPlayerEntity player = client.player;
            HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;

            abilityClickTicks[0]++;
            if (abilityClickTicks[0] < ABILITY_MENU_SETTLE_TICKS) {
                return;
            }

            if (pendingPageSwitch.get()) {
                pageSwitchTicks.incrementAndGet();

                if (isAbilityShownAtSlot(screen, pendingPageTargetSlot[0], pendingPageTargetName[0])) {
                    currentPage[0] = pendingPageTargetPage[0];
                    pendingPageSwitch.set(false);
                    pageSwitchTicks.set(0);
                    pageSwitchAttempts[0] = 0;
                    pendingPageTargetName[0] = null;
                    pendingPageTargetSlot[0] = -1;
                    failCycles.set(0);
                } else {
                    if (pageSwitchTicks.get() >= PAGE_SWITCH_RETRY_TICKS) {
                        pageSwitchAttempts[0]++;
                        if (pageSwitchAttempts[0] > MAX_PAGE_SWITCH_ATTEMPTS) {
                            failCycles.set(MAX_FAIL_CYCLES);
                            pendingPageSwitch.set(false);
                            return;
                        }
                        String direction = pendingPageDirection[0] > 0 ? "Next Page" : "Previous Page";
                        clickOnAbility(client, player, direction, screen);
                        pageSwitchTicks.set(0);
                    }
                    return;
                }
            }


            if (failCycles.get() >= MAX_FAIL_CYCLES) {
                resetAll();
                if(MinecraftUtils.mc().currentScreen != null) MinecraftUtils.mc().currentScreen.close();
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Something went wrong! Try again")));
                abilitiesToClick2 = null;
                abilityClickTicks[0] = 0;
                failCycles.set(0);
                pendingClick = null;
                return;
            }

            if (pendingClick != null) {
                pendingClick.ticksWaiting++;

                if (isAbilityClickConfirmed(pendingClick, screen)) {
                    abilitiesToClick2.removeFirst();
                    failCycles.set(0);
                    pendingClick = null;
                } else {
                    int revision = screen.getScreenHandler().getRevision();
                    if (pendingClick.revisionRetries < 3 && revision != pendingClick.sentRevision) {
                        pendingClick.attempts++;
                        pendingClick.revisionRetries++;
                        if (clickOnAbility(client, player, pendingClick.abilityName, screen) >= 0) {
                            pendingClick.sentRevision = revision;
                            pendingClick.ticksWaiting = 0;
                        }
                        return;
                    }
                    if (pendingClick.ticksWaiting >= ABILITY_CLICK_RETRY_TICKS) {
                        pendingClick.attempts++;
                        if (pendingClick.attempts > MAX_ATTEMPTS_PER_ABILITY) {
                            failCycles.set(MAX_FAIL_CYCLES);
                            return;
                        }
                        if (clickOnAbility(client, player, pendingClick.abilityName, screen) >= 0) {
                            pendingClick.sentRevision = screen.getScreenHandler().getRevision();
                            pendingClick.ticksWaiting = 0;
                        }
                    }
                    return;
                }
            }
            if(abilitiesToClick2 == null) return;
            if(abilitiesToClick2.isEmpty()) {
                resetAll();
                if(MinecraftUtils.mc().currentScreen != null) MinecraftUtils.mc().currentScreen.close();
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Finished loading the ability tree." + (loadSkillpoints ? " Continuing with skill points. " : ""))));
                if(skillPointSet != null) {
                    int[] points = skillPointSet;
                    skillPointSet = null;
                    SkillPointLoader.getInstance().load(
                            points[0], points[1], points[2], points[3], points[4]);
                }
                return;
            }

            AbilityMapData.Node abilityNode = abilitiesToClick2.getFirst();
            AbilityTreeData.Ability abilityFromNode = getAbilityFromNode(abilityNode, classTree);

            if (abilityFromNode == null) {
                return;
            }

            String abilityName = extractAbilityNameFromHtml(abilityFromNode.name);
            if (abilityName == null) {
                return;
            }
            int pageOffset = abilityNode.meta.page - currentPage[0];
            if (pageOffset != 0 && !pendingPageSwitch.get()) {
                String direction = pageOffset > 0 ? "Next Page" : "Previous Page";
                if (clickOnNameInInventory(direction, screen, client)) {
                    pendingPageDirection[0] = pageOffset > 0 ? 1 : -1;
                    pendingPageTargetPage[0] = currentPage[0] + pendingPageDirection[0];
                    pendingPageTargetSlot[0] = abilityFromNode.slot;
                    pendingPageTargetName[0] = abilityName;
                    pageSwitchAttempts[0] = 0;
                    pendingPageSwitch.set(true);
                    pageSwitchTicks.set(0);
                }
                return;
            }

            if (hasUnlockPrefix(abilityName, screen)) {
                int clickedSlot = clickOnAbility(client, player, abilityName, screen);
                if (clickedSlot >= 0) {
                    pendingClick = new PendingClick(
                            abilityName,
                            clickedSlot,
                            screen.getScreenHandler().getRevision());
                }
                return;
            } else {
                int replacementIndex = findVisibleQueueCandidate(screen, currentPage[0]);
                if (replacementIndex > 0) {
                    AbilityMapData.Node replacement = abilitiesToClick2.remove(replacementIndex);
                    abilitiesToClick2.addFirst(replacement);
                    return;
                }

                failCycles.incrementAndGet();
            }
        });

    }

    public static String extractAbilityNameFromHtml(String html) {
        if (html == null) return null;
        // 1) Remove HTML tags and replace them with spaces so words do not merge.
        String plain = html.replaceAll("<[^>]+>", " ");
        // 2) Remove Minecraft color and formatting codes (§x).
        plain = plain.replaceAll("§.", "");
        // 3) Unescape common entities.
        plain = plain.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&#39;", "'")
                .replace("&quot;", "\"");
        // 4) Normalize typographic apostrophes to ASCII apostrophes.
        plain = plain.replace('\u2019', '\'').replace('\u2018', '\'');
        // 5) Remove control characters, collapse whitespace, and trim.
        plain = plain.replaceAll("[\\p{C}]+", " ").replaceAll("\\s+", " ").trim();
        // 6) Remove surrounding punctuation if it remains around the name.
        plain = plain.replaceAll("^[\\p{Punct}\\s]+", "").replaceAll("[\\p{Punct}\\s]+$", "");
        return plain.isEmpty() ? null : plain;
    }


    public static AbilityMapData.Node getNodeFromAbility(AbilityTreeData.Ability ability, AbilityMapData treeData) {
        int page = ability.page;

        for(AbilityMapData.Node node : treeData.pages.get(page)) {
            if(ability.coordinates.x != node.coordinates.x) {
                continue;
            }
            if(ability.coordinates.y != node.coordinates.y % 6) {
                continue;
            }
            return node;
        }

        return null;
    }


    public static AbilityTreeData.Ability getAbilityFromNode(AbilityMapData.Node node, AbilityTreeData treeData) {
        if (treeData == null) return null;
        if (node == null || node.meta == null || node.meta.id == null || node.meta.id.isEmpty()) return null;

        for (Map<String, AbilityTreeData.Ability> page : treeData.pages.values()) {
            if (page == null) continue;
            AbilityTreeData.Ability ability = page.get(node.meta.id);
            if (ability != null) return ability;
        }
        return null;
    }

    public static List<AbilityMapData.Node> convertNodeMapToList(AbilityMapData treeMap) {
        List<AbilityMapData.Node> result = new ArrayList<>();

        if(treeMap == null) return result;
        if(treeMap.pages == null) return result;

        for(List<AbilityMapData.Node> page : treeMap.pages.values()) {
            result.addAll(page);
        }

        return result;
    }

    public static List<AbilityTreeData.Ability> convertNodeTreeToList(AbilityTreeData treeData) {
        List<AbilityTreeData.Ability> result = new ArrayList<>();

        if(treeData == null) return result;
        if(treeData.pages == null) return result;

        for(Map<String, AbilityTreeData.Ability> page : treeData.pages.values()) {
            result.addAll(page.values());
        }

        return result;
    }

    private static String normalizeArchetypeKey(String display) {
        if (display == null) return null;
        return (display + " Archetype").toLowerCase();
    }

    private static Optional<String> extractArchetypeInfo(List<String> description) {
        if (description == null) return Optional.empty();
        Pattern archetypeLine = Pattern.compile("(.+?)\\s+Archetype", Pattern.CASE_INSENSITIVE);
        for (String line : description) {
            if (line == null) continue;
            String plain = line.replaceAll("<[^>]+>", "").replaceAll("§.", "").trim();
            Matcher m = archetypeLine.matcher(plain);
            if (m.find()) {
                return Optional.of(m.group(1).trim()); // e.g. "Paladin" (without the word "Archetype")
            }
        }
        return Optional.empty();
    }

    public static Optional<Integer> extractCountFromComponentsString(String componentsToString) {
        if (componentsToString == null) return Optional.empty();
        String plain = componentsToString.replaceAll("§.", ""); // remove all color and formatting codes
        Pattern p = Pattern.compile("\\b(\\d+)\\/(\\d+)\\b");
        Matcher m = p.matcher(plain);
        if (m.find()) {
            int max = Integer.parseInt(m.group(2));
            return Optional.of(max);
        }
        return Optional.empty();
    }

    public static Map<String, Integer> getArchetypeCounts(Map<String, AbilityTreeData.Archetype> archetypes) {
        Map<String, Integer> result = new HashMap<>();
        if (archetypes == null) return result;
        for (Map.Entry<String, AbilityTreeData.Archetype> e : archetypes.entrySet()) {
            String internalKey = e.getKey(); // z.B. your internal id like "monk" or similar
            AbilityTreeData.Archetype at = e.getValue();
            if (at == null) continue;

            ItemStack archetypeItem;
            try {
                archetypeItem = MinecraftUtils.inventory().getStack(at.slot);
            } catch (Exception ex) {
                continue;
            }
            if (archetypeItem == null) continue;

            String displayName = null;
            try {
                Object cn = archetypeItem.getCustomName();
                if (cn != null) displayName = String.valueOf(cn).trim(); //TODO: maybe remove the trim
            } catch (Exception ignored) {}

            String lore = null;
            try {
                if (archetypeItem.getComponents() != null) lore = archetypeItem.getComponents().toString();
            } catch (Exception ignored) {}

            Optional<Integer> res = lore == null ? Optional.empty() : extractCountFromComponentsString(lore);
            if (res.isEmpty()) continue;

            int count = res.get();

            // mapKey: if the displayed name exists, prefer that, else fallback to internal archetype key
            String mapKey;
            if (displayName != null && !"null".equalsIgnoreCase(displayName) && !displayName.isEmpty()) {
                mapKey = normalizeArchetypeKey(displayName);
            } else {
                // fallback: use the provided map key (internal) but normalize to the same format
                // If your archetypes keys are already like "Paladin Archetype", adapt accordingly.
                mapKey = (internalKey == null ? "" : internalKey.toLowerCase());
            }

            if (!mapKey.isEmpty()) result.put(mapKey, count);
        }
        return result;
    }

    public static List<AbilityTreeData.Ability> calculateNodeOrder(
            Map<String, AbilityTreeData.Archetype> archetypes,
            List<AbilityMapData.Node> nodez,
            List<String> unlockedNodes,
            AbilityTreeData treeData) {

        List<AbilityTreeData.Ability> result = new ArrayList<>();
        if (nodez == null || nodez.isEmpty()) return result;

        List<AbilityTreeData.Ability> nodes = new ArrayList<>();
        for(AbilityMapData.Node node : nodez) {
            nodes.add(getAbilityFromNode(node, treeData));
        }

        // Map: lowercased name -> Ability
        Map<String, AbilityTreeData.Ability> byName = new HashMap<>();
        for (AbilityTreeData.Ability a : nodes) {
            if (a == null || a.name == null) continue;
            byName.put(a.name.toLowerCase(), a);
        }

        // Normalisiere initial unlocked (lowercase)
        Set<String> unlocked = new HashSet<>();
        if (unlockedNodes != null) {
            for (String s : unlockedNodes) if (s != null) unlocked.add(s.toLowerCase());
            // keep unlockedNodes normalized as well
            unlockedNodes.clear();
            unlockedNodes.addAll(unlocked);
        }

        // State for DFS / topological sort
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        // Archetype counts (unlocks nodes when enough archetype points are available)
        Map<String, Integer> archetypeCounts = getArchetypeCounts(archetypes);
        // Ensure archetypeCounts keys are normalized (already done in getArchetypeCounts)

        Deque<AbilityTreeData.Ability> stack = new ArrayDeque<>();

        class Resolver {
            // returns true if node is resolved/unlocked (either already unlocked or added to stack)
            boolean resolve(String name) {
                if (name == null || name.isEmpty()) return true;
                String key = name.toLowerCase();
                if (unlocked.contains(key)) return true;
                if (visited.contains(key)) return true;
                if (visiting.contains(key)) {
                    throw new IllegalStateException("Cycle detected in requirements at: " + key);
                }

                AbilityTreeData.Ability node = byName.get(key);
                if (node == null) {
                    // Unknown requirement: treat as already unlocked to allow progress
                    unlocked.add(key);
                    if (unlockedNodes != null && !unlockedNodes.contains(key)) unlockedNodes.add(key);
                    return true;
                }

                // Archetype requirement check
                AbilityTreeData.ArchetypeRequirement arReq = null;
                if (node.requirements != null) arReq = node.requirements.ARCHETYPE;
                if (arReq != null) {
                    String arcName = arReq.name == null ? null : arReq.name.trim();
                    String arcKey = arcName == null ? null : normalizeArchetypeKey(arcName);
                    int need = arReq.amount;
                    int have = arcKey == null ? 0 : archetypeCounts.getOrDefault(arcKey, 0);
                    if (have < need) {
                        return false;
                    }
                }

                // Optional: check ABILITY_POINTS requirement here if needed
                // if (node.requirements != null && node.requirements.ABILITY_POINTS != null) { ... }

                visiting.add(key);

                // NODE requirement is a single node name
                if (node.requirements != null && node.requirements.NODE != null) {
                    String req = node.requirements.NODE.trim();
                    if (!req.isEmpty()) {
                        boolean ok = resolve(req);
                        if (!ok) {
                            visiting.remove(key);
                            return false;
                        }
                    }
                }

                visiting.remove(key);
                visited.add(key);

                if (!unlocked.contains(key)) {
                    stack.push(node);
                    unlocked.add(key);

                    // Extract the archetype and increment its counter when the ability exposes one.
                    try {
                        Optional<String> optDisplay = extractArchetypeInfo(node.description);
                        if (optDisplay.isPresent()) {
                            String display = optDisplay.get();
                            String internalArchetypeName = getInternalName(display, archetypes);
                            String mapKey = normalizeArchetypeKey(internalArchetypeName);
                            archetypeCounts.put(mapKey, archetypeCounts.getOrDefault(mapKey, 0) + 1);
                        }
                    } catch (Exception ignored) {
                    }

                    if (unlockedNodes != null) {
                        if (!unlockedNodes.contains(key)) unlockedNodes.add(key);
                    }
                }
                return true;
            }
        }

        Resolver resolver = new Resolver();

        // Resolve all nodes; nodes blocked by archetype requirements are retried later.
        for (AbilityTreeData.Ability a : nodes) {
            if (a == null || a.name == null) continue;
            String key = a.name.toLowerCase();
            if (unlocked.contains(key) || visited.contains(key)) continue;
            resolver.resolve(key);
        }

        // Retry when archetype dependencies are fulfilled later.
        boolean progress;
        do {
            progress = false;
            for (AbilityTreeData.Ability a : nodes) {
                if (a == null || a.name == null) continue;
                String key = a.name.toLowerCase();
                if (visited.contains(key)) continue;
                boolean resolved = resolver.resolve(key);
                if (resolved) progress = true;
            }
        } while (progress);

        // Convert the stack to the correct order (first resolved -> first in list)
        while (!stack.isEmpty()) result.add(stack.removeLast());

        return result;
    }

    public static String getInternalName(String displayName, Map<String, AbilityTreeData.Archetype> archetypes) {
        if (displayName == null || archetypes == null) return null;
        String target = normalizeDisplay(displayName);

        // 1) exact match against cleaned archetype.name field
        for (Map.Entry<String, AbilityTreeData.Archetype> e : archetypes.entrySet()) {
            AbilityTreeData.Archetype at = e.getValue();
            if (at == null) continue;
            String raw = at.name;
            String cand = normalizeDisplay(raw);
            if (cand.equals(target)) return e.getKey();
        }

        // 2) contains / word-match (handles cases where target is a substring)
        for (Map.Entry<String, AbilityTreeData.Archetype> e : archetypes.entrySet()) {
            AbilityTreeData.Archetype at = e.getValue();
            if (at == null) continue;
            String cand = normalizeDisplay(at.name);
            if (!cand.isEmpty() && (cand.contains(target) || target.contains(cand))) return e.getKey();
        }
        return null;
    }

    // Helper: removes HTML/color codes, normalizes apostrophes and hyphens, and collapses whitespace.
    private static String normalizeDisplay(String s) {
        if (s == null) return "";
        // remove tags -> replace with space so words don't merge
        String plain = s.replaceAll("<[^>]+>", " ");
        // remove minecraft color codes like §a
        plain = plain.replaceAll("§.", "");
        // unescape common entities
        plain = plain.replace("&nbsp;", " ").replace("&amp;", "&").replace("&#39;", "'").replace("&quot;", "\"");
        // normalize typographic apostrophes to ASCII
        plain = plain.replace('\u2019', '\'').replace('\u2018', '\'');
        // remove control chars, collapse spaces, trim and lowercase
        plain = plain.replaceAll("[\\p{C}]+", " ").replaceAll("\\s+", " ").trim().toLowerCase();
        return plain;
    }



    public static void savePlayerAbilityTree(String playerName, String characterUUID, String className, SkillPoints skillPoints, AbilityMapData classMap, AbilityTreeData classTree, AbilityMapData playerTree) {
        try {
            if (characterUUID == null) {
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Couldnt save tree: characterUUID == null")));
                return;
            }
            String abilityApiUrl = "https://api.wynncraft.com/v3/player/" + playerName + "/characters/" + characterUUID + "/abilities";
            String abilityResponse = makeHttpRequest(abilityApiUrl);
            if (abilityResponse == null) {
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Failed to fetch ability tree data: abilityResponse == null")));
                return;
            }



            Path treesDir = FabricLoader.getInstance().getConfigDir().resolve("wynnextras/trees");
            Files.createDirectories(treesDir);

            String baseName = playerName + "_" + characterUUID;
            String fileName = baseName + ".json";
            Path filePath = treesDir.resolve(fileName);

            int counter = 1;
            while (Files.exists(filePath)) {
                fileName = baseName + " (" + counter + ").json";
                filePath = treesDir.resolve(fileName);
                counter++;
            }

            JsonObject out = new JsonObject();
            out.addProperty("name", fileName.replace(".json", ""));
            out.addProperty("visibleName", "");
            out.addProperty("strength", skillPoints.getStrength());
            out.addProperty("dexterity", skillPoints.getDexterity());
            out.addProperty("intelligence", skillPoints.getIntelligence());
            out.addProperty("defence", Math.max(skillPoints.getDefence(), skillPoints.getDefense()));
            out.addProperty("agility", skillPoints.getAgility());
            String formatted = className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase();
            out.addProperty("className", formatted);
            // Mark all nodes in the player's personal map as unlocked (they're only present if unlocked)
            if (playerTree != null && playerTree.pages != null) {
                for (List<AbilityMapData.Node> pageNodes : playerTree.pages.values()) {
                    if (pageNodes == null) continue;
                    for (AbilityMapData.Node n : pageNodes) {
                        if (n != null && "ability".equalsIgnoreCase(n.type)) {
                            n.unlocked = true;
                        }
                    }
                }
            }

            out.add("playerMap", gson.toJsonTree(playerTree));
            out.add("playerTree", gson.toJsonTree(classTree));


            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                String prettyJson = gson.toJson(out);
                writer.write(prettyJson);
                writer.flush();
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("The Ability tree was saved successfully. Use /Wynnextras tree (or /we tree) to view or load it.")));
                TreeData.loadAll();
                return;
            } catch (IOException e) {
                WynnExtras.LOGGER.error("[WynnExtras] Couldn't write ability tree file:");
                e.printStackTrace();
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Failed to save ability tree file")));
            }
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error fetching ability tree:");
            e.printStackTrace();
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Error fetching ability tree")));
        }
    }

    public static void deletePlayerAbilityTree(String fileName) {
        try {
            Path treesDir = FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("wynnextras/trees");
            Files.createDirectories(treesDir);

            Path filePath = treesDir.resolve(fileName);

            if (Files.deleteIfExists(filePath)) {
                MinecraftUtils.sendMessageToClient(
                        WynnExtras.addWynnExtrasPrefix(Text.of("The Ability tree was deleted successfully."))
                );
                TreeData.loadAll(); // reload the list
            } else {
                MinecraftUtils.sendMessageToClient(
                        WynnExtras.addWynnExtrasPrefix(Text.of("Ability tree file not found: " + fileName))
                );
            }
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't delete ability tree file:");
            e.printStackTrace();
            MinecraftUtils.sendMessageToClient(
                    WynnExtras.addWynnExtrasPrefix(Text.of("Failed to delete ability tree file"))
            );
        }
    }


    private static String makeHttpRequest(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "WynnExtras-Mod/1.0");
            String authHeader = WynncraftAuthManager.getAuthorizationHeaderValue();
            if (authHeader != null) connection.setRequestProperty("Authorization", authHeader);
            int responseCode = connection.getResponseCode();
            WynncraftAuthManager.handleWynncraftUnauthorized(responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else if (responseCode == 403) {
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("HTTP Request failed: 403")));
                return null;
            } else if (responseCode == 401) {
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("HTTP Request failed: 401")));
                return null;
            } else {
                WynnExtras.LOGGER.error("[WynnExtras] HTTP Error: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Network error:");
            e.printStackTrace();
            return null;
        }
    }


    static public void openTreeMenu(MinecraftClient client, PlayerEntity player) {
        int currentSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(7);
        client.options.sneakKey.setPressed(true);
        client.interactionManager.interactItem(player, Hand.MAIN_HAND);
        client.options.sneakKey.setPressed(false);
        player.getInventory().setSelectedSlot(currentSlot);
        treeMenuWasOpened = true;
    }

    static public void openTreeResetMenu(MinecraftClient client, PlayerEntity player, HandledScreen<?> screen) {
        if (!inTreeMenu) return;
        resetMenuWasOpened = clickOnNameInInventory("Reset", screen, client);
    }

    static public void confirmReset(MinecraftClient client, PlayerEntity player, HandledScreen<?> screen) {
        if (!inResetMenu) return;
        wasReset = clickOnNameInInventory("Confirm", screen, client);
    }

    static public int clickOnAbility(MinecraftClient client, PlayerEntity player, String nameToClick, HandledScreen<?> screen) {
        if (!inTreeMenu) return -1;
        // "Next Page" / "Previous Page" are menu buttons, not abilities — use the fuzzy matcher
        if (nameToClick.equals("Next Page") || nameToClick.equals("Previous Page")) {
            return clickOnNameInInventory(nameToClick, screen, client) ? 0 : -1;
        }
        // For ability clicks, use exact-name match to avoid hitting tiered names like
        // "Haste II" when the target is "Haste".
        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            if (!slot.hasStack()) continue;
            if (isUnlockableAbilityName(slot.getStack().getName().getString(), nameToClick)) {
                clickSlotHelper(i, screen, client);
                return i;
            }
        }
        return -1;
    }

    static public boolean hasUnlockPrefix(String ability, HandledScreen<?> screen) {
        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            if (!slot.hasStack()) continue;
            if (isUnlockableAbilityName(slot.getStack().getName().getString(), ability)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAbilityShownAtSlot(HandledScreen<?> screen, int slot, String ability) {
        if (ability == null || slot < 0 || slot >= screen.getScreenHandler().slots.size()) return false;
        ItemStack stack = screen.getScreenHandler().slots.get(slot).getStack();
        if (stack.isEmpty()) return false;
        String displayedName = stripFormatting(stack.getName().getString());
        return displayedName.equals(ability) || isUnlockableAbilityName(displayedName, ability);
    }

    private static boolean isAbilityClickConfirmed(PendingClick click, HandledScreen<?> screen) {
        if (click.slot < 0 || click.slot >= screen.getScreenHandler().slots.size()) return false;
        ItemStack stack = screen.getScreenHandler().slots.get(click.slot).getStack();
        return !stack.isEmpty() && !isUnlockableAbilityName(stack.getName().getString(), click.abilityName);
    }

    private static int findVisibleQueueCandidate(HandledScreen<?> screen, int currentPage) {
        for (int i = 1; i < abilitiesToClick2.size(); i++) {
            AbilityMapData.Node node = abilitiesToClick2.get(i);
            if (node == null || node.meta == null || node.meta.page != currentPage) continue;
            AbilityTreeData.Ability ability = getAbilityFromNode(node, classTree);
            if (ability == null) continue;
            String name = extractAbilityNameFromHtml(ability.name);
            if (name != null && hasUnlockPrefix(name, screen)) return i;
        }
        return -1;
    }

    private static String stripFormatting(String value) {
        if (value == null) return "";
        return value.replaceAll("§#[0-9a-fA-F]{8}", "").replaceAll("§.", "").trim();
    }

    private static boolean isUnlockableAbilityName(String displayedName, String ability) {
        if (displayedName == null || ability == null) return false;
        String stripped = stripFormatting(displayedName);
        String target = "Unlock " + ability;
        return stripped.equals(target) || stripped.equals(target + " ability");
    }

    private static int findNamedSlot(HandledScreen<?> screen, String name) {
        int limit = Math.min(ABILITY_TREE_CONTENT_SLOTS, screen.getScreenHandler().slots.size());
        for (int i = 0; i < limit; i++) {
            if (isSlotNamed(screen, i, name)) return i;
        }
        return -1;
    }

    private static boolean isSlotNamed(HandledScreen<?> screen, int slot, String name) {
        if (screen == null || slot < 0 || slot >= screen.getScreenHandler().slots.size()) return false;
        ItemStack stack = screen.getScreenHandler().getSlot(slot).getStack();
        return !stack.isEmpty() && stripFormatting(stack.getName().getString()).contains(name);
    }

    private static int countFilledAbilityShardSockets(HandledScreen<?> screen) {
        int filled = 0;
        for (int slot : new int[]{11, 15, 40}) {
            if (isSlotNamed(screen, slot, "Ability Shard")) filled++;
        }
        return filled;
    }

    static public boolean clickOnNameInInventory(String nameToClick, HandledScreen<?> screen, MinecraftClient client) {
        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            if (!slot.hasStack() || slot.getStack().getCustomName() == null) continue;
            String name = slot.getStack().getCustomName().getString();

            if (name.contains(nameToClick)) {
                clickSlotHelper(i, screen, client);
                return true;
            }
        }
        return false;
    }

    static public void clickSlotHelper(int slotid, HandledScreen<?> screen, MinecraftClient client) {
        ContainerUtils.clickOnSlot(
                slotid,
                screen.getScreenHandler().syncId,
                screen.getScreenHandler().getRevision(),
                0,
                screen.getScreenHandler().getStacks());
        ticksSinceLastAction = 0;
    }

    private static void shiftClickSlotHelper(int slotId, int mouseButton, HandledScreen<?> screen) {
        ContainerUtils.shiftClickOnSlot(
                slotId,
                screen.getScreenHandler().syncId,
                screen.getScreenHandler().getRevision(),
                mouseButton,
                screen.getScreenHandler().getStacks());
        ticksSinceLastAction = 0;
    }
}
