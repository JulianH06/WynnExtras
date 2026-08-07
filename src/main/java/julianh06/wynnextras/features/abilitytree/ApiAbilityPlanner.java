package julianh06.wynnextras.features.abilitytree;

import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.profileviewer.data.AbilityMapData;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeCache;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeData;
import net.minecraft.text.Text;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plans a 100% valid click order for an ability build using live WAPI data.
 *
 * Input:
 *   - Saved TreeData (to know which abilities the build wants).
 *   - Live AbilityTreeData + AbilityMapData from AbilityTreeCache (class tree & map).
 *
 * Output:
 *   - List<AbilityMapData.Node> in the exact order they should be clicked.
 */
public final class ApiAbilityPlanner {

    private ApiAbilityPlanner() {}

    /**
     * Entry point used from TreeScreen.LoadButton:
     *  - Extract desired ability IDs from the saved playerMap.
     *  - Use live class tree + map from AbilityTreeCache.
     *  - Pre-plan click order.
     */
    public static List<AbilityMapData.Node> planFromSavedTree(TreeData savedTree) {
        if (savedTree == null || savedTree.className == null) {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    Text.of("Cannot plan ability tree: saved tree has no class.")
            ));
            return Collections.emptyList();
        }

        String classKey = savedTree.className.toLowerCase(Locale.ROOT);

        AbilityTreeData classTree = AbilityTreeCache.getClassTree(classKey);
        AbilityMapData classMap = AbilityTreeCache.getClassMap(classKey);
        if (classTree == null || classMap == null) {
            // Trigger async load and ask user to retry once done.
            AbilityTreeCache.loadClassTree(classKey);
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    Text.of("Loading ability data for " + savedTree.className +
                            " from the Wynn API, please try again in a moment.")
            ));
            return Collections.emptyList();
        }

        // 1) What this build actually wants: all unlocked abilities from the saved playerMap.
        Set<String> desiredIds = extractDesiredAbilityIds(savedTree);

        if (desiredIds.isEmpty()) {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    Text.of("Saved tree has no unlocked abilities to load.")
            ));
            return Collections.emptyList();
        }

        // 2) Compute full, valid click order using live tree + map.
        try {
            List<String> abilityIdOrder = planAbilityOrder(classTree, classMap, desiredIds);
            return mapIdsToNodes(abilityIdOrder, classMap);
        } catch (PlanningException ex) {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    Text.of("Cannot load ability tree: " + ex.getMessage())
            ));
            return Collections.emptyList();
        }
    }

    // -------------------------------------------------------------
    //  Extract desired ability IDs from saved playerMap
    // -------------------------------------------------------------

    private static Set<String> extractDesiredAbilityIds(TreeData savedTree) {
        Set<String> ids = new HashSet<>();

        // playerMap is the correct source: only nodes with unlocked=true were
        // actually selected by the player when this build was saved.
        if (savedTree.playerMap != null && savedTree.playerMap.pages != null) {
            for (List<AbilityMapData.Node> pageNodes : savedTree.playerMap.pages.values()) {
                if (pageNodes == null) continue;
                for (AbilityMapData.Node n : pageNodes) {
                    if (n == null) continue;
                    if (!"ability".equalsIgnoreCase(n.type)) continue;
                    if (!n.unlocked) continue;
                    if (n.meta == null || n.meta.id == null) continue;
                    ids.add(n.meta.id);
                }
            }
        }

        return ids;
    }



    // -------------------------------------------------------------
    //  Core planner: from AbilityTreeData + AbilityMapData + target IDs
    // -------------------------------------------------------------

    private static List<String> planAbilityOrder(AbilityTreeData tree,
                                                 AbilityMapData map,
                                                 Set<String> targetIds) throws PlanningException {

        if (tree == null || tree.pages == null || tree.pages.isEmpty()) {
            throw new PlanningException("class ability tree is missing or empty.");
        }
        if (map == null || map.pages == null || map.pages.isEmpty()) {
            throw new PlanningException("class ability map is missing or empty.");
        }

        // Index abilities by ID (keys of tree.pages maps are ability IDs per WAPI docs).
        Map<String, AbilityTreeData.Ability> abilitiesById = buildAbilityIndex(tree);

        // Index map nodes by ability ID for later conversion to Node list.
        Map<String, AbilityMapData.Node> nodesById = buildNodeIndex(map);

        // Root node: no NODE requirement, top-left by (page, y, x).
        String rootId = findRootAbilityId(tree, abilitiesById);
        if (rootId == null) {
            throw new PlanningException("could not find a root ability (no NODE requirement).");
        }

        // Expand desired set with all required previous nodes.
        Set<String> desired = expandNodeRequirements(abilitiesById, targetIds);

        // Sanity: every desired id must exist in the class tree.
        for (String id : desired) {
            if (!abilitiesById.containsKey(id)) {
                throw new PlanningException("ability '" + id + "' no longer exists in class tree.");
            }
        }

        // Build adjacency graph from links.
        Map<String, List<String>> adjacency = buildAdjacency(abilitiesById);

        // Reachability from root.
        Set<String> reachable = bfsReachable(rootId, adjacency);
        for (String id : desired) {
            if (!reachable.contains(id)) {
                throw new PlanningException("ability '" + id + "' is no longer reachable from the root.");
            }
        }


        // Archetype mapping: which archetype each ability *gives* a point in (if any).
        Map<String, String> abilityIdToArchKey = buildAbilityArchetypeMap(tree);

        // Simulate unlocks with full constraints.
        return simulateUnlockOrder(abilitiesById, desired, abilityIdToArchKey);
    }

    private static Map<String, AbilityTreeData.Ability> buildAbilityIndex(AbilityTreeData tree) {
        Map<String, AbilityTreeData.Ability> result = new HashMap<>();
        for (Map.Entry<Integer, Map<String, AbilityTreeData.Ability>> pageEntry : tree.pages.entrySet()) {
            Map<String, AbilityTreeData.Ability> page = pageEntry.getValue();
            if (page == null) continue;
            result.putAll(page);
        }
        return result;
    }

    private static Map<String, AbilityMapData.Node> buildNodeIndex(AbilityMapData map) {
        Map<String, AbilityMapData.Node> result = new HashMap<>();
        for (List<AbilityMapData.Node> pageNodes : map.pages.values()) {
            if (pageNodes == null) continue;
            for (AbilityMapData.Node n : pageNodes) {
                if (n == null) continue;
                if (!"ability".equalsIgnoreCase(n.type)) continue;
                if (n.meta == null || n.meta.id == null) continue;
                result.put(n.meta.id, n);
            }
        }
        return result;
    }

    private static String findRootAbilityId(AbilityTreeData tree,
                                            Map<String, AbilityTreeData.Ability> abilitiesById) {
        String bestId = null;
        AbilityTreeData.Ability best = null;

        for (Map.Entry<Integer, Map<String, AbilityTreeData.Ability>> pageEntry : tree.pages.entrySet()) {
            int page = pageEntry.getKey();
            for (Map.Entry<String, AbilityTreeData.Ability> e : pageEntry.getValue().entrySet()) {
                String id = e.getKey();
                AbilityTreeData.Ability a = e.getValue();
                if (a == null) continue;
                if (a.requirements != null && a.requirements.NODE != null && !a.requirements.NODE.isEmpty()) {
                    continue; // has a prerequisite
                }

                if (best == null ||
                        page < best.page ||
                        (page == best.page && (
                                a.coordinates.y < best.coordinates.y ||
                                        (a.coordinates.y == best.coordinates.y && a.coordinates.x < best.coordinates.x)
                        ))) {
                    bestId = id;
                    best = a;
                }
            }
        }
        return bestId;
    }

    private static Set<String> expandNodeRequirements(Map<String, AbilityTreeData.Ability> abilitiesById,
                                                      Set<String> targetIds) {
        Set<String> desired = new HashSet<>(targetIds);
        boolean changed;
        do {
            changed = false;
            for (String id : new HashSet<>(desired)) {
                AbilityTreeData.Ability a = abilitiesById.get(id);
                if (a == null || a.requirements == null || a.requirements.NODE == null) continue;
                String prev = a.requirements.NODE;
                if (prev != null && !prev.isEmpty() && !desired.contains(prev)) {
                    desired.add(prev);
                    changed = true;
                }
            }
        } while (changed);
        return desired;
    }

    private static Map<String, List<String>> buildAdjacency(Map<String, AbilityTreeData.Ability> abilitiesById) {
        Map<String, List<String>> adj = new HashMap<>();
        for (Map.Entry<String, AbilityTreeData.Ability> e : abilitiesById.entrySet()) {
            String id = e.getKey();
            AbilityTreeData.Ability a = e.getValue();
            if (a == null || a.links == null) continue;
            adj.computeIfAbsent(id, k -> new ArrayList<>());
            for (String other : a.links) {
                if (other == null) continue;
                adj.get(id).add(other);
                adj.computeIfAbsent(other, k -> new ArrayList<>());
                if (!adj.get(other).contains(id)) {
                    adj.get(other).add(id);
                }
            }
        }
        return adj;
    }

    private static Set<String> bfsReachable(String rootId, Map<String, List<String>> adj) {
        Set<String> seen = new HashSet<>();
        Deque<String> dq = new ArrayDeque<>();
        dq.add(rootId);
        seen.add(rootId);

        while (!dq.isEmpty()) {
            String cur = dq.removeFirst();
            List<String> neigh = adj.get(cur);
            if (neigh == null) continue;
            for (String n : neigh) {
                if (!seen.add(n)) continue;
                dq.addLast(n);
            }
        }
        return seen;
    }

