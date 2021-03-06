package org.winterblade.minecraft.mods.needs.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.Formatting;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.needs.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;
import org.winterblade.minecraft.mods.needs.client.gui.components.*;
import org.winterblade.minecraft.mods.needs.client.gui.widgets.InventoryButton;
import org.winterblade.minecraft.mods.needs.mixins.UiMixin;
import org.winterblade.minecraft.mods.needs.util.MathLib;

import java.util.List;

public class NeedDisplayScreen extends ComponentScreen {
    @SuppressWarnings("WeakerAccess")
    public static final int ITEM_HEIGHT = 38;

    private NeedDisplayScreen() {
        super(new StringTextComponent(""), 318, 222);

        texture = new Texture(NeedsMod.MODID, "need_ui", 384, 384);
        window = new WindowComponent(texture.getSubtexture(guiWidth, guiHeight), guiWidth, guiHeight);

        // Components
        window.addComponent(new TextComponent("Needs, Wants, and Desires", 7, 7, 0x404040, false));

        final ScrollbarComponent bar = new ScrollbarComponent(
            new Rectangle2d(298, 24, 12, 190),
            texture.getSubtexture(12, 15, 318, 0),
            texture.getSubtexture(12, 15, 330, 0),
            () -> (UiMixin.getLocalNeeds().size() * ITEM_HEIGHT) - 190
        );
        window.addComponent(bar);
        window.addComponent(new ScrollpaneComponent<>(
                bar,
                new Rectangle2d(8, 24, 284, 190),
                (i) -> new NeedComponent(texture.getSubtexture(284, ITEM_HEIGHT, 0, 222), new Rectangle2d(0, 0, 284, ITEM_HEIGHT)),
                ITEM_HEIGHT,
                (c, i) -> {
                    final List<LocalCachedNeed> localNeeds = UiMixin.getLocalNeeds();
                    if (localNeeds.size() <= i) {
                        c.setVisible(false);
                        return;
                    }

                    final LocalCachedNeed localNeed = localNeeds.get(i);
                    final Need need = localNeed.getNeed().get();
                    if (need == null) {
                        c.setVisible(false);
                        return;
                    }

                    final UiMixin mixin = UiMixin.getInstance(need);

                    c.setVisible(true);
                    c.setTitle(mixin.getDisplayName());

                    final PlayerEntity player = Minecraft.getInstance().player;
                    final Formatting formatter = mixin.getFormatting();
                    c.setBarValues(
                        formatter.calculate(MathLib.max3(localNeed.getMin(), mixin.getMin(), mixin.getBarMin()), player),
                        formatter.calculate(MathLib.min3(localNeed.getMax(), mixin.getMax(), mixin.getBarMax()), player),
                        formatter.calculate(MathHelper.clamp(localNeed.getValue(), mixin.getMin(), mixin.getMax()), player),
                        mixin.getColor(),
                        formatter.calculate(MathLib.max3(localNeed.getLower(), mixin.getMin(), mixin.getBarMin()), player),
                        formatter.calculate(MathLib.min3(localNeed.getUpper(), mixin.getMax(), mixin.getBarMax()), player),
                        formatter::format
                    );

                    c.setLevel(localNeed.getLevel(), localNeed.hasLevels());

                    mixin.getIconTexture().setAndRecalculate(NeedExpressionContext.CURRENT_NEED_VALUE, localNeed::getValue);
                    c.setIcon(mixin.getIconTexture(), mixin.getIconOffsetX(), mixin.getIconOffsetY());
                }
        ));
    }

    public static void open() {
        Minecraft.getInstance().displayGuiScreen(new NeedDisplayScreen());
    }

    public static void returnToInventory() {
        Minecraft.getInstance().displayGuiScreen(new InventoryScreen(Minecraft.getInstance().player));
    }

    @Override
    public boolean mouseClicked(final double x, final double y, final int button) {
        if (mapButton(button) != MouseButtons.BACK) return super.mouseClicked(x, y, button);

        returnToInventory();
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        // Allow backspace to also back out
        if (key != 259 && !Minecraft.getInstance().gameSettings.keyBindInventory.matchesKey(key, scanCode)) return super.keyPressed(key, scanCode, modifiers);

        returnToInventory();
        return true;
    }

    @Override
    protected void init() {
        super.init();

        // Add a button to go back to the inventory:
        addButton(new InventoryButton(guiLeft + 319,guiTop + 1));
    }
}
