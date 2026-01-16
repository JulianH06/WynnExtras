package julianh06.wynnextras.features.crafting.data;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;

public final class VcitCompat {
    private static final String MOD_ID = "variants-cit";
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded(MOD_ID);
    private static volatile boolean reflectionReady;
    private static Method getItemModuleMethod;

    private VcitCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    public static Identifier getModel(ItemStack stack) {
        if (!LOADED || stack == null || stack.isEmpty()) {
            return null;
        }
        ensureReflection();
        if (getItemModuleMethod == null) {
            return null;
        }
        try {
            Object module = getItemModuleMethod.invoke(null, stack.getItem());
            if (module == null) {
                return null;
            }
            Method getModelMethod = module.getClass().getMethod("GetModelForItem", ItemStack.class);
            Object result = getModelMethod.invoke(module, stack);
            if (result instanceof Identifier id) {
                return id;
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    public static boolean hasModel(ItemStack stack) {
        return getModel(stack) != null;
    }

    private static void ensureReflection() {
        if (reflectionReady) {
            return;
        }
        synchronized (VcitCompat.class) {
            if (reflectionReady) {
                return;
            }
            reflectionReady = true;
            try {
                Class<?> modClass = Class.forName("fr.estecka.variantscit.VariantsCitMod");
                getItemModuleMethod = modClass.getMethod("GetItemModule", Item.class);
            } catch (ReflectiveOperationException ignored) {
                getItemModuleMethod = null;
            }
        }
    }
}
