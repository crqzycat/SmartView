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
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.Set;

public class HudEditScreen extends Screen {

    private static final int PANEL_WIDTH  = 200;
    private static final int HANDLE_SIZE  = 5;
    private static final int TOGGLE_BTN_W = 18;
    private static final int TOGGLE_BTN_H = 36;
    private static final int RESET_BTN_W  = 16;
    private static final int KB_BTN_W     = 60;

    private static final int OUTLINE_NORMAL   = 0x80FFFFFF;
    private static final int OUTLINE_HOVER    = 0xFFFFFF00;
    private static final int OUTLINE_DRAGGING = 0xFF00FF66;
    private static final int OUTLINE_RESIZE   = 0xFF00CCFF;

    private enum SortMode {
        AZ("A → Z"), ZA("Z → A"), ACTIVE_FIRST("Active first"), INACTIVE_FIRST("Inactive first");
        final String label;
        SortMode(String l) { label = l; }
        SortMode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private boolean  panelVisible = true;
    private SortMode sortMode     = SortMode.AZ;
    private String   searchText   = "";

    // Drag state
    private HudModule dragging;
    private int dragOffsetX, dragOffsetY;

    // Resize state
    private HudModule resizing;
    private double resizeStartMouseX, resizeStartMouseY;
    private float  resizeStartScale;
    private int    resizeStartBaseDim;

    // Keybind listening state
    private HudModule listeningModule = null; // which module we're binding a key for
    private Map<String, ButtonWidget> keybindButtons = new LinkedHashMap<>();

    private ButtonWidget     togglePanelBtn;
    private TextFieldWidget  searchField;
    private final List<CheckboxWidget> moduleCheckboxes = new ArrayList<>();

    public HudEditScreen() {
        super(Text.literal("SmartView"));
    }

    @Override
    protected void init() {
        moduleCheckboxes.clear();
        keybindButtons.clear();
        listeningModule = null;

        this.addDrawableChild(
            ButtonWidget.builder(Text.translatable("gui.done"), btn -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 28, 100, 20)
                .build()
        );

        int toggleX = panelVisible ? (panelX() - TOGGLE_BTN_W) : (this.width - TOGGLE_BTN_W);
        togglePanelBtn = this.addDrawableChild(
            ButtonWidget.builder(Text.literal(panelVisible ? "»" : "«"), btn -> togglePanel())
                .dimensions(toggleX, this.height / 2 - TOGGLE_BTN_H / 2, TOGGLE_BTN_W, TOGGLE_BTN_H)
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
        searchField.setChangedListener(s -> { searchText = s; rebuildModuleRows(); });
        searchField.setPlaceholder(Text.translatable("smartview.gui.search"));
        top += 20;

        // Sort button
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("⇅ " + sortMode.label), btn -> {
                sortMode = sortMode.next();
                btn.setMessage(Text.literal("⇅ " + sortMode.label));
                rebuildModuleRows();
            }).dimensions(px + 4, top, PANEL_WIDTH - 8, 16).build()
        );
        top += 22;

        buildModuleRows(top);
    }

    private void buildModuleRows(int startY) {
        for (CheckboxWidget cb : moduleCheckboxes) this.remove(cb);
        moduleCheckboxes.clear();
        for (ButtonWidget btn : keybindButtons.values()) this.remove(btn);
        keybindButtons.clear();

        int px = panelX();
        int y  = startY;
        MinecraftClient client = MinecraftClient.getInstance();

        for (HudModule module : sortedFiltered()) {
            ModulePosition pos = ModuleManager.getPosition(module.getId());

            // Checkbox
            CheckboxWidget cb = this.addDrawableChild(
                CheckboxWidget.builder(Text.literal(module.getDisplayName()), this.textRenderer)
                    .pos(px + 4, y)
                    .checked(pos.enabled)
                    .callback((widget, checked) -> pos.enabled = checked)
                    .build()
            );
            moduleCheckboxes.add(cb);

            // Keybind button
            ButtonWidget kbBtn = this.addDrawableChild(
                ButtonWidget.builder(keybindLabel(module), btn -> startListening(module))
                    .dimensions(px + PANEL_WIDTH - KB_BTN_W - RESET_BTN_W - 6, y, KB_BTN_W, 14)
                    .build()
            );
            keybindButtons.put(module.getId(), kbBtn);

            // Reset button
            this.addDrawableChild(
                ButtonWidget.builder(Text.literal("↺"), btn -> {
                    pos.x = module.getDefaultX();
                    pos.y = module.getDefaultY();
                    pos.scale = 1.0f;
                    pos.backgroundAlpha = 0;
                }).dimensions(px + PANEL_WIDTH - RESET_BTN_W - 4, y, RESET_BTN_W, 14).build()
            );

            y += 20;
        }
    }

