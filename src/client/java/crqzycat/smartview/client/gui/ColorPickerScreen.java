package crqzycat.smartview.client.gui;

import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class ColorPickerScreen extends Screen {

    private final Screen parent;
    private final ModulePosition pos;
    private final String moduleName;
    private final Runnable onClose;

    // HSB sliders (0.0 - 1.0)
    private float hue;
    private float saturation;
    private float brightness;
    private int   alpha; // 0-255

    // Slider drag state
    private enum DragTarget { NONE, HUE, SAT, BRI, ALPHA }
    private DragTarget dragging = DragTarget.NONE;

    private TextFieldWidget hexField;
    private boolean updatingHex = false;

    private static final int SLIDER_W = 180;
    private static final int SLIDER_H = 12;
    private static final int SLIDER_X_OFF = 60; // label offset

    public ColorPickerScreen(Screen parent, ModulePosition pos, String moduleName, Runnable onClose) {
        super(Text.literal("Color: " + moduleName));
        this.parent = parent;
        this.pos = pos;
        this.moduleName = moduleName;
        this.onClose = onClose;

        // Init HSB from current textColor
        int color = pos.textColor;
        this.alpha = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >>  8) & 0xFF;
        int b = (color      ) & 0xFF;
        float[] hsb = rgbToHsb(r, g, b);
        this.hue        = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2 - 60;

        hexField = this.addDrawableChild(new TextFieldWidget(
            this.textRenderer,
            cx - 40, cy + 115, 90, 16,
            Text.literal("Hex")
        ));
        hexField.setMaxLength(8);
        hexField.setText(toHexString());
        hexField.setChangedListener(this::onHexChanged);

        this.addDrawableChild(
            ButtonWidget.builder(Text.translatable("gui.done"), btn -> close())
                .dimensions(cx - 40, cy + 140, 80, 20).build()
        );

        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Reset"), btn -> {
                pos.textColor = 0xFFFFFFFF;
                hue = 0; saturation = 0; brightness = 1; alpha = 255;
                syncHexField();
            }).dimensions(cx + 44, cy + 140, 50, 20).build()
        );
    }

    // ── hex sync ─────────────────────────────────────────────────────────────

    private void onHexChanged(String hex) {
        if (updatingHex) return;
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            if (clean.length() == 6) clean = "FF" + clean;
            if (clean.length() == 8) {
                int color = (int) Long.parseLong(clean, 16);
                alpha      = (color >> 24) & 0xFF;
                int r      = (color >> 16) & 0xFF;
                int g      = (color >>  8) & 0xFF;
                int b      = (color      ) & 0xFF;
                float[] hsb = rgbToHsb(r, g, b);
                hue        = hsb[0];
                saturation = hsb[1];
                brightness = hsb[2];
                applyColor();
            }
        } catch (NumberFormatException ignored) {}
    }

    private void syncHexField() {
        updatingHex = true;
        if (hexField != null) hexField.setText(toHexString());
        updatingHex = false;
    }

    private String toHexString() {
        return String.format("%08X", pos.textColor & 0xFFFFFFFFL);
    }

    // ── apply ─────────────────────────────────────────────────────────────────

    private void applyColor() {
        int rgb = hsbToRgb(hue, saturation, brightness);
        pos.textColor = (alpha << 24) | (rgb & 0x00FFFFFF);
        syncHexField();
    }

    // ── rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xCC000000);

        int cx = this.width / 2;
        int cy = this.height / 2 - 60;
        int bx = cx - SLIDER_W / 2;

        // Title
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, cy - 16, 0xFFFFFFFF);

        // Hue slider
        drawLabel(ctx, "Hue", bx - SLIDER_X_OFF, cy);
        drawHueSlider(ctx, bx, cy, SLIDER_W, SLIDER_H);
        drawThumb(ctx, bx + (int)(hue * SLIDER_W), cy, SLIDER_H);

        // Saturation
        drawLabel(ctx, "Sat", bx - SLIDER_X_OFF, cy + 22);
        drawSatSlider(ctx, bx, cy + 22, SLIDER_W, SLIDER_H);
        drawThumb(ctx, bx + (int)(saturation * SLIDER_W), cy + 22, SLIDER_H);

        // Brightness
        drawLabel(ctx, "Bri", bx - SLIDER_X_OFF, cy + 44);
        drawBriSlider(ctx, bx, cy + 44, SLIDER_W, SLIDER_H);
        drawThumb(ctx, bx + (int)(brightness * SLIDER_W), cy + 44, SLIDER_H);

        // Alpha
        drawLabel(ctx, "Alpha", bx - SLIDER_X_OFF, cy + 66);
        drawAlphaSlider(ctx, bx, cy + 66, SLIDER_W, SLIDER_H);
        drawThumb(ctx, bx + (int)(alpha / 255f * SLIDER_W), cy + 66, SLIDER_H);

        // Preview box
        ctx.fill(cx - 20, cy + 88, cx + 20, cy + 108, 0xFF333333);
        ctx.fill(cx - 18, cy + 90, cx + 18, cy + 106, pos.textColor);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Aa"), cx, cy + 94, pos.textColor);

        // Hex label
        ctx.drawTextWithShadow(this.textRenderer, "Hex:", cx - 50, cy + 119, 0xFFAAAAAA);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawLabel(DrawContext ctx, String label, int x, int y) {
        ctx.drawTextWithShadow(this.textRenderer, label, x + SLIDER_X_OFF - this.textRenderer.getWidth(label) - 4, y + 2, 0xFFCCCCCC);
    }

    private void drawHueSlider(DrawContext ctx, int x, int y, int w, int h) {
        for (int i = 0; i < w; i++) {
            float h2 = i / (float) w;
            ctx.fill(x + i, y, x + i + 1, y + h, 0xFF000000 | hsbToRgb(h2, 1f, 1f));
        }
    }

    private void drawSatSlider(DrawContext ctx, int x, int y, int w, int h) {
        for (int i = 0; i < w; i++) {
            float s = i / (float) w;
            ctx.fill(x + i, y, x + i + 1, y + h, 0xFF000000 | hsbToRgb(hue, s, brightness));
        }
    }

    private void drawBriSlider(DrawContext ctx, int x, int y, int w, int h) {
        for (int i = 0; i < w; i++) {
            float b = i / (float) w;
            ctx.fill(x + i, y, x + i + 1, y + h, 0xFF000000 | hsbToRgb(hue, saturation, b));
        }
    }

    private void drawAlphaSlider(DrawContext ctx, int x, int y, int w, int h) {
        // Checkerboard for transparency
        for (int i = 0; i < w; i += 4) {
            for (int j = 0; j < h; j += 4) {
                ctx.fill(x+i, y+j, x+i+4, y+j+4, ((i+j)/4 % 2 == 0) ? 0xFFAAAAAA : 0xFF555555);
            }
        }
        int rgb = hsbToRgb(hue, saturation, brightness);
        for (int i = 0; i < w; i++) {
            int a = (int)(i / (float) w * 255);
            ctx.fill(x + i, y, x + i + 1, y + h, (a << 24) | rgb);
        }
    }

    private void drawThumb(DrawContext ctx, int x, int y, int h) {
        x = Math.clamp(x, 0, this.width);
        ctx.fill(x - 1, y - 2, x + 1, y + h + 2, 0xFF000000);
        ctx.fill(x - 1, y - 1, x + 1, y + h + 1, 0xFFFFFFFF);
    }

    // ── mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        dragging = getSliderAt(click.x(), click.y());
        if (dragging != DragTarget.NONE) { updateSlider(click.x()); return true; }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double ox, double oy) {
        if (dragging != DragTarget.NONE) { updateSlider(click.x()); return true; }
        return super.mouseDragged(click, ox, oy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = DragTarget.NONE;
        return super.mouseReleased(click);
    }

    private DragTarget getSliderAt(double mx, double my) {
        int cx = this.width / 2;
        int cy = this.height / 2 - 60;
        int bx = cx - SLIDER_W / 2;
        if (mx < bx || mx > bx + SLIDER_W) return DragTarget.NONE;
        if (my >= cy      && my < cy      + SLIDER_H) return DragTarget.HUE;
        if (my >= cy + 22 && my < cy + 22 + SLIDER_H) return DragTarget.SAT;
        if (my >= cy + 44 && my < cy + 44 + SLIDER_H) return DragTarget.BRI;
        if (my >= cy + 66 && my < cy + 66 + SLIDER_H) return DragTarget.ALPHA;
        return DragTarget.NONE;
    }

    private void updateSlider(double mx) {
        int cx = this.width / 2;
        int bx = cx - SLIDER_W / 2;
        float t = (float) Math.clamp((mx - bx) / SLIDER_W, 0.0, 1.0);
        switch (dragging) {
            case HUE   -> hue        = t;
            case SAT   -> saturation = t;
            case BRI   -> brightness = t;
            case ALPHA -> alpha      = (int)(t * 255);
            default    -> {}
        }
        applyColor();
    }

    @Override
    public void close() {
        if (onClose != null) onClose.run();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override public boolean shouldPause() { return false; }

    // ── color math ────────────────────────────────────────────────────────────

    private static int hsbToRgb(float h, float s, float b) {
        if (s == 0) {
            int v = (int)(b * 255);
            return (v << 16) | (v << 8) | v;
        }
        float sector = h * 6f;
        int i = (int) sector;
        float f = sector - i;
        float p = b * (1 - s);
        float q = b * (1 - s * f);
        float t = b * (1 - s * (1 - f));
        float r, g, bv;
        switch (i % 6) {
            case 0 -> { r = b; g = t; bv = p; }
            case 1 -> { r = q; g = b; bv = p; }
            case 2 -> { r = p; g = b; bv = t; }
            case 3 -> { r = p; g = q; bv = b; }
            case 4 -> { r = t; g = p; bv = b; }
            default-> { r = b; g = p; bv = q; }
        }
        return ((int)(r*255) << 16) | ((int)(g*255) << 8) | (int)(bv*255);
    }

    private static float[] rgbToHsb(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;
        float h = 0, s = (max == 0) ? 0 : delta / max, bv = max;
        if (delta != 0) {
            if (max == rf)      h = (gf - bf) / delta % 6;
            else if (max == gf) h = (bf - rf) / delta + 2;
            else                h = (rf - gf) / delta + 4;
            h /= 6;
            if (h < 0) h += 1;
        }
        return new float[]{ h, s, bv };
    }
}
