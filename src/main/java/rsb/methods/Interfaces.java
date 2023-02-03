package rsb.methods;

import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import rsb.globval.GlobalWidgetInfo;
import rsb.globval.WidgetIndices;
import rsb.wrappers.RSWidget;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Provides access to interfaces.
 */
public class Interfaces {

    private MethodContext ctx;
    Interfaces(final MethodContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Gets a widget corresponding to the index
     *
     * @param index The index of the interface.
     * @return The <code>RSWidget</code> for the given index.
     */
    public RSWidget get(final int index) {
        return new RSWidget(ctx, ctx.proxy.getWidget(index, 0));
    }

    /**
     * Gets a widget corresponding to the indexes
     *
     * @param index      The parent interface index
     * @param childIndex The component index
     * @return <code>RSWidget</code> for the given index and child index.
     */
    public RSWidget getComponent(final int index, final int childIndex) {
        return new RSWidget(ctx, ctx.proxy.getWidget(index, childIndex));
    }

    /**
     * Gets a widget corresponding to the widget info
     * @param info      The WidgetInfo for the corresponding RSWidget to retrieve
     * @return          The RSWidget for the WidgetInfo
     */
    public RSWidget getComponent(WidgetInfo info) {
        return new RSWidget(ctx, ctx.proxy.getWidget(info.getGroupId(), info.getChildId()));
    }

    /**
     * Gets a widget corresponding to the widget info
     * @param info      The WidgetInfo for the corresponding RSWidget to retrieve
     * @return          The RSWidget for the WidgetInfo
     */
    public RSWidget getComponent(GlobalWidgetInfo info) {
        return new RSWidget(ctx, ctx.proxy.getWidget(info.getGroupId(), info.getChildId()));
    }

    /**
     * Checks for the click here to continue widget
     * @return <code>true</code> if continue component is valid; otherwise
     *         <code>false</code>.
     */
    public boolean canContinue() {
        return getContinueComponent() != null;
    }

    /**
     * Clicks the click here to continue widget
     * @return <code>true</code> if continue component was clicked; otherwise
     *         <code>false</code>.
     */
    public boolean clickContinue() {
        RSWidget cont = getContinueComponent();
        return cont != null && cont.isValid() && cont.doClick(true);
    }

    /**
     * Gets the click here to continue widget
     * @return <code>RSWidget</code> containing "Click here to continue";
     *         otherwise null.
     */
    public RSWidget getContinueComponent() {
        Widget widget = ctx.proxy.getWidget(GlobalWidgetInfo.DIALOG_NPC_CONTINUE.getPackedId());
        if (widget != null && !widget.isHidden())
        {
            return new RSWidget(ctx, ctx.proxy.getWidget(GlobalWidgetInfo.DIALOG_NPC_CONTINUE.getGroupId(), GlobalWidgetInfo.DIALOG_NPC_CONTINUE.getChildId()));
        }
        return null;
    }

    /**
     * Clicks the dialogue option that contains the desired string.
     *
     * @param inter  The interface of the dialogue menu.
     * @param option The text we want to click.
     * @return <code>true</code> if the option was clicked; otherwise <code>false</code>
     *         .
     */
    public boolean clickDialogueOption(final RSWidget inter, String option) {
        // This is superfluous but it just makes life a little easier
        // so you don't have to look up the component.
        // Just grab the interface and the text you want to click.
        if (inter.isValid()) {
            option = option.toLowerCase();
            for (RSWidget c : inter.getComponents()) {
                if (c.getText().toLowerCase().contains(option)) {
                    return c.doClick();
                }
            }
        }
        return false;
    }

    /**
     * Clicks the dialogue option that contains the desired string.
     *
     * @param inter  The interface of the dialogue menu.
     * @param options The text we want to click.
     * @return <code>true</code> if the option was clicked; otherwise <code>false</code>
     *         .
     */
    public boolean clickDialogueOption(final RSWidget inter, String ... options) {
        // This is superfluous but it just makes life a little easier
        // so you don't have to look up the component.
        // Just grab the interface and the text you want to click.
        if (inter.isValid()) {
            for (RSWidget c : inter.getComponents()) {
                for (String option : options) {
                    if (c.getText().toLowerCase().contains(option.toLowerCase())) {
                        return c.doClick();
                    }
                }
            }
        }
        return false;
    }

    /**
     * Scrolls to the component
     *
     * @param component   component to scroll to
     * @param scrollBarID scrollbar to scroll with
     * @return true when scrolled successfully
     */
    public boolean scrollTo(RSWidget component, int scrollBarID) {
        RSWidget scrollBar = getComponent(scrollBarID, 0);
        return scrollTo(component, scrollBar);
    }

    /**
     * Scrolls to the component
     *
     * @param component component to scroll to
     * @param scrollBar scrollbar to scroll with
     * @return true when scrolled successfully
     */
    public boolean scrollTo(RSWidget component, RSWidget scrollBar) {
        // Check arguments
        if (component == null || scrollBar == null || !component.isValid()) {
            return false;
        }

        if (scrollBar.getComponents().length != 6) {
            return true; // no scrollbar, so probably not scrollable
        }

        // Find scrollable area
        RSWidget scrollableArea = component;
        while ((scrollableArea.getScrollableContentHeight() == 0)
                && (scrollableArea.getParentId() != -1)) {
            scrollableArea = getComponent(scrollableArea.getParentId(), 0);
        }

        // Check scrollable area
        if (scrollableArea.getId() == -1) {
            return true;
        }
        if (scrollableArea.getScrollableContentHeight() == 0) {
            return false;
        }

        // Get scrollable area height
        int areaY = scrollableArea.getAbsoluteY();
        int areaHeight = scrollableArea.getRealHeight();

        // Check if the component is already visible
        if ((component.getAbsoluteY() >= areaY)
                && (component.getAbsoluteY() <= areaY + areaHeight
                - component.getRealHeight())) {
            return true;
        }

        // Calculate scroll bar position to click
        RSWidget scrollBarArea = scrollBar.getComponent(0);
        int contentHeight = scrollableArea.getScrollableContentHeight();

        int pos = (int) ((float) scrollBarArea.getRealHeight() / contentHeight * (component
                .getRelativeY() + ctx.random(-areaHeight / 2, areaHeight / 2
                - component.getRealHeight())));
        if (pos < 0) // inner
        {
            pos = 0;
        } else if (pos >= scrollBarArea.getRealHeight()) {
            pos = scrollBarArea.getRealHeight() - 1; // outer
        }

        // Click on the scrollbar
        ctx.mouse.click(
                scrollBarArea.getAbsoluteX()
                        + ctx.random(0, scrollBarArea.getRealWidth()),
                scrollBarArea.getAbsoluteY() + pos, true);

        // Wait a bit
        ctx.sleepRandom(200, 400);

        // Scroll to it if we missed it
        while (component.getAbsoluteY() < areaY
                || component.getAbsoluteY() > (areaY + areaHeight - component
                .getRealHeight())) {
            boolean scrollUp = component.getAbsoluteY() < areaY;
            scrollBar.getComponent(scrollUp ? 4 : 5).doAction("");

            ctx.sleepRandom(100, 200);
        }

        // Return whether or not the component is visible now.
        return (component.getAbsoluteY() >= areaY)
                && (component.getAbsoluteY() <= areaY + areaHeight
                - component.getRealHeight());
    }

    /**
     * Waits for an interface to be closed/opened.
     *
     * @param iface The interface to wait for.
     * @param valid True if open, false if close.
     * @param timer Milliseconds to wait for the interface to open/close.
     * @return <code>true</code> if the interface was successfully closed/opened.
     */
    public boolean waitFor(RSWidget iface, boolean valid, int timer) {
        for (int w = 0; w < timer && iface.isValid() == valid ? true : false; w++) {
            ctx.sleepRandom(10, 25);
        }

        return iface.isValid() == valid ? true : false;
    }

    public boolean isInterfaceSubstantiated(int index) {
        return get(index).isVisible() && get(index).isValid() && get(index).isSelfVisible();
    }

    public boolean isValid(int index) {
        return get(index).isValid();
    }
}
