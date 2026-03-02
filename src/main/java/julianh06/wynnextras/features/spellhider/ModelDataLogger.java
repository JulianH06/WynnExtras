package julianh06.wynnextras.features.spellhider;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import net.minecraft.util.Identifier;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@WEModule
public class ModelDataLogger {
    public enum State {
        OFF,
        CONSOLE_ALL,
        CONSOLE_UNKNOWN,
        CHAT_ALL,
        CHAT_UNKNOWN;

        public static State from(String state) {
            try {
                return valueOf(state.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private static State currentState = State.OFF;

    public static void setState(State state) {
        currentState = state;
    }

    public static State getCurrentState() {
        return currentState;
    }

    private static final ConcurrentLinkedQueue<Data> unknownQueue = new ConcurrentLinkedQueue<>();

    public static void handleUnknownModel(Float customModel, Set<Identifier> names) {
        Data data = new Data(customModel, names);
        if (!unknownQueue.contains(data)) {
            unknownQueue.add(data);
            if (SpellHider.debug)
                WynnExtras.LOGGER.info("unknown model: " + customModel + " names: " + names.stream().map(Identifier::getPath).collect(Collectors.toSet()));
        }
    }

    private record Data(Float customModel, Set<Identifier> names) {
    }
}
