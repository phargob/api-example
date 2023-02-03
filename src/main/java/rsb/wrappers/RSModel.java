package rsb.wrappers;

import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.model.Jarvis;

import rsb.methods.MethodContext;

import java.awt.Shape;
import java.awt.Rectangle;
import java.util.Random;


@Slf4j
public class RSModel {

    protected Model model;

    protected MethodContext ctx;
    public RSModel(MethodContext ctx, Model model) {
        this.ctx = ctx;
        this.model = model;
    }

    protected int getLocalX() {
        return -1;
    }

    protected int getLocalY() {
        return -1;
    }

    protected int getOrientation() {
        return 0;
    }

    public Model getModel() {
        return model;
    }

    public boolean doClick(boolean leftClick) {
        for (int i = 0; i < 3; i++) {
            if (this.contains(ctx.mouse.getLocation())) {
                // XXX no way to really determine if this succeeds or not
                ctx.mouse.click(leftClick);
                return true;
            }

            ctx.mouse.move(getPoint());
        }

        return false;
    }

    public void doHover() {
        ctx.mouse.move(getPoint());
    }

    public boolean doAction(String action, String target) {
        for (int i = 0; i < 5; i++) {
            if (this.contains(ctx.mouse.getLocation()) &&
                ctx.menu.doAction(action, target)) {
                return true;
            }

            ctx.mouse.move(getPoint());
        }

        return false;
    }

    public boolean doAction(String action) {
        return doAction(action, null);
    }

    public Point getPoint() {
        Shape s = getConvexHull();
        if (s != null) {
            Random rand = new Random();
            Rectangle r = s.getBounds();
            double midW = r.getWidth() / 2;
            double midH = r.getHeight() / 2;
            double x, y;
            for (int i=0; i<100; i++) {
                x = r.getX() + midW + midW * 0.5 * rand.nextGaussian();
                y = r.getY() + midH + midH * 0.5 * rand.nextGaussian();

                if (s.contains(x, y)) {
                    return new Point((int) x, (int) y);
                }
            }
        }

        return new Point(-1, -1);
    }

    private Shape getConvexHull() {
        if (model == null) {
            return null;
        }

        int locX = getLocalX();
        int locY = getLocalY();
        int tileHeight = Perspective.getTileHeight(ctx.proxy,
                                                   new LocalPoint(locX, locY),
                                                   ctx.proxy.getPlane());

        int count = model.getVerticesCount();
        int[] x2d = new int[count];
        int[] y2d = new int[count];

        Perspective.modelToCanvas(ctx.proxy, count,
                                  locX, locY, tileHeight, getOrientation(),
                                  model.getVerticesX(), model.getVerticesZ(), model.getVerticesY(),
                                  x2d, y2d);

        return Jarvis.convexHull(x2d, y2d);
    }

    private boolean contains(Point p) {
        var s = getConvexHull();
        if (s != null) {
            return s.contains(p.getX(), p.getY());
        }

        return false;
    }

}
