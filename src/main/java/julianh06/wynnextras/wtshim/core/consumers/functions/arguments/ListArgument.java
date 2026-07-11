// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — ListArgument<T> (faithful port). */
package julianh06.wynnextras.wtshim.core.consumers.functions.arguments;

import java.util.List;

public class ListArgument<T> extends Argument<List> {
    private final Class<T> innerType;

    public ListArgument(String name, Class<T> innerType) {
        super(name, List.class, null, false);

        if (!SUPPORTED_ARGUMENT_TYPES.contains(innerType)) {
            throw new IllegalArgumentException("Unsupported inner argument type: " + innerType);
        }

        this.innerType = innerType;
    }

    public Class<T> getInnerType() {
        return innerType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> List<U> getList(Class<U> assumedType) {
        if (!assumedType.equals(this.innerType)) {
            throw new IllegalStateException("List argument is not a " + assumedType.getSimpleName() + ".");
        }

        return (List<U>) getValueChecked(List.class);
    }
}
