package julianh06.wynnextras.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import me.shedaniel.math.Color;

import java.util.ArrayList;
import java.util.List;

public class WynnExtrasModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> SimpleConfig.getConfigScreen(WynnExtrasConfig.class, parent).get();
    }

//    private static final boolean IS_CLOTH_LOADED = FabricLoader.getInstance().isModLoaded("cloth-config");
//    public Screen configScreen = null;
//
//    public void registerConfig() {
//        getModConfigScreenFactory().create(MinecraftClient.getInstance().currentScreen);
//    }
//
//    @Override
//    public ConfigScreenFactory<?> getModConfigScreenFactory() {
//        if (!IS_CLOTH_LOADED) {
//            return parent -> null;
//        }
//
//        return parent -> {
//            // Cloth Config Builder
//            ConfigBuilder builder = ConfigBuilder.create()
//                    .setParentScreen(parent)
//                    .setTitle(Text.of("WynnExtras Mod Config"));
//
//            ConfigCategory Hider = builder.getOrCreateCategory(Text.of("Party Member Hider"));
//            ConfigCategory ChatNotifier = builder.getOrCreateCategory(Text.of("Chat Notifier"));
//            ConfigCategory ChatBlocker = builder.getOrCreateCategory(Text.of("Chat Blocker"));
//            ConfigCategory Bank = builder.getOrCreateCategory(Text.of("Bank Overlay"));
//            ConfigCategory Totem = builder.getOrCreateCategory(Text.of("Totem Range Visualizer"));
//
//            Hider.addEntry(
//                    builder.entryBuilder()
//                            .startBooleanToggle(Text.of("Enable/Disable party member hide feature"), WynnExtrasConfig.INSTANCE.partyMemberHide)
//                            .setDefaultValue(true)
//                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.partyMemberHide = newValue)
//                            .build()
//            );
//
//            Hider.addEntry(
//                    builder.entryBuilder()
//                            .startBooleanToggle(Text.of("Only Hide in NOTG"), WynnExtrasConfig.INSTANCE.onlyInNotg)
//                            .setDefaultValue(false)
//                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.onlyInNotg = newValue)
//                            .build()
//            );
//
////            Hider.addEntry(
////                    builder.entryBuilder()
////                            .startBooleanToggle(Text.of("Print debug messages to minecrafts internal console"), WynnExtrasConfig.INSTANCE.printDebugToConsole)
////                            .setDefaultValue(false)
////                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.printDebugToConsole = newValue)
////                            .build()
////            );
//
//            Hider.addEntry(
//                    builder.entryBuilder()
//                            .startIntField(Text.of("Party member maximum hide distance"), WynnExtrasConfig.INSTANCE.maxHideDistance)
//                            .setDefaultValue(3)
//                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.maxHideDistance = newValue)
//                            .build()
//            );
//
//            Hider.addEntry(
//                    builder.entryBuilder()
//                            .startStrList(Text.of("Hidden Players"), WynnExtrasConfig.INSTANCE.playerHiderList)
//                            .setExpanded(true)
//                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.playerHiderList = new ArrayList<>(newValue))
//                            .build()
//            );
//
//            Hider.addEntry(
//                    builder.entryBuilder()
//                            .startTextDescription(Text.of("NOTE: this feature can break. " +
//                                    "If players who should be hidden are still shown, " +
//                                    "try \"/WynnExtras playerhiderfix\" (or \"/party list\"). This seems to be a Wynntils issue."))
//                            .build()
//            );
//
//            SubCategoryBuilder textSub = builder.entryBuilder()
//                    .startSubCategory(Text.of("Text"));
//
//            textSub.add(
//                    builder.entryBuilder()
//                            .startFloatField(Text.of("Text Scale"), WynnExtrasConfig.INSTANCE.TextScale)
//                            .setDefaultValue(5f)
//                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.TextScale = newValue)
//                            .build()
//            );
//
//            textSub.add(
//                    builder.entryBuilder()
//                            .startIntField(Text.of("Text Offset X"), WynnExtrasConfig.INSTANCE.TextOffsetX)
//                            .setDefaultValue(75)
//                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.TextOffsetX = newValue)
//                            .build()
//            );
//
//            textSub.add(
//                    builder.entryBuilder()
//                            .startIntField(Text.of("Text Offset Y"), WynnExtrasConfig.INSTANCE.TextOffsetY)
//                            .setDefaultValue(40)
//                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.TextOffsetY = newValue)
//                            .build()
//            );
//
//            textSub.add(
//                    builder.entryBuilder()
//                            .startIntField(Text.of("Text Duration (in milliseconds)"), WynnExtrasConfig.INSTANCE.TextDurationInMs)
//                            .setDefaultValue(2000)
//                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.TextDurationInMs = newValue)
//                            .build()
//            );
//
//            textSub.add(
//                    builder.entryBuilder()
//                            .startColorField(Text.of("Text Color (Hexadecimal)"), WynnExtrasConfig.INSTANCE.TextColor)
//                            .setDefaultValue2(() -> Color.ofRGB(255, 255, 255))
//                            .setSaveConsumer2(newValue -> WynnExtrasConfig.INSTANCE.TextColor = newValue)
//                            .build()
//            );
//
//            textSub.add(
//                    builder.entryBuilder()
//                            .startBooleanToggle(Text.of("Preview"), WynnExtrasConfig.INSTANCE.NotifierPreview)
//                            .setDefaultValue(false)
//                            .setSaveConsumer(newValue -> {
//                                WynnExtrasConfig.INSTANCE.NotifierPreview = newValue;
//                                System.out.println(newValue);
//                                System.out.println(WynnExtrasConfig.INSTANCE.NotifierPreview);
//                            })
//                            .build()
//            );
//
//            SubCategoryBuilder soundSub = builder.entryBuilder()
//                    .startSubCategory(Text.of("Sound"));
//
//            DropdownMenuBuilder<String> soundDropdown =
//                builder.entryBuilder()
//                    .startStringDropdownMenu(Text.of("Notification Sound (previewed on save)"), WynnExtrasConfig.INSTANCE.Sound)
//                    .setSelections(List.of(
//                        "entity.experience_orb.pickup",
//                        "block.bell.use",
//                        "entity.player.levelup",
//                        "block.anvil.place",
//                        "block.note_block.pling",
//                        "block.note_block.bell",
//                        "block.note_block.flute",
//                        "block.note_block.harp",
//                        "entity.firework_rocket.launch",
//                        "entity.item.pickup"
//                    ))
//                    .setSuggestionMode(false)
//                    .setDefaultValue("entity.experience_orb.pickup")
//                    .setSaveConsumer(newValue -> {
//                        String oldValue = WynnExtrasConfig.INSTANCE.Sound;
//                        WynnExtrasConfig.INSTANCE.Sound = newValue;
//                        if(!oldValue.equals(newValue)) {
//                            McUtils.playSoundAmbient(SoundEvent.of(Identifier.of(WynnExtrasConfig.INSTANCE.Sound)), WynnExtrasConfig.INSTANCE.SoundVolume, WynnExtrasConfig.INSTANCE.SoundPitch);
//                        }
//                    });
//
//            soundSub.add(soundDropdown.build());
//
//            soundSub.add(
//                    builder.entryBuilder()
//                            .startFloatField(Text.of("Sound volume"), WynnExtrasConfig.INSTANCE.SoundVolume)
//                            .setDefaultValue(0.1f)
//                            .setSaveConsumer(newValue -> {
//                                float oldValue = WynnExtrasConfig.INSTANCE.SoundVolume;
//                                WynnExtrasConfig.INSTANCE.SoundVolume = newValue;
//                                if(oldValue != newValue) {
//                                    McUtils.playSoundAmbient(SoundEvent.of(Identifier.of(WynnExtrasConfig.INSTANCE.Sound)), WynnExtrasConfig.INSTANCE.SoundVolume, WynnExtrasConfig.INSTANCE.SoundPitch);
//                                }
//                            })
//                            .build()
//            );
//
//            soundSub.add(
//                    builder.entryBuilder()
//                            .startFloatField(Text.of("Sound pitch"), WynnExtrasConfig.INSTANCE.SoundPitch)
//                            .setDefaultValue(1)
//                            .setSaveConsumer(newValue -> {
//                                float oldValue = WynnExtrasConfig.INSTANCE.SoundPitch;
//                                WynnExtrasConfig.INSTANCE.SoundPitch = newValue;
//                                if(oldValue != newValue) {
//                                    McUtils.playSoundAmbient(SoundEvent.of(Identifier.of(WynnExtrasConfig.INSTANCE.Sound)), WynnExtrasConfig.INSTANCE.SoundVolume, WynnExtrasConfig.INSTANCE.SoundPitch);
//                                }
//                            })
//                            .build()
//            );
//
//            ChatNotifier.addEntry(textSub.build());
//            ChatNotifier.addEntry(soundSub.build());
//
//            ChatNotifier.addEntry(
//                    builder.entryBuilder()
//                            .startStrList(Text.of("Words"), WynnExtrasConfig.INSTANCE.notifierWords)
//                            .setExpanded(true)
//                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.notifierWords = new ArrayList<>(newValue))
//                            .build()
//            );
//
//            ChatNotifier.addEntry(
//                    builder.entryBuilder()
//                            .startTextDescription(Text.of("The Phrase on the Left is what needs to be in the Message to trigger and " +
//                                    "the one on the right is the text that will be displayed. Separate them by | for it to work."))
//                            .build()
//            );
//
//            ChatBlocker.addEntry(
//                    builder.entryBuilder()
//                            .startStrList(Text.of("Words"), WynnExtrasConfig.INSTANCE.blockedWords)
//                            .setExpanded(true)
//                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.blockedWords = new ArrayList<>(newValue))
//                            .build()
//            );
//
//            Bank.addEntry(
//                    builder.entryBuilder()
//                            .startBooleanToggle(Text.of("Enable/Disable the Bank Overlay [WIP]"), WynnExtrasConfig.INSTANCE.toggleBankOverlay)
//                            .setDefaultValue(true)
//                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.toggleBankOverlay = newValue)
//                            .build()
//            );
//
//            Bank.addEntry(
//                    builder.entryBuilder()
//                            .startTextDescription(Text.of("This Feature is still Work in Progress, bugs can (and probably will) occur. " +
//                                    "Please report any issues you have on discord. If you haven't joined yet, run \"/WynnExtras Discord\". " +
//                                    "The feature is currently only available for the account bank but implementations for character banks, " +
//                                    "the tome shelf and the misc bucket are planned. Buying pages is possible but it's a bit buggy so it's " +
//                                    "recommended to disable the feature to buy pages."))
//                            .build()
//            );
//
//            Totem.addEntry(
//                    builder.entryBuilder()
//                            .startBooleanToggle(Text.of("Enable/Disable"), WynnExtrasConfig.INSTANCE.totemRangeVisualizerToggle)
//                            .setDefaultValue(true)
//                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.totemRangeVisualizerToggle = newValue)
//                            .build()
//            );
//
//            Totem.addEntry(
//                    builder.entryBuilder()
//                            .startIntField(Text.of("Totem Radius"), WynnExtrasConfig.INSTANCE.totemRange)
//                            .setDefaultValue(10)
//                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.totemRange = newValue)
//                            .build()
//            );
//
//            Totem.addEntry(
//                    builder.entryBuilder()
//                            .startIntField(Text.of("Eldritch Call Radius"), WynnExtrasConfig.INSTANCE.eldritchCallRange)
//                            .setDefaultValue(15)
//                            .setSaveConsumer(newValue -> WynnExtrasConfig.INSTANCE.eldritchCallRange = newValue)
//                            .build()
//            );
//
//            return configScreen = builder.build();
//        };
//    }
}
