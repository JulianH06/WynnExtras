package julianh06.wynnextras.core;

import com.wynntils.models.items.WynnItem;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.core.loader.WELoader;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayData;
import julianh06.wynnextras.features.misc.ProvokeTimer;
import julianh06.wynnextras.features.misc.PlayerHider;
import julianh06.wynnextras.features.raid.RaidListData;
import julianh06.wynnextras.mixin.Accessor.KeybindingAccessor;
import julianh06.wynnextras.utils.MinecraftUtils;
import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.logging.log4j.core.net.Priority;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;


// TODO: Use WELogger instead of normal logger
// TODO: Use real event system instead of fabric events directly
@WEModule
public class WynnExtras implements ClientModInitializer {
	private static Command discordCmd = new Command(
			"Discord",
			"",
			context -> {
				McUtils.sendMessageToClient(Text.literal("[WynnExtras] https://discord.gg/UbC6vZDaD5").setStyle(Style.EMPTY
						.withColor(Formatting.AQUA)
						.withUnderline(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/UbC6vZDaD5"))
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
				Screen configScreen = SimpleConfig.getConfigScreen(WynnExtrasConfig.class, null).get();
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
	public static int testBackgroundWidth;
	public static int testBackgroundHeight;

	GLFWKeyCallbackI previousCallback;
	GLFWCharCallbackI previousCharCallback;

	@Override
	public void onInitializeClient() {
		Core.init(MOD_ID);
		//WynnExtrasConfig.load();

		WELoader.loadAll();

		//Runtime.getRuntime().addShutdownHook(new Thread(WynnExtrasConfig::save));

		PlayerHider.registerBossPlayerHider();
		BankOverlay.registerBankOverlay();
		ProvokeTimer.init();

		ArrayList<WynnItem> tempList = new ArrayList<>();
		for (int i = 0; i < 54; i++) {
			tempList.add(null);
		}

		BankOverlayData.load();
		RaidListData.load();
	}

//
//	AtomicReference<Character> character = new AtomicReference<>((char) 0);
//	boolean charEventInit = false;
//
//	@SubscribeEvent(priority = EventPriority.HIGHEST)
//	public void onCharEvent(TickEvent event) {
//		if(charEventInit || MinecraftClient.getInstance().getWindow() == null) return;
//		previousCharCallback = GLFW.glfwSetCharCallback(MinecraftClient.getInstance().getWindow().getHandle(), (window, codepoint) -> {
//			if (!KeyInputEvent.initialized) {
//				KeyInputEvent.init();
//			}
//
//			character.set((char) codepoint);
//
//			if (previousCharCallback != null) {
//				previousCharCallback.invoke(window, codepoint);
//			}
//		});
//		charEventInit = true;
//	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void initKeyInputEvent(TickEvent event) {
		if(!KeyInputEvent.initialized && MinecraftClient.getInstance().getWindow() != null) {
			KeyInputEvent.init();

			previousCallback = GLFW.glfwSetKeyCallback(MinecraftClient.getInstance().getWindow().getHandle(), (window, key, scancode, action, mods) -> {
				if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT || action == GLFW.GLFW_RELEASE) {
					new KeyInputEvent(key, scancode, action, mods).post();//, character.get()).post();
				}

				if(BankOverlay.isBank && BankOverlay.activeTextInput != null && key == ((KeybindingAccessor) MinecraftClient.getInstance().options.inventoryKey).getBoundKey().getCode()) return;
				if(BankOverlay.isBank && (GLFW.GLFW_KEY_1 <= key && key <= GLFW.GLFW_KEY_9)) return;

				if (previousCallback != null) {
					previousCallback.invoke(window, key, scancode, action, mods);
				}
			});

		}
	}
}