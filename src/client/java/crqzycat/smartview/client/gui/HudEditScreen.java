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
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

/**
 * Edit mode overlay. Controls per hovered module:
 *   Scroll         → resize (scale)
 *   Shift + Scroll → background opacity
 *   Drag           → reposition
 *   Checkbox panel → toggle on/off
 */
public class HudEditScreen extends Screen {

    private static final int PANEL_WIDTH = 160;
    private static final int OUTLINE_NORMAL   = 0x80FFFFFF;
    private static final int OUTLINE_HOVER    = 0xFFFFFF00;
    private static final int OUTLINE_DRAGGING = 0xFF00FF66;
    private static final int FILL_EDIT        = 0x33FFFFFF;

    private HudModule dragging;
    private int dragOffsetX;
    private int dragOffsetY;

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
        // No renderBackground() – would double-blur in 1.21.11 and we want to see the world.

        // Panel background
        context.fill(this.width - PANEL_WIDTH, 20, this.width, this.height - 34, 0x88000000);

        // Panel title
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.translatable("smartview.gui.edit_title"),
            this.width - PANEL_WIDTH / 2, 8, 0xFFFFFFFF);

        // Draw each enabled module with its scale applied
        MinecraftClient client = MinecraftClient.getInstance();
        for (HudModule module : ModuleManager.getModules()) {
            ModulePosition pos = ModuleManager.getPosition(module.getId());
            if (!pos.enabled) continue;

            float scale = Math.max(0.25f, pos.scale);
            int sw = ModuleManager.scaledWidth(module, pos, client);
            int sh = ModuleManager.scaledHeight(module, pos);

            boolean hovered = mouseX >= pos.x && mouseX < pos.x + sw
                           && mouseY >= pos.y && mouseY < pos.y + sh;

            // Draw the module content scaled
            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(pos.x, pos.y);
            matrices.scale(scale, scale);
            context.fill(0, 0, module.getBaseWidth(client), module.getBaseHeight(), FILL_EDIT);
            module.render(context, client, 0, 0, pos);
            matrices.popMatrix();

            // Outline in screen space (no matrix)
            int outlineColor = module == dragging ? OUTLINE_DRAGGING : (hovered ? OUTLINE_HOVER : OUTLINE_NORMAL);
            drawOutline(context, pos.x, pos.y, sw, sh, outlineColor);

            // Hint text when hovered
            if (hovered && module != dragging) {
                context.drawTextWithShadow(this.textRenderer,
                    "Scroll: Groesse  |  Shift+Scroll: Transparenz",
                    pos.x, pos.y + sh + 2, 0xFFAAAAAA);
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        MinecraftClient client = MinecraftClient.getInstance();
        long window = client.getWindow().getHandle();
        boolean shiftHeld = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                          || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS);

        for (HudModule module : ModuleManager.getModules()) {
            ModulePosition pos = ModuleManager.getPosition(module.getId());
            if (!pos.enabled) continue;
            int sw = ModuleManager.scaledWidth(module, pos, client);
            int sh = ModuleManager.scaledHeight(module, pos);
            if (mouseX >= pos.x && mouseX < pos.x + sw && mouseY >= pos.y && mouseY < pos.y + sh) {
                if (shiftHeld) {
                    pos.backgroundAlpha = Math.clamp(pos.backgroundAlpha + (int)(vertical * 16), 0, 255);
                } else {
                    pos.scale = Math.clamp(pos.scale + (float)(vertical * 0.1), 0.25f, 4.0f);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            for (HudModule module : ModuleManager.getModules()) {
                ModulePosition pos = ModuleManager.getPosition(module.getId());
                if (!pos.enabled) continue;
                int sw = ModuleManager.scaledWidth(module, pos, client);
                int sh = ModuleManager.scaledHeight(module, pos);
                if (click.x() >= pos.x && click.x() < pos.x + sw
                 && click.y() >= pos.y && click.y() < pos.y + sh) {
                    dragging = module;
                    dragOffsetX = (int) click.x() - pos.x;
                    dragOffsetY = (int) click.y() - pos.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging != null) {
            ModulePosition pos = ModuleManager.getPosition(dragging.getId());
            pos.x = Math.max(0, Math.min((int) click.x() - dragOffsetX, this.width  - ModuleManager.scaledWidth(dragging, pos, MinecraftClient.getInstance())));
            pos.y = Math.max(0, Math.min((int) click.y() - dragOffsetY, this.height - ModuleManager.scaledHeight(dragging, pos)));
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0 && dragging != null) { dragging = null; return true; }
        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        ModuleManager.save();
        MinecraftClient.getInstance().setScreen(null);
    }

    @Override public boolean shouldPause() { return false; }

    private static void drawOutline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color);
        ctx.fill(x,         y,         x + 1,     y + h,     color);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color);
    }
}
