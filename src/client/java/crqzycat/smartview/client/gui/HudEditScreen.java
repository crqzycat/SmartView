package crqzycat.smartview.client.gui;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModuleManager;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

/**
 * Edit mode overlay.
 *   Drag module body  → reposition
 *   Drag corner handle → resize (scale)
 *   Shift + Scroll     → background opacity
 *   Checkbox panel     → toggle on/off
 */
public class HudEditScreen extends Screen {

    private static final int PANEL_WIDTH  = 160;
    private static final int HANDLE_SIZE  = 8;   // corner handle hitbox in px

    private static final int OUTLINE_NORMAL   = 0x80FFFFFF;
    private static final int OUTLINE_HOVER    = 0xFFFFFF00;
    private static final int OUTLINE_DRAGGING = 0xFF00FF66;
    private static final int OUTLINE_RESIZE   = 0xFF00CCFF;
    private static final int HANDLE_COLOR     = 0xFFFFFFFF;
    private static final int FILL_EDIT        = 0x33FFFFFF;

    // Drag state
    private HudModule dragging;
    private int dragOffsetX, dragOffsetY;

    // Resize state
    private HudModule resizing;
    private double resizeStartMouseX, resizeStartMouseY;
    private float  resizeStartScale;
    private int    resizeStartBaseDim; // larger of base w/h, used to normalize delta

    public HudEditScreen() {
        super(Text.literal("SmartView"));
    }

