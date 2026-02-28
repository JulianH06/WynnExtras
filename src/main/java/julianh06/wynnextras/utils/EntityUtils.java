package julianh06.wynnextras.utils;

import julianh06.wynnextras.mixin.Invoker.GetTransformationInvoker;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.AffineTransformation;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class EntityUtils {

    public static boolean setScale(DisplayEntity e, Vector3fc scale) {
        AffineTransformation current = ((GetTransformationInvoker) e).invokeGetTransformation(e.getDataTracker());
        if (current == null) return false;

        Vector3f newScale = new Vector3f();
        current.getScale().mul(scale, newScale);
        AffineTransformation affineTransformation = new AffineTransformation(
                current.getTranslation(),
                current.getLeftRotation(),
                newScale,
                current.getRightRotation());
        e.setTransformation(affineTransformation);
        return true;
    }
}
