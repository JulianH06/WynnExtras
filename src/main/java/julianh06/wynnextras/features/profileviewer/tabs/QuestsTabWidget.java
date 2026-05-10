package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.colors.WynncraftShaderColor;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.Searchbar;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static julianh06.wynnextras.features.profileviewer.PVScreen.*;

public class QuestsTabWidget extends PVScreen.TabWidget {
    static Identifier questBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questbackground.png");
    static Identifier questBackgroundBorderTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questbackgroundborders.png");
    static Identifier questSearchbarTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questsearchbar.png");
    static Identifier questBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questbackground_dark.png");
    static Identifier questBackgroundBorderTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questbackgroundborders_dark.png");
    static Identifier questSearchbarTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questsearchbar_dark.png");
    List<String> allQuests = Arrays.asList("???", "A Grave Mistake", "A Hunter's Calling", "A Journey Beyond", "A Journey Further", "A Journey Home", "A Marauder's Dues", "A New Beginning", "A Sandy Scandal", "Acquiring Credentials", "Aldorei's Secret Part I", "Aldorei's Secret Part II", "All Roads To Peace", "An Iron Heart Part I", "An Iron Heart Part II", "Apotheosis", "Arachnids' Ascent", "Beneath the Depths", "Beyond the Grave", "Blazing Retribution", "Bob's Lost Soul", "Burning Bonds", "Canyon Condor", "Celebrations in Smoke", "Clearing the Camps", "Cluck Cluck", "Cook Assistant", "Corrupted Betrayal", "Cowfusion", "Creeper Infiltration", "Crop Failure", "Death Whistle", "Deja Vu", "Desperate Metal", "Dwarves and Doguns Part I", "Dwarves and Doguns Part II", "Dwarves and Doguns Part III", "Dwarves and Doguns Part IV", "Dwelling Walls", "Echoes of Change", "Elemental Exercise", "Ensemble of Hope", "Enter the Dojo", "Enzan's Brother", "Fallen Delivery", "Fantastic Voyage", "Fate of the Fallen", "Flight in Distress", "Forbidden Prison", "From the Bottom", "From the Mountains", "Frost Bite", "General's Orders", "Grand Youth", "Grave Digger", "Green Gloop", "Haven Antiquity", "Heart of Llevigar", "Hollow Serenity", "Hunger of the Gerts Part I", "Hunger of the Gerts Part II", "Ice Nations", "Infested Plants", "Jungle Fever", "King's Recruit", "Kingdom of Sand", "Lava Springs", "Lazarus Pit", "Lexdale Witch Trials", "Lost in the Jungle", "Lost Royalty", "Lost Soles", "Lost Tower", "Maltic's Well", "Master Piece", "Meaningful Holiday", "Memory Paranoia", "Mini-Quest - Gather Acacia Logs", "Mini-Quest - Gather Acacia Logs II", "Mini-Quest - Gather Avo Logs", "Mini-Quest - Gather Avo Logs II", "Mini-Quest - Gather Avo Logs III", "Mini-Quest - Gather Avo Logs IV", "Mini-Quest - Gather Bamboo", "Mini-Quest - Gather Barley", "Mini-Quest - Gather Bass", "Mini-Quest - Gather Bass II", "Mini-Quest - Gather Bass III", "Mini-Quest - Gather Bass IV", "Mini-Quest - Gather Birch Logs", "Mini-Quest - Gather Carp", "Mini-Quest - Gather Carp II", "Mini-Quest - Gather Cobalt", "Mini-Quest - Gather Cobalt II", "Mini-Quest - Gather Cobalt III", "Mini-Quest - Gather Copper", "Mini-Quest - Gather Dark Logs", "Mini-Quest - Gather Dark Logs II", "Mini-Quest - Gather Dark Logs III", "Mini-Quest - Gather Decay Roots", "Mini-Quest - Gather Decay Roots II", "Mini-Quest - Gather Decay Roots III", "Mini-Quest - Gather Diamonds", "Mini-Quest - Gather Diamonds II", "Mini-Quest - Gather Diamonds III", "Mini-Quest - Gather Diamonds IV", "Mini-Quest - Gather Gold", "Mini-Quest - Gather Gold II", "Mini-Quest - Gather Granite", "Mini-Quest - Gather Gudgeon", "Mini-Quest - Gather Gylia Fish", "Mini-Quest - Gather Gylia Fish II", "Mini-Quest - Gather Gylia Fish III", "Mini-Quest - Gather Hops", "Mini-Quest - Gather Hops II", "Mini-Quest - Gather Icefish", "Mini-Quest - Gather Icefish II", "Mini-Quest - Gather Iron", "Mini-Quest - Gather Iron II", "Mini-Quest - Gather Jungle Logs", "Mini-Quest - Gather Jungle Logs II", "Mini-Quest - Gather Kanderstone", "Mini-Quest - Gather Kanderstone II", "Mini-Quest - Gather Kanderstone III", "Mini-Quest - Gather Koi", "Mini-Quest - Gather Koi II", "Mini-Quest - Gather Koi III", "Mini-Quest - Gather Light Logs", "Mini-Quest - Gather Light Logs II", "Mini-Quest - Gather Light Logs III", "Mini-Quest - Gather Malt", "Mini-Quest - Gather Malt II", "Mini-Quest - Gather Millet", "Mini-Quest - Gather Millet II", "Mini-Quest - Gather Millet III", "Mini-Quest - Gather Molten Eel", "Mini-Quest - Gather Molten Eel II", "Mini-Quest - Gather Molten Eel III", "Mini-Quest - Gather Molten Eel IV", "Mini-Quest - Gather Molten Ore", "Mini-Quest - Gather Molten Ore II", "Mini-Quest - Gather Molten Ore III", "Mini-Quest - Gather Molten Ore IV", "Mini-Quest - Gather Oak Logs", "Mini-Quest - Gather Oats", "Mini-Quest - Gather Oats II", "Mini-Quest - Gather Pine Logs", "Mini-Quest - Gather Pine Logs II", "Mini-Quest - Gather Pine Logs III", "Mini-Quest - Gather Piranhas", "Mini-Quest - Gather Piranhas II", "Mini-Quest - Gather Rice", "Mini-Quest - Gather Rice II", "Mini-Quest - Gather Rice III", "Mini-Quest - Gather Rice IV", "Mini-Quest - Gather Rye", "Mini-Quest - Gather Rye II", "Mini-Quest - Gather Salmon", "Mini-Quest - Gather Salmon II", "Mini-Quest - Gather Sandstone", "Mini-Quest - Gather Sandstone II", "Mini-Quest - Gather Silver", "Mini-Quest - Gather Silver II", "Mini-Quest - Gather Sorghum", "Mini-Quest - Gather Sorghum II", "Mini-Quest - Gather Sorghum III", "Mini-Quest - Gather Sorghum IV", "Mini-Quest - Gather Spruce Logs", "Mini-Quest - Gather Spruce Logs II", "Mini-Quest - Gather Trout", "Mini-Quest - Gather Wheat", "Mini-Quest - Gather Willow Logs", "Mini-Quest - Gather Willow Logs II", "Mini-Quest - Slay Ailuropodas", "Mini-Quest - Slay Angels", "Mini-Quest - Slay Astrochelys Manis", "Mini-Quest - Slay Azers", "Mini-Quest - Slay Conures", "Mini-Quest - Slay Coyotes", "Mini-Quest - Slay Creatures of Nesaak Forest", "Mini-Quest - Slay Creatures of the Void", "Mini-Quest - Slay Dead Villagers", "Mini-Quest - Slay Dragonlings", "Mini-Quest - Slay Felrocs", "Mini-Quest - Slay Frosted Guards & Cryostone Golems", "Mini-Quest - Slay Hobgoblins", "Mini-Quest - Slay Idols", "Mini-Quest - Slay Ifrits", "Mini-Quest - Slay Jinkos", "Mini-Quest - Slay Lizardmen", "Mini-Quest - Slay Magma Entities", "Mini-Quest - Slay Mooshrooms", "Mini-Quest - Slay Myconids", "Mini-Quest - Slay Orcs", "Mini-Quest - Slay Pernix Monkeys", "Mini-Quest - Slay Robots", "Mini-Quest - Slay Scarabs", "Mini-Quest - Slay Skeletons", "Mini-Quest - Slay Slimes", "Mini-Quest - Slay Spiders", "Mini-Quest - Slay Weirds", "Mini-Quest - Slay Wraiths & Phantasms", "Mini-Quest - Slay Hedoro", "Mini-Quest - Hunt Banshee Stags", "Mini-Quest - Hunt Woolly Rhinos", "Mini-Quest - Collect White Lilies", "Misadventure on the Sea", "Mixed Feelings", "Murder Mystery", "Mushroom Man", "Off the Rails", "One Thousand Meters Under", "Out of my Mind", "Overture to Despair", "Pirate's Trove", "Pit of the Dead", "Point of No Return", "Poisoning the Pest", "Potion Making", "Purple and Blue", "Queen's Recruit", "Realm of Light I - The Worm Holes", "Realm of Light II - Taproot", "Realm of Light III - A Headless History", "Realm of Light IV - Finding the Light", "Realm of Light V - The Realm of Light", "Recipe For Disaster", "Reclaiming the House", "Recover the Past", "Redbeard's Booty", "Reincarnation", "Revelations in Fall", "Rise of the Quartron", "Royal Trials", "Shattered Minds", "Shrouded in Mist", "Solidarity of Steel", "Stable Story", "Star Thief", "Supply and Delivery", "Taking the Tower", "Temple of the Legends", "Tempo Town Trouble", "The Bigger Picture", "The Breaking Point", "The Canary Calls", "The Canyon Guides", "The Corrupted Village", "The Cursed One", "The Dark Descent", "The Envoy Part I", "The Envoy Part II", "The Feathers Fly Part I", "The Feathers Fly Part II", "The Hero of Gavel", "The Hidden City", "The House of Twain", "The Lost", "The Maiden Tower", "The Mercenary", "The Missing Piece", "The Olmic Rune", "The Order of the Grook", "The Passage", "The Price of Ingenuity", "The Qira Hive", "The Scarred Springs", "The Sewers of Ragni", "The Shadow of the Beast", "The Strong Survive", "The Thanos Depository", "The Ultimate Weapon", "Through the Pipes", "Tower of Ascension", "Tribal Aggression", "Troubled Tribesmen", "True Colours", "Tunnel Trouble", "UndericeÀ", "Undersupply", "Underwater", "Wrath of the Mummy", "WynnExcavation Site A", "WynnExcavation Site B", "WynnExcavation Site C", "WynnExcavation Site D", "Zhight Island");

