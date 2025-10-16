package julianh06.wynnextras.features.profileviewer.data;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AbilityTreeCache {
    private static final Map<String, AbilityTreeData> classTrees = new HashMap<>();
    private static final Map<String /* character uuid*/, AbilityTreeData> playerTrees = new HashMap<>();
    private static final Set<String> loading = new HashSet<>();

    public static boolean isLoading(String className) {
        return loading.contains(className);
    }

    public static void loadClassTree(String className) {
        if (classTrees.containsKey(className) || loading.contains(className)) return;

        loading.add(className);
        WynncraftApiHandler.fetchClassAbilityMap(className).thenAccept(tree -> {
            classTrees.put(className, tree);
            loading.remove(className);
        }).exceptionally(ex -> {
            System.err.println("Failed to load ability tree for " + className + ": " + ex.getMessage());
            loading.remove(className);
            return null;
        });
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

    public static void cacheClassTree(String className, AbilityTreeData tree) {
        classTrees.put(className, tree);
    }

    public static AbilityTreeData getClassTree(String className /* warrior, archer, etc. */) {
        return classTrees.get(className);
    }

    public static void cachePlayerTree(String characterUUID, AbilityTreeData tree) {
        playerTrees.put(characterUUID, tree);
    }

    public static AbilityTreeData getPlayerTree(String characterUUID) {
        return playerTrees.get(characterUUID);
    }

    public static void clear() {
        classTrees.clear();
        playerTrees.clear();
    }
}

