package julianh06.wynnextras.utils;

import julianh06.wynnextras.mixin.Invoker.GetTransformationInvoker;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.AffineTransformation;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

public class EntityUtils {

    public static void setScale(DisplayEntity e, Vector3fc scale) {
        AffineTransformation current = ((GetTransformationInvoker) e).invokeGetTransformation(e.getDataTracker());
        if (current == null) return;

        Matrix4fc matrix = current.getMatrix();
        Matrix4f scaleMatrix = new Matrix4f().scaling(scale.x(), scale.y(), scale.z());
        matrix = scaleMatrix.mul(matrix, new Matrix4f());

        AffineTransformation resultMatrix = new AffineTransformation(matrix);

        AffineTransformation result = new AffineTransformation(
                resultMatrix.getTranslation(),
                current.getLeftRotation(),
                resultMatrix.getScale(),
                current.getRightRotation()
        );
        e.setTransformation(result);


        /*
        Vector3f newScale = new Vector3f();
        current.getScale().mul(scale, newScale);

        AffineTransformation affineTransformation = new AffineTransformation(
                current.getTranslation(),
                current.getLeftRotation(),
                newScale,
                current.getRightRotation());
        e.setTransformation(affineTransformation);

         */
    }
}
