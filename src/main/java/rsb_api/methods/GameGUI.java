package rsb_api.methods;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;
import rsb_api.globval.WidgetIndices;
import rsb_api.globval.GlobalWidgetInfo;
import rsb_api.globval.enums.InterfaceTab;
import rsb_api.globval.enums.ViewportLayout;

/**
 * For internal use to find GUI components.
 *
 * @author GigiaJ
 */
@Slf4j
public class GameGUI {

    private MethodContext ctx;
    GameGUI(final MethodContext ctx) {
        this.ctx = ctx;
    }

    /**
     * @return The compasses <code>Widget</code>;otherwise null.
     */
    public synchronized Widget getCompass() {
        ViewportLayout layout = getViewportLayout();
        if (layout != null) {
            return switch (layout) {
                case FIXED_CLASSIC -> ctx.proxy.getWidget(
                        WidgetIndices.FixedClassicViewport.GROUP_INDEX,
                        WidgetIndices.FixedClassicViewport.MINIMAP_COMPASS_SPRITE);
                case RESIZABLE_CLASSIC -> ctx.proxy.getWidget(
                        WidgetIndices.ResizableClassicViewport.GROUP_INDEX,
                        WidgetIndices.ResizableClassicViewport.MINIMAP_COMPASS_SPRITE);
                case RESIZABLE_MODERN -> ctx.proxy.getWidget(
                        WidgetIndices.ResizableModernViewport.GROUP_INDEX,
                        WidgetIndices.ResizableModernViewport.MINIMAP_COMPASS_SPRITE);
            };
        }
        return null;
    }

    /**
     * @return The minimap <code>Widget</code>; otherwise null.
     */
    public synchronized Widget getMinimap() {
        ViewportLayout layout = getViewportLayout();
        if (layout != null) {
            return switch (layout) {
                case FIXED_CLASSIC -> ctx.proxy.getWidget(
                        WidgetIndices.FixedClassicViewport.GROUP_INDEX,
                        WidgetIndices.FixedClassicViewport.MINIMAP_CONTAINER);
                case RESIZABLE_CLASSIC -> ctx.proxy.getWidget(
                        WidgetIndices.ResizableClassicViewport.GROUP_INDEX,
                        WidgetIndices.ResizableClassicViewport.MINIMAP_CONTAINER);
                case RESIZABLE_MODERN -> ctx.proxy.getWidget(
                        WidgetIndices.ResizableModernViewport.GROUP_INDEX,
                        WidgetIndices.ResizableModernViewport.MINIMAP_CONTAINER);
            };
        }
        return null;
    }

    /**
     * @param interfaceTab The enumerated tab containing WidgetInfo of the tab.
     * @return The specified tab <code>Widget</code>; otherwise null.
     */

    public synchronized Widget getTab(final InterfaceTab interfaceTab) {
        ViewportLayout layout = getViewportLayout();
        if (layout != null) {
            GlobalWidgetInfo info = interfaceTab.getWidgetInfo(layout);
            return ctx.proxy.getWidget(info.getGroupId(), info.getChildId());
        }

        return null;
    }

    /**
     * Determines client viewport layout mode.
     *
     * @return <code>ViewportLayout</code>; otherwise <code>null</code>.
     */
    public ViewportLayout getViewportLayout() {
        Widget minimapOnFixedClassic = ctx.proxy.getWidget(
                WidgetIndices.FixedClassicViewport.GROUP_INDEX,
                WidgetIndices.FixedClassicViewport.MINIMAP_COMPASS_SPRITE);
        Widget minimapOnResizableClassic = ctx.proxy.getWidget(
                WidgetIndices.ResizableClassicViewport.GROUP_INDEX,
                WidgetIndices.ResizableClassicViewport.MINIMAP_COMPASS_SPRITE);
        Widget minimapOnResizableModern = ctx.proxy.getWidget(
                WidgetIndices.ResizableModernViewport.GROUP_INDEX,
                WidgetIndices.ResizableModernViewport.MINIMAP_COMPASS_SPRITE);

        if (minimapOnFixedClassic != null)
            return ViewportLayout.FIXED_CLASSIC;
        else if (minimapOnResizableClassic != null)
            return ViewportLayout.RESIZABLE_CLASSIC;
        else if (minimapOnResizableModern != null)
            return ViewportLayout.RESIZABLE_MODERN;
        return null;
    }
}
