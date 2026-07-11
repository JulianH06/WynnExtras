// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ProfessionModel.
 *
 * Parses Wynncraft profession signals from chat:
 *   • XP gain    — "... [+<gain> <prof> XP] [<percent>%]"
 *   • Level-up   — "You are now level <n> in <prof>"
 *
 * Tracks (level, xpPercent) per profession. Fires ProfessionXpGainEvent on every gain so
 * WynnExtras' ProfessionXpGainMixin -> ProfessionOverlay.onXpGain runs.
 */
package julianh06.wynnextras.wtshim.models.profession;

import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.handlers.chat.event.ChatMessageEvent;
import julianh06.wynnextras.wtshim.models.profession.event.ProfessionXpGainEvent;
import julianh06.wynnextras.wtshim.models.profession.type.ProfessionType;
import julianh06.wynnextras.wtshim.utils.type.CappedValue;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.neoforged.bus.api.SubscribeEvent;

public class ProfessionModel extends Model {
    // Source: Wynntils ProfessionModel#PROFESSION_CRAFT_PATTERN — § codes stripped, glyph preserved.
    // Original: "(§dx[\\d\\.]+ )?§7\\[\\+(§d)?(?<gain>\\d+) §f[ⓀⒸⒷⒿⒺⒹⓁⒶⒼⒻⒾⒽ] §7(?<name>.+) XP\\] §6\\[(?<current>[\\d.]+)%\\]"
    private static final Pattern XP_GAIN = Pattern.compile(
            "^(?:x[\\d\\.]+ )?\\[\\+(?<gain>\\d+) [ⓀⒸⒷⒿⒺⒹⓁⒶⒼⒻⒾⒽ] (?<name>.+) XP\\] \\[(?<pct>[\\d.]+)%\\]$");

    // Source: Wynntils PROFESSION_LEVELUP_PATTERN — § codes stripped.
    // Original: "§e\\s+You are now level (?<level>\\d+) in §f[ⓀⒸⒷⒿⒺⒹⓁⒶⒼⒻⒾⒽ]§e (?<name>.+)"
    private static final Pattern LEVEL_UP = Pattern.compile(
            "^\\s+You are now level (?<level>\\d+) in [ⓀⒸⒷⒿⒺⒹⓁⒶⒼⒻⒾⒽ] (?<name>.+)$");

    private static final int[] LEVEL_UP_XP_REQUIREMENTS = {
        30, 35, 42, 48, 56, 64, 74, 84, 96, 109, 123, 140, 158, 178, 200, 225, 253, 284, 319, 358,
        401, 449, 502, 562, 629, 703, 786, 878, 981, 1096, 1224, 1367, 1526, 1704, 1901, 2122, 2368,
        2643, 2948, 3289, 3670, 4094, 4567, 5094, 5682, 6337, 7068, 7882, 8791, 9804, 10933, 12193,
        13597, 15162, 16908, 18855, 21025, 23445, 26143, 29151, 32506, 36246, 40416, 45066, 50250,
        56031, 62477, 69664, 77677, 86612, 96574, 107682, 120068, 133877, 149275, 166444, 185587,
        206932, 230731, 257267, 286855, 319845, 356629, 397643, 443374, 494364, 551218, 614610,
        685292, 764103, 851977, 949956, 1059203, 1181014, 1316832, 1468270, 1637123, 1825394,
        2035317, 2269380, 2530361, 2821354, 3145812, 3507582, 3910956, 4360718, 4862203, 5421358,
        6044816, 6739972, 7515071, 8379306, 9342928, 10417367, 11615366, 12951135, 14440517,
        16101179, 17952817, 20017392, 22319395, 24886127, 27748034, 30939059, 34497053, 38464216,
        42887603, 47819680, 53318945, 59450625, 66287449
    };

    private static final class ProgressState {
        int level = 1;
        float xpPercent = 0f;
    }

    private final Map<ProfessionType, ProgressState> progress = new EnumMap<>(ProfessionType.class);

    public ProfessionModel() {
        for (ProfessionType t : ProfessionType.values()) progress.put(t, new ProgressState());
    }

    public int getLevel(ProfessionType type) {
        if (type == null) return 0;
        return progress.getOrDefault(type, new ProgressState()).level;
    }

    public CappedValue getXP(ProfessionType type) {
        if (type == null) return CappedValue.EMPTY;
        ProgressState st = progress.getOrDefault(type, new ProgressState());
        int max = xpNeededToLevelUp(st.level);
        int current = Math.round(max * (st.xpPercent / 100f));
        return new CappedValue(current, max);
    }

    private int xpNeededToLevelUp(int level) {
        int idx = Math.max(0, level - 1);
        if (idx >= LEVEL_UP_XP_REQUIREMENTS.length) return LEVEL_UP_XP_REQUIREMENTS[LEVEL_UP_XP_REQUIREMENTS.length - 1];
        return LEVEL_UP_XP_REQUIREMENTS[idx];
    }

    // Wynntils drives profession XP from ChatMessageEvent.Match (crafting/gathering XP + level-up
    // chat lines) and additionally from LabelIdentifiedEvent (floating world labels above gathering
    // nodes). The label path needs Handlers.Label, which is NOT ported in the shim (documented
    // deviation) — so only the chat lines are parsed here. Fires ProfessionXpGainEvent on each gain
    // so WynnExtras' ProfessionXpGainMixin -> ProfessionOverlay.onXpGain still runs. Patterns are
    // color-stripped and matched against StyledText#getStringWithoutFormatting.
    @SubscribeEvent
    public void onChatMessage(ChatMessageEvent.Match event) {
        String plainText = event.getMessage().getStringWithoutFormatting();
        if (plainText == null || plainText.isEmpty()) return;

        Matcher gain = XP_GAIN.matcher(plainText);
        if (gain.matches()) {
            ProfessionType prof = ProfessionType.fromString(gain.group("name"));
            if (prof == null) return;
            float rawGain;
            float pct;
            try {
                rawGain = Float.parseFloat(gain.group("gain"));
                pct = Float.parseFloat(gain.group("pct"));
            } catch (NumberFormatException e) { return; }
            progress.get(prof).xpPercent = pct;
            // Instantiating fires WynnExtras' ProfessionXpGainMixin -> ProfessionOverlay.onXpGain
            new ProfessionXpGainEvent(prof, rawGain, pct);
            return;
        }

        Matcher lvl = LEVEL_UP.matcher(plainText);
        if (lvl.matches()) {
            ProfessionType prof = ProfessionType.fromString(lvl.group("name"));
            if (prof == null) return;
            try {
                ProgressState st = progress.get(prof);
                st.level = Integer.parseInt(lvl.group("level"));
                st.xpPercent = 0f;
            } catch (NumberFormatException ignored) {}
        }
    }
}