    @Override
    protected void init() {
        this.addDrawableChild(
            ButtonWidget.builder(Text.translatable("gui.done"), btn -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 28, 100, 20)
                .build()
        );
        int listY = 30;
        for (HudModule module : ModuleManager.getModules()) {
            ModulePosition pos = ModuleManager.getPosition(module.getId());
            this.addDrawableChild(
                CheckboxWidget.builder(Text.literal(module.getDisplayName()), this.textRenderer)
                    .pos(this.width - PANEL_WIDTH + 4, listY)
                    .checked(pos.enabled)
                    .callback((cb, checked) -> pos.enabled = checked)
                    .build()
            );
            listY += 22;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // No renderBackground() – avoid double-blur crash in 1.21.11.

        context.fill(this.width - PANEL_WIDTH, 20, this.width, this.height - 34, 0x88000000);
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.translatable("smartview.gui.edit_title"),
            this.width - PANEL_WIDTH / 2, 8, 0xFFFFFFFF);

        MinecraftClient client = MinecraftClient.getInstance();

        for (HudModule module : ModuleManager.getModules()) {
            ModulePosition pos = ModuleManager.getPosition(module.getId());
            if (!pos.enabled) continue;

            float scale = Math.max(0.25f, pos.scale);
            int sw = ModuleManager.scaledWidth(module, pos, client);
            int sh = ModuleManager.scaledHeight(module, pos);

            boolean hovered = mouseX >= pos.x && mouseX < pos.x + sw
                           && mouseY >= pos.y && mouseY < pos.y + sh;

            // Draw module content scaled
            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(pos.x, pos.y);
            matrices.scale(scale, scale);
            context.fill(0, 0, module.getBaseWidth(client), module.getBaseHeight(), FILL_EDIT);
            module.render(context, client, 0, 0, pos);
            matrices.popMatrix();

            // Outline
            int outlineColor = (module == resizing) ? OUTLINE_RESIZE
                             : (module == dragging)  ? OUTLINE_DRAGGING
                             : (hovered)             ? OUTLINE_HOVER
                             :                         OUTLINE_NORMAL;
            drawOutline(context, pos.x, pos.y, sw, sh, outlineColor);

            // Corner resize handles – small white squares at all 4 corners
            drawHandle(context, pos.x,              pos.y);               // top-left
            drawHandle(context, pos.x + sw - HANDLE_SIZE, pos.y);         // top-right
            drawHandle(context, pos.x,              pos.y + sh - HANDLE_SIZE); // bottom-left
            drawHandle(context, pos.x + sw - HANDLE_SIZE, pos.y + sh - HANDLE_SIZE); // bottom-right

            // Hint when hovered and idle
            if (hovered && module != dragging && module != resizing) {
                context.drawTextWithShadow(this.textRenderer,
                    Text.translatable("smartview.gui.hint"),
                    pos.x, pos.y + sh + 2, 0xFFAAAAAA);
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        MinecraftClient client = MinecraftClient.getInstance();
        long window = client.getWindow().getHandle();
        boolean shiftHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT)  == GLFW.GLFW_PRESS
                         || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (!shiftHeld) return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);

        for (HudModule module : ModuleManager.getModules()) {
            ModulePosition pos = ModuleManager.getPosition(module.getId());
            if (!pos.enabled) continue;
            int sw = ModuleManager.scaledWidth(module, pos, client);
            int sh = ModuleManager.scaledHeight(module, pos);
            if (mouseX >= pos.x && mouseX < pos.x + sw && mouseY >= pos.y && mouseY < pos.y + sh) {
                pos.backgroundAlpha = Math.clamp(pos.backgroundAlpha + (int)(vertical * 16), 0, 255);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        MinecraftClient client = MinecraftClient.getInstance();
        for (HudModule module : ModuleManager.getModules()) {
            ModulePosition pos = ModuleManager.getPosition(module.getId());
            if (!pos.enabled) continue;
            int sw = ModuleManager.scaledWidth(module, pos, client);
            int sh = ModuleManager.scaledHeight(module, pos);

            // Check corners first (priority over body drag)
            if (isOnCorner(click.x(), click.y(), pos.x, pos.y, sw, sh)) {
                resizing = module;
                resizeStartMouseX = click.x();
                resizeStartMouseY = click.y();
                resizeStartScale  = pos.scale;
                resizeStartBaseDim = Math.max(module.getBaseWidth(client), module.getBaseHeight());
                return true;
            }

            // Then body drag
            if (click.x() >= pos.x && click.x() < pos.x + sw
             && click.y() >= pos.y && click.y() < pos.y + sh) {
                dragging = module;
                dragOffsetX = (int) click.x() - pos.x;
                dragOffsetY = (int) click.y() - pos.y;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (resizing != null) {
            ModulePosition pos = ModuleManager.getPosition(resizing.getId());
            // Average of horizontal + vertical delta, normalised to the module's base size
            double delta = ((click.x() - resizeStartMouseX) + (click.y() - resizeStartMouseY)) / 2.0;
            float newScale = (float)(resizeStartScale + delta / resizeStartBaseDim);
            pos.scale = Math.clamp(newScale, 0.25f, 4.0f);
            return true;
        }

        if (dragging != null) {
            ModulePosition pos = ModuleManager.getPosition(dragging.getId());
            pos.x = Math.clamp((int) click.x() - dragOffsetX, 0,
                    this.width  - ModuleManager.scaledWidth(dragging, pos, client));
            pos.y = Math.clamp((int) click.y() - dragOffsetY, 0,
                    this.height - ModuleManager.scaledHeight(dragging, pos));
            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            resizing = null;
            dragging = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        ModuleManager.save();
        MinecraftClient.getInstance().setScreen(null);
    }

    @Override public boolean shouldPause() { return false; }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Returns true if (mx,my) is within any of the four corner handle regions. */
    private static boolean isOnCorner(double mx, double my, int x, int y, int w, int h) {
        return (inHandle(mx, my, x,         y))           // top-left
            || (inHandle(mx, my, x + w - HANDLE_SIZE, y)) // top-right
            || (inHandle(mx, my, x,         y + h - HANDLE_SIZE)) // bottom-left
            || (inHandle(mx, my, x + w - HANDLE_SIZE, y + h - HANDLE_SIZE)); // bottom-right
    }

    private static boolean inHandle(double mx, double my, int hx, int hy) {
        return mx >= hx && mx < hx + HANDLE_SIZE && my >= hy && my < hy + HANDLE_SIZE;
    }

    private static void drawHandle(DrawContext ctx, int x, int y) {
        ctx.fill(x, y, x + HANDLE_SIZE, y + HANDLE_SIZE, HANDLE_COLOR);
        ctx.fill(x + 1, y + 1, x + HANDLE_SIZE - 1, y + HANDLE_SIZE - 1, 0xFF333333);
    }

    private static void drawOutline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color);
        ctx.fill(x,         y,         x + 1,     y + h,     color);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color);
    }
}
