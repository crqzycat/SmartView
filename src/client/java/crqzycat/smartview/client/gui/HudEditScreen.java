package crqzycat.smartview.client.gui;

import crqzycat.smartview.client.config.SmartViewConfig;
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

public class HudEditScreen extends Screen {

    private static final int PANEL_WIDTH  = 200;
    private static final int HANDLE_SIZE  = 5;
    private static final int TOGGLE_BTN_W = 18;
    private static final int TOGGLE_BTN_H = 36;
    private static final int RESET_BTN_W  = 16;
    private static final int COLOR_BTN_W  = 16;
    private static final int KB_BTN_W     = 60;

    private static final int OUTLINE_NORMAL   = 0x80FFFFFF;
    private static final int OUTLINE_HOVER    = 0xFFFFFF00;
    private static final int OUTLINE_DRAGGING = 0xFF00FF66;
    private static final int OUTLINE_RESIZE   = 0xFF00CCFF;

    private static final int COLOR_CATEGORY_BG   = 0xFF1A1A2E;
    private static final int COLOR_CATEGORY_TEXT  = 0xFF88CCFF;

    private enum SortMode {
        AZ("A → Z"), ZA("Z → A"), ACTIVE_FIRST("Active first"), INACTIVE_FIRST("Inactive first");
        final String label;
        SortMode(String l) { label = l; }
        SortMode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private boolean  panelVisible = true;
    private String   searchText   = "";
    private int      scrollOffset = 0;
    private int      totalContentHeight = 0;

    private final Map<HudModule.Category, SortMode>  categorySortModes    = new LinkedHashMap<>();
    /** Persists collapse state across screen opens within the same game session. */
    private static final Map<HudModule.Category, Boolean> categoryCollapsed = new LinkedHashMap<>();

    private HudModule dragging;
    private int dragOffsetX, dragOffsetY;
    private HudModule resizing;
    private double resizeStartMouseX, resizeStartMouseY;
    private float  resizeStartScale;
    private int    resizeStartBaseDim;

    private HudModule listeningModule = null;
    private final Map<String, ButtonWidget>          keybindButtons        = new LinkedHashMap<>();
    private final Map<String, ButtonWidget>          resetButtons          = new LinkedHashMap<>();
    private final Map<String, ButtonWidget>          colorButtons          = new LinkedHashMap<>();
    private final Map<HudModule.Category, ButtonWidget> categorySortButtons = new LinkedHashMap<>();
    private final Map<HudModule.Category, ButtonWidget> collapseButtons     = new LinkedHashMap<>();
    private final List<CheckboxWidget>               moduleCheckboxes      = new ArrayList<>();

    private TextFieldWidget searchField;

    public HudEditScreen() {
        super(Text.literal("SmartView"));
        for (HudModule.Category cat : HudModule.Category.values()) {
            categorySortModes.put(cat, SortMode.AZ);
            categoryCollapsed.putIfAbsent(cat, false);
        }
    }

    @Override
    protected void init() {
        moduleCheckboxes.clear();
        keybindButtons.clear();
        categorySortButtons.clear();
        collapseButtons.clear();
        listeningModule = null;
        scrollOffset = 0;

        this.addDrawableChild(
            ButtonWidget.builder(Text.translatable("gui.done"), btn -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 28, 100, 20)
                .build()
        );

        int toggleX = panelVisible ? (panelX() - TOGGLE_BTN_W) : (this.width - TOGGLE_BTN_W);
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal(panelVisible ? "»" : "«"), btn -> togglePanel())
                .dimensions(toggleX, this.height / 2 - TOGGLE_BTN_H / 2, TOGGLE_BTN_W, TOGGLE_BTN_H)
                .build()
        );

