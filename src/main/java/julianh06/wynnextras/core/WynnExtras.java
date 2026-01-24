package julianh06.wynnextras.core;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.loader.CommandLoader;
import julianh06.wynnextras.event.CharInputEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.core.loader.WELoader;
import julianh06.wynnextras.event.WorldChangeEvent;
import julianh06.wynnextras.features.abilitytree.TreeLoader;
import julianh06.wynnextras.features.aspects.maintracking;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.chat.RaidChatNotifier;
import julianh06.wynnextras.features.guildviewer.GV;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import julianh06.wynnextras.features.inventory.data.AccountBankData;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.data.BookshelfData;
import julianh06.wynnextras.features.inventory.data.CharacterBankData;
import julianh06.wynnextras.features.inventory.data.MiscBucketData;
import julianh06.wynnextras.features.misc.CustomClassSelection;
import julianh06.wynnextras.features.misc.FastRequeue;
import julianh06.wynnextras.features.misc.ProvokeTimer;
import julianh06.wynnextras.features.misc.PlayerHider;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.raid.RaidListData;
import julianh06.wynnextras.features.raid.RaidLootConfig;
import julianh06.wynnextras.features.raid.RaidLootTracker;
import julianh06.wynnextras.features.raid.RaidLootTrackerOverlay;
import julianh06.wynnextras.features.waypoints.WaypointData;
import julianh06.wynnextras.features.waypoints.Waypoints;
import julianh06.wynnextras.mixin.Accessor.KeybindingAccessor;
import julianh06.wynnextras.sound.ModSounds;
import julianh06.wynnextras.mixin.InventoryScreenMixin;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.TickScheduler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;


// TODO: Use WELogger instead of normal logger
// TODO: Use real event system instead of fabric events directly
@WEModule
public class WynnExtras implements ClientModInitializer {
	private static Command discordCmd = new Command(
			"discord",
			"",
			context -> {
				McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.literal("")).append(Text.literal("https://discord.gg/UbC6vZDaD5").setStyle(Style.EMPTY
						.withColor(Formatting.AQUA)
						.withUnderline(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/UbC6vZDaD5")))
				));
				return 1;
			},
			null,
			null
	);

	private static Command configCmd = new Command(
			"config",
			"",
			context -> {
				Screen configScreen = WynnExtrasConfig.createConfigScreen(null);
				MinecraftUtils.mc().send(() -> {
					MinecraftUtils.mc().setScreen(configScreen);
				});
				return 1;
			},
			null,
			null
	);

	public static final String MOD_ID = "wynnextras";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static DefaultedList<Slot> testInv;
	public static int testInvSize;

	GLFWKeyCallbackI previousCallback;

	private static final Identifier PILL_FONT = Identifier.ofVanilla("banner/pill");
	private static final Style BACKGROUND_STYLE;
	private static final Style FOREGROUND_STYLE;
	private static final Text WYNNEXTRAS_BACKGROUND_PILL;
	private static final Text WYNNEXTRAS_FOREGROUND_PILL;

	public static String latestVersion = null;



	static {
		BACKGROUND_STYLE = Style.EMPTY.withFont(PILL_FONT).
		withColor(Formatting.DARK_GREEN);
		FOREGROUND_STYLE = Style.EMPTY.withFont(PILL_FONT).
		withColor(Formatting.WHITE);
		WYNNEXTRAS_BACKGROUND_PILL = Text.literal("\uE060\uDAFF\uDFFF\uE046\uDAFF\uDFFF\uE048\uDAFF\uDFFF\uE03D\uDAFF\uDFFF\uE03D\uDAFF\uDFFF\uE034\uDAFF\uDFFF\uE047\uDAFF\uDFFF\uE043\uDAFF\uDFFF\uE041\uDAFF\uDFFF\uE030\uDAFF\uDFFF\uE042\uDAFF\uDFFF\uE062\uDAFF\uDFC2").
		fillStyle(BACKGROUND_STYLE);
		WYNNEXTRAS_FOREGROUND_PILL = Text.literal("\uE016\uE018\uE00D\uE00D\uE004\uE017\uE013\uE011\uE000\uE012\uDB00\uDC06").
		fillStyle(FOREGROUND_STYLE);
	}


	public static MutableText addWynnExtrasPrefix(Text text) {
		return Text.empty().
				append(WYNNEXTRAS_BACKGROUND_PILL).
				append(WYNNEXTRAS_FOREGROUND_PILL).
				//append(Text.literal("\uE02f\uE02f\uDB00\uDC04").fillStyle(Style.EMPTY.withFont(PILL_FONT).withColor(Formatting.DARK_GREEN))). // adds ">>"
				append(text);
	}

	public static MutableText addWynnExtrasPrefix(String text) {
		return addWynnExtrasPrefix(Text.of(text));
	}


