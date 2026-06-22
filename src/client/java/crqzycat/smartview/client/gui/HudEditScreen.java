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
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HudEditScreen extends Screen {

    // ── layout constants ─────────────────────────────────────────────────────
    private static final int PANEL_WIDTH   = 180;
    private static final int HANDLE_SIZE   = 5;
    private static final int TOGGLE_BTN_W  = 18;
    private static final int TOGGLE_BTN_H  = 36;

    // ── colours ──────────────────────────────────────────────────────────────
    private static final int OUTLINE_NORMAL   = 0x80FFFFFF;
    private static final int OUTLINE_HOVER    = 0xFFFFFF00;
    private static final int OUTLINE_DRAGGING = 0xFF00FF66;
    private static final int OUTLINE_RESIZE   = 0xFF00CCFF;
    private static final int HANDLE_COLOR     = 0xFFFFFFFF;
    private static final int FILL_EDIT        = 0x33FFFFFF;

    // ── sort modes ───────────────────────────────────────────────────────────
    private enum SortMode {
        AZ("A → Z"), ZA("Z → A"), ACTIVE_FIRST("Active first"), INACTIVE_FIRST("Inactive first");

        final String label;
        SortMode(String label) { this.label = label; }
        SortMode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    // ── state ─────────────────────────────────────────────────────────────────
    private boolean panelVisible = true;
    private SortMode sortMode    = SortMode.AZ;
    private String   searchText  = "";

    private HudModule dragging;
    private int dragOffsetX, dragOffsetY;
    private HudModule resizing;
    private double resizeStartMouseX, resizeStartMouseY;
    private float  resizeStartScale;
    private int    resizeStartBaseDim;

    // widgets rebuilt on init
    private ButtonWidget      togglePanelBtn;
    private ButtonWidget      sortBtn;
    private TextFieldWidget   searchField;
    private final List<CheckboxWidget> moduleCheckboxes = new ArrayList<>();

    public HudEditScreen() {
        super(Text.literal("SmartView"));
    }

    // ── init ──────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        moduleCheckboxes.clear();

        // Done button (always visible, centred at bottom)
        this.addDrawableChild(
            ButtonWidget.builder(Text.translatable("gui.done"), btn -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 28, 100, 20)
                .build()
        );

        // Panel toggle button – right edge of screen when collapsed, left edge of panel when open
        int toggleX = panelVisible ? (panelX() - TOGGLE_BTN_W) : (this.width - TOGGLE_BTN_W);
        togglePanelBtn = this.addDrawableChild(
            ButtonWidget.builder(Text.literal(panelVisible ? "»" : "«"), btn -> togglePanel())
                .dimensions(toggleX, this.height / 2 - TOGGLE_BTN_H / 2,
                            TOGGLE_BTN_W, TOGGLE_BTN_H)
                .build()
        );

        if (panelVisible) buildPanelWidgets();
    }

    private void buildPanelWidgets() {
        int px = panelX();
        int top = 28;

        // Search field
        searchField = this.addDrawableChild(
            new TextFieldWidget(this.textRenderer, px + 4, top, PANEL_WIDTH - 8, 16,
                Text.translatable("smartview.gui.search"))
        );
        searchField.setMaxLength(64);
        searchField.setText(searchText);
        searchField.setChangedListener(s -> { searchText = s; rebuildCheckboxes(); });
        searchField.setPlaceholder(Text.translatable("smartview.gui.search"));
        top += 20;

        // Sort button
        sortBtn = this.addDrawableChild(
            ButtonWidget.builder(Text.literal("⇅ " + sortMode.label), btn -> {
                sortMode = sortMode.next();
                btn.setMessage(Text.literal("⇅ " + sortMode.label));
                rebuildCheckboxes();
            }).dimensions(px + 4, top, PANEL_WIDTH - 8, 16).build()
        );
        top += 22;

        buildCheckboxesAt(top);
    }

    private static final int RESET_BTN_W  = 16;

    private void buildCheckboxesAt(int startY) {
        for (CheckboxWidget cb : moduleCheckboxes) this.remove(cb);
        moduleCheckboxes.clear();

        List<HudModule> sorted = sortedFiltered();
        int y = startY;
        int px = panelX();
        MinecraftClient client = MinecraftClient.getInstance();

        for (HudModule module : sorted) {
            ModulePosition pos = ModuleManager.getPosition(module.getId());

            // Checkbox (slightly narrower to leave room for reset button)
            CheckboxWidget cb = this.addDrawableChild(
                CheckboxWidget.builder(Text.literal(module.getDisplayName()), this.textRenderer)
                    .pos(px + 4, y)
                    .checked(pos.enabled)
                    .callback((widget, checked) -> pos.enabled = checked)
                    .build()
            );
            moduleCheckboxes.add(cb);

            // Reset button: resets position, scale and background alpha to defaults
            this.addDrawableChild(
                ButtonWidget.builder(Text.literal("↺"), btn -> {
                    pos.x = module.getDefaultX();
                    pos.y = module.getDefaultY();
                    pos.scale = 1.0f;
                    pos.backgroundAlpha = 128;
                }).dimensions(px + PANEL_WIDTH - RESET_BTN_W - 4, y, RESET_BTN_W, 16).build()
            );

            y += 22;
        }
    }

    private void rebuildCheckboxes() {
        int checkboxTop = panelVisible ? (28 + 20 + 22) : 0;
        buildCheckboxesAt(checkboxTop);
    }

    private void togglePanel() {
        panelVisible = !panelVisible;
        // Rebuild everything so widget positions update
        this.clearChildren();
        this.init();
    }

    // ── rendering ─────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Panel background + title
        if (panelVisible) {
            context.fill(panelX(), 20, this.width, this.height - 34, 0x88000000);
            context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("smartview.gui.edit_title"),
                panelX() + PANEL_WIDTH / 2, 8, 0xFFFFFFFF);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        for (HudModule module : ModuleManager.getModules()) {
            ModulePosition pos = ModuleManager.getPosition(module.getId());
            if (!pos.enabled) continue;

            float scale = Math.max(0.25f, pos.scale);
            int sw = ModuleManager.scaledWidth(module, pos, client);
            int sh = ModuleManager.scaledHeight(module, pos);
            boolean hovered = mouseX >= pos.x && mouseX < pos.x + sw
                           && mouseY >= pos.y && mouseY < pos.y + sh;

            // Draw module content with scale
            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(pos.x, pos.y);
            matrices.scale(scale, scale);
            context.fill(0, 0, module.getBaseWidth(client), module.getBaseHeight(), FILL_EDIT);
            module.render(context, client, 0, 0, pos);
            matrices.popMatrix();

            // Outline
            int outlineColor = module == resizing ? OUTLINE_RESIZE
                             : module == dragging  ? OUTLINE_DRAGGING
                             : hovered             ? OUTLINE_HOVER
                             :                       OUTLINE_NORMAL;
            drawOutline(context, pos.x, pos.y, sw, sh, outlineColor);

            // Corner handles only when hovered or resizing
            if (hovered || module == resizing) {
                drawHandle(context, pos.x,                    pos.y);
                drawHandle(context, pos.x + sw - HANDLE_SIZE, pos.y);
                drawHandle(context, pos.x,                    pos.y + sh - HANDLE_SIZE);
                drawHandle(context, pos.x + sw - HANDLE_SIZE, pos.y + sh - HANDLE_SIZE);
            }

            // Hint text
            if (hovered && module != dragging && module != resizing) {
                context.drawTextWithShadow(this.textRenderer,
                    Text.translatable("smartview.gui.hint"),
                    pos.x, pos.y + sh + 2, 0xFFAAAAAA);
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    // ── input ─────────────────────────────────────────────────────────────────
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

            if (isOnCorner(click.x(), click.y(), pos.x, pos.y, sw, sh)) {
                resizing = module;
                resizeStartMouseX  = click.x();
                resizeStartMouseY  = click.y();
                resizeStartScale   = pos.scale;
                resizeStartBaseDim = Math.max(module.getBaseWidth(client), module.getBaseHeight());
                return true;
            }
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
            double delta = ((click.x() - resizeStartMouseX) + (click.y() - resizeStartMouseY)) / 2.0;
            pos.scale = Math.clamp((float)(resizeStartScale + delta / resizeStartBaseDim), 0.25f, 4.0f);
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
        if (click.button() == 0) { resizing = null; dragging = null; return true; }
        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        ModuleManager.save();
        MinecraftClient.getInstance().setScreen(null);
    }

    @Override public boolean shouldPause() { return false; }

    // ── helpers ───────────────────────────────────────────────────────────────
    /** Left x-coordinate of the side panel. */
    private int panelX() { return this.width - PANEL_WIDTH; }

    private List<HudModule> sortedFiltered() {
        List<HudModule> list = new ArrayList<>(ModuleManager.getModules());
        String q = searchText.toLowerCase();
        if (!q.isEmpty()) list.removeIf(m -> !m.getDisplayName().toLowerCase().contains(q));
        Comparator<HudModule> cmp = switch (sortMode) {
            case AZ             -> Comparator.comparing(HudModule::getDisplayName);
            case ZA             -> Comparator.comparing(HudModule::getDisplayName).reversed();
            case ACTIVE_FIRST   -> Comparator.comparingInt(m ->
                    ModuleManager.getPosition(m.getId()).enabled ? 0 : 1);
            case INACTIVE_FIRST -> Comparator.comparingInt(m ->
                    ModuleManager.getPosition(m.getId()).enabled ? 1 : 0);
        };
        list.sort(cmp);
        return list;
    }

    private static boolean isOnCorner(double mx, double my, int x, int y, int w, int h) {
        return inHandle(mx, my, x,             y)
            || inHandle(mx, my, x + w - HANDLE_SIZE, y)
            || inHandle(mx, my, x,             y + h - HANDLE_SIZE)
            || inHandle(mx, my, x + w - HANDLE_SIZE, y + h - HANDLE_SIZE);
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
