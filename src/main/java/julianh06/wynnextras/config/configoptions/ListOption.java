package julianh06.wynnextras.config.configoptions;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ListOption<T> extends DropdownOption<T> {
    private final List<T> values;

    public ListOption(String name, String desc, List<T> values, Supplier<T> get, Consumer<T> set, ConfigScreenContext ctx) {
        super(name, desc, get, set, ctx);
        this.values = values;
    }

    @Override
    public void setValueByIndex(int idx) {
        T[] vals = getValues();
        if (idx >= 0 && idx < vals.length) setter.accept(vals[idx]);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T[] getValues() {
        return (T[]) values.toArray();
    }
}