    private void rebuildModuleRows() {
        int rowsTop = panelVisible ? (28 + 20 + 22) : 0;
        buildModuleRows(rowsTop);
    }

    private void togglePanel() {
        panelVisible = !panelVisible;
        this.clearChildren();
        this.init();
    }

    // ── keybind listening ─────────────────────────────────────────────────────

    private void startListening(HudModule module) {
        listeningModule = module;
        ButtonWidget btn = keybindButtons.get(module.getId());
        if (btn != null) btn.setMessage(Text.literal("> ..."));
    }

    // Keys that must never be bound
    private static final Set<Integer> BLOCKED_KEYS = Set.of(
        GLFW.GLFW_KEY_LEFT_SUPER,    // Windows / Mac Command
        GLFW.GLFW_KEY_RIGHT_SUPER,
        GLFW.GLFW_KEY_PRINT_SCREEN,  // Druck
        GLFW.GLFW_KEY_PAUSE,
        GLFW.GLFW_KEY_CAPS_LOCK,
        GLFW.GLFW_KEY_NUM_LOCK,
        GLFW.GLFW_KEY_SCROLL_LOCK
    );

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (listeningModule != null) {
            int key = keyInput.key();
            if (!BLOCKED_KEYS.contains(key)) {
                KeyBinding kb = ModuleManager.getKeybind(listeningModule.getId());
                if (kb != null) {
                    if (key == GLFW.GLFW_KEY_ESCAPE) {
                        kb.setBoundKey(InputUtil.UNKNOWN_KEY);
                    } else {
                        kb.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(key));
                    }
                    KeyBinding.updateKeysByCode();
                    MinecraftClient.getInstance().options.write();
                    ButtonWidget btn = keybindButtons.get(listeningModule.getId());
                    if (btn != null) btn.setMessage(keybindLabel(listeningModule));
                }
            }
            // Blocked key → just cancel listening without binding
            listeningModule = null;
            return true;
        }
        return super.keyPressed(keyInput);
    }

    private Text keybindLabel(HudModule module) {
        KeyBinding kb = ModuleManager.getKeybind(module.getId());
        if (kb == null || kb.isUnbound()) return Text.literal("[ --- ]");
        return Text.literal("[").append(kb.getBoundKeyLocalizedText()).append("]");
    }

    // ── rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
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
            if (module.getBaseWidth(client) == 0 && module.getBaseHeight() == 0) continue;

            float scale = Math.max(0.25f, pos.scale);
            int sw = ModuleManager.scaledWidth(module, pos, client);
            int sh = ModuleManager.scaledHeight(module, pos);
            boolean hovered = mouseX >= pos.x && mouseX < pos.x + sw
                           && mouseY >= pos.y && mouseY < pos.y + sh;

            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(pos.x, pos.y);
            matrices.scale(scale, scale);
            context.fill(0, 0, module.getBaseWidth(client), module.getBaseHeight(), 0x33FFFFFF);
            module.render(context, client, 0, 0, pos);
            matrices.popMatrix();

            int outlineColor = module == resizing ? OUTLINE_RESIZE
                             : module == dragging  ? OUTLINE_DRAGGING
                             : hovered             ? OUTLINE_HOVER
                             :                       OUTLINE_NORMAL;
            drawOutline(context, pos.x, pos.y, sw, sh, outlineColor);

            if (hovered || module == resizing) {
                drawHandle(context, pos.x,                    pos.y);
                drawHandle(context, pos.x + sw - HANDLE_SIZE, pos.y);
                drawHandle(context, pos.x,                    pos.y + sh - HANDLE_SIZE);
                drawHandle(context, pos.x + sw - HANDLE_SIZE, pos.y + sh - HANDLE_SIZE);
            }

            if (hovered && module != dragging && module != resizing) {
                context.drawTextWithShadow(this.textRenderer,
                    Text.translatable("smartview.gui.hint"),
                    pos.x, pos.y + sh + 2, 0xFFAAAAAA);
            }
        }

        super.render(context, mouseX, mouseY, delta);

        // Listening overlay drawn AFTER widgets so it covers everything
        if (listeningModule != null) {
            context.fill(0, 0, this.width, this.height, 0xDD000000);
            String line1 = "Press a key for  \"" + listeningModule.getDisplayName() + "\"";
            String line2 = "ESC = unbind  |  any system key = cancel";
            context.drawCenteredTextWithShadow(this.textRenderer, line1,
                this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, line2,
                this.width / 2, this.height / 2 + 4, 0xFFAAAAAA);
        }
    }

    // ── mouse input ───────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        MinecraftClient client = MinecraftClient.getInstance();
        long window = client.getWindow().getHandle();
        boolean shift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT)  == GLFW.GLFW_PRESS
                     || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (!shift) return super.mouseScrolled(mx, my, h, v);
        for (HudModule module : ModuleManager.getModules()) {
            ModulePosition pos = ModuleManager.getPosition(module.getId());
            if (!pos.enabled) continue;
            int sw = ModuleManager.scaledWidth(module, pos, client);
            int sh = ModuleManager.scaledHeight(module, pos);
            if (mx >= pos.x && mx < pos.x + sw && my >= pos.y && my < pos.y + sh) {
                pos.backgroundAlpha = Math.clamp(pos.backgroundAlpha + (int)(v * 16), 0, 255);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (listeningModule != null) { listeningModule = null; return true; }
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        MinecraftClient client = MinecraftClient.getInstance();
        for (HudModule module : ModuleManager.getModules()) {
            ModulePosition pos = ModuleManager.getPosition(module.getId());
            if (!pos.enabled) continue;
            if (module.getBaseWidth(client) == 0 && module.getBaseHeight() == 0) continue;
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
    public boolean mouseDragged(Click click, double ox, double oy) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (resizing != null) {
            ModulePosition pos = ModuleManager.getPosition(resizing.getId());
            double delta = ((click.x() - resizeStartMouseX) + (click.y() - resizeStartMouseY)) / 2.0;
            pos.scale = Math.clamp((float)(resizeStartScale + delta / resizeStartBaseDim), 0.25f, 4.0f);
            return true;
        }
        if (dragging != null) {
            ModulePosition pos = ModuleManager.getPosition(dragging.getId());
            int maxX = Math.max(0, this.width  - ModuleManager.scaledWidth(dragging, pos, client));
            int maxY = Math.max(0, this.height - ModuleManager.scaledHeight(dragging, pos));
            pos.x = Math.clamp((int) click.x() - dragOffsetX, 0, maxX);
            pos.y = Math.clamp((int) click.y() - dragOffsetY, 0, maxY);
            return true;
        }
        return super.mouseDragged(click, ox, oy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) { resizing = null; dragging = null; return true; }
        return super.mouseReleased(click);
    }

    @Override
    public void close() { ModuleManager.save(); MinecraftClient.getInstance().setScreen(null); }
    @Override public boolean shouldPause() { return false; }

    // ── helpers ───────────────────────────────────────────────────────────────

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
        ctx.fill(x, y, x + HANDLE_SIZE, y + HANDLE_SIZE, 0xFFFFFFFF);
        ctx.fill(x + 1, y + 1, x + HANDLE_SIZE - 1, y + HANDLE_SIZE - 1, 0xFF333333);
    }

    private static void drawOutline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color);
        ctx.fill(x,         y,         x + 1,     y + h,     color);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color);
    }
}
