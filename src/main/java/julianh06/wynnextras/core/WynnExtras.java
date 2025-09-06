package julianh06.wynnextras.core;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.CharInputEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.core.loader.WELoader;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayData;
import julianh06.wynnextras.features.misc.ProvokeTimer;
import julianh06.wynnextras.features.misc.PlayerHider;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.raid.RaidListData;
import julianh06.wynnextras.mixin.Accessor.KeybindingAccessor;
import julianh06.wynnextras.utils.MinecraftUtils;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.slot.Slot;
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

	GLFWKeyCallbackI previousCallback;

	private static final Identifier PILL_FONT = Identifier.ofVanilla("banner/pill");
	private static final Style BACKGROUND_STYLE;
	private static final Style FOREGROUND_STYLE;
	private static final Text WYNNEXTRAS_BACKGROUND_PILL;
	private static final Text WYNNEXTRAS_FOREGROUND_PILL;

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


	public static MutableText addWynnExtrasPrefix(Text component) {
		return Text.empty().
				append(WYNNEXTRAS_BACKGROUND_PILL).
				append(WYNNEXTRAS_FOREGROUND_PILL).
				//append(Text.literal("\uE02f\uE02f\uDB00\uDC04").fillStyle(Style.EMPTY.withFont(PILL_FONT).withColor(Formatting.DARK_GREEN))). // adds ">>"
				append(component);
	}


	@Override
	public void onInitializeClient() {
		Core.init(MOD_ID);

		WELoader.loadAll();

		PlayerHider.registerBossPlayerHider();
		BankOverlay.registerBankOverlay();
		PV.register();
		ProvokeTimer.init();

		BankOverlayData.load();
		RaidListData.load();

	}

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

			GLFW.glfwSetCharCallback(MinecraftClient.getInstance().getWindow().getHandle(), (win, codepoint) -> {
				new CharInputEvent((char) codepoint).post();
			});

		}
	}
}