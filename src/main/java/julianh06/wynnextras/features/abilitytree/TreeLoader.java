package julianh06.wynnextras.features.abilitytree;

import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynntils.core.components.Models;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.*;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@WEModule
public class TreeLoader {
    static final int GUI_SETTLE_TICKS = 4;
    static int ticksSinceLastAction = 0;
    static int socketClicksPerformed = 0;

    static boolean inTreeMenu = false;
    static boolean inResetMenu = false;
    static boolean wasStarted = false;
    static boolean treeMenuWasOpened = false;
    static boolean resetMenuWasOpened = false;
    static boolean wasReset = false;
    static int clickedSockets = 0;
    static HandledScreen<?> screen = null;
    static boolean resetTree = false;
    static List<String> abilitiesToClick = new ArrayList<>();
    static List<AbilityMapData.Node> abilitiesToClick2 = null;
    static AbilityTreeData classTree = null;

    static Gson gson = new GsonBuilder()
            .registerTypeAdapter(AbilityMapData.class, new AbilityMapDataDeserializer())
            .registerTypeAdapter(AbilityTreeData.class, new AbilityTreeDataDeserializer())
            .registerTypeAdapter(AbilityMapData.Icon.class, new IconDeserializer())
            .registerTypeAdapter(AbilityMapData.Node.class, new NodeDeserializer())
            .registerTypeAdapter(AbilityTreeData.Icon.class, new IconDeserializer())
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

//    secure tree loader
    private static Command treeload = new Command(
        "treeload",
        "",
        (ctx) -> {
            TreeData.loadAll();
            String arg = StringArgumentType.getString(ctx, "tree");
            TreeData tree = TreeData.getTree(arg);
            if(tree == null) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("This tree doesn't exist.")));
                return 1;
            }
            resetAll();
            wasStarted = true;
            resetTree = true;
            List<String> abilities = new ArrayList<>(tree.input);
//            abilitiesToClicksecure(abilities);
            return 1;
        },
        null,
        List.of(ClientCommandManager.argument("tree", StringArgumentType.word()))
    );

    private static Command loadtree = new Command(
            "loadtree","",
            (ctx) -> {
                String arg = StringArgumentType.getString(ctx, "tree");
                TreeData tree = TreeData.getTree(arg);
                if(tree == null) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("This tree doesn't exist.")));
                    return 1;
                }
                resetAll();
                wasStarted = true;
                resetTree = true;
                abilitiesToClick = new ArrayList<>(tree.input);
                return 1;
            },
            null,
            List.of(ClientCommandManager.argument("tree", StringArgumentType.word()))
    );

//    // Command to save ability tree (minimal format)
//    private static Command saveAbilityTree = new Command(
//            "savePlayerAbilityTree",
//            "",
//            (ctx) -> {
//                String playerName = StringArgumentType.getString(ctx, "player");
//                String characterUUID = StringArgumentType.getString(ctx, "characterUUID");
//                savePlayerAbilityTree(playerName, characterUUID, className);
//                String abilityFileName = playerName + "_" + characterUUID + "_abilities.json";
//                AbilityIdConverter.convert(className, abilityFileName); // calls method from your new class with class argument
//                TreeData.loadAll();
//                return 1;
//            },
//            null,
//            List.of(ClientCommandManager.argument("player" ,StringArgumentType.word()), ClientCommandManager.argument("characterUUID" ,StringArgumentType.word()))
//    );
    private static Command convertAbilityFile = new Command(
            "convertAbilityFile",
            "",
            (ctx) -> {
                String playerclass = StringArgumentType.getString(ctx, "playerclass");
                String abilityFileName = StringArgumentType.getString(ctx, "filename");
                AbilityIdConverter.convert(playerclass, abilityFileName); // calls method from your new class with class argument
                TreeData.loadAll();
                return 1;
            },
            null,
            List.of(
                    ClientCommandManager.argument("playerclass", StringArgumentType.word()),
                    ClientCommandManager.argument("filename", StringArgumentType.word())
            )
    );


    static public void resetAll() {
        treeMenuWasOpened = false;
        resetMenuWasOpened = false;
        wasReset = false;
        clickedSockets = 0;
        resetTree = false;
        abilitiesToClick = new ArrayList<>();
        abilitiesToClick2 = null;
        ticksSinceLastAction = 0;
        socketClicksPerformed = 0;
    }

    public static void init() {
        TreeData.loadAll();
        // Register commands
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> {
                    dispatcher.register(
                            ClientCommandManager.literal("loadtree")
                                    .then(
                                            ClientCommandManager.argument("tree", StringArgumentType.word())
                                                    .executes(ctx -> {
                                                        String arg = StringArgumentType.getString(ctx, "tree");
                                                        TreeData tree = TreeData.getTree(arg);
                                                        if (tree == null) {
                                                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("This tree doesn't exist.")));
                                                            return 1;
                                                        }
                                                        resetAll();
                                                        wasStarted = true;
                                                        resetTree = true;
                                                        abilitiesToClick = new ArrayList<>(tree.input);
                                                        return 1;
                                                    })
                                    )
                    );
