package julianh06.wynnextras.utils.UI;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
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

    Identifier buttontld = Identifier.of("wynnextras", "textures/general/buttondark/cornertl.png");
    Identifier buttontrd = Identifier.of("wynnextras", "textures/general/buttondark/cornertr.png");
    Identifier buttonbld = Identifier.of("wynnextras", "textures/general/buttondark/cornerbl.png");
    Identifier buttonbrd = Identifier.of("wynnextras", "textures/general/buttondark/cornerbr.png");
    Identifier buttontopd = Identifier.of("wynnextras", "textures/general/buttondark/top.png");
    Identifier buttonbotd = Identifier.of("wynnextras", "textures/general/buttondark/bot.png");
    Identifier buttonleftd = Identifier.of("wynnextras", "textures/general/buttondark/left.png");
    Identifier buttonrightd = Identifier.of("wynnextras", "textures/general/buttondark/right.png");

    Identifier buttontlHd = Identifier.of("wynnextras", "textures/general/buttondark/cornertlh.png");
    Identifier buttontrHd = Identifier.of("wynnextras", "textures/general/buttondark/cornertrh.png");
    Identifier buttonblHd = Identifier.of("wynnextras", "textures/general/buttondark/cornerblh.png");
    Identifier buttonbrHd = Identifier.of("wynnextras", "textures/general/buttondark/cornerbrh.png");
    Identifier buttontopHd = Identifier.of("wynnextras", "textures/general/buttondark/toph.png");
    Identifier buttonbotHd = Identifier.of("wynnextras", "textures/general/buttondark/both.png");
    Identifier buttonleftHd = Identifier.of("wynnextras", "textures/general/buttondark/lefth.png");
    Identifier buttonrightHd = Identifier.of("wynnextras", "textures/general/buttondark/righth.png");

    Identifier sliderButtontl = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornertl.png");
    Identifier sliderButtontr = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornertr.png");
    Identifier sliderButtonbl = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornerbl.png");
    Identifier sliderButtonbr = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornerbr.png");
    Identifier sliderButtontop = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/top.png");
    Identifier sliderButtonbot = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/bot.png");
    Identifier sliderButtonleft = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/left.png");
    Identifier sliderButtonright = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/right.png");

    Identifier sliderButtontlDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornertld.png");
    Identifier sliderButtontrDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornertrd.png");
    Identifier sliderButtonblDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornerbld.png");
    Identifier sliderButtonbrDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornerbrd.png");
    Identifier sliderButtontopDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/topd.png");
    Identifier sliderButtonbotDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/botd.png");
    Identifier sliderButtonleftDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/leftd.png");
    Identifier sliderButtonrightDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/rightd.png");

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

    // --- Drawing helpers: Background / Text / Image ---
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
        drawButton(x, y, width, height, scale, hovered, false);
    }

    public void drawButton(float x, float y, float width, float height, int scale, boolean hovered, boolean darkMode) {
        if(width > scale * 2 || height > scale * 2) {
            RenderUtils.drawRect(
                    drawContext.getMatrices(),
                    darkMode ? CustomColor.fromHexString("2c2d2f") : CustomColor.fromHexString("82654C"),
                    sx(x + scale) - 1, sy(y + scale) - 1, 0,
                    sw(width - scale * 2) + 2, sh(height - scale * 2) + 2
            );
        }
        if(darkMode) {
            drawButtonTextures(x, y, width, height, scale, hovered, buttontlHd, buttontrHd, buttonblHd, buttonbrHd, buttontopHd, buttonbotHd, buttonleftHd, buttonrightHd, buttontld, buttontrd, buttonbld, buttonbrd, buttontopd, buttonbotd, buttonleftd, buttonrightd);
        } else {
            drawButtonTextures(x, y, width, height, scale, hovered, buttontlH, buttontrH, buttonblH, buttonbrH, buttontopH, buttonbotH, buttonleftH, buttonrightH, buttontl, buttontr, buttonbl, buttonbr, buttontop, buttonbot, buttonleft, buttonright);
        }
    }

    private void drawButtonTextures(float x, float y, float width, float height, int scale, boolean hovered, Identifier buttontlHd, Identifier buttontrHd, Identifier buttonblHd, Identifier buttonbrHd, Identifier buttontopHd, Identifier buttonbotHd, Identifier buttonleftHd, Identifier buttonrightHd, Identifier buttontld, Identifier buttontrd, Identifier buttonbld, Identifier buttonbrd, Identifier buttontopd, Identifier buttonbotd, Identifier buttonleftd, Identifier buttonrightd) {
        if (hovered) {
            drawImage(buttontlHd, x, y, scale, scale);
            drawImage(buttontrHd, x + width - scale, y, scale, scale);
            drawImage(buttonblHd, x, y + height - scale, scale, scale);
            drawImage(buttonbrHd, x + width - scale, y + height - scale, scale, scale);
            if (width > scale * 2) {
                drawImage(buttontopHd, x + scale - 2, y, width - scale * 2 + 4, scale);
                drawImage(buttonbotHd, x + scale - 2, y + height - scale, width - scale * 2 + 4, scale);
            }
            if (height > scale * 2) {
                drawImage(buttonleftHd, x, y + scale - 2, scale, height - scale * 2 + 4);
                drawImage(buttonrightHd, x + width - scale, y + scale - 2, scale, height - scale * 2 + 4);
            }
        } else {
            drawImage(buttontld, x, y, scale, scale);
            drawImage(buttontrd, x + width - scale, y, scale, scale);
            drawImage(buttonbld, x, y + height - scale, scale, scale);
            drawImage(buttonbrd, x + width - scale, y + height - scale, scale, scale);
            if (width > scale * 2) {
                drawImage(buttontopd, x + scale - 2, y, width - scale * 2 + 4, scale);
                drawImage(buttonbotd, x + scale - 2, y + height - scale, width - scale * 2 + 4, scale);
            }
            if (height > scale * 2) {
                drawImage(buttonleftd, x, y + scale - 2, scale, height - scale * 2 + 4);
                drawImage(buttonrightd, x + width - scale, y + scale - 2, scale, height - scale * 2 + 4);
            }
        }
    }

    public void drawSliderBackground(float x, float y, float width, float height, int scale, boolean darkMode) {
        if(width > scale * 2 || height > scale * 2) {
            RenderUtils.drawRect(
                    drawContext.getMatrices(),
                    darkMode ? CustomColor.fromHexString("1b1b1c") : CustomColor.fromHexString("50352d"),
                    sx(x + scale) - 1, sy(y + scale) - 1, 0,
                    sw(width - scale * 2) + 2, sh(height - scale * 2) + 2
            );
        }

        drawButtonTextures(x, y, width, height, scale, darkMode, sliderButtontlDark, sliderButtontrDark, sliderButtonblDark, sliderButtonbrDark, sliderButtontopDark, sliderButtonbotDark, sliderButtonleftDark, sliderButtonrightDark, sliderButtontl, sliderButtontr, sliderButtonbl, sliderButtonbr, sliderButtontop, sliderButtonbot, sliderButtonleft, sliderButtonright);
    }

    public void drawNineSlice(float x, float y, float width, float height, int scale, Identifier l, Identifier r, Identifier t, Identifier b, Identifier tl, Identifier tr, Identifier bl, Identifier br, CustomColor fillColor) {
        if(width > scale * 2 || height > scale * 2) {
            RenderUtils.drawRect(
                    drawContext.getMatrices(),
                    fillColor,
                    sx(x + scale) - 1, sy(y + scale) - 1, 0,
                    sw(width - scale * 2) + 2, sh(height - scale * 2) + 2
            );
        }

        drawImage(tl, x, y, scale, scale);
        drawImage(tr, x + width - scale, y, scale, scale);
        drawImage(bl, x, y + height - scale, scale, scale);
        drawImage(br, x + width - scale, y + height - scale, scale, scale);
        if (width > scale * 2) {
            drawImage(t, x + scale - 2, y, width - scale * 2 + 4, scale);
            drawImage(b, x + scale - 2, y + height - scale, width - scale * 2 + 4, scale);
        }
        if (height > scale * 2) {
            drawImage(l, x, y + scale - 2, scale, height - scale * 2 + 4);
            drawImage(r, x + width - scale, y + scale - 2, scale, height - scale * 2 + 4);
        }
    }
}


