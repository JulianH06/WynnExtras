package julianh06.wynnextras.features.profileviewer.data;

import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;

import java.util.*;

public class AbilityTreeCache {
    private static final Map<String, AbilityMapData> classMaps = new HashMap<>();
    private static final Map<String, AbilityTreeData> classTrees = new HashMap<>();
    private static final Map<String /* character uuid*/, AbilityMapData> playerTrees = new HashMap<>();
    private static final Set<String> loading = new HashSet<>();
    private static final Map<String, Long> failedLoads = new HashMap<>(); // Track failed loads with timestamp
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds before retry

    public static boolean isLoading(String className) {
        return loading.contains(className);
    }

    public static void loadClassTree(String className) {
        // Check if already loaded
        if (classMaps.containsKey(className) && classTrees.containsKey(className)) return;
        if (loading.contains(className) || loading.contains(className + "tree")) return;

        // Check if we recently failed - allow retry after delay
        Long failedTime = failedLoads.get(className);
        if (failedTime != null && System.currentTimeMillis() - failedTime < RETRY_DELAY_MS) {
            return; // Still in cooldown
        }
        failedLoads.remove(className);

        if (!classMaps.containsKey(className)) {
            loading.add(className);
            WynncraftApiHandler.fetchClassAbilityMap(className).thenAccept(tree -> {
                if (tree != null && tree.pages != null && !tree.pages.isEmpty()) {
                    classMaps.put(className, tree);
                    System.out.println("[AbilityTreeCache] Loaded class map for " + className + " with " + tree.pages.size() + " pages");
                } else {
                    System.err.println("[AbilityTreeCache] Loaded empty/null class map for " + className);
                    failedLoads.put(className, System.currentTimeMillis());
                }
                loading.remove(className);
            }).exceptionally(ex -> {
                System.err.println("[AbilityTreeCache] Failed to load ability map for " + className + ": " + ex.getMessage());
                ex.printStackTrace();
                loading.remove(className);
                failedLoads.put(className, System.currentTimeMillis());
                return null;
            });
        }

        if (!classTrees.containsKey(className)) {
            loading.add(className + "tree");
            WynncraftApiHandler.fetchClassAbilityTree(className).thenAccept(tree -> {
                if (tree != null && tree.pages != null && !tree.pages.isEmpty()) {
                    classTrees.put(className, tree);
                    System.out.println("[AbilityTreeCache] Loaded class tree for " + className + " with " + tree.pages.size() + " pages");
                } else {
                    System.err.println("[AbilityTreeCache] Loaded empty/null class tree for " + className);
                    failedLoads.put(className + "tree", System.currentTimeMillis());
                }
                loading.remove(className + "tree");
            }).exceptionally(ex -> {
                System.err.println("[AbilityTreeCache] Failed to load ability tree for " + className + ": " + ex.getMessage());
                ex.printStackTrace();
                loading.remove(className + "tree");
                failedLoads.put(className + "tree", System.currentTimeMillis());
                return null;
            });
        }
    }

    public static void loadCharacterTree(String characterUUID) {
        if (playerTrees.containsKey(characterUUID) || loading.contains(characterUUID)) return;

        loading.add(characterUUID);
        WynncraftApiHandler.fetchPlayerAbilityMap(PV.currentPlayerData.getUuid().toString(), characterUUID).thenAccept(tree -> {
            playerTrees.put(characterUUID, tree);
            loading.remove(characterUUID);
        }).exceptionally(ex -> {
            System.err.println("Failed to load ability tree for " + characterUUID + ": " + ex.getMessage());
            loading.remove(characterUUID);
            return null;
        });
    }

    public static void cacheClassTree(String className, AbilityMapData tree) {
        classMaps.put(className, tree);
    }

    public static AbilityMapData getClassMap(String className /* warrior, archer, etc. */) {
        return classMaps.get(className);
    }

    public static AbilityTreeData getClassTree(String className) {
        return classTrees.get(className);
    }

    public static void cachePlayerTree(String characterUUID, AbilityMapData tree) {
        playerTrees.put(characterUUID, tree);
    }

    public static AbilityMapData getPlayerTree(String characterUUID) {
        return playerTrees.get(characterUUID);
    }

    public static void clear() {
        classMaps.clear();
        classTrees.clear();
        playerTrees.clear();
        failedLoads.clear();
        loading.clear();
    }
}

