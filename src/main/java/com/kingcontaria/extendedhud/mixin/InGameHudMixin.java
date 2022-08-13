package com.kingcontaria.extendedhud.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.Collection;
import java.util.Iterator;

@Mixin(InGameHud.class)

public class InGameHudMixin extends DrawableHelper {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ItemRenderer itemRenderer;
    private static final Identifier INVENTORY_TEXTURE = new Identifier("textures/gui/container/inventory.png");
    private TextRenderer textRenderer;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initialize(MinecraftClient client, CallbackInfo ci) {
         this.textRenderer = client.textRenderer;
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private void renderAttributes(Window window, float tickDelta, CallbackInfo ci) {
        if (!client.options.debugEnabled && this.client.getCameraEntity() instanceof PlayerEntity) {
            this.client.profiler.swap("extrahud");
            this.client.profiler.push("armor");
            renderArmor();
            this.client.profiler.push("weapon");
            renderWeapon();
            this.client.profiler.push("statuseffects");
            drawStatusEffects(window);
            this.client.profiler.pop();
        }
    }

    private void renderArmor() {
        int x = 5;
        int y = 5 + 3 + (client.player.inventory.armor.length) * 16;
        int stringWidth = 0;

        for (ItemStack armorPiece : client.player.inventory.armor) {
            if (armorPiece != null && armorPiece.getItem() instanceof ArmorItem) {
                stringWidth = Math.max(stringWidth, this.textRenderer.getStringWidth(armorPiece.getMaxDamage() - armorPiece.getDamage() + "/" + armorPiece.getMaxDamage()));
            }
        }

        if (stringWidth == 0) return;

        fill(x, 5, x + 22 + stringWidth + 5, y + 2, -1873784752);

        for (ItemStack armorPiece : client.player.inventory.armor) {
            y -= 16;
            if (armorPiece == null) continue;
            this.itemRenderer.renderInGuiWithOverrides(armorPiece, x + 3, y);
            this.textRenderer.draw(armorPiece.getMaxDamage() - armorPiece.getDamage() + "/" + armorPiece.getMaxDamage(), x + 22, y + 4, Color.WHITE.getRGB());
        }
    }

    private void renderWeapon() {
        ItemStack itemStack = client.player.inventory.getMainHandStack();
        if (itemStack != null) {
            if (itemStack.getItem().isDamageable()) {
                int x = 5;
                int y = 5 + 15 + (client.player.inventory.armor.length) * 16;

                int stringWidth = this.textRenderer.getStringWidth(itemStack.getMaxDamage() - itemStack.getDamage() + "/" + itemStack.getMaxDamage());

                ItemStack arrowItemStack = null;
                int arrows = 0;
                if (itemStack.getItem() instanceof BowItem) {
                    for (ItemStack itemStack1 : client.player.inventory.main) {
                        if (itemStack1 != null && itemStack1.getItem().equals(Items.ARROW)) {
                            arrows += itemStack1.count;
                            arrowItemStack = itemStack1;
                        }
                    }
                    stringWidth = Math.max(stringWidth, this.textRenderer.getStringWidth("x " + arrows));
                }

                fill(x, y - 3, x + 22 + stringWidth + 5, y + 19 + (arrowItemStack != null ? 16 : 0), -1873784752);

                if (arrowItemStack != null) {
                    this.itemRenderer.renderInGuiWithOverrides(arrowItemStack, x + 3, y + 16);
                    this.textRenderer.draw("x " + arrows, x + 22, y + 20, Color.WHITE.getRGB());
                }

                this.itemRenderer.renderInGuiWithOverrides(itemStack, x + 3, y);
                this.textRenderer.draw(itemStack.getMaxDamage() - itemStack.getDamage() + "/" + itemStack.getMaxDamage(), x + 22, y + 4, Color.WHITE.getRGB());
            }
            if (itemStack.getItem() instanceof PotionItem) {
                int x = 5;
                int y = 5 + 15 + (client.player.inventory.armor.length) * 16;

                int stringWidth = this.textRenderer.getStringWidth(itemStack.getTooltip(client.player, false).get(1).split("7", 2)[1]);
                fill(x, y - 3, x + 22 + stringWidth + 5, y + 19, -1873784752);

                this.itemRenderer.renderInGuiWithOverrides(itemStack, x + 3, y);
                this.textRenderer.draw(itemStack.getTooltip(client.player, false).get(1).split("7", 2)[1], x + 22, y + 4, Color.WHITE.getRGB());
            }
        }
    }

    private void drawStatusEffects(Window window) {
        if (this.client.getCameraEntity() instanceof PlayerEntity) {
            Collection<StatusEffectInstance> collection = this.client.player.getStatusEffectInstances();
            if (!collection.isEmpty()) {
                int stringWidth = 0;
                for (StatusEffectInstance statusEffectInstance : collection.toArray(new StatusEffectInstance[0])) {
                    stringWidth = Math.max(stringWidth, textRenderer.getStringWidth(I18n.translate(statusEffectInstance.getTranslationKey()) + getLevel(statusEffectInstance)));
                }
                int x = window.getWidth() - stringWidth - 28 - 10;
                int y = 5;

                fill(x + 3, y, window.getWidth() - 5, y + 4 + collection.size() * 25, -1873784752);

                for (Iterator<StatusEffectInstance> iterator = collection.iterator(); iterator.hasNext(); y += 25) {
                    StatusEffectInstance statusEffectInstance = iterator.next();
                    StatusEffect statusEffect = StatusEffect.STATUS_EFFECTS[statusEffectInstance.getEffectId()];
                    GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                    this.client.getTextureManager().bindTexture(INVENTORY_TEXTURE);
                    if (statusEffect.method_2443()) {
                        int m = statusEffect.method_2444();
                        this.drawTexture(x + 6, y + 7, m % 8 * 18, 198 + m / 8 * 18, 18, 18);
                    }

                    String string = I18n.translate(statusEffect.getTranslationKey()) + getLevel(statusEffectInstance);

                    this.textRenderer.drawWithShadow(string, (float) (x + 10 + 18), (float) (y + 6), 16777215);
                    String string2 = StatusEffect.method_2436(statusEffectInstance);
                    this.textRenderer.drawWithShadow(string2, (float) (x + 10 + 18), (float) (y + 6 + 10), 8355711);
                }
            }
        }
    }

    private String getLevel(StatusEffectInstance statusEffectInstance) {
        switch (statusEffectInstance.getAmplifier()) {
            case 1: return  " " + I18n.translate("enchantment.level.2");
            case 2: return  " " + I18n.translate("enchantment.level.3");
            case 3: return  " " + I18n.translate("enchantment.level.4");
            default: return "";
        }
    }

}
