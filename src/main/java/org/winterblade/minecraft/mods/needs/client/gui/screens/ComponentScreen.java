package org.winterblade.minecraft.mods.needs.client.gui.screens;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;
import org.winterblade.minecraft.mods.needs.client.gui.components.ComponentBase;

import javax.annotation.Nonnull;

@SuppressWarnings("WeakerAccess")
public abstract class ComponentScreen extends Screen {
    protected ComponentBase window;
    protected Texture texture;

    protected final int guiWidth;
    protected final int guiHeight;

    protected int guiLeft;
    protected int guiTop;

    protected static final MouseButtons[] buttons = {
        MouseButtons.LEFT,
        MouseButtons.RIGHT,
        MouseButtons.MIDDLE,
        MouseButtons.BACK,
        MouseButtons.FORWARD
    };

    public ComponentScreen(final ITextComponent titleIn, final int guiWidth, final int guiHeight) {
        super(titleIn);
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
    }

    @Override
    protected void init() {
        guiLeft = (width - guiWidth)/2;
        guiTop = (height - guiHeight)/2;
        super.init();
    }

    @Nonnull
    @Override
    public String getNarrationMessage() {
        // TODO: Read out needs list?
        return super.getNarrationMessage();
    }

    @Override
    public void render(final int mouseX, final int mouseY, final float partialTicks) {
        window.draw(guiLeft, guiTop);
        super.render(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isMouseOver(final double x, final double y) {
        return super.isMouseOver(x, y);
    }

    /**
     * Called when the mouse is clicked
     * @param x      The x coordinate of the mouse
     * @param y      The y coordinate of the mouse
     * @param button The button used, 0: left click, 1: right click, 2: middle, 3: 'back', 4: 'forward'
     * @return       True if this should count as a drag, false otherwise?
     */
    @Override
    public boolean mouseClicked(final double x, final double y, final int button) {
        return window.mouseClicked(x - guiLeft, y - guiTop, mapButton(button));
    }

    /**
     * Called when the mouse is released
     * @param x      The x coordinate of the mouse
     * @param y      The y coordinate of the mouse
     * @param button The button used, 0: left click, 1: right click, 2: middle, 3: 'back', 4: 'forward'
     * @return       True if this should count as a drag, false otherwise?
     */
    @Override
    public boolean mouseReleased(final double x, final double y, final int button) {
        return window.mouseReleased(x - guiLeft, y - guiTop, mapButton(button));
    }

    /**
     * Called when the mouse is released
     * @param x      The x coordinate of the mouse
     * @param y      The y coordinate of the mouse
     * @param button The button used, 0: left click, 1: right click, 2: middle, 3: 'back', 4: 'forward'
     * @param dragX  The end (start?) x position of the drag
     * @param dragY  The end (start?) y position of the drag
     * @return       True if this should count as a drag, false otherwise?
     */
    @Override
    public boolean mouseDragged(final double x, final double y, final int button, final double dragX, final double dragY) {
        return window.mouseDragged(x - guiLeft, y - guiTop, mapButton(button), dragX, dragY);
    }

    /**
     * Called when mouse is scrolled
     * @param x     The x coordinate of the mouse at the time of scroll
     * @param y     The y coordinate of the mouse at the time of scroll
     * @param lines The number of lines to scroll; -1 is down, 1 is up
     * @return      If scrolling occurred?
     */
    @Override
    public boolean mouseScrolled(final double x, final double y, final double lines) {
        return window.mouseScrolled(x - guiLeft, y - guiTop, lines);
    }

    private static MouseButtons mapButton(final int button) {
        return button < 5 ? buttons[button] : MouseButtons.UNKNOWN;
    }

    public enum MouseButtons {
        LEFT,
        RIGHT,
        MIDDLE,
        BACK,
        FORWARD,
        UNKNOWN
    }
}