//    private static void checkLocks(Map<String, AbilityTreeData.Ability> abilitiesById,
//                                   Set<String> desired) throws PlanningException {
//        for (String id : desired) {
//            AbilityTreeData.Ability a = abilitiesById.get(id);
//            if (a == null || a.locks == null) continue;
//            for (String locked : a.locks) {
//                if (desired.contains(locked)) {
//                    throw new PlanningException("abilities '" + id + "' and '" + locked +
//                            "' cannot both be taken (lock conflict).");
//                }
//            }
//        }
//    }

    // -------------------------------------------------------------
    //  Archetype parsing from description + requirements
    // -------------------------------------------------------------

    /** Canonical key for archetype names: strip HTML/codes, drop \"Archetype\" suffix, lower-case. */
    private static String canonicalArchetypeKey(String raw) {
        if (raw == null) return null;
        String plain = stripFormatting(raw);
        plain = plain.toLowerCase(Locale.ROOT).trim();
        plain = plain.replace(" archetype", "").trim();
        return plain;
    }

    /** Very small HTML/Minecraft formatting stripper used for descriptions and names. */
    private static String stripFormatting(String s) {
        if (s == null) return "";
        // Decode the hex-percent encoding used by Wynncraft API (003c = <, 003e = >)
        String plain = s.replace("003c", "<").replace("003e", ">")
                .replace("003C", "<").replace("003E", ">")
                .replace("0027", "'");
        // Now strip HTML tags
        plain = plain.replaceAll("<[^>]+>", " ");
        plain = plain.replaceAll("§.", "");
        plain = plain.replace("\u2019", "'").replace("\u2018", "'");
        plain = plain.replaceAll("[\\p{C}]+", " ");
        plain = plain.replaceAll("\\s+", " ").trim();
        return plain;
    }


    /** Extract \"Boltslinger\" from a line like \"**Boltslinger Archetype**\". */
    private static Optional<String> extractArchetypeFromLine(String line) {
        if (line == null) return Optional.empty();
        String plain = stripFormatting(line);
        Pattern p = Pattern.compile("(.+?)\\s+Archetype", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(plain);
        if (m.find()) {
            return Optional.of(m.group(1).trim());
        }
        return Optional.empty();
    }

    /** Build map abilityId -> canonical archetype key it gives a point in (if any). */
    /** Build map abilityId -> internal archetype key it GIVES a point in (if any). */
    private static Map<String, String> buildAbilityArchetypeMap(AbilityTreeData tree) {
        Map<String, String> map = new HashMap<>();
        if (tree.pages == null) return map;

        // Build display-name -> internal key lookup from tree.archetypes
        // e.g. "Fallen" -> "berserker", "Battle Monk" -> "monk", "Paladin" -> "tank"
        Map<String, String> displayToInternal = new HashMap<>();
        if (tree.archetypes != null) {
            for (Map.Entry<String, AbilityTreeData.Archetype> entry : tree.archetypes.entrySet()) {
                String internalKey = entry.getKey(); // "berserker", "monk", "tank"
                String displayName = stripFormatting(entry.getValue().name).toLowerCase(Locale.ROOT)
                        .replace(" archetype", "").trim();
                displayToInternal.put(displayName, internalKey);
            }
        }

        for (Map<String, AbilityTreeData.Ability> page : tree.pages.values()) {
            if (page == null) continue;
            for (Map.Entry<String, AbilityTreeData.Ability> e : page.entrySet()) {
                String abilityId = e.getKey();
                AbilityTreeData.Ability a = e.getValue();
                if (a == null || a.description == null) continue;

                // ALL abilities tagged with an archetype in their description give a point —
                // even if they also require archetype points themselves.
                for (String line : a.description) {
                    Optional<String> arch = extractArchetypeFromLine(line);
                    if (arch.isPresent()) {
                        String displayKey = arch.get().trim().toLowerCase(Locale.ROOT);
                        String internalKey = displayToInternal.get(displayKey);
                        if (internalKey != null && !internalKey.isEmpty()) {
                            map.put(abilityId, internalKey);
                        }
                        break;
                    }
                }
            }
        }
        return map;
    }




    // -------------------------------------------------------------
    //  Unlock simulation with constraints
    // -------------------------------------------------------------

    private static List<String> simulateUnlockOrder(Map<String, AbilityTreeData.Ability> abilitiesById,
                                                    Set<String> desired,
                                                    Map<String, String> abilityIdToArchKey)
            throws PlanningException {

        Set<String> unlocked = new HashSet<>();
        Map<String, Integer> archetypeCounts = new HashMap<>();
        List<String> order = new ArrayList<>();

        while (unlocked.size() < desired.size()) {
            String bestId = null;
            AbilityTreeData.Ability bestAbility = null;

            for (String id : desired) {
                if (unlocked.contains(id)) continue;
                AbilityTreeData.Ability a = abilitiesById.get(id);
                if (a == null) continue;
                if (!canUnlockNow(a, unlocked, archetypeCounts)) continue;

                if (bestId == null) {
                    bestId = id;
                    bestAbility = a;
                } else if (isVisuallyBefore(a, bestAbility)) {
                    bestId = id;
                    bestAbility = a;
                }
            }

            if (bestId == null) {
                throw new PlanningException(
                        "no further ability can be unlocked given current NODE/archetype requirements; " +
                                "the build is no longer valid under the latest tree."
                );
            }

            unlocked.add(bestId);
            order.add(bestId);

            String archKey = abilityIdToArchKey.get(bestId);
            if (archKey != null && !archKey.isEmpty()) {
                archetypeCounts.merge(archKey, 1, Integer::sum);
            }
        }

        return order;
    }

    private static boolean canUnlockNow(AbilityTreeData.Ability a,
                                        Set<String> unlocked,
                                        Map<String, Integer> archetypeCounts) {

        // NODE prerequisite
        if (a.requirements != null && a.requirements.NODE != null && !a.requirements.NODE.isEmpty()) {
            if (!unlocked.contains(a.requirements.NODE)) {
                return false;
            }
        }

        // Archetype requirement
        if (a.requirements != null && a.requirements.ARCHETYPE != null) {
            String reqName = a.requirements.ARCHETYPE.name; // already internal key e.g. "berserker"
            int needed = a.requirements.ARCHETYPE.amount;
            int have = archetypeCounts.getOrDefault(reqName, 0);
            if (have < needed) return false;
        }



        // NOTE: ABILITY_POINTS here is a cost, not a threshold, and order-agnostic.
        // Since we are replaying an already-valid build, we don't need to gate by it.

        return true;
    }

    /** Tie-breaker: click earlier pages / rows / columns first, purely cosmetic. */
    private static boolean isVisuallyBefore(AbilityTreeData.Ability a, AbilityTreeData.Ability b) {
        if (a.page != b.page) return a.page < b.page;
        if (a.coordinates.y != b.coordinates.y) return a.coordinates.y < b.coordinates.y;
        return a.coordinates.x < b.coordinates.x;
    }

    // -------------------------------------------------------------
    //  Map ability IDs back to Nodes for TreeLoader
    // -------------------------------------------------------------

    private static List<AbilityMapData.Node> mapIdsToNodes(List<String> abilityIdOrder,
                                                           AbilityMapData map) throws PlanningException {
        Map<String, AbilityMapData.Node> nodesById = buildNodeIndex(map);
        List<AbilityMapData.Node> result = new ArrayList<>(abilityIdOrder.size());

        for (String id : abilityIdOrder) {
            AbilityMapData.Node node = nodesById.get(id);
            if (node == null) {
                throw new PlanningException("no map node found for ability id '" + id + "'.");
            }
            result.add(node);
        }
        return result;
    }

    // -------------------------------------------------------------
    //  Exception type
    // -------------------------------------------------------------

    private static final class PlanningException extends Exception {
        PlanningException(String msg) { super(msg); }
    }
}
