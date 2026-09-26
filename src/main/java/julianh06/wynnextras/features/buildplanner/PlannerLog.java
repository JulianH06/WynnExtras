package julianh06.wynnextras.features.buildplanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlannerLog {
    // Share the mod's logger without initializing the client entrypoint in data tests.
    public static final Logger LOGGER = LoggerFactory.getLogger("wynnextras");

    private PlannerLog() {}
}