    private final PVScreen.PVScrollBarWidget pvScrollBar;

    public QuestsTabWidget() {
        super(0, 0, 0, 0);
        questSearchBar = null;
        pvScrollBar = new PVScreen.PVScrollBarWidget();
        pvScrollBar.setVisible(false);
        addChild(pvScrollBar);
    }

    //TODO: g von missing ist teilweise noch sichtbar

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(PV.currentPlayerData == null) return;
        if(questSearchBar == null) {
            questSearchBar = new Searchbar( -1, -1, -1, -1);
            questSearchBar.setSearchText("Search...");
        }

        if(selectedCharacter == null) {
            ui.drawCenteredText("Select a character to view quests.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);
            return;
        }
        List<String> quests = selectedCharacter.getQuests();
        if(quests == null) {
            List<String> apiKeyInfo = new ArrayList<>();
            if(MinecraftClient.getInstance().player != null && WynncraftApiHandler.INSTANCE.API_KEY == null || WynncraftApiHandler.INSTANCE.API_KEY.isEmpty()) {
                if(PV.currentPlayer.equalsIgnoreCase(MinecraftClient.getInstance().player.getName().getString())) {
                    apiKeyInfo.add("To get access to your private stats you need to set an api-key.");
                    apiKeyInfo.add("You can find more info by using \"/we apikey\"");
                } else {
                    apiKeyInfo.add("You might be able to see them if you set an api-key.");
                    apiKeyInfo.add("You can find more info by using \"/we apikey\"");
                }
            }

            int apiKeyInfoY = y + 385;
            for(String line : apiKeyInfo) {
                ui.drawCenteredText(line, x + 900, apiKeyInfoY, CustomColor.fromHexString("FF0000"));
                apiKeyInfoY += 30;
            }

            ui.drawCenteredText("This player has their quest stats private.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);
            return;
        }

        int numRows = (int) Math.ceil(allQuests.size() / 2.0);
        PVScreen.maxScrollOffset = Math.max(0, 204 + numRows * 36 - 690 + 100);
        pvScrollBar.setVisible(true);
        pvScrollBar.setBounds(x + 1910, y, 30, 750);

        String titleString;
        CustomColor textColor;
        if(quests.size() == 287 && !WynnExtrasConfig.INSTANCE.removeChroma) {
            textColor = WynncraftShaderColor.RAINBOW.color;
        } else {
            textColor = CustomColor.fromHexString("FFFFFF");
        }
        double value = (quests.size()/287f) * 100;
        double rounded = Math.floor(value * 10) / 10.0;
        titleString = "Completed Quests on " + getClassName(selectedCharacter) + ": " + quests.size() + "/287 (" + rounded + "%)";

        PVScreen.DarkModeToggleWidget.drawImageWithFade(questBackgroundTextureDark, questBackgroundTexture, x + 30, y + 90, 1740, 600, ui);
        PVScreen.DarkModeToggleWidget.drawImageWithFade(questSearchbarTextureDark, questSearchbarTexture, x + 600F, y + height, 1050, 60, ui);

        questSearchBar.setX((int) ((x + 200 * 3) / ui.getScaleFactor()));
        questSearchBar.setY((int) ((y + height + 7 * 3) / ui.getScaleFactor()));
        questSearchBar.setWidth((int) (350 * 3 / ui.getScaleFactor()));
        questSearchBar.setHeight((int) (14 * 3 / ui.getScaleFactor()));
        questSearchBar.drawWithoutBackgroundButWithSearchtext(ctx, CustomColor.fromHexString("FFFFFF"), (float) ui.getScaleFactor());

        int i = 0;
        List<String> allQuestsCopy = new ArrayList<>(List.copyOf(allQuests));
        allQuestsCopy.sort(Comparator.comparing(String::toLowerCase));
        quests.sort(Comparator.comparing(String::toLowerCase));
        for(String quest : quests) {
            if(!questSearchBar.getInput().isEmpty()) {
                if(!quest.toLowerCase().contains(questSearchBar.getInput().toLowerCase())) {
                    continue;
                }
            }
            int yPos = 114 + Math.floorDiv(i, 2) * 36 - scrollOffset;
            if(yPos > 690) break;
            if(yPos > 60) {
                HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;
                int rightOffset = 0;
                if (i % 2 == 1) {
                    rightOffset = 1680;
                    horizontalAlignment = HorizontalAlignment.RIGHT;
                }

                ui.drawText(quest, (float) x + 60 + rightOffset, (float) y + yPos, textColor, horizontalAlignment, VerticalAlignment.TOP, 3f);
            }
            i++;
        }

        {
            int yPos = 60 + 114 + Math.floorDiv(i, 2) * 36 - scrollOffset;
            if (yPos > 60 && yPos < 690) {
                ui.drawText("Missing:", (float) x + 60, (float) y + yPos, textColor, 5.75f);
                if(i % 2 == 0) i += 6;
                else i++;
            }
        }
        allQuestsCopy.removeAll(quests);
        for(String quest : allQuestsCopy) {
            if(!questSearchBar.getInput().isEmpty()) {
                if(!quest.toLowerCase().contains(questSearchBar.getInput().toLowerCase())) {
                    continue;
                }
            }
            int yPos = 204 + Math.floorDiv(i, 2) * 36 - scrollOffset;
            if(yPos > 690) break;
            if(yPos > 60) {
                HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;
                int rightOffset = 0;
                if (i % 2 == 1) {
                    rightOffset = 1680;
                    horizontalAlignment = HorizontalAlignment.RIGHT;
                }

                ui.drawText(quest, (float) x + 60 + rightOffset, (float) y + yPos, textColor, horizontalAlignment, VerticalAlignment.TOP, 3f);
            }
            i++;
        }

        PVScreen.DarkModeToggleWidget.drawImageWithFade(questBackgroundBorderTextureDark, questBackgroundBorderTexture, x + 30, y + 60, 1740, 660, ui);

        ui.drawCenteredText(titleString, x + 900, y + 50, textColor, 4.5f);
    }
}
