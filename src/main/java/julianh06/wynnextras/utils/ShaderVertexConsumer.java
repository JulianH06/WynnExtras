package julianh06.wynnextras.utils;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.ColorHelper;

public final class ShaderVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float rMul;
    private final float gMul;
    private final float bMul;
    private final float aMul;

    private ShaderVertexConsumer(VertexConsumer delegate, int shaderColor) {
        this.delegate = delegate;
        this.rMul = ColorHelper.getRed(shaderColor) / 255.0f;
        this.gMul = ColorHelper.getGreen(shaderColor) / 255.0f;
        this.bMul = ColorHelper.getBlue(shaderColor) / 255.0f;
        this.aMul = ColorHelper.getAlpha(shaderColor) / 255.0f;
    }

    public static VertexConsumer wrap(VertexConsumer delegate, int shaderColor) {
        return new ShaderVertexConsumer(delegate, shaderColor);
    }

    private int tint(int channel, float mul) {
        return Math.min(255, Math.round(channel * mul));
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(tint(red, rMul), tint(green, gMul), tint(blue, bMul), tint(alpha, aMul));
        return this;
    }

    @Override
    public VertexConsumer color(int argb) {
        int r = tint(ColorHelper.getRed(argb), rMul);
        int g = tint(ColorHelper.getGreen(argb), gMul);
        int b = tint(ColorHelper.getBlue(argb), bMul);
        int a = tint(ColorHelper.getAlpha(argb), aMul);
        delegate.color(ColorHelper.getArgb(a, r, g, b));
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        delegate.texture(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        delegate.overlay(u, v);
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        delegate.light(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer lineWidth(float width) {
        delegate.lineWidth(width);
        return this;
    }
}
