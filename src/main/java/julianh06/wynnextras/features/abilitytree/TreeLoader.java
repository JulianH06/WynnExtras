package julianh06.wynnextras.features.abilitytree;

import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.PlayerData;
import julianh06.wynnextras.features.profileviewer.data.SkillPoints;
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
import java.util.ArrayList;
import java.util.List;


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
    static GsonBuilder builder = new GsonBuilder();
    static Gson gson = builder.setPrettyPrinting().create();

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
            if (ticksSinceLastAction < GUI_SETTLE_TICKS) return;
            if (!treeMenuWasOpened) openTreeMenu(client, player);
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
        int[] failCycles = {0}; // How many times we've cycled the list
        final int MAX_FAIL_CYCLES = 45;


        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
            if (abilitiesToClick.isEmpty()) {
                abilityClickTicks[0] = 0;
                failCycles[0] = 0;
                return;
            }
            if (resetTree) {
                abilityClickTicks[0] = 0;
                currentPage[0] = 1;
                failCycles[0] = 0;
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

            if (failCycles[0] >= MAX_FAIL_CYCLES) {
                System.out.println("Reached max cycles without unlocking abilities. Aborting!");
                abilitiesToClick.clear(); // Or handle differently
                abilityClickTicks[0] = 0;
                failCycles[0] = 0;
                return;
            }

            String ability = abilitiesToClick.get(0);
            int pageOffset = CheckPageOfAbility.checkpage(currentPage[0], ability);

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
            if (hasUnlockPrefix(ability, screen)) {
                System.out.println("Clicking ability: Unlock " + ability);
                clickOnAbility(client, player, ability, screen);
                abilitiesToClick.remove(0);
                failCycles[0] = 0; // Success: reset fail counter
            } else {
                // If not present, move ability to end for retry and track cycles
                if (abilitiesToClick.size() > 1) {
                    abilitiesToClick.add(abilitiesToClick.remove(0));
                    failCycles[0]++; // Count a cycle only if list is requeued
                }
            }

            abilityClickTicks[0] = 0;
        });

    }


        public static PlayerData currentPlayerData;


    public static void savePlayerAbilityTree(String playerName, String characterUUID, String className, SkillPoints skillPoints) {
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
            out.addProperty("visibleName", playerName + "_" + characterUUID);
            out.addProperty("strength", skillPoints.getStrength());
            out.addProperty("dexterity", skillPoints.getDexterity());
            out.addProperty("intelligence", skillPoints.getIntelligence());
            out.addProperty("defence", skillPoints.getDefence());
            out.addProperty("agility", skillPoints.getAgility());
            out.addProperty("className", className);
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
