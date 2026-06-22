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

/**
 * Edit mode: every enabled module is drawn with an outline that can be dragged
 * to a new position, and the right-hand panel lets you toggle modules on/off.
 * Positions/toggles are written straight into ModuleManager's live config and
 * persisted on close.
 */
public class HudEditScreen extends Screen {

    private static final int PANEL_WIDTH = 150;
    private static final int OUTLINE_COLOR_NORMAL = 0x80FFFFFF;
    private static final int OUTLINE_COLOR_HOVER = 0xFFFFFF00;
    private static final int OUTLINE_COLOR_DRAGGING = 0xFF00FF66;
    private static final int FILL_COLOR = 0x55000000;

    private HudModule dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public HudEditScreen() {
        super(Text.literal("SmartView"));
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 28, 100, 20)
                .build());

        int listY = 28;
        for (HudModule module : ModuleManager.getModules()) {
            ModulePosition pos = ModuleManager.getPosition(module.getId());
            this.addDrawableChild(CheckboxWidget.builder(Text.literal(module.getDisplayName()), this.textRenderer)
                    .pos(this.width - PANEL_WIDTH, listY)
                    .checked(pos.enabled)
                    .callback((checkbox, checked) -> pos.enabled = checked)
                    .build());
            listY += 22;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // No background/blur call here on purpose: the outer screen wrapper in 1.21.11
        // already does one blur pass per frame, and calling it again here throws
        // "Can only blur once per frame". We also don't want a blurred world anyway -
        // you need a clear view to place HUD modules.

        context.drawCenteredTextWithShadow(this.textRenderer,
                "SmartView - Module ziehen zum Verschieben", this.width / 2, 8, 0xFFFFFF);
        context.fill(this.width - PANEL_WIDTH - 6, 20, this.width, this.height - 34, 0x40000000);

        for (HudModule module : ModuleManager.getModules()) {
            ModulePosition pos = ModuleManager.getPosition(module.getId());
            if (!pos.enabled) {
                continue;
            }
            int w = module.getWidth();
            int h = module.getHeight();
            boolean hovered = mouseX >= pos.x && mouseX < pos.x + w && mouseY >= pos.y && mouseY < pos.y + h;

            context.fill(pos.x, pos.y, pos.x + w, pos.y + h, FILL_COLOR);
            module.render(context, MinecraftClient.getInstance(), pos.x, pos.y);

            int outlineColor = module == dragging
                    ? OUTLINE_COLOR_DRAGGING
                    : (hovered ? OUTLINE_COLOR_HOVER : OUTLINE_COLOR_NORMAL);
            drawOutline(context, pos.x, pos.y, w, h, outlineColor);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    /** DrawContext has no drawBorder() in 1.21.11 anymore - draw the four edges by hand. */
    private static void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            for (HudModule module : ModuleManager.getModules()) {
                ModulePosition pos = ModuleManager.getPosition(module.getId());
                if (!pos.enabled) {
                    continue;
                }
                if (click.x() >= pos.x && click.x() < pos.x + module.getWidth()
                        && click.y() >= pos.y && click.y() < pos.y + module.getHeight()) {
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
            int newX = (int) click.x() - dragOffsetX;
            int newY = (int) click.y() - dragOffsetY;
            pos.x = Math.max(0, Math.min(newX, this.width - dragging.getWidth()));
            pos.y = Math.max(0, Math.min(newY, this.height - dragging.getHeight()));
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0 && dragging != null) {
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

    @Override
    public boolean shouldPause() {
        return false;
    }
}
