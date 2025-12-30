package julianh06.wynnextras.sound;

import julianh06.wynnextras.core.WynnExtras;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import static julianh06.wynnextras.features.misc.SourceOfThruth.sounds;

public class ModSounds {
    public static final SoundEvent YES = registerSoundEvent("yes");
    public static final SoundEvent NO = registerSoundEvent("no");
    public static final SoundEvent NOTHING = registerSoundEvent("nothing");
    public static final SoundEvent IDTS = registerSoundEvent("idts");
    public static final SoundEvent ASKAGAIN = registerSoundEvent("askagain");
    public static final SoundEvent NEITHER = registerSoundEvent("neither");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of(WynnExtras.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds() {
        sounds.add(YES);
        sounds.add(NO);
        sounds.add(NOTHING);
        sounds.add(IDTS);
        sounds.add(ASKAGAIN);
        sounds.add(NEITHER);
    }
}
