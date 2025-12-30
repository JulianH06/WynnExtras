package julianh06.wynnextras.core;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import static julianh06.wynnextras.features.misc.SourceOfThruth.sounds;

public class WynnExtrasSounds {
    public static Identifier yesSound = Identifier.of("wynnextras", "yes");
    public static Identifier noSound = Identifier.of("wynnextras", "no");
    public static Identifier no2Sound = Identifier.of("wynnextras", "no2");
    public static Identifier nothingSound = Identifier.of("wynnextras", "nothing");
    public static Identifier nothing2Sound = Identifier.of("wynnextras", "nothing2");
    public static Identifier idtsSound = Identifier.of("wynnextras", "idts");
    public static Identifier askagainSound = Identifier.of("wynnextras", "askagain");
    public static Identifier neitherSound = Identifier.of("wynnextras", "neither");

    public static SoundEvent yes = SoundEvent.of(yesSound);
    public static SoundEvent no = SoundEvent.of(noSound);
    public static SoundEvent no2 = SoundEvent.of(no2Sound);
    public static SoundEvent nothing = SoundEvent.of(nothingSound);
    public static SoundEvent nothing2 = SoundEvent.of(nothing2Sound);
    public static SoundEvent idts = SoundEvent.of(idtsSound);
    public static SoundEvent askagain = SoundEvent.of(askagainSound);
    public static SoundEvent neither = SoundEvent.of(neitherSound);

    public static void register() {
        Registry.register(Registries.SOUND_EVENT, yesSound, yes);
        Registry.register(Registries.SOUND_EVENT, noSound, no);
        Registry.register(Registries.SOUND_EVENT, no2Sound, no2);
        Registry.register(Registries.SOUND_EVENT, nothingSound, nothing);
        Registry.register(Registries.SOUND_EVENT, nothing2Sound, nothing2);
        Registry.register(Registries.SOUND_EVENT, idtsSound, idts);
        Registry.register(Registries.SOUND_EVENT, askagainSound, askagain);
        Registry.register(Registries.SOUND_EVENT, neitherSound, neither);
    }
}
