package crqzycat.smartview.client.modules;

import crqzycat.smartview.client.hud.HudModule;
import crqzycat.smartview.client.hud.ModulePosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class ArmorStatusModule implements HudModule {

    // One row per armor piece: icon (16px) + bar (40px) + gap
    private static final int SLOT_HEIGHT = 18;
    private static final int ICON_SIZE   = 16;
    private static final int BAR_W       = 40;
    private static final int BAR_H       = 4;
    private static final int PAD         = 4;
    private static final int WIDTH       = ICON_SIZE + PAD + BAR_W + PAD;

    // HEAD → CHEST → LEGS → FEET (top to bottom display order)
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST,
        EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @Override public String getId() { return "armor_status"; }
    @Override public String getDisplayName() { return "Armor Status"; }
    @Override public int getDefaultX() { return 10; }
    @Override public int getDefaultY() { return 50; }
    @Override public int getBaseWidth(MinecraftClient c) { return WIDTH; }
    @Override public int getBaseHeight() { return SLOT_HEIGHT * SLOTS.length; }

    @Override
    public void render(DrawContext context, MinecraftClient client, int x, int y, ModulePosition pos) {
        if (client.player == null) return;

        boolean anyArmor = false;
        for (EquipmentSlot slot : SLOTS) {
            if (!client.player.getEquippedStack(slot).isEmpty()) { anyArmor = true; break; }
        }
        // No armor equipped → render nothing (background stays invisible)
        if (!anyArmor) return;

        context.fill(x, y, x + WIDTH, y + getBaseHeight(), pos.backgroundAlpha << 24);

        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = client.player.getEquippedStack(SLOTS[i]);
            int rowY = y + i * SLOT_HEIGHT;

            // Item icon
            context.drawItem(stack, x, rowY);

            if (stack.isEmpty()) continue;

            if (stack.isDamageable()) {
                int damage   = stack.getDamage();
                int maxDmg   = stack.getMaxDamage();
                float frac   = 1f - (float) damage / maxDmg;

                // Bar background
                int barX = x + ICON_SIZE + PAD;
                int barY = rowY + (ICON_SIZE - BAR_H) / 2;
                context.fill(barX, barY, barX + BAR_W, barY + BAR_H, 0xFF333333);
                context.fill(barX, barY, barX + Math.round(BAR_W * frac), barY + BAR_H, durabilityColor(frac));
            } else {
                // Unbreakable – show a small "∞" indicator
                int barX = x + ICON_SIZE + PAD;
                int barY = rowY + (ICON_SIZE - BAR_H) / 2;
                context.fill(barX, barY, barX + BAR_W, barY + BAR_H, 0xFF333333);
                context.fill(barX, barY, barX + BAR_W, barY + BAR_H, 0xFF55FFFF);
            }
        }
    }

    private static int durabilityColor(float frac) {
        if (frac > 0.6f) return 0xFF55FF55; // green
        if (frac > 0.3f) return 0xFFFFFF55; // yellow
        return 0xFFFF5555;                   // red
    }
    @Override public boolean enabledByDefault() { return false; }
}
