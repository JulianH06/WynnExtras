// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.handlers.container.scriptedquery;

import julianh06.wynnextras.wtshim.handlers.container.ContainerQueryStep;
import julianh06.wynnextras.wtshim.handlers.container.ContainerQueryException;
import julianh06.wynnextras.wtshim.handlers.container.type.ContainerContent;
import julianh06.wynnextras.wtshim.handlers.container.type.ContainerPredicate;

class RepeatedQueryStep extends QueryStep {
    private final ContainerPredicate checkRepeat;

    RepeatedQueryStep(ContainerPredicate checkRepeat, QueryStep queryStep) {
        super(queryStep);
        this.checkRepeat = checkRepeat;
    }

    @Override
    boolean startStep(ScriptedContainerQuery query, ContainerContent container) throws ContainerQueryException {
        if (!checkRepeat.execute(container)) {
            // Skip this, and retry with next step from query
            if (!query.popOneStep()) return false;
            return query.startStep(container);
        }

        // Otherwise run this as a normal step
        return super.startStep(query, container);
    }

    @Override
    ContainerQueryStep getNextStep(ScriptedContainerQuery query) {
        // Try this again
        return query;
    }
}
