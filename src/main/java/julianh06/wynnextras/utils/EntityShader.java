package julianh06.wynnextras.utils;

import net.minecraft.client.render.model.BakedQuad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class EntityShader {
    public static Integer activeShader = null;

    private EntityShader() {}

    public static int[] mixedTintLayers(int[] tintLayers, List<BakedQuad> quads, int shader) {
        int maxTintIndex = 0;
        for (BakedQuad q : quads) {
            if (q.hasTint()) maxTintIndex = Math.max(maxTintIndex, q.tintIndex());
        }
        int len = Math.max(Math.max(tintLayers == null ? 0 : tintLayers.length, maxTintIndex + 1), 1);
        int[] out = tintLayers == null ? new int[len] : Arrays.copyOf(tintLayers, len);
        for (int i = 0; i < out.length; i++) {
            out[i] = shader;
        }
        return out;
    }

    public static List<BakedQuad> quadsWithDefaultTintIndex(List<BakedQuad> quads) {
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad q : quads) {
            int tintIndex = q.hasTint() ? q.tintIndex() : 0;
            out.add(new BakedQuad(
                q.position0(), q.position1(), q.position2(), q.position3(),
                q.packedUV0(), q.packedUV1(), q.packedUV2(), q.packedUV3(),
                tintIndex,
                q.face(), q.sprite(), q.shade(), q.lightEmission()
            ));
        }
        return out;
    }
}