        if (panelVisible) buildPanelWidgets();
    }

    private void buildPanelWidgets() {
        int px = panelX();
        int top = 28;

        // Profile bar
        buildProfileBar(px, top);
        top += 20;

        searchField = this.addDrawableChild(
            new TextFieldWidget(this.textRenderer, px + 4, top, PANEL_WIDTH - 8, 16,
                Text.translatable("smartview.gui.search"))
        );
        searchField.setMaxLength(64);
        searchField.setText(searchText);
        searchField.setChangedListener(s -> { searchText = s; rebuildModuleRowsResetScroll(); });
        searchField.setPlaceholder(Text.translatable("smartview.gui.search"));
        top += 22;

        buildModuleRows(top);
    }

    private static final int ROW_START_Y = 70;

    /** profile name → its button x/y/w for right-click detection */
    private final java.util.Map<String, int[]> profileButtonBounds = new java.util.LinkedHashMap<>();

    private void buildProfileBar(int px, int top) {
        profileButtonBounds.clear();
        java.util.List<String> profiles = new java.util.ArrayList<>(ModuleManager.getProfileNames());
        int btnW = (PANEL_WIDTH - 22) / Math.max(1, profiles.size());
        btnW = Math.min(btnW, 50);

        for (int i = 0; i < profiles.size(); i++) {
            String name = profiles.get(i);
            boolean active = name.equals(ModuleManager.getActiveProfile());
            int bx = px + 2 + i * (btnW + 1);
            profileButtonBounds.put(name, new int[]{bx, top, btnW, 14});
            this.addDrawableChild(
                ButtonWidget.builder(Text.literal(active ? "§e" + name : name), btn -> {
                    ModuleManager.switchProfile(name);
                    this.clearChildren();
                    this.init();
                }).dimensions(bx, top, btnW, 14).build()
            );
        }

        // + new profile button
        int usedW = profiles.size() * (btnW + 1) + 2;
        if (profiles.size() < SmartViewConfig.MAX_PROFILES) {
            this.addDrawableChild(
                ButtonWidget.builder(Text.literal("+"), btn -> {
                    String newName = "Profile " + (profiles.size() + 1);
                    ModuleManager.switchProfile(newName);
                    this.clearChildren();
                    this.init();
                }).dimensions(px + usedW, top, 14, 14).build()
            );
        }

        // ✕ delete current profile button
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("✕"), btn -> {
                ModuleManager.deleteProfile(ModuleManager.getActiveProfile());
                this.clearChildren();
                this.init();
            }).dimensions(px + PANEL_WIDTH - 16, top, 14, 14).build()
        );
    }

    /** Opens an inline rename field for a profile. */
    private void openRenameField(String profileName) {
        int[] bounds = profileButtonBounds.get(profileName);
        if (bounds == null) return;
        TextFieldWidget field = new TextFieldWidget(
            this.textRenderer, bounds[0], bounds[1], bounds[2], bounds[3],
            Text.literal("Rename")
        );
        field.setMaxLength(24);
        field.setText(profileName);
        field.setFocused(true);
        field.setChangedListener(s -> {}); // handled on confirm
        // Store reference so we can confirm on Enter/Esc
        renamingProfile = profileName;
        renameField = field;
        this.addDrawableChild(field);
    }

    private String renamingProfile = null;
    private TextFieldWidget renameField = null;

    private void confirmRename() {
        if (renamingProfile != null && renameField != null) {
            String newName = renameField.getText().trim();
            if (!newName.isEmpty() && !newName.equals(renamingProfile)) {
                ModuleManager.renameProfile(renamingProfile, newName);
            }
        }
        renamingProfile = null;
        renameField = null;
        this.clearChildren();
        this.init();
    }

    private void buildModuleRows(int startY) {
        // Remove old widgets
        for (CheckboxWidget cb : moduleCheckboxes) this.remove(cb);
        moduleCheckboxes.clear();
        for (ButtonWidget btn : keybindButtons.values()) this.remove(btn);
        keybindButtons.clear();
        for (ButtonWidget btn : categorySortButtons.values()) this.remove(btn);
        categorySortButtons.clear();
        for (ButtonWidget btn : collapseButtons.values()) this.remove(btn);
        collapseButtons.clear();
        for (ButtonWidget btn : resetButtons.values()) this.remove(btn);
        resetButtons.clear();
        for (ButtonWidget btn : colorButtons.values()) this.remove(btn);
        colorButtons.clear();

        int px = panelX();
        int contentY = startY; // tracks logical height for scrolling
        String q = searchText.toLowerCase();

        for (HudModule.Category cat : HudModule.Category.values()) {
            List<HudModule> modules = getModulesForCategory(cat, q);
            if (modules.isEmpty()) continue;

            boolean collapsed = categoryCollapsed.getOrDefault(cat, false);
            int widgetY = contentY - scrollOffset;

            // Collapse toggle button (arrow)
            HudModule.Category catFinal = cat;
            ButtonWidget collapseBtn = this.addDrawableChild(
                ButtonWidget.builder(Text.literal(collapsed ? "▶" : "▼"), btn -> {
                    categoryCollapsed.put(catFinal, !categoryCollapsed.get(catFinal));
                    rebuildModuleRows();
                }).dimensions(px + 2, widgetY, 14, 12).build()
            );
            collapseButtons.put(cat, collapseBtn);

            // Sort button – only when expanded
            if (!collapsed) {
                SortMode sm = categorySortModes.get(cat);
                ButtonWidget sortBtn = this.addDrawableChild(
                    ButtonWidget.builder(Text.literal("⇅ " + sm.label), btn -> {
                        categorySortModes.put(catFinal, categorySortModes.get(catFinal).next());
                        rebuildModuleRows();
                    }).dimensions(px + PANEL_WIDTH - 74, widgetY, 70, 12).build()
                );
                categorySortButtons.put(cat, sortBtn);
            }

            contentY += 16; // header row height

            if (!collapsed) {
                // Sort modules within this category
                SortMode sm = categorySortModes.get(cat);
                modules.sort(switch (sm) {
                    case AZ             -> Comparator.comparing(HudModule::getDisplayName);
                    case ZA             -> Comparator.comparing(HudModule::getDisplayName).reversed();
                    case ACTIVE_FIRST   -> Comparator.comparingInt(m -> ModuleManager.getPosition(m.getId()).enabled ? 0 : 1);
                    case INACTIVE_FIRST -> Comparator.comparingInt(m -> ModuleManager.getPosition(m.getId()).enabled ? 1 : 0);
                });

                for (HudModule module : modules) {
                    ModulePosition pos = ModuleManager.getPosition(module.getId());
                    int rowY = contentY - scrollOffset;

                    CheckboxWidget cb = this.addDrawableChild(
                        CheckboxWidget.builder(Text.literal(module.getDisplayName()), this.textRenderer)
                            .pos(px + 4, rowY)
                            .checked(pos.enabled)
                            .callback((widget, checked) -> pos.enabled = checked)
                            .build()
                    );
                    moduleCheckboxes.add(cb);

                    ButtonWidget kbBtn = this.addDrawableChild(
                        ButtonWidget.builder(keybindLabel(module), btn -> startListening(module))
                            .dimensions(px + PANEL_WIDTH - KB_BTN_W - RESET_BTN_W - COLOR_BTN_W - 8, rowY, KB_BTN_W, 14)
                            .build()
                    );
                    keybindButtons.put(module.getId(), kbBtn);

                    // Color picker button – only for modules that render text
                    HudModule moduleFinal = module;
                    if (module.getBaseWidth(MinecraftClient.getInstance()) > 0 || module.getBaseHeight() > 0) {
                        ButtonWidget colorBtn = this.addDrawableChild(
                            ButtonWidget.builder(Text.literal("■").withColor(pos.textColor), btn -> {
                                MinecraftClient.getInstance().setScreen(
                                    new ColorPickerScreen(this, pos, moduleFinal.getDisplayName(), this::rebuildModuleRows));
                            }).dimensions(px + PANEL_WIDTH - RESET_BTN_W - COLOR_BTN_W - 6, rowY, COLOR_BTN_W, 14).build()
                        );
                        colorButtons.put(module.getId(), colorBtn);
                    }

                    ButtonWidget resetBtn = this.addDrawableChild(
                        ButtonWidget.builder(Text.literal("↺"), btn -> {
                            pos.x = module.getDefaultX();
                            pos.y = module.getDefaultY();
                            pos.scale = 1.0f;
                            pos.backgroundAlpha = 0;
                            pos.textColor = 0xFFFFFFFF;
                        }).dimensions(px + PANEL_WIDTH - RESET_BTN_W - 4, rowY, RESET_BTN_W, 14).build()
                    );
                    resetButtons.put(module.getId(), resetBtn);

                    contentY += 20;
                }

                contentY += 4; // spacing after expanded category
            }
        }

        totalContentHeight = contentY - startY;
        updateWidgetVisibility();
    }

    /** Hide widgets that are scrolled outside the visible panel area. */
    private void updateWidgetVisibility() {
        int topClip    = ROW_START_Y;
        int bottomClip = this.height - 34;

        for (CheckboxWidget cb : moduleCheckboxes) {
            boolean visible = cb.getY() >= topClip && cb.getY() + cb.getHeight() <= bottomClip;
            cb.visible = visible;
        }
        for (ButtonWidget btn : keybindButtons.values()) {
            boolean visible = btn.getY() >= topClip && btn.getY() + btn.getHeight() <= bottomClip;
            btn.visible = visible;
        }
        for (ButtonWidget btn : resetButtons.values()) {
            boolean visible = btn.getY() >= topClip && btn.getY() + btn.getHeight() <= bottomClip;
            btn.visible = visible;
        }
        for (ButtonWidget btn : colorButtons.values()) {
            boolean visible = btn.getY() >= topClip && btn.getY() + btn.getHeight() <= bottomClip;
            btn.visible = visible;
        }
        for (ButtonWidget btn : categorySortButtons.values()) {
            boolean visible = btn.getY() >= topClip && btn.getY() + btn.getHeight() <= bottomClip;
            btn.visible = visible;
        }
        for (ButtonWidget btn : collapseButtons.values()) {
            boolean visible = btn.getY() >= topClip && btn.getY() + btn.getHeight() <= bottomClip;
            btn.visible = visible;
        }
    }

    private List<HudModule> getModulesForCategory(HudModule.Category cat, String query) {
        List<HudModule> list = new ArrayList<>();
        for (HudModule m : ModuleManager.getModules()) {
            if (m.getCategory() != cat) continue;
            if (!query.isEmpty() && !m.getDisplayName().toLowerCase().contains(query)) continue;
            list.add(m);
        }
        return list;
    }

    private void rebuildModuleRows() {
        buildModuleRows(ROW_START_Y);
    }

    private void rebuildModuleRowsResetScroll() {
        scrollOffset = 0;
        buildModuleRows(ROW_START_Y);
    }

    private void togglePanel() {
        panelVisible = !panelVisible;
        this.clearChildren();
        this.init();
    }

    // ── keybind ───────────────────────────────────────────────────────────────

    private void startListening(HudModule module) {
        listeningModule = module;
        ButtonWidget btn = keybindButtons.get(module.getId());
        if (btn != null) btn.setMessage(Text.literal("> ..."));
    }

    private static final Set<Integer> BLOCKED_KEYS = Set.of(
        GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER,
        GLFW.GLFW_KEY_PRINT_SCREEN, GLFW.GLFW_KEY_PAUSE,
        GLFW.GLFW_KEY_CAPS_LOCK, GLFW.GLFW_KEY_NUM_LOCK, GLFW.GLFW_KEY_SCROLL_LOCK
    );

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        // Rename field confirm/cancel
        if (renamingProfile != null) {
            int key = keyInput.key();
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                confirmRename();
                return true;
            } else if (key == GLFW.GLFW_KEY_ESCAPE) {
                renamingProfile = null;
                renameField = null;
                this.clearChildren();
                this.init();
                return true;
            }
            return super.keyPressed(keyInput);
        }
        // Rename field confirm/cancel
        if (renamingProfile != null) {
            int key = keyInput.key();
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                confirmRename();
                return true;
            } else if (key == GLFW.GLFW_KEY_ESCAPE) {
                renamingProfile = null;
                renameField = null;
                this.clearChildren();
                this.init();
                return true;
            }
            return super.keyPressed(keyInput);
        }

        if (listeningModule != null) {
            int key = keyInput.key();
            if (!BLOCKED_KEYS.contains(key)) {
                KeyBinding kb = ModuleManager.getKeybind(listeningModule.getId());
                if (kb != null) {
                    kb.setBoundKey(key == GLFW.GLFW_KEY_ESCAPE
                        ? InputUtil.UNKNOWN_KEY
                        : InputUtil.Type.KEYSYM.createFromCode(key));
                    KeyBinding.updateKeysByCode();
                    MinecraftClient.getInstance().options.write();
                    ButtonWidget btn = keybindButtons.get(listeningModule.getId());
                    if (btn != null) btn.setMessage(keybindLabel(listeningModule));
                }
            }
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
        int px = panelX();

        if (panelVisible) {
            context.fill(px, 20, this.width, this.height - 34, 0x88000000);
            context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("smartview.gui.edit_title"),
                px + PANEL_WIDTH / 2, 8, 0xFFFFFFFF);

            // Clip category headers to the area below the search bar
            context.enableScissor(px, ROW_START_Y, this.width, this.height - 34);
            drawCategoryHeaders(context);
            context.disableScissor();
        }

        // HUD module overlays on screen
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

        if (listeningModule != null) {
            context.fill(0, 0, this.width, this.height, 0xDD000000);
            context.drawCenteredTextWithShadow(this.textRenderer,
                "Press a key for \"" + listeningModule.getDisplayName() + "\"",
                this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer,
                "ESC = unbind  |  any system key = cancel",
                this.width / 2, this.height / 2 + 4, 0xFFAAAAAA);
        }
    }

    private void drawCategoryHeaders(DrawContext context) {
        int px = panelX();
        int y = ROW_START_Y - scrollOffset;
        String q = searchText.toLowerCase();

        for (HudModule.Category cat : HudModule.Category.values()) {
            List<HudModule> modules = getModulesForCategory(cat, q);
            if (modules.isEmpty()) continue;

            boolean collapsed = categoryCollapsed.getOrDefault(cat, false);

            // Header background full width
            context.fill(px, y, px + PANEL_WIDTH, y + 14, COLOR_CATEGORY_BG);

            // Category label (offset to leave room for the collapse button)
            String arrow = collapsed ? "▶" : "▼";
            context.drawTextWithShadow(this.textRenderer,
                Text.literal(arrow + " " + cat.label),
                px + 18, y + 3, COLOR_CATEGORY_TEXT);

            y += 16;
            if (!collapsed) {
                y += modules.size() * 20 + 4;
            }
        }
    }

    // ── scroll ────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        MinecraftClient client = MinecraftClient.getInstance();
        long window = client.getWindow().getHandle();
        boolean shift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT)  == GLFW.GLFW_PRESS
                     || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        if (shift) {
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
        }

        if (panelVisible) {
            int maxScroll = Math.max(0, totalContentHeight - (this.height - ROW_START_Y - 34));
            if (maxScroll > 0) {
                scrollOffset = (int) Math.clamp(scrollOffset - v * 12, 0, maxScroll);
                buildModuleRows(ROW_START_Y);
                return true;
            }
        }

        return super.mouseScrolled(mx, my, h, v);
    }

    // ── mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (listeningModule != null) { listeningModule = null; return true; }

        // Right-click on profile button → open rename field
        if (click.button() == 1) {
            for (java.util.Map.Entry<String, int[]> e : profileButtonBounds.entrySet()) {
                int[] b = e.getValue();
                if (click.x() >= b[0] && click.x() < b[0]+b[2] && click.y() >= b[1] && click.y() < b[1]+b[3]) {
                    openRenameField(e.getKey());
                    return true;
                }
            }
        }

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

    private static boolean isOnCorner(double mx, double my, int x, int y, int w, int h) {
        return inHandle(mx, my, x,                 y)
            || inHandle(mx, my, x + w - HANDLE_SIZE, y)
            || inHandle(mx, my, x,                 y + h - HANDLE_SIZE)
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
