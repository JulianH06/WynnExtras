package julianh06.wynnextras.core;

import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.*;
import julianh06.wynnextras.core.loader.WELoader;
import julianh06.wynnextras.features.abilitytree.TreeLoader;
import julianh06.wynnextras.features.achievements.Achievements;
import julianh06.wynnextras.features.aspects.maintracking;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.chat.RaidChatNotifier;
import julianh06.wynnextras.features.crafting.data.MaterialTextureResolver;
import julianh06.wynnextras.features.crafting.data.CraftingDataService;
import julianh06.wynnextras.features.guildviewer.BannerGuiRenderer;
import julianh06.wynnextras.features.guildviewer.GV;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import julianh06.wynnextras.features.inventory.data.AccountBankData;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.data.BookshelfData;
import julianh06.wynnextras.features.inventory.data.CharacterBankData;
import julianh06.wynnextras.features.inventory.data.MiscBucketData;
import julianh06.wynnextras.features.chat.ChatNotificator;
import julianh06.wynnextras.features.loader.SkillPointLoader;
import julianh06.wynnextras.features.misc.BloodSorrowTimer;
import julianh06.wynnextras.features.misc.FastRequeue;
import julianh06.wynnextras.features.misc.ItemComponentsDebugOverlay;
import julianh06.wynnextras.features.misc.LunarScreenOverlayFallback;
import julianh06.wynnextras.features.misc.ProvokeTimer;
import julianh06.wynnextras.features.misc.PlayerHider;
import julianh06.wynnextras.features.misc.QuickRepair;
import julianh06.wynnextras.features.misc.TotemTimer;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.ProfileTitleService;
import julianh06.wynnextras.features.qol.EncounterOverlay;
import julianh06.wynnextras.features.raid.*;
import julianh06.wynnextras.features.raid.tna.TreeRoomMinimap;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import julianh06.wynnextras.features.raid.RaidListData;
import julianh06.wynnextras.features.raid.RaidLootConfig;
import julianh06.wynnextras.features.raid.RaidLootTracker;
import julianh06.wynnextras.features.raid.RaidLootTrackerOverlay;
import julianh06.wynnextras.features.shoppinglist.ShoppingListFeature;
import julianh06.wynnextras.features.shoppinglist.service.ShoppingListTradeMarketSearchService;
import julianh06.wynnextras.features.shoppinglist.ui.ShoppingListHudOverlay;
import julianh06.wynnextras.features.shoppinglist.ui.ShoppingListMenuExtension;
import julianh06.wynnextras.features.waypoints.data.WaypointData;
import julianh06.wynnextras.mixin.Accessor.KeybindingAccessor;
import julianh06.wynnextras.sound.ModSounds;
import julianh06.wynnextras.utils.LunarCompat;
import julianh06.wynnextras.utils.TickScheduler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.*;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;


// TODO: Use WELogger instead of normal logger
// TODO: Use real event system instead of fabric events directly
@WEModule
public class WynnExtras implements ClientModInitializer {
	private static Command discordCmd = new Command(
			"discord",
			"",
			context -> {
                try {
                    MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.literal("")).append(Text.literal("https://discord.gg/UbC6vZDaD5").setStyle(Style.EMPTY
                            .withColor(Formatting.AQUA)
                            .withUnderline(true)
                            .withClickEvent(new ClickEvent.OpenUrl(new URI("https://discord.gg/UbC6vZDaD5"))))
                    ));
                } catch (URISyntaxException ignored) {}
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

