package julianh06.wynnextras.utils.UI;

import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.mixin.Invoker.NativeImageInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;

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

    public static final Identifier sliderButtontl = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornertl.png");
    public static final Identifier sliderButtontr = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornertr.png");
    public static final Identifier sliderButtonbl = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornerbl.png");
    public static final Identifier sliderButtonbr = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornerbr.png");
    public static final Identifier sliderButtontop = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/top.png");
    public static final Identifier sliderButtonbot = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/bot.png");
    public static final Identifier sliderButtonleft = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/left.png");
    public static final Identifier sliderButtonright = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/right.png");

    public static final Identifier sliderButtontlDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornertld.png");
    public static final Identifier sliderButtontrDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornertrd.png");
    public static final Identifier sliderButtonblDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornerbld.png");
    public static final Identifier sliderButtonbrDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/cornerbrd.png");
    public static final Identifier sliderButtontopDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/topd.png");
    public static final Identifier sliderButtonbotDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/botd.png");
    public static final Identifier sliderButtonleftDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/leftd.png");
    public static final Identifier sliderButtonrightDark = Identifier.of("wynnextras", "textures/general/sliderbackgrounds/rightd.png");

    private DrawContext drawContext;
    private double scaleFactor;
    private int xStart;
    private int yStart;

    public UIUtils(DrawContext drawContext, double scaleFactor, int xStart, int yStart) {
        this.drawContext = drawContext;
        this.scaleFactor = scaleFactor;
        this.xStart = xStart;
        this.yStart = yStart;
        clearSeparatorCache();
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
        drawContext.fillGradient(
                0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height,
                0xC0101010,
                0xD0101010
        );
    }

    public void drawRect(float x, float y, float width, float height, CustomColor color) {
        RenderUtils.drawRect(
                drawContext,
                color,
                sx(x), sy(y),
                sw(width), sh(height)
        );
    }

    public void drawRect(float x, float y, float width, float heigt) {
        this.drawRect(x, y, width, heigt, CustomColor.fromHexString("FFFFFF"));
    }

    public void drawRectBorders(float x, float y, float width, float height, CustomColor color) {
        RenderUtils.drawRectBorders(
                drawContext,
                color,
                sx(x), sy(y),
                sw(width), sh(height), 1
        );
    }

    public void drawLine(float x1, float y1, float x2, float y2, float width, CustomColor color) {
        RenderUtils.drawLine(
                drawContext,
                color,
                sx(x1), sy(y1),
                sx(x2), sy(y2),
                sw(width)
        );
    }

    public void drawText(String text, float x, float y, CustomColor color, HorizontalAlignment hAlign, VerticalAlignment vAlign, TextShadow shadow, float textScale) {
        FontRenderer.getInstance().renderText(
                drawContext,
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

    public void drawText(Text text, float x, float y, CustomColor color, HorizontalAlignment hAlign, VerticalAlignment vAlign, TextShadow shadow, float textScale) {
        FontRenderer.getInstance().renderText(
                drawContext,
                StyledText.fromComponent(text),
                sx(x),
                sy(y),
                color,
                hAlign,
                vAlign,
                shadow,
                (float)(textScale / scaleFactor)
        );
    }

    public void drawText(Text text, float x, float y, CustomColor color, HorizontalAlignment hAlign, VerticalAlignment vAlign, float textScale) {
        drawText(text, x, y, color, hAlign, vAlign, TextShadow.NORMAL, textScale);
    }

    public void drawText(Text text, float x, float y, CustomColor color, float textScale) {
        drawText(text, x, y, color, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, textScale);
    }

    public void drawText(Text text, float x, float y, CustomColor color) {
        drawText(text, x, y, color, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 3f);
    }

    public void drawText(Text text, float x, float y) {
        drawText(text, x, y, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 3f);
    }

    public void drawCenteredText(String text, float x, float y, CustomColor color, float textScale) {
        drawText(text, x, y, color, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, textScale);
    }

    public void drawCenteredText(String text, float x, float y, float textScale) {
        drawText(text, x, y, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, textScale);
    }

    public void drawCenteredText(String text, float x, float y, CustomColor color) {
        drawText(text, x, y, color, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 3f);
    }

    public void drawCenteredText(String text, float x, float y) {
        drawText(text, x, y, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 3f);
    }

    public void drawCenteredText(Text text, float x, float y, CustomColor color, float textScale) {
        drawText(text, x, y, color, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, textScale);
    }

    public void drawCenteredText(Text text, float x, float y, float textScale) {
        drawText(text, x, y, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, textScale);
    }

    public void drawCenteredText(Text text, float x, float y, CustomColor color) {
        drawText(text, x, y, color, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 3f);
    }

    public void drawCenteredText(Text text, float x, float y) {
        drawText(text, x, y, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 3f);
    }

    public void drawImage(
            Identifier texture,
            float x, float y,
            float width, float height,
            float u, float v,
            float uWidth, float vHeight,
            int textureWidth, int textureHeight,
            float alpha, CustomColor color
    ) {
        RenderUtils.drawTexturedRect(
                drawContext,
                texture,
                color.withAlpha(alpha),
                sx(x), sy(y),
                sw(width), sh(height),
                u, v,
                uWidth, vHeight,
                textureWidth, textureHeight
        );
    }

    public void drawImage(
            Identifier texture,
            float x, float y,
            float width, float height,
            float u, float v,
            float uWidth, float vHeight,
            int textureWidth, int textureHeight,
            float alpha
    ) {
        drawImage(texture, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight, alpha, CustomColor.NONE);
    }

    public void drawImage(
            Identifier texture,
            float x, float y,
            float width, float height,
            float u, float v,
            float uWidth, float vHeight
    ) {
        drawImage(
                texture,
                x, y, width, height,
                u, v,
                uWidth, vHeight,
                (int) uWidth, (int) vHeight,
                1.0f
        );
    }

    public void drawImage(
            Identifier texture,
            float x, float y,
            float width, float height,
            float u, float v,
            float uWidth, float vHeight,
            float alpha
    ) {
        drawImage(
                texture,
                x, y, width, height,
                u, v,
                uWidth, vHeight,
                (int) uWidth, (int) vHeight,
                alpha
        );
    }

    public void drawImage(
            Identifier texture,
            float x, float y,
            float width, float height,
            float u, float v,
            float uWidth, float vHeight,
            CustomColor color
    ) {
        drawImage(
                texture,
                x, y, width, height,
                u, v,
                uWidth, vHeight,
                (int) width, (int) height,
                1, color
        );
    }

    public void drawImage(
            Identifier texture,
            float x, float y,
            float width, float height,
            float u, float v,
            float uWidth, float vHeight,
            int textureWidth, int textureHeight
    ) {
        drawImage(
                texture,
                x, y, width, height,
                u, v,
                uWidth, vHeight,
                textureWidth, textureHeight,
                1.0f
        );
    }

    public void drawImage(Identifier texture, float x, float y, float width, float height, CustomColor color) {
        drawImage(
                texture,
                x, y, width, height,
                0, 0,
                width, height,
                (int) width, (int) height,
                1, color
        );
    }

    public void drawImage(Identifier texture, float x, float y, float width, float height, float alpha) {
        drawImage(
                texture,
                x, y, width, height,
                0, 0,
                width, height,
                (int) width, (int) height,
                alpha
        );
    }

    public void drawImage(Identifier texture, float x, float y, float width, float height) {
        drawImage(texture, x, y, width, height, 1.0f);
    }

    public void drawButton(float x, float y, float width, float height, boolean hovered) {
        Identifier sprite = hovered
                ? Identifier.ofVanilla("widget/button_highlighted")
                : Identifier.ofVanilla("widget/button");
        drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, (int) sx(x), (int) sy(y), sw(width), sh(height));
    }

    public void drawButtonCustom(float x, float y, float width, float height, int scale, boolean hovered, boolean darkMode) {
        if(width > scale * 2 || height > scale * 2) {
            RenderUtils.drawRect(
                    drawContext,
                    darkMode ? CustomColor.fromHexString("2c2d2f") : CustomColor.fromHexString("82654C"),
                    sx(x + scale) - 1, sy(y + scale) - 1,
                    sw(width - scale * 2) + 2, sh(height - scale * 2) + 2
            );
        }
        if(darkMode) {
            drawButtonTextures(x, y, width, height, scale, hovered, buttontlHd, buttontrHd, buttonblHd, buttonbrHd, buttontopHd, buttonbotHd, buttonleftHd, buttonrightHd, buttontld, buttontrd, buttonbld, buttonbrd, buttontopd, buttonbotd, buttonleftd, buttonrightd, 1);
        } else {
            drawButtonTextures(x, y, width, height, scale, hovered, buttontlH, buttontrH, buttonblH, buttonbrH, buttontopH, buttonbotH, buttonleftH, buttonrightH, buttontl, buttontr, buttonbl, buttonbr, buttontop, buttonbot, buttonleft, buttonright, 1);
        }
    }

    public void drawButtonFade(
            float x, float y, float width, float height,
            int scale, boolean hovered
    ) {
        float a = PVScreen.DarkModeToggleWidget.fade;

        if (width > scale * 2 || height > scale * 2) {

            // LIGHT base
            RenderUtils.drawRect(
                    drawContext,
                    CustomColor.fromHexString("82654C").withAlpha(1f - a),
                    sx(x + scale) - 1, sy(y + scale) - 1,
                    sw(width - scale * 2) + 2, sh(height - scale * 2) + 2
            );

            // DARK overlay
            RenderUtils.drawRect(
                    drawContext,
                    CustomColor.fromHexString("2c2d2f").withAlpha(a),
                    sx(x + scale) - 1, sy(y + scale) - 1,
                    sw(width - scale * 2) + 2, sh(height - scale * 2) + 2
            );
        }

        drawButtonTexturesFaded(x, y, width, height, scale, hovered, a);
    }


    public void drawButtonTextures(
            float x, float y, float width, float height, int scale,
            boolean hovered,
            Identifier buttontlH, Identifier buttontrH, Identifier buttonblH, Identifier buttonbrH,
            Identifier buttontopH, Identifier buttonbotH, Identifier buttonleftH, Identifier buttonrightH,
            Identifier buttontl, Identifier buttontr, Identifier buttonbl, Identifier buttonbr,
            Identifier buttontop, Identifier buttonbot, Identifier buttonleft, Identifier buttonright,
            float alpha
    ) {
        if (alpha <= 0.001f) return;

        if (hovered) {
            if (width > scale * 2) {
                drawImage(buttontopH, x + scale - 2, y, width - scale * 2 + 4, scale, alpha);
                drawImage(buttonbotH, x + scale - 2, y + height - scale, width - scale * 2 + 4, scale, alpha);
            }
            if (height > scale * 2) {
                drawImage(buttonleftH, x, y + scale - 2, scale, height - scale * 2 + 4, alpha);
                drawImage(buttonrightH, x + width - scale, y + scale - 2, scale, height - scale * 2 + 4, alpha);
            }
            drawImage(buttontlH, x, y, scale, scale, alpha);
            drawImage(buttontrH, x + width - scale, y, scale, scale, alpha);
            drawImage(buttonblH, x, y + height - scale, scale, scale, alpha);
            drawImage(buttonbrH, x + width - scale, y + height - scale, scale, scale, alpha);
        } else {
            if (width > scale * 2) {
                drawImage(buttontop, x + scale - 2, y, width - scale * 2 + 4, scale, alpha);
                drawImage(buttonbot, x + scale - 2, y + height - scale - 1, width - scale * 2 + 4, scale + 1, alpha);
            }
            if (height > scale * 2) {
                drawImage(buttonleft, x, y + scale - 2, scale, height - scale * 2 + 4, alpha);
                drawImage(buttonright, x + width - scale, y + scale - 2, scale, height - scale * 2 + 4, alpha);
            }
            drawImage(buttontl, x, y, scale, scale, alpha);
            drawImage(buttontr, x + width - scale, y, scale, scale, alpha);
            drawImage(buttonbl, x, y + height - scale - 1, scale, scale + 1, alpha);
            drawImage(buttonbr, x + width - scale, y + height - scale - 1, scale, scale + 1, alpha);
        }
    }

    public void drawButtonTexturesFaded(
            float x, float y, float width, float height, int scale,
            boolean hovered, float alpha
    ) {
        drawButtonTextures(
                x, y, width, height, scale, hovered,
                buttontlH, buttontrH, buttonblH, buttonbrH,
                buttontopH, buttonbotH, buttonleftH, buttonrightH,
                buttontl, buttontr, buttonbl, buttonbr,
                buttontop, buttonbot, buttonleft, buttonright,
                1f - alpha
        );

        drawButtonTextures(
                x, y, width, height, scale, hovered,
                buttontlHd, buttontrHd, buttonblHd, buttonbrHd,
                buttontopHd, buttonbotHd, buttonleftHd, buttonrightHd,
                buttontld, buttontrd, buttonbld, buttonbrd,
                buttontopd, buttonbotd, buttonleftd, buttonrightd,
                alpha
        );
    }


    public void drawSliderBackground(float x, float y, float width, float height) {
        drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, Identifier.ofVanilla("widget/slider"), (int) sx(x), (int) sy(y), sw(width), sh(height));
    }

    public void drawSliderFade(float x, float y, float width, float height, int scale) {
        float fade = PVScreen.DarkModeToggleWidget.fade;
        if (width > scale * 2 || height > scale * 2) {
            RenderUtils.drawRect(drawContext,
                    CustomColor.fromHexString("50352d").withAlpha(1f - fade),
                    sx(x + scale) - 1, sy(y + scale) - 1,
                    sw(width - scale * 2) + 2, sh(height - scale * 2) + 2);
            RenderUtils.drawRect(drawContext,
                    CustomColor.fromHexString("1b1b1c").withAlpha(fade),
                    sx(x + scale) - 1, sy(y + scale) - 1,
                    sw(width - scale * 2) + 2, sh(height - scale * 2) + 2);
        }
        drawButtonTextures(x, y, width, height, scale, false,
                sliderButtontl, sliderButtontr, sliderButtonbl, sliderButtonbr,
                sliderButtontop, sliderButtonbot, sliderButtonleft, sliderButtonright,
                sliderButtontl, sliderButtontr, sliderButtonbl, sliderButtonbr,
                sliderButtontop, sliderButtonbot, sliderButtonleft, sliderButtonright, 1f - fade);
        drawButtonTextures(x, y, width, height, scale, false,
                sliderButtontlDark, sliderButtontrDark, sliderButtonblDark, sliderButtonbrDark,
                sliderButtontopDark, sliderButtonbotDark, sliderButtonleftDark, sliderButtonrightDark,
                sliderButtontlDark, sliderButtontrDark, sliderButtonblDark, sliderButtonbrDark,
                sliderButtontopDark, sliderButtonbotDark, sliderButtonleftDark, sliderButtonrightDark, fade);
    }

    private static final Identifier GENERIC_CONTAINER_TEX = Identifier.ofVanilla("textures/gui/container/generic_54.png");
    private static final int GENERIC_W  = 256;
    private static final int GENERIC_H  = 256;

    public void drawVanillaPanel(float x, float y, float width, float height, int scale, int leftOffset, int rightOffset, int topOffset, int botOffset) {
        int tw = GENERIC_W, th = GENERIC_H;

        int uvWH = 4;

        /*
         * OUTER
         * */
        // corners
        drawImage(GENERIC_CONTAINER_TEX, x, y, scale, scale, 0, 0, uvWH, uvWH, tw, th); // TL
        drawImage(GENERIC_CONTAINER_TEX, x + width - scale, y, scale, scale, 172, 0, uvWH, uvWH, tw, th); // TR
        drawImage(GENERIC_CONTAINER_TEX, x, y + height - scale, scale, scale, 0, 218, uvWH, uvWH, tw, th); // BL
        drawImage(GENERIC_CONTAINER_TEX, x + width - scale, y + height - scale, scale, scale, 172, 218, uvWH, uvWH, tw, th); // BR

        // fill
        drawImage(GENERIC_CONTAINER_TEX, x + scale - 1, y + scale - 1, width - 2 * scale + 2, height - 2 * scale + 2, 4, 4, 1, 1, tw, th);

        // top/bot edges
        drawImage(GENERIC_CONTAINER_TEX, x + scale - 2, y, width- 2 * scale + 4, scale, 4, 0, 1, uvWH, tw, th);
        drawImage(GENERIC_CONTAINER_TEX, x + scale - 2, y + height - scale, width - 2 * scale + 4, scale, 4, 218,  1, uvWH, tw, th);

        // left/right edges
        drawImage(GENERIC_CONTAINER_TEX, x, y + scale - 2, scale, height - 2 * scale + 4, 0, 4, uvWH, 1, tw, th);
        drawImage(GENERIC_CONTAINER_TEX, x + width - scale, y + scale - 2, scale, height - 2 * scale + 4, 172, 4, uvWH, 1, tw, th);
        
        /*
        * INNER
        * */
        // corners
        drawImage(GENERIC_CONTAINER_TEX, x + leftOffset, y + topOffset, scale, scale, 7, 15, uvWH, uvWH, tw, th); // TL
        drawImage(GENERIC_CONTAINER_TEX, x + width - scale - rightOffset, y + topOffset, scale, scale, 165, 15, uvWH, uvWH, tw, th); // TR
        drawImage(GENERIC_CONTAINER_TEX, x + leftOffset, y + height - scale - botOffset, scale, scale, 7, 124, uvWH, uvWH, tw, th); // BL
        drawImage(GENERIC_CONTAINER_TEX, x + width - scale - rightOffset, y + height - scale - botOffset, scale, scale, 165, 124, uvWH, uvWH, tw, th); // BR

        // fill
        drawImage(GENERIC_CONTAINER_TEX, x + scale + leftOffset, y + scale + topOffset, width - leftOffset - rightOffset - scale * 2, height - topOffset - botOffset - scale * 2, 16, 20, 1, 1, tw, th);

        // top/bot edges
        drawImage(GENERIC_CONTAINER_TEX, x + scale + leftOffset, y + topOffset, width - leftOffset - rightOffset - scale * 2, scale, 8, 16, 1, uvWH, tw, th);
        drawImage(GENERIC_CONTAINER_TEX, x + scale + leftOffset, y + height - scale - botOffset, width - leftOffset - rightOffset - scale * 2, scale, 8, 123,  1, uvWH, tw, th);

        // left/right edges
        drawImage(GENERIC_CONTAINER_TEX, x + leftOffset, y + scale + topOffset, scale, height - topOffset - botOffset - scale * 2, 7, 21, uvWH, 1, tw, th);
        drawImage(GENERIC_CONTAINER_TEX, x + width - scale - rightOffset, y + scale + topOffset, scale, height - topOffset - botOffset - scale * 2, 165, 120, uvWH, 1, tw, th);
    }

    public void drawNineSlice(float x, float y, float width, float height, int scale, Identifier l, Identifier r, Identifier t, Identifier b, Identifier tl, Identifier tr, Identifier bl, Identifier br, CustomColor fillColor) {
        if(width > scale * 2 || height > scale * 2) {
            RenderUtils.drawRect(
                    drawContext,
                    fillColor,
                    sx(x + scale) - 1, sy(y + scale) - 1,
                    sw(width - scale * 2) + 2, sh(height - scale * 2) + 2
            );
        }

        if (width > scale * 2) {
            if(t != null) drawImage(t, x + scale - 2, y, width - scale * 2 + 4, scale);
            if(b != null) drawImage(b, x + scale - 2, y + height - scale, width - scale * 2 + 4, scale);
        }
        if (height > scale * 2) {
            if(l != null) drawImage(l, x, y + scale - 2, scale, height - scale * 2 + 4);
            if(r != null) drawImage(r, x + width - scale, y + scale - 2, scale, height - scale * 2 + 4);
        }
        if(tl != null) drawImage(tl, x, y, scale, scale);
        if(tr != null) drawImage(tr, x + width - scale, y, scale, scale);
        if(bl != null) drawImage(bl, x, y + height - scale, scale, scale);
        if(br != null) drawImage(br, x + width - scale, y + height - scale, scale, scale);
    }

    public void drawProgressBar(float x, float y, float width, float height, float textScale, float progress, Identifier progressTexture, DrawContext context) {
        drawProgressBar(x, y, width, height, textScale, progress, progressTexture, context, false);
    }

    public void drawProgressBar(float x, float y, float width, float height, float textScale, float progress, Identifier progressTexture, DrawContext context, boolean chroma) {
        drawRect(x, y, width, height, getVanillaPanelBgColor());

        context.enableScissor((int) sx(x), (int) sy(y), (int) sx(x + width * (progress)), (int) sy(y + height));
        if(chroma) {
            RenderUtils.drawTexturedRect(
                    drawContext,
                    progressTexture,
                    getRainbowColor(7f, 0),
                    sx(x), sy(y),
                    sw(width), sh(height),
                    0, 0,
                    sw(width), sh(height),
                    sw(width), sh(height)
            );
        } else drawImage(progressTexture, x, y, width, height);
        context.disableScissor();

        drawRectBorders(x, y, width, height, getVanillaPanelBorderColor());
        drawCenteredText(String.format("%.2f%%", progress * 100), x + width / 2f, y + height / 2f + 2, CustomColor.fromHexString("FFFFFF"), textScale);
    }

    public void drawProgressBar(float x, float y, float width, float height, float textScale, float progress, Identifier border, Identifier background, Identifier progressTexture, DrawContext context) {
        drawProgressBar(x, y, width, height, textScale, progress, border, background, progressTexture, context, false);
    }

    public void drawProgressBar(float x, float y, float width, float height, float textScale, float progress, Identifier border, Identifier background, Identifier progressTexture, DrawContext context, boolean chroma) {
        drawImage(background, x, y, width, height);

        context.enableScissor((int) sx(x), (int) sy(y), (int) sx(x + width * (progress)), (int) sy(y + height));
        if(chroma) {
            RenderUtils.drawTexturedRect(
                    drawContext,
                    progressTexture,
                    getRainbowColor(7f, 0),
                    sx(x), sy(y),
                    sw(width), sh(height),
                    0, 0,
                    sw(width), sh(height),
                    sw(width), sh(height)
            );
        } else drawImage(progressTexture, x, y, width, height);
        context.disableScissor();

        drawImage(border, x, y, width, height);
        drawCenteredText(String.format("%.2f%%", progress * 100), x + width / 2f, y + height / 2f + 2, CustomColor.fromHexString("FFFFFF"), textScale);
    }

    public static CustomColor getRainbowColor(float speed, float offset) {
        float hue = (System.currentTimeMillis() % (int)(speed * 1000)) / (speed * 1000f);
        hue += offset;
        hue %= 1.0f;

        int rgb = Color.HSBtoRGB(hue, 1.0f, 0.75f);
        return CustomColor.fromInt(rgb & 0xFFFFFF);
    }

    private static final Identifier BUTTON_TEX = Identifier.ofVanilla("textures/gui/sprites/widget/button.png");
    private static final Identifier BUTTON_HIGHLIGHTED_TEX = Identifier.ofVanilla("textures/gui/sprites/widget/button_highlighted.png");

    private static CustomColor cachedSepNormal = null;
    private static CustomColor cachedSepHovered = null;
    private static CustomColor cachedSepNormalDark = null;
    private static CustomColor cachedSepHoveredDark = null;
    private static CustomColor cachedPanelBg = null;
    private static CustomColor cachedPanelBorder = null;

    public static void clearSeparatorCache() {
        cachedSepNormal = null;
        cachedSepHovered = null;
        cachedSepNormalDark = null;
        cachedSepHoveredDark = null;
        cachedPanelBg = null;
        cachedPanelBorder = null;
    }

    /**
     * Returns the separator line color derived from the resource pack's button sprites
     * so it harmonises with drawButton on any texture pack.
     * Normal: center of widget/button.png darkened slightly.
     * Hovered: center of widget/button_highlighted.png darkened slightly.
     */
    public static CustomColor getVanillaSeparatorColor(boolean hovered) {
        if (cachedSepNormal == null || cachedSepHovered == null) {
            int[] normal = sampleSpriteCenter(BUTTON_TEX);
            int[] highlighted = sampleSpriteCenter(BUTTON_HIGHLIGHTED_TEX);
            if (normal == null) normal = new int[]{166, 138, 115};
            if (highlighted == null) highlighted = normal;
            cachedSepNormal = toColor(normal, 1f);
            cachedSepHovered = toColor(highlighted, 1f);
        }
        return hovered ? cachedSepHovered : cachedSepNormal;
    }

    public static CustomColor getVanillaDarkSeparatorColor(boolean hovered) {
        if (cachedSepNormalDark == null || cachedSepHoveredDark == null) {
            int[] normal = sampleSpriteNotCenterButAtThePositionForTheDarkerColor(BUTTON_TEX);
            int[] highlighted = sampleSpriteNotCenterButAtThePositionForTheDarkerColor(BUTTON_HIGHLIGHTED_TEX);
            if (normal == null) normal = new int[]{166, 138, 115};
            if (highlighted == null) highlighted = normal;
            cachedSepNormalDark = toColor(normal, 1f);
            cachedSepHoveredDark = toColor(highlighted, 1f);
        }
        return hovered ? cachedSepHoveredDark : cachedSepNormalDark;
    }

    private static CustomColor toColor(int[] rgb, float factor) {
        return CustomColor.fromHexString(String.format("%02X%02X%02X",
                Math.min(255, (int)(rgb[0] * factor)),
                Math.min(255, (int)(rgb[1] * factor)),
                Math.min(255, (int)(rgb[2] * factor))));
    }

    // Reads the second-outermost pixel from the left edge of a GUI sprite using NativeImage.
    // Returns [R, G, B] (0-255) or null on failure.
    private static int[] sampleSpriteCenter(Identifier id) {
        try {
            var res = MinecraftClient.getInstance().getResourceManager().getResource(id);
            if (res.isEmpty()) return null;
            try (var is = res.get().getInputStream();
                 NativeImage img = NativeImage.read(is)) {
                // NativeImage.getColor is private; accessed via @Invoker mixin.
                // Pixel format is ABGR (little-endian RGBA): lowest byte = R.
                int c = ((NativeImageInvoker) (Object) img).invokeGetColor(1, img.getHeight() / 2);
                return new int[]{c & 0xFF, (c >> 8) & 0xFF, (c >> 16) & 0xFF};
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static int[] sampleSpriteNotCenterButAtThePositionForTheDarkerColor(Identifier id) {
        try {
            var res = MinecraftClient.getInstance().getResourceManager().getResource(id);
            if (res.isEmpty()) return null;
            try (var is = res.get().getInputStream();
                 NativeImage img = NativeImage.read(is)) {
                // NativeImage.getColor is private; accessed via @Invoker mixin.
                // Pixel format is ABGR (little-endian RGBA): lowest byte = R.
                int c = ((NativeImageInvoker) (Object) img).invokeGetColor(2, 2);
                return new int[]{c & 0xFF, (c >> 8) & 0xFF, (c >> 16) & 0xFF};
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static int[] sampleSpriteAt(Identifier id, int u, int v) {
        try {
            var res = MinecraftClient.getInstance().getResourceManager().getResource(id);
            if (res.isEmpty()) return null;
            try (var is = res.get().getInputStream();
                 NativeImage img = NativeImage.read(is)) {
                int c = ((NativeImageInvoker) (Object) img).invokeGetColor(u, v);
                return new int[]{c & 0xFF, (c >> 8) & 0xFF, (c >> 16) & 0xFF};
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static CustomColor getVanillaPanelBgColor() {
        if (cachedPanelBg == null) {
            int[] px = sampleSpriteAt(GENERIC_CONTAINER_TEX, 16, 20);
            if (px == null) px = new int[]{198, 198, 198};
            cachedPanelBg = toColor(px, 1f);
            cachedPanelBorder = toColor(px, 0.6f);
        }
        return cachedPanelBg;
    }

    public static CustomColor getVanillaPanelBorderColor() {
        getVanillaPanelBgColor();
        return cachedPanelBorder;
    }

    public static boolean isVanillaPanelDark() {
        int[] px = sampleSpriteAt(GENERIC_CONTAINER_TEX, 16, 20);
        if (px == null) return false;
        float lum = 0.299f * px[0] + 0.587f * px[1] + 0.114f * px[2];
        return lum < 100f;
    }

}