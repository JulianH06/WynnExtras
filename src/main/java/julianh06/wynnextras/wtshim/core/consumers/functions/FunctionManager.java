// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — FunctionManager (MINIMAL faithful port).
 *
 * Scope: this is the mixin-target surface that WynnExtras' FunctionManagerMixin binds to
 * (@Shadow registerFunction + @Inject on registerAllFunctions). Wynntils' full built-in
 * function catalogue + the expression/template evaluation pipeline are NOT ported — the shim
 * has no template consumer. registerAllFunctions() is therefore empty here; the mixin injects
 * at its TAIL to register WynnExtras' own functions.
 *
 * DEVIATION: Wynntils calls registerAllFunctions() from init(); the shim has no Manager init
 * lifecycle, so it is invoked from the constructor instead (equivalent — runs once at load).
 */
package julianh06.wynnextras.wtshim.core.consumers.functions;

import julianh06.wynnextras.wtshim.core.components.Manager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class FunctionManager extends Manager {
    private final List<Function<?>> functions = new ArrayList<>();

    public FunctionManager() {
        registerAllFunctions();
    }

    public List<Function<?>> getFunctions() {
        return functions;
    }

    public Optional<Function<?>> forName(String functionName) {
        for (Function<?> function : getFunctions()) {
            if (hasName(function, functionName)) {
                return Optional.of(function);
            }
        }

        return Optional.empty();
    }

    private boolean hasName(Function<?> function, String name) {
        if (function.getName().equalsIgnoreCase(name)) return true;
        for (String alias : function.getAliasList()) {
            if (alias.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    protected void registerFunction(Function<?> function) {
        functions.add(function);
    }

    private void registerAllFunctions() {
        // Shim: no Wynntils built-in functions are ported. WynnExtras' FunctionManagerMixin
        // injects at the TAIL of this method to register its own functions.
    }
}
