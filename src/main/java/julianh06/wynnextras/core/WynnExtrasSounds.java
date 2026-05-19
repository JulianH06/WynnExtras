package julianh06.wynnextras.core;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class WynnExtrasSounds {
    public static final Identifier yesSound = Identifier.of("wynnextras", "yes");
    public static final Identifier noSound = Identifier.of("wynnextras", "no");
    public static final Identifier no2Sound = Identifier.of("wynnextras", "no2");
    public static final Identifier nothingSound = Identifier.of("wynnextras", "nothing");
    public static final Identifier nothing2Sound = Identifier.of("wynnextras", "nothing2");
    public static final Identifier idtsSound = Identifier.of("wynnextras", "idts");
    public static final Identifier askagainSound = Identifier.of("wynnextras", "askagain");
    public static final Identifier neitherSound = Identifier.of("wynnextras", "neither");

    public static final SoundEvent yes = SoundEvent.of(yesSound);
    public static final SoundEvent no = SoundEvent.of(noSound);
    public static final SoundEvent no2 = SoundEvent.of(no2Sound);
    public static final SoundEvent nothing = SoundEvent.of(nothingSound);
    public static final SoundEvent nothing2 = SoundEvent.of(nothing2Sound);
    public static final SoundEvent idts = SoundEvent.of(idtsSound);
    public static final SoundEvent askagain = SoundEvent.of(askagainSound);
    public static final SoundEvent neither = SoundEvent.of(neitherSound);

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
