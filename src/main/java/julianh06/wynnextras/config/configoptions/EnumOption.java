package julianh06.wynnextras.config.configoptions;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EnumOption<T extends Enum<T>> extends DropdownOption<T> {
    final Class<T> enumClass;

    public EnumOption(String name, String desc, Class<T> cls, Supplier<T> get, Consumer<T> set, ConfigScreenContext ctx) {
        super(name, desc, get, set, ctx);
        this.enumClass = cls;
    }

    @Override
    public void setValueByIndex(int idx) {
        T[] vals = getValues();
        if (idx >= 0 && idx < vals.length) setter.accept(vals[idx]);
    }

    @Override
    public T[] getValues() {
        return enumClass.getEnumConstants();
    }
}
