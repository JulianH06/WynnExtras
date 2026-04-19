package julianh06.wynnextras.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ModMixinPlugin implements IMixinConfigPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("wmd$")) {
            return FabricLoader.getInstance().isModLoaded("wynnmod");
        }

        try {
            String classPath = targetClassName.replace('.', '/') + ".class";
            boolean exists = getClass().getClassLoader().getResourceAsStream(classPath) != null;

            if (!exists) {
                LogManager.getLogger("WynnExtras").warn(
                        "Skipping mixin {} - target class {} not found",
                        mixinClassName, targetClassName
                );
            }
            return exists;
        } catch (Exception ignored) { return false; }
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