	private static Command versionCmd = new Command(
			"version",
			"",
			context -> {
				MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("You are using version " + CurrentVersionData.INSTANCE.version));
				return 1;
			},
			null,
			null
	);

	public static final String MOD_ID = "wynnextras";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static DefaultedList<Slot> testInv;
	private static int testInvSize;

	GLFWKeyCallbackI previousCallback;
	GLFWCharCallbackI previousCharCallback;

	private static final Identifier PILL_FONT = Identifier.ofVanilla("banner/pill");
	private static final Style BACKGROUND_STYLE;
	private static final Style FOREGROUND_STYLE;
	private static final Text WYNNEXTRAS_BACKGROUND_PILL;
	private static final Text WYNNEXTRAS_FOREGROUND_PILL;

	private static String latestVersion = null;

	static {
		BACKGROUND_STYLE = Style.EMPTY.withFont(new StyleSpriteSource.Font(PILL_FONT)).
		withColor(Formatting.DARK_GREEN);
		FOREGROUND_STYLE = Style.EMPTY.withFont(new StyleSpriteSource.Font(PILL_FONT)).
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

	public static void sendMessageToClient(Text text) {
		MinecraftUtils.sendMessageToClient(addWynnExtrasPrefix(text));
	}

	public static void sendMessageToClient(String text) {
		MinecraftUtils.sendMessageToClient(addWynnExtrasPrefix(text));
	}


	@Override
	public void onInitializeClient() {
		Core.init(MOD_ID);
		ProfileTitleService.fetch();
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> CraftingDataService.getInstance().initialize());
		updateVersionData();

		SpecialGuiElementRegistry.register(context -> new BannerGuiRenderer(context.vertexConsumers(), MinecraftClient.getInstance().getAtlasManager()));

		WELoader.loadAll();
		TickScheduler.init();
		ChatEvent.register();
		ShoppingListTradeMarketSearchService.register();
		ShoppingListHudOverlay.register();

        new InitEvent().post();

		julianh06.wynnextras.event.ClickEvent.register();

		PlayerHider.registerBossPlayerHider();
		BankOverlay.registerBankOverlay();
		PV.register();
		GV.register();
		ProvokeTimer.init();
		TotemTimer.register();
		BloodSorrowTimer.register();
		julianh06.wynnextras.features.misc.CurseTracker.register();
		julianh06.wynnextras.features.misc.RadiantHud.init();
		julianh06.wynnextras.features.misc.ProfessionOverlay.register();
		julianh06.wynnextras.features.bankoverlay.BankOverlay2.registerScreenHooks();
		LunarScreenOverlayFallback.register();
		ItemComponentsDebugOverlay.registerInventoryScreenHooks();
		ChatNotificator.init();
		FastRequeue.registerFastRequeue();
		TreeLoader.init();
		maintracking.init();
        RaidLootTracker.register();
        RaidLootTrackerOverlay.register();
        RaidSessionTracker.register();
        julianh06.wynnextras.features.raid.PartyIgnoreOnRaid.register();
        TreeRoomMinimap.register();
        QuickRepair.register();
        julianh06.wynnextras.features.qol.AutoSkipDialogue.register();
        julianh06.wynnextras.features.qol.AutoSkipCutscenes.register();
        julianh06.wynnextras.features.chat.ChainsAttachedTracker.register();
        julianh06.wynnextras.features.qol.AuraPing.register();
        julianh06.wynnextras.features.qol.WeeklyWarCount.register();
        julianh06.wynnextras.features.qol.WarDPS.register();
        julianh06.wynnextras.features.qol.AttackTimer.register();
        julianh06.wynnextras.features.qol.WarBeacon.register();
        julianh06.wynnextras.features.qol.TerritoryMenuKey.register();
        julianh06.wynnextras.features.chat.mediapreview.ChatMediaPreview.register();
        RaidLootConfig.INSTANCE.load();
		MaterialTextureResolver.register();
		SkillPointLoader.init();

		RaidListData.load();
		WaypointData.load();
		RaidChatNotifier.load();


        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			Achievements.load();
			AccountBankData.INSTANCE.load();
			CharacterBankData.INSTANCE.load();
			BookshelfData.INSTANCE.load();
			MiscBucketData.INSTANCE.load();
			ShoppingListFeature.loadPersistedCart();
			BankOverlay2.invalidateBagTotalCache();
			WynncraftApiHandler.load();

			CompletableFuture.runAsync(WeightDisplay::getWeightsFromWynnpool).thenRunAsync(WeightDisplay::populateStatRangesFromDatabase);
		});

		// Flush any pending (debounced) achievement upload when leaving a server, so a change made
		// within the debounce window isn't lost. Fire-and-forget: don't block the disconnect.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> Achievements.flushServerSave());

		// On JVM shutdown wait briefly for the flush, since the upload runs on a daemon thread that
		// would otherwise be killed before it finishes.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				Achievements.flushServerSave().get(8, java.util.concurrent.TimeUnit.SECONDS);
			} catch (Exception e) {
				LOGGER.error("[WynnExtras] Failed to flush achievements on shutdown:", e);
			}
		}, "WynnExtras-Achievement-Shutdown-Flush"));

		ModSounds.registerSounds();

		if(FabricLoader.getInstance().isModLoaded("devauth")) {
			try {
				((org.apache.logging.log4j.core.Logger) LogManager.getLogger("wynntils")).setLevel(Level.ERROR);
			} catch (Throwable ignored) {}
		}

		ResetTimeConfig.INSTANCE.fetchIfNeeded();
	}

	private static void updateVersionData() {
		CurrentVersionData.INSTANCE.version = FabricLoader.getInstance().getModContainer("wynnextras").map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
		CurrentVersionData.save();
		//TODO: remove once test version is gone
		latestVersion = CurrentVersionData.fetchLatestVersion();
	}

	public static boolean hasTestInventory() {
		return testInv != null;
	}

	public static void updateTestInventory(DefaultedList<Slot> slots) {
		testInv = slots;
		testInvSize = slots.size() - 36;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void initKeyInputEvent(TickEvent event) {
		if(!KeyInputEvent.initialized && MinecraftClient.getInstance().getWindow() != null) {
			KeyInputEvent.init();

			previousCallback = GLFW.glfwSetKeyCallback(MinecraftClient.getInstance().getWindow().getHandle(), (window, key, scancode, action, mods) -> {
				if (ShoppingListMenuExtension.handleGlobalKeyInput(key, scancode, action, mods)) return;
				if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT || action == GLFW.GLFW_RELEASE) {
					new KeyInputEvent(key, scancode, action, mods).post();//, character.get()).post();
				}

				if (BankOverlay.currentOverlayType != BankOverlayType.NONE
						&& BankOverlay2.isAnyTextInputFocused()
						&& key == ((KeybindingAccessor) MinecraftClient.getInstance().options.inventoryKey).getBoundKey().getCode()) return;

				if(BankOverlay.currentOverlayType != BankOverlayType.NONE && (GLFW.GLFW_KEY_1 <= key && key <= GLFW.GLFW_KEY_9)) return;

				if (previousCallback != null) {
					previousCallback.invoke(window, key, scancode, action, mods);
				}
			});

			previousCharCallback = GLFW.glfwSetCharCallback(MinecraftClient.getInstance().getWindow().getHandle(), (win, codepoint) -> {
				if (ShoppingListMenuExtension.handleGlobalCharTyped((char) codepoint)) return;
				if (BankOverlay.handleScreenCharTyped((char) codepoint)) return;

				new CharInputEvent((char) codepoint).post();
				if (previousCharCallback != null) {
					previousCharCallback.invoke(win, codepoint);
				}
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

	private static int normalGUIScale = -1;

	@SubscribeEvent
	public void onClientTick(TickEvent event) {
		WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
		EncounterOverlay.clearLatchIfNoContainerOpen();
		if(config.differentGUIScale && MinecraftClient.getInstance().currentScreen == null) {
			restoreNormalGuiScale();
		}

		tickVersionNotificationCountdown();
	}

	private static void tickVersionNotificationCountdown() {
		if (ticksUntilNotify < 0) return;
		ticksUntilNotify--;
		if (ticksUntilNotify == 0) {
			tryNotifyVersionUpdate(CurrentVersionData.INSTANCE.version, latestVersion);
		}
	}

	public static boolean hasStoredNormalGuiScale() {
		return normalGUIScale != -1;
	}

	public static void storeNormalGuiScale(int guiScale) {
		normalGUIScale = guiScale;
	}

	private static void restoreNormalGuiScale() {
		if (normalGUIScale != -1) {
			MinecraftClient.getInstance().options.getGuiScale().setValue(normalGUIScale);
			clearStoredNormalGuiScale();
		}
	}

	private static void clearStoredNormalGuiScale() {
		normalGUIScale = -1;
	}

	private static Instant lastNotificationTime = null;
	private static final Duration COOLDOWN = Duration.ofMinutes(60);

	public static void tryNotifyVersionUpdate(String currentVersion, String latestVersion) {
		if (latestVersion == null || currentVersion.equals(latestVersion)) return;

		Instant now = Instant.now();
		if (lastNotificationTime == null || Duration.between(lastNotificationTime, now).compareTo(COOLDOWN) >= 0) {
			lastNotificationTime = now;
			MinecraftUtils.sendMessageToClient(
				addWynnExtrasPrefix(Text.of("§aA new version of WynnExtras is available: §b" + latestVersion + "§a! You're currently using version §b" + currentVersion + "§a. You can download it now on Modrinth!"))
			);

			if(isLunarClient()) {
				MinecraftUtils.sendMessageToClient(
					addWynnExtrasPrefix(Text.of("§aSeems like you are using Lunar Client. Some features might not work correctly with Lunar. We recommend using a different launcher like prism or Modrinth."))
				);
			}
		}
	}

	public static boolean isLunarClient() {
		return LunarCompat.isLunarClient();
	}

	public static boolean isOnBeta() {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client == null) return false;
		if (client.getCurrentServerEntry() == null) return false;

		String serverIP = client.getCurrentServerEntry().address;
		return serverIP.equalsIgnoreCase("beta.wynncraft.com");
	}
}