	@Override
	public void onInitializeClient() {
		Core.init(MOD_ID);
		CurrentVersionData.INSTANCE.version = FabricLoader.getInstance().getModContainer("wynnextras").map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
		CurrentVersionData.save();
		latestVersion = CurrentVersionData.fetchLatestVersion();

		WELoader.loadAll();
		TickScheduler.init();

		julianh06.wynnextras.event.ClickEvent.register();

		PlayerHider.registerBossPlayerHider();
		BankOverlay.registerBankOverlay();
		PV.register();
		GV.register();
		ProvokeTimer.init();
		Waypoints.register();
		FastRequeue.registerFastRequeue();
		TreeLoader.init();
		maintracking.init();
        RaidLootTracker.register();
        RaidLootTrackerOverlay.register();
        RaidLootConfig.INSTANCE.load();

		RaidListData.load();
		WaypointData.load();
		RaidChatNotifier.INSTANCE.load();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			AccountBankData.INSTANCE.load();
			CharacterBankData.INSTANCE.load();
			BookshelfData.INSTANCE.load();
			MiscBucketData.INSTANCE.load();
			WynncraftApiHandler.load();
			System.out.println("loaded bankdata");
		});

		//WynnExtrasSounds.register();
		ModSounds.registerSounds();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void initKeyInputEvent(TickEvent event) {
		if(!KeyInputEvent.initialized && MinecraftClient.getInstance().getWindow() != null) {
			KeyInputEvent.init();

			previousCallback = GLFW.glfwSetKeyCallback(MinecraftClient.getInstance().getWindow().getHandle(), (window, key, scancode, action, mods) -> {
				if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT || action == GLFW.GLFW_RELEASE) {
					new KeyInputEvent(key, scancode, action, mods).post();//, character.get()).post();
				}

				if(BankOverlay2.searchbar2 != null) {
					if (BankOverlay.currentOverlayType != BankOverlayType.NONE && BankOverlay2.searchbar2.isFocused() && key == ((KeybindingAccessor) MinecraftClient.getInstance().options.inventoryKey).getBoundKey().getCode()) return;
				}

				for(BankOverlay2.PageWidget page : BankOverlay2.pages) {
					if(page.sign == null) continue;
					if(page.sign.textInputWidget == null) continue;

					if (BankOverlay.currentOverlayType != BankOverlayType.NONE && page.sign.textInputWidget.isFocused() && key == ((KeybindingAccessor) MinecraftClient.getInstance().options.inventoryKey).getBoundKey().getCode()) return;
				}

				if(BankOverlay.currentOverlayType != BankOverlayType.NONE && (GLFW.GLFW_KEY_1 <= key && key <= GLFW.GLFW_KEY_9)) return;

				if (previousCallback != null) {
					previousCallback.invoke(window, key, scancode, action, mods);
				}
			});

			GLFW.glfwSetCharCallback(MinecraftClient.getInstance().getWindow().getHandle(), (win, codepoint) -> {
				new CharInputEvent((char) codepoint).post();
			});
		}
	}

	private static int ticksUntilNotify = -1;

	@SubscribeEvent
	public void onWorldChange(WorldChangeEvent event) {
		if (latestVersion != null && !CurrentVersionData.INSTANCE.version.equals(latestVersion)) {
			ticksUntilNotify = 50; //small delay
		}
	}

	public static int normalGUIScale = -1;

	@SubscribeEvent
	public void onClientTick(TickEvent event) {
		WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
		if(config.differentGUIScale) {
			if (MinecraftClient.getInstance().currentScreen == null) {
				if (normalGUIScale != -1) {
					MinecraftClient.getInstance().options.getGuiScale().setValue(normalGUIScale);
					normalGUIScale = -1;
				}
			}
		}

//		try {
//			if(McUtils.containerMenu() != null) {
//				ItemStack rightArrow = McUtils.containerMenu().getSlot(52).getStack();
//				List<Text> lore = rightArrow.getComponents().get(DataComponentTypes.LORE).lines();
//				//System.out.println(lore);
//				if(rightArrow.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA).getFloat(0).equals(240.0f)) {
//					//System.out.println("RED ARROW");
//					for(Text text : lore) {
//						if(text.getString().contains("§7Price")) {
//							//System.out.println(text.getString() + (text.getString().contains("✖") ? " CANNOT AFFORD" : " CAN AFFORD"));
//							break;
//						}
//					}
//				} else if (rightArrow.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA).getFloat(0).equals(239.0f)) {
//					System.out.println("GREEN ARROW");
//				} else if (rightArrow.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA).getFloat(0).equals(237.0f)){
//					System.out.println("BOUGHT");
//				} else {
////					System.out.println(rightArrow.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA).getFloat(0));
//
//				}
//			}
//		} catch (Exception ignored) {
//			//System.out.println("crash");
//		}

		if (ticksUntilNotify < 0) return;

		ticksUntilNotify--;
		if (ticksUntilNotify == 0) {
			tryNotifyVersionUpdate(CurrentVersionData.INSTANCE.version, latestVersion);
		}
	}

	private static Instant lastNotificationTime = null;
	private static final Duration COOLDOWN = Duration.ofMinutes(15);

	public static void tryNotifyVersionUpdate(String currentVersion, String latestVersion) {
		if (latestVersion == null || currentVersion.equals(latestVersion)) return;

		Instant now = Instant.now();
		if (lastNotificationTime == null || Duration.between(lastNotificationTime, now).compareTo(COOLDOWN) >= 0) {
			lastNotificationTime = now;
			McUtils.sendMessageToClient(
				addWynnExtrasPrefix(Text.of("§aA new version of WynnExtras is available: §b" + latestVersion + "§a! You're currently using version §b" + currentVersion + "§a. You can download it now on Modrinth!"))
			);
		}
	}
}