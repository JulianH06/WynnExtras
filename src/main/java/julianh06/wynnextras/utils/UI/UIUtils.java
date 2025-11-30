package julianh06.wynnextras.utils.UI;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public final class UIUtils {
    Identifier buttontl = Identifier.of("wynnextras", "textures/general/button/cornertl.png");
    Identifier buttontr = Identifier.of("wynnextras", "textures/general/button/cornertr.png");
    Identifier buttonbl = Identifier.of("wynnextras", "textures/general/button/cornerbl.png");
    Identifier buttonbr = Identifier.of("wynnextras", "textures/general/button/cornerbr.png");
    Identifier buttontop = Identifier.of("wynnextras", "textures/general/button/top.png");
    Identifier buttonbot = Identifier.of("wynnextras", "textures/general/button/bot.png");
    Identifier buttonleft = Identifier.of("wynnextras", "textures/general/button/left.png");
    Identifier buttonright = Identifier.of("wynnextras", "textures/general/button/right.png");

    Identifier buttontlH = Identifier.of("wynnextras", "textures/general/button/cornertlh.png");
    Identifier buttontrH = Identifier.of("wynnextras", "textures/general/button/cornertrh.png");
    Identifier buttonblH = Identifier.of("wynnextras", "textures/general/button/cornerblh.png");
    Identifier buttonbrH = Identifier.of("wynnextras", "textures/general/button/cornerbrh.png");
    Identifier buttontopH = Identifier.of("wynnextras", "textures/general/button/toph.png");
    Identifier buttonbotH = Identifier.of("wynnextras", "textures/general/button/both.png");
    Identifier buttonleftH = Identifier.of("wynnextras", "textures/general/button/lefth.png");
    Identifier buttonrightH = Identifier.of("wynnextras", "textures/general/button/righth.png");

    private DrawContext drawContext;
    private double scaleFactor;
    private int xStart;
    private int yStart;

    public UIUtils(DrawContext drawContext, double scaleFactor, int xStart, int yStart) {
        this.drawContext = drawContext;
        this.scaleFactor = scaleFactor;
        this.xStart = xStart;
        this.yStart = yStart;
    }

    // --- Kontext aktualisieren (bei jedem Render) ---
    public void updateContext(DrawContext ctx, double scaleFactor, int xStart, int yStart) {
        this.drawContext = ctx;
        this.scaleFactor = scaleFactor;
        this.xStart = xStart;
        this.yStart = yStart;
    }

    // --- Getter / Setter ---
    public double getScaleFactor() { return scaleFactor; }
    public float getScaleFactorF() { return (float) scaleFactor; }
    public void setScaleFactor(double scaleFactor) { this.scaleFactor = scaleFactor; }
    public int getXStart() { return xStart; }
    public int getYStart() { return yStart; }
    public void setOffset(int xStart, int yStart) { this.xStart = xStart; this.yStart = yStart; }

    // --- Coordinate transforms (logical -> screen pixels) ---
    public float sx(float logicalX) { return xStart + (float)(logicalX / scaleFactor); }
    public float sy(float logicalY) { return yStart + (float)(logicalY / scaleFactor); }
    public int sw(float logicalW) { return Math.max(0, (int)Math.round(logicalW / scaleFactor)); }
    public int sh(float logicalH) { return Math.max(0, (int)Math.round(logicalH / scaleFactor)); }

    // --- Drawing helpers: Background / Text / Image (Overloads wie in deinem WEScreen) ---
    public void drawBackground() {
        if (MinecraftClient.getInstance().currentScreen == null) return;
        RenderUtils.drawRect(
                drawContext.getMatrices(),
                CustomColor.fromInt(-804253680),
                0, 0, 0,
                MinecraftClient.getInstance().currentScreen.width,
                MinecraftClient.getInstance().currentScreen.height
        );
    }

    public void drawRect(float x, float y, float width, float height, CustomColor color) {
        RenderUtils.drawRect(
                drawContext.getMatrices(),
                color,
                sx(x), sy(y), 0,
                sw(width), sh(height)
        );
    }

    public void drawRect(float x, float y, float width, float heigt) {
        this.drawRect(x, y, width, heigt, CustomColor.fromHexString("FFFFFF"));
    }

    public void drawRectBorders(float x, float y, float width, float height, CustomColor color) {
        RenderUtils.drawRectBorders(
                drawContext.getMatrices(),
                color,
                sx(x), sy(y),
                sw(width), sh(height), 0, 1
        );
    }

    public void drawLine(float x1, float y1, float x2, float y2, float width, CustomColor color) {
        RenderUtils.drawLine(
                drawContext.getMatrices(),
                color,
                sx(x1), sy(y1),
                sx(x2), sy(y2),
                0.0f,
                sw(width)
        );
    }

    public void drawText(String text, float x, float y, CustomColor color, HorizontalAlignment hAlign, VerticalAlignment vAlign, TextShadow shadow, float textScale) {
        FontRenderer.getInstance().renderText(
                drawContext.getMatrices(),
                StyledText.fromComponent(Text.of(text)),
                sx(x),
                sy(y),
                color,
                hAlign,
                vAlign,
                shadow,
                (float)(textScale / scaleFactor)
        );
    }

    public void drawText(String text, float x, float y, CustomColor color, HorizontalAlignment hAlign, VerticalAlignment vAlign, float textScale) {
        drawText(text, x, y, color, hAlign, vAlign, TextShadow.NORMAL, textScale);
    }

    public void drawText(String text, float x, float y, CustomColor color, float textScale) {
        drawText(text, x, y, color, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, textScale);
    }

    public void drawText(String text, float x, float y, CustomColor color) {
        drawText(text, x, y, color, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 3f);
    }

    public void drawText(String text, float x, float y) {
        drawText(text, x, y, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 3f);
    }

    public void drawCenteredText(String text, float x, float y, CustomColor color, float textScale) {
        drawText(text, x, y, color, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, textScale);
    }

    public void drawCenteredText(String text, float x, float y, CustomColor color) {
        drawText(text, x, y, color, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 3f);
    }

    public void drawCenteredText(String text, float x, float y) {
        drawText(text, x, y, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 3f);
    }

    public void drawImage(Identifier texture, float x, float y, float width, float height) {
        RenderUtils.drawTexturedRect(
                drawContext.getMatrices(),
                texture,
                sx(x), sy(y),
                sw(width), sh(height),
                sw(width), sh(height)
        );
    }

    public void drawImage(Identifier texture, float x, float y, float width, float height, float alpha) {
        drawTexturedRect(
                drawContext.getMatrices(),
                texture,
                sx(x), sy(y), 0.0F,
                sw(width), sh(height), 0, 0,
                sw(width), sh(height),
                sw(width), sh(height),
                alpha
        );
    }

    public static void drawTexturedRect(MatrixStack matrixStack, Identifier tex, float x, float y, float z, float width, float height, int uOffset, int vOffset, int u, int v, int textureWidth, int textureHeight, float alpha) {
        float uScale = 1.0F / (float)textureWidth;
        float vScale = 1.0F / (float)textureHeight;
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR
        );

        bufferBuilder.vertex(matrix, x, y + height, z)
                .texture((float)uOffset * uScale, (float)(vOffset + v) * vScale)
                .color(1f, 1f, 1f, alpha);

        bufferBuilder.vertex(matrix, x + width, y + height, z)
                .texture((float)(uOffset + u) * uScale, (float)(vOffset + v) * vScale)
                .color(1f, 1f, 1f, alpha);

        bufferBuilder.vertex(matrix, x + width, y, z)
                .texture((float)(uOffset + u) * uScale, (float)vOffset * vScale)
                .color(1f, 1f, 1f, alpha);

        bufferBuilder.vertex(matrix, x, y, z)
                .texture((float)uOffset * uScale, (float)vOffset * vScale)
                .color(1f, 1f, 1f, alpha);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.endNullable());
    }

    public void drawButton(float x, float y, float width, float height, int scale, boolean hovered) {
        if(width > scale * 2 || height > scale * 2) {
            RenderUtils.drawRect(
                    drawContext.getMatrices(),
                    CustomColor.fromHexString("82654C"),
                    sx(x + scale) - 1, sy(y + scale) - 1, 0,
                    sw(width - scale * 2) + 2, sh(height - scale * 2) + 2
            );
        }
        if(hovered) {
            drawImage(buttontlH, x, y, scale, scale);
            drawImage(buttontrH, x + width - scale, y, scale, scale);
            drawImage(buttonblH, x, y + height - scale, scale, scale);
            drawImage(buttonbrH, x + width - scale, y + height - scale, scale, scale);
            if (width > scale * 2) {
                drawImage(buttontopH, x + scale - 2, y, width - scale * 2 + 4, scale);
                drawImage(buttonbotH, x + scale - 2, y + height - scale, width - scale * 2 + 4, scale);
            }
            if (height > scale * 2) {
                drawImage(buttonleftH, x, y + scale - 2, scale, height - scale * 2 + 4);
                drawImage(buttonrightH, x + width - scale, y + scale - 2, scale, height - scale * 2 + 4);
            }
        } else {
            drawImage(buttontl, x, y, scale, scale);
            drawImage(buttontr, x + width - scale, y, scale, scale);
            drawImage(buttonbl, x, y + height - scale, scale, scale);
            drawImage(buttonbr, x + width - scale, y + height - scale, scale, scale);
            if (width > scale * 2) {
                drawImage(buttontop, x + scale - 2, y, width - scale * 2 + 4, scale);
                drawImage(buttonbot, x + scale - 2, y + height - scale, width - scale * 2 + 4, scale);
            }
            if (height > scale * 2) {
                drawImage(buttonleft, x, y + scale - 2, scale, height - scale * 2 + 4);
                drawImage(buttonright, x + width - scale, y + scale - 2, scale, height - scale * 2 + 4);
            }
        }
    }
}


