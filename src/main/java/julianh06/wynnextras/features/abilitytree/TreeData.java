package julianh06.wynnextras.features.abilitytree;

import com.google.gson.*;
import com.wynntils.models.character.SkillPointModel;
import com.wynntils.models.character.type.SavableSkillPointSet;
import julianh06.wynnextras.features.profileviewer.data.AbilityMapData;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeData;
import julianh06.wynnextras.features.profileviewer.data.SkillPoints;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class TreeData {
    public String name;
    public String visibleName;
    public int strength;
    public int dexterity;
    public int intelligence;
    public int defence;
    public int agility;
    public AbilityMapData classMap;
    public AbilityTreeData classTree;
    public AbilityMapData playerTree;
    public String className;
    public List<String> input;
    public static Map<String, TreeData> trees = new HashMap<>();
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static TreeData safeLoadTree(Path pathToFile) {
        try (BufferedReader reader = Files.newBufferedReader(pathToFile)) {
            reader.mark(2);
            int firstChar = reader.read();
            if (firstChar == '[') {
                throw new IOException("Tree file " + pathToFile.getFileName() +
                        " is in API array form! Use /savePlayerAbilityTree to convert it to object form.");
            }
            reader.reset();
            return gson.fromJson(reader, TreeData.class);
        } catch (IOException | JsonParseException e) {
            System.err.println("[TreeData] Failed to load " + pathToFile + ":");
            e.printStackTrace();
            return null;
        }
    }

    public static void loadAll() {
        trees.clear();
        Path treesDir = FabricLoader.getInstance().getConfigDir().resolve("wynnextras/trees");
        if (!Files.isDirectory(treesDir)) {
            try { Files.createDirectories(treesDir); } catch (IOException ignore) {}
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(treesDir, "*.json")) {
            for (Path file : stream) {
                TreeData t = safeLoadTree(file);
                if (t != null && t.name != null) {
                    trees.put(t.name, t);
                    System.out.println("[TreeData] Loaded ability tree: " + t.name);
                }
            }
        } catch (IOException e) {
            System.err.println("[TreeData] Failed to scan directory: " + treesDir);
        }
    }

    public static void saveAll() {
        Path treesDir = FabricLoader.getInstance().getConfigDir().resolve("wynnextras/trees");
        if (!Files.isDirectory(treesDir)) {
            try { Files.createDirectories(treesDir); } catch (IOException ignore) {}
        }

        for (TreeData tree : trees.values()) {
            Path file = treesDir.resolve(tree.name + ".json");
            try {
                String json = gson.toJson(tree);
                Files.writeString(file, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                System.err.println("[TreeData] Failed to save tree: " + tree.name);
            }
        }
    }

    public static TreeData getTree(String name) {
        return trees.get(name);
    }
}