// And
//                    dispatcher.register(
//                            ClientCommandManager.literal("savePlayerAbilityTree")
//                                    .then(
//                                            ClientCommandManager.argument("player", StringArgumentType.word())
//                                                    .executes(ctx -> {
//                                                        String playerName = StringArgumentType.getString(ctx, "player");
//                                                        String playerclass = savePlayerAbilityTree(playerName).toLowerCase();
//                                                        String abilityFileName = playerName  + "_abilities.json";
//                                                        AbilityIdConverter.convert(playerclass, abilityFileName); // calls method from your new class with class argument
//                                                        TreeData.loadAll();
//                                                        return 1;
//                                                    })
//                                    )
//                    );
                    dispatcher.register(
                            ClientCommandManager.literal("convertAbilityFile")
                                    .then(
                                            ClientCommandManager.argument("playerclass", StringArgumentType.word())
                                                    .then(
                                                            ClientCommandManager.argument("filename", StringArgumentType.word())
                                                                    .executes(ctx -> {
                                                                        String playerclass = StringArgumentType.getString(ctx, "playerclass");
                                                                        String abilityFileName = StringArgumentType.getString(ctx, "filename");
                                                                        AbilityIdConverter.convert(playerclass, abilityFileName);
                                                                        return 1;
                                                                    })
                                                    )
                                    )
                    );

                }
        );

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
            inTreeMenu = InventoryTitle.equals("\uDAFF\uDFEA\uE000");
            inResetMenu = InventoryTitle.equals("\uDAFF\uDFEA\uE001");
            if (!inTreeMenu && !inResetMenu && !wasStarted && oneTrue) resetAll();
            if (wasStarted && inTreeMenu) wasStarted = false;
        });

        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
            if (!resetTree) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            ClientPlayerEntity player = client.player;
            ticksSinceLastAction++;
            boolean hasTreeManipulation = Models.StatusEffect.getStatusEffects().stream()
                    .anyMatch(effect -> effect.getName().getStringWithoutFormatting().equals("Tree Manipulation"));
            //hasTreeManipulation = false;
            if (ticksSinceLastAction < GUI_SETTLE_TICKS) return;
            if (!treeMenuWasOpened) openTreeMenu(client, player);
            else if (hasTreeManipulation && inTreeMenu && !resetMenuWasOpened) {
                if(McUtils.player().getInventory().getStack(12).getItem() != Items.AIR) {
                    System.out.println("GO UP");
                    client.interactionManager.clickSlot(
                            screen.getScreenHandler().syncId,
                            54 + 3,
                            1,
                            SlotActionType.PICKUP,
                            client.player
                    );
                    client.interactionManager.clickSlot(
                            screen.getScreenHandler().syncId,
                            54 + 3,
                            1,
                            SlotActionType.PICKUP,
                            client.player
                    );
                    return;
                }
                System.out.println("HAS TREE MANIPULATION EFFECT");
                if(client.interactionManager == null) return;
                client.interactionManager.clickSlot(
                        screen.getScreenHandler().syncId,
                        4,
                        1,
                        SlotActionType.PICKUP,
                        client.player
                );
                resetMenuWasOpened = true;
            }
            //else if(<player has the ability tree buff where you can freely change stuff>) <right click the very first node to reset the whole tree>
            else if (inTreeMenu && !resetMenuWasOpened) openTreeResetMenu(client, player, screen);
            else if (inResetMenu && !wasReset) {
                if (countOccurences("Ability Shard", screen) < 3)
                    if (socketClicksPerformed < countOccurences("Ability Shard", screen) + 1) {
                        clickOnSockets(client, player, screen);
                        socketClicksPerformed++;
                    } else {
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("You don't have 3 Ability Shards in your Inventory")));
                        resetAll();
                    }
                else
                    confirmReset(client, player, screen);
            } else if (inTreeMenu && wasReset) resetTree = false;
            if (clickedSockets >= 6) resetAll();
        });

        // Ability Selection
        int[] abilityClickTicks = {0};
        int[] currentPage = {1};
        AtomicInteger failCycles = new AtomicInteger(); // How many times we've cycled the list
        final int MAX_FAIL_CYCLES = 30;

        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
            if(abilitiesToClick2 == null) {
                abilityClickTicks[0] = 0;
                failCycles.set(0);
                return;
            }
            if(abilitiesToClick2.isEmpty()) {
                abilityClickTicks[0] = 0;
                failCycles.set(0);
                return;
            }
            if (resetTree) {
                abilityClickTicks[0] = 0;
                currentPage[0] = 1;
                failCycles.set(0);
                return;
            }
            if (!inTreeMenu) {
                abilityClickTicks[0] = 0;
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                abilityClickTicks[0] = 0;
                return;
            }
            ClientPlayerEntity player = client.player;
            if (client.currentScreen == null) {
                abilityClickTicks[0] = 0;
                return;
            }
            HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;

            abilityClickTicks[0]++;
            if (abilityClickTicks[0] < GUI_SETTLE_TICKS) return;

            if (failCycles.get() >= MAX_FAIL_CYCLES) {
                System.out.println("Reached max cycles without unlocking abilities. Aborting!");
                abilitiesToClick2 = null; // Or handle differently
                abilityClickTicks[0] = 0;
                failCycles.set(0);
                return;
            }

            AbilityMapData.Node ability1 = abilitiesToClick2.getFirst();
            System.out.println(ability1.meta.page + " " + currentPage[0]);
            int pageOffset = ability1.meta.page - currentPage[0];

            // Navigation logic
            if (pageOffset > 0) {
                clickOnAbility(client, player, "Next Page", screen);
                currentPage[0]++;
                abilityClickTicks[0] = 0;
                return;
            }
            if (pageOffset < 0) {
                clickOnAbility(client, player, "Previous Page", screen);
                currentPage[0]--;
                abilityClickTicks[0] = 0;
                return;
            }

            // Only click if "Unlock <Ability>" is present as substring
            if(classTree == null) return;
            AbilityTreeData.Ability abilityFromNode = getAbilityFromNode(ability1, classTree);
            if(abilityFromNode == null) return;
            String name = extractAbilityNameFromHtml(abilityFromNode.name);
            System.out.println(name);
            if (hasUnlockPrefix(name, screen)) {
                System.out.println("Clicking ability: Unlock " + ability1);
                clickOnAbility(client, player, name, screen);
                abilitiesToClick2.removeFirst();
                if(abilitiesToClick2.isEmpty()) {
                    System.out.println("FINISHED"); //TODO: FIX THAT IT BREAKS WHEN LAG (E.G WHEN PLAYING ON ASIA SERVERS)
                }
                failCycles.set(0); // Success: reset fail counter
            } else {
                System.out.println("cant click");
                // If not present, move ability one spot down in the list and try unlocking the next one first
                if (abilitiesToClick2.size() > 1) {
                    abilitiesToClick2.add(Math.min(failCycles.get(), abilitiesToClick2.size() - 1), abilitiesToClick2.removeFirst());
                    failCycles.set(failCycles.get() + 1); // Count a cycle only if list is requeued
                }
            }

            abilityClickTicks[0] = 0;
        });

    }

    public static String extractAbilityNameFromHtml(String html) {
        if (html == null) return null;
        // 1) Entferne HTML-Tags, ersetze sie durch ein Leerzeichen damit Worte nicht zusammenlaufen
        String plain = html.replaceAll("<[^>]+>", " ");
        // 2) Entferne Minecraft-Farb-/Formatcodes (§x)
        plain = plain.replaceAll("§.", "");
        // 3) Unescape einiger häufiger Entities
        plain = plain.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&#39;", "'")
                .replace("&quot;", "\"");
        // 4) Normalisiere typographische Apostrophe auf ASCII-Apostroph
        plain = plain.replace('\u2019', '\'').replace('\u2018', '\'');
        // 5) Entferne Steuerzeichen, collapse multiple whitespaces, trim
        plain = plain.replaceAll("[\\p{C}]+", " ").replaceAll("\\s+", " ").trim();
        // 6) Falls der Name in Klammern oder mit vorangestellten/trailenden Satzzeichen bleibt, säubere Ränder
        plain = plain.replaceAll("^[\\p{Punct}\\s]+", "").replaceAll("[\\p{Punct}\\s]+$", "");
        return plain.isEmpty() ? null : plain;
    }


    public static AbilityMapData.Node getNodeFromAbility(AbilityTreeData.Ability ability, AbilityMapData treeData) {
        int page = ability.page;

        for(AbilityMapData.Node node : treeData.pages.get(page)) {
            if(ability.coordinates.x != node.coordinates.x) {
                //System.out.println("skipped because x");
                continue;
            }
            if(ability.coordinates.y != node.coordinates.y % 6) {
                //System.out.println("skipped because y " + ability.name);
                continue;
            }
            return node;
        }

        return null;
    }


    public static AbilityTreeData.Ability getAbilityFromNode(AbilityMapData.Node node, AbilityTreeData treeData) {
        Map<String, AbilityTreeData.Ability> page = treeData.pages.get(node.meta.page);
        for(AbilityTreeData.Ability ability : page.values()) {
            if(ability.coordinates.x != node.coordinates.x) {
                //System.out.println("skipped because x");
                continue;
            }
            if(ability.coordinates.y != node.coordinates.y % 6) {
                //System.out.println("skipped because y " + ability.name);
                continue;
            }
            return ability;
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

    // Hilfsfunktionen oben in deiner Klasse

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
                return Optional.of(m.group(1).trim()); // z.B. "Paladin" (ohne das Wort "Archetype")
            }
        }
        return Optional.empty();
    }

    public static Optional<Integer> extractCountFromComponentsString(String componentsToString) {
        if (componentsToString == null) return Optional.empty();
        String plain = componentsToString.replaceAll("§.", ""); // alle Farb-/Formatcodes entfernen
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
                archetypeItem = McUtils.inventory().getStack(at.slot);
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
            if(a.name.toLowerCase().contains("bak") || a.name.toLowerCase().contains("half")) {
                System.out.println("BAK OR HALF");
            }
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

        // Status für DFS / Toposort
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        // Archetype-Counts (öffnet Knoten wenn genug Archetype-Punkte vorhanden)
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
                    System.out.println("getting with " + arcKey);
                    int need = arReq.amount;
                    int have = arcKey == null ? 0 : archetypeCounts.getOrDefault(arcKey, 0);
                    if (have < need) {
                        return false; // hier wird bei denen dann returned
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

                    // Archetype-Extraction: falls die Ability eine Archetype-Anzeigezeile hat, erhöhe den passenden Counter
                    try {
                        Optional<String> optDisplay = extractArchetypeInfo(node.description);
                        if (optDisplay.isPresent()) {
                            String display = optDisplay.get();
                            String internalArchetypeName = getInternalName(display, archetypes);
                            String mapKey = normalizeArchetypeKey(internalArchetypeName);
                            archetypeCounts.put(mapKey, archetypeCounts.getOrDefault(mapKey, 0) + 1);
                            System.out.println("putting with " + mapKey);
                            //System.out.println(mapKey + " now has this many points: " + archetypeCounts.get(mapKey));
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

        // Versuche alle Knoten zu lösen; Knoten, die wegen Archetype-Requirements nicht gelöst werden können, werden später erneut geprüft
        for (AbilityTreeData.Ability a : nodes) {
            if (a == null || a.name == null) continue;
            String key = a.name.toLowerCase();
            if (unlocked.contains(key) || visited.contains(key)) continue;
            resolver.resolve(key);
        }

        // Wiederholte Versuche falls Archetype-Abhängigkeiten später erfüllt werden
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

        // Stack in richtige Reihenfolge umwandeln (first resolved -> first in list)
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

    // Hilfsmethode: bereinigt HTML/Farbcodes, normalisiert Apostrophe/Hyphen und collapsed whitespace
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



//    public static List<AbilityTreeData.Ability> calculateNodeOrder(List<AbilityTreeData.Ability> nodes, List<String> unlockedNodes) {
//        List<AbilityTreeData.Ability> result = new ArrayList<>();
//
//        if(nodes == null) return result;
//        if(nodes.isEmpty()) return result;
//
//        List<AbilityTreeData.Ability> copy = new ArrayList<>(nodes);
//        for(AbilityTreeData.Ability node : nodes) {
//            if(unlockedNodes.contains(node.name)) continue; //if its already added, e.g. when its required for another node earlier in the list
//            if(node.requirements != null && node.requirements.NODE != null) {
//                if(!unlockedNodes.contains(node.requirements.NODE.toLowerCase())) {
//                    List<AbilityTreeData.Ability> required = getRequiredNodes(node.requirements.NODE.toLowerCase(), copy, unlockedNodes);
//                    //for the line above it may be better to recursively call this function instead of another function but that would
//                    //require this function to be changed a little bit
//
//                    result.addAll(required);
//                    for(AbilityTreeData.Ability ability : required) {
//                        unlockedNodes.add(ability.name);
//                    }
//                    copy.removeAll(required);
//                } else {
//                    result.add(node);
//                    unlockedNodes.add(node.name);
//                    copy.remove(node);
//                }
//            } else {
//                result.add(node);
//                unlockedNodes.add(node.name.toLowerCase());
//                copy.remove(node);
//            }
//            //missing: check for archetype requirement, maybe add a queue that nodes get added to if the archetype requirement is not met
//            //and the total amount of nodes per archetype is tracked and if the needed amount is reached the element in the queue is unlocked
//            //maybe the queue can be sorted with the lowest amount needed being the first node
//        }
//
//        return result;
//    }

//    public static List<AbilityTreeData.Ability> getRequiredNodes(String nodeName, List<AbilityTreeData.Ability> nodes, List<String> unlockedNodes) {
//        List<AbilityTreeData.Ability> result = new ArrayList<>();
//
//        if(nodes == null) return result;
//        if(nodes.isEmpty()) return result;
//
//        for(AbilityTreeData.Ability node : nodes) {
//            if(!node.name.equals(nodeName)) continue;
//            if(node.requirements != null && node.requirements.NODE != null) {
//                if(unlockedNodes.contains(node.requirements.NODE.toLowerCase())) {
//
//                }
//            } else {
//                result.add(node);
//                unlockedNodes.add(node.name.toLowerCase());
//                break;
//            }
//        }
//    }



    public static void savePlayerAbilityTree(String playerName, String characterUUID, String className, SkillPoints skillPoints, AbilityMapData classMap, AbilityTreeData classTree, AbilityMapData playerTree) {
        try {
//            String playerApiUrl = "https://api.wynncraft.com/v3/player/" + playerName + "?fullResult";
////            String playerResponse = makeHttpRequest(playerApiUrl);
////            if (playerResponse == null) {
////                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Failed to fetch player data")));
////                return characterClass;
////            }
////            JsonObject playerData = JsonParser.parseString(playerResponse).getAsJsonObject();
////            String characterUuid = null;
////            JsonElement activeCharacterElement = playerData.get("activeCharacter");
////            if (activeCharacterElement != null && !activeCharacterElement.isJsonNull()) {
////                characterUuid = activeCharacterElement.getAsString();
//                if(playerData.has("characters")) {
//                    JsonObject chars = playerData.getAsJsonObject("characters");
//                    if(chars.has(characterUuid)) {
//                        JsonObject charObj = chars.get(characterUuid).getAsJsonObject();
//                        characterClass = charObj.get("type").getAsString();
//                        System.out.println(characterClass);
//                    }
//
//
//                }
//            } else if (playerData.has("characters")) {
//                JsonArray chars = playerData.getAsJsonArray("characters");
//                for (JsonElement charElement : chars) {
//                    JsonObject charObj = charElement.getAsJsonObject();
//                    if (charObj.has("uuid")) {
//                        characterUuid = charObj.get("uuid").getAsString();
//                        break;
//                    }
//                }
//            }
            if (characterUUID == null) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Player is not online or has no character UUID.")));
                return;
            }
            String abilityApiUrl = "https://api.wynncraft.com/v3/player/" + playerName + "/characters/" + characterUUID + "/abilities";
            String abilityResponse = makeHttpRequest(abilityApiUrl);
            if (abilityResponse == null) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Failed to fetch ability tree data")));
                return;
            }
            JsonArray abilityArr = JsonParser.parseString(abilityResponse).getAsJsonArray();
            List<String> ids = new ArrayList<>();
            for (JsonElement el : abilityArr) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("type") && obj.get("type").getAsString().equals("ability")) {
                    JsonObject meta = obj.getAsJsonObject("meta");
                    if (meta.has("id")) {
                        ids.add(meta.get("id").getAsString());
                    }
                }
            }
            JsonObject out = new JsonObject();
            out.addProperty("name", playerName + "_" + characterUUID);
            out.addProperty("visibleName", "");
            out.addProperty("strength", skillPoints.getStrength());
            out.addProperty("dexterity", skillPoints.getDexterity());
            out.addProperty("intelligence", skillPoints.getIntelligence());
            out.addProperty("defence", skillPoints.getDefence());
            out.addProperty("agility", skillPoints.getAgility());
            String formatted = className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase();
            out.addProperty("className", formatted);
            out.add("playerMap", gson.toJsonTree(playerTree));
            out.add("playerTree", gson.toJsonTree(classTree));
            JsonArray inArr = new JsonArray();
            for (String id : ids) inArr.add(id);
            out.add("input", inArr);

            Path treesDir = FabricLoader.getInstance().getConfigDir().resolve("wynnextras/trees");
            Files.createDirectories(treesDir);
            String fileName = playerName + "_" + characterUUID + ".json";
            Path filePath = treesDir.resolve(fileName);

            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                String prettyJson = gson.toJson(out);
                writer.write(prettyJson);
                writer.flush();
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Ability tree saved to: " + fileName)));
                TreeData.loadAll();
                return;
            } catch (IOException e) {
                System.err.println("[WynnExtras] Couldn't write ability tree file:");
                e.printStackTrace();
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Failed to save ability tree file")));
            }
        } catch (Exception e) {
            System.err.println("[WynnExtras] Error fetching ability tree:");
            e.printStackTrace();
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Error fetching ability tree")));
        }
    }

    private static String makeHttpRequest(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "WynnExtras-Mod/1.0");
            connection.setRequestProperty("Authorization", "Bearer " + WynncraftApiHandler.INSTANCE.API_KEY);

            int responseCode = connection.getResponseCode();
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
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Player has hidden their ability tree")));
                return null;
            } else if (responseCode == 401) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Invalid API key")));
                return null;
            } else {
                System.err.println("[WynnExtras] HTTP Error: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            System.err.println("[WynnExtras] Network error:");
            e.printStackTrace();
            return null;
        }
    }


    static public void openTreeMenu(MinecraftClient client, PlayerEntity player) {
        int currentSlot = player.getInventory().selectedSlot;
        player.getInventory().selectedSlot = 7;
        client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        client.interactionManager.interactItem(player, Hand.MAIN_HAND);
        client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        player.getInventory().selectedSlot = currentSlot;
        treeMenuWasOpened = true;
    }

    static public void openTreeResetMenu(MinecraftClient client, PlayerEntity player, HandledScreen<?> screen) {
        if (!inTreeMenu) return;
        resetMenuWasOpened = clickOnNameInInventory("Reset", screen, client);
    }

    static public void clickOnSockets(MinecraftClient client, PlayerEntity player, HandledScreen<?> screen) {
        if (!inResetMenu) return;
        boolean wasClicked = clickOnNameInInventory("Empty Socket", screen, client);
        if (wasClicked) clickedSockets++;
    }

    static public void confirmReset(MinecraftClient client, PlayerEntity player, HandledScreen<?> screen) {
        if (!inResetMenu) return;
        wasReset = clickOnNameInInventory("Confirm", screen, client);
    }

    static public void clickOnAbility(MinecraftClient client, PlayerEntity player, String nameToClick, HandledScreen<?> screen) {
        if (!inTreeMenu) return;
        clickOnNameInInventory(nameToClick, screen, client);
    }
    static public boolean hasUnlockPrefix(String ability, HandledScreen<?> screen) {
        String unlockName = "Unlock " + ability;
        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            if (!slot.hasStack() || slot.getStack().getCustomName() == null) continue;
            String name = slot.getStack().getCustomName().getString();
            if (name.contains(unlockName)) { // Substring match instead of exact match
                return true;
            }
        }
        return false;
    }


    static public int countOccurences(String count, HandledScreen<?> screen){
        int socketsFilled = 0;
        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++){
            Slot slot = screen.getScreenHandler().slots.get(i);
            if (!slot.hasStack() || slot.getStack().getCustomName() == null) continue;
            String name = slot.getStack().getCustomName().getString();
            if(name.contains(count)){
                socketsFilled++;
            }
        }
        return socketsFilled;
    }

    static public boolean clickOnNameInInventory(String nameToClick, HandledScreen<?> screen, MinecraftClient client) {
        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            if (!slot.hasStack() || slot.getStack().getCustomName() == null) continue;
            String name = slot.getStack().getCustomName().getString();

            if (name.contains(nameToClick)) {
                // Print item components (simplified - no NBT)
                ItemStack stack = slot.getStack();
                System.out.println("Clicked item: " + stack.getName().getString());

                clickSlotHelper(i, screen, client);
                return true;
            }
        }
        return false;
    }

    static public void clickSlotHelper(int slotid, HandledScreen<?> screen, MinecraftClient client) {
        client.interactionManager.clickSlot(
                screen.getScreenHandler().syncId,
                slotid,
                0,
                SlotActionType.PICKUP,
                client.player
        );
        ticksSinceLastAction = 0;
    }
}
