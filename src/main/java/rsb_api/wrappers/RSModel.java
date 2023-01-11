package rsb_api.wrappers;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.geometry.SimplePolygon;
import net.runelite.api.model.Jarvis;

import net.runelite.rsb.utils.Filter;
import rsb_api.methods.MethodContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A screen space model.
 */
@Slf4j
public class RSModel {

    protected Model model;
    protected int[] xPoints;
    protected int[] yPoints;
    protected int[] zPoints;
    protected int[] indices1;
    protected int[] indices2;
    protected int[] indices3;

    protected MethodContext ctx;
    public RSModel(MethodContext ctx, Model model) {
        this.ctx = ctx;

        this.model = model;

        if (model != null) {
            xPoints = model.getVerticesX();
            yPoints = model.getVerticesY();
            zPoints = model.getVerticesZ();
            indices1 = model.getFaceIndices1();
            indices2 = model.getFaceIndices2();
            indices3 = model.getFaceIndices3();
        }
    }

    protected int getLocalX() {
        return -1;
    }

    protected int getLocalY() {
        return -1;
    }

    /**
     * @param p A point on the screen
     * @return true of the point is within the bounds of the model
     */
    private boolean contains(Point p) {
        if (this == null) {
            return false;
        }
        Polygon[] triangles = getTriangles();
        if (triangles == null) {
            Polygon tilePoly = Perspective.getCanvasTilePoly(ctx.proxy,
                                                             new LocalPoint(getLocalX(), getLocalY()));
            int minX = 0, maxX = 0, minY = 0, maxY = 0;
            for (int i = 0; i < tilePoly.xpoints.length; i++) {
                if (i == 0) {
                    minX = tilePoly.xpoints[i];
                    maxX = tilePoly.xpoints[i];
                    minY = tilePoly.ypoints[i];
                    maxY = tilePoly.ypoints[i];
                }
                minX = (minX > tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : minX;
                maxX = (maxX < tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : maxX;
                minY = (minY > tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : minY;
                maxY = (maxY < tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : maxY;
            }
            for (int x = minX; x < maxX; x++) {
                for (int y = minY; y < maxY; y++) {
                    if (new Point(x, y).equals(p)) {
                        return true;
                    }
                }
            }
        }
        for (Polygon poly : triangles) {
            if (poly.contains(new java.awt.Point(p.getX(), p.getY()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clicks the RSModel.
     *
     * @param leftClick if true it left clicks.
     * @return true if clicked.
     */
    public boolean doClick(boolean leftClick) {
        try {
            ctx.mouse.move(getPoint());
            ctx.sleepRandom(30, 80);
            if (this.contains(ctx.mouse.getLocation())) {
                ctx.mouse.click(leftClick);
                return true;
            }

        } catch (Exception ignored) {
            log.warn("Model click error", ignored);
        }

        return false;
    }


    /**
     * Clicks the RSModel and clicks the menu action
     *
     * @param action the action to be clicked in the menu
     * @param target the option of the action to be clicked in the menu
     * @return true if clicked, false if failed.
     */
        public boolean doAction(String action, String target) {
        try {
            // only hop if it failed twice
            for (int i = 0; i < 5; i++) {
                ctx.mouse.move(getPoint());
                if (ctx.menu.doAction(action, target)) {
                    return true;
                }
            }

        } catch (Exception ignored) {
            log.warn("Model action perform error", ignored);
        }

        return false;
    }

    /**
     * Clicks the RSModel and clicks the menu action
     *
     * @param action the action to be clicked in the menu
     * @return true if clicked, false if failed.
     */
    public boolean doAction(String action) {
        return doAction(action, null);
    }

    /**
     * Returns a random screen point.
     *
     * @return A screen point, or Point(-1, -1) if the model is not on screen.
     * @see #getPointOnScreen()
     */
    public Point getPoint() {
        int len = model.getVerticesCount();
        int sever = ctx.random(0, len);
        Point point = getPointInRange(sever, len);
        if (point != null) {
            return point;
        }
        point = getPointInRange(0, sever);
        if (point != null) {
            return point;
        }
        return new Point(-1, -1);
    }

    /**
     * Returns all the screen points.
     *
     * @return All the points that are on the screen, if the model is not on the
     *         screen it will return null.
     */
    public Point[] getPoints() {
        if (this == null) {
            return null;
        }
        Polygon[] polys = getTriangles();
        ArrayList<Point> points = new ArrayList<>();
        if (polys == null) {
            Polygon tilePoly = Perspective.getCanvasTilePoly(ctx.proxy,
                                                             new LocalPoint(getLocalX(), getLocalY()));
            int minX = 0, maxX = 0, minY = 0, maxY = 0;
            for (int i = 0; i < tilePoly.xpoints.length; i++) {
                if ( i == 0) {
                    minX = tilePoly.xpoints[i];
                    maxX = tilePoly.xpoints[i];
                    minY = tilePoly.ypoints[i];
                    maxY = tilePoly.ypoints[i];
                }
                minX = (minX > tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : minX;
                maxX = (maxX < tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : maxX;
                minY = (minY > tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : minY;
                maxY = (maxY < tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : maxY;
            }
            for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                    points.add(new Point(x, y));
                }
            }
        }

        //Point[] points = new Point[polys.length * 3];
        int index = 0;
        for (Polygon poly : polys) {
            for (int i = 0; i < 3; i++) {
                points.add(index++, new Point(poly.xpoints[i], poly.ypoints[i]));
            }
        }
        return (Point[]) points.toArray();
    }

    /**
     * Gets a point on a model that is on screen.
     *
     * @return First point that it finds on screen else a random point on screen
     *         of an object.
     */
    public Point getPointOnScreen() {
        ArrayList<Point> list = new ArrayList<>();
        try {
            Polygon[] tris = getTriangles();
            if (tris == null) {
                Polygon tilePoly = Perspective.getCanvasTilePoly(ctx.proxy,
                                                                 new LocalPoint(getLocalX(), getLocalY()));
                int minX = 0, maxX = 0, minY = 0, maxY = 0;
                for (int i = 0; i < tilePoly.xpoints.length; i++) {
                    if ( i == 0) {
                        minX = tilePoly.xpoints[i];
                        maxX = tilePoly.xpoints[i];
                        minY = tilePoly.ypoints[i];
                        maxY = tilePoly.ypoints[i];
                    }
                    minX = (minX > tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : minX;
                    maxX = (maxX < tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : maxX;
                    minY = (minY > tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : minY;
                    maxY = (maxY < tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : maxY;
                }
                for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                        Point firstPoint = new Point(x, y);
                        if (ctx.calc.pointOnScreen(firstPoint)) {
                            return firstPoint;
                        } else {
                            list.add(firstPoint);
                        }
                    }
                }
            }
            for (Polygon p : tris) {
                for (int j = 0; j < p.xpoints.length; j++) {
                    Point firstPoint = new Point(p.xpoints[j], p.ypoints[j]);
                    if (ctx.calc.pointOnScreen(firstPoint)) {
                        return firstPoint;
                    } else {
                        list.add(firstPoint);
                    }
                }
            }
        } catch (Exception ignored) {
            log.warn("Model failed to get points on screen", ignored);
        }

        return list.size() > 0 ? list.get(ctx.random(0, list.size())) : null;
    }

    /**
     * Returns an array of triangles containing the screen points of this model.
     *
     * @return The on screen triangles of this model.
     */
    public Polygon[] getTriangles() {
        final int NO_MODEL = 2;
        if (model == null) {
            return null;
        }
        int count = model.getVerticesCount();

        int[] x2d = new int[count];
        int[] y2d = new int[count];

        int localX = getLocalX();
        int localY = getLocalY();

        final int tileHeight = Perspective.getTileHeight(ctx.proxy,
                                                         new LocalPoint(localX, localY), ctx.proxy.getPlane());

        // XXX is this right?  Looks very wrong?
        Perspective.modelToCanvas(ctx.proxy, count, localX, localY,
                                  tileHeight, getOrientation(),
                                  model.getVerticesX(), model.getVerticesZ(),
                                  model.getVerticesY(), x2d, y2d);
        ArrayList polys = new ArrayList(model.getFaceCount());

        int[] trianglesX = model.getFaceIndices1();
        int[] trianglesY = model.getFaceIndices2();
        int[] trianglesZ = model.getFaceIndices3();

        double averageTriangleLength = (trianglesX.length + trianglesY.length + trianglesZ.length) / 3;

        for (int triangle = 0; triangle < count; ++triangle) {
            if (averageTriangleLength <= NO_MODEL) {
                return null;
            }
            if (averageTriangleLength <= triangle) {
                break;
            }
            int[] xx =
                    {
                            x2d[trianglesX[triangle]], x2d[trianglesY[triangle]], x2d[trianglesZ[triangle]]
                    };

            int[] yy =
                    {
                            y2d[trianglesX[triangle]], y2d[trianglesY[triangle]], y2d[trianglesZ[triangle]]
                    };

            polys.add(new Polygon(xx, yy, 3));
        }
        return (Polygon[]) polys.toArray(new Polygon[0]);
    }

    /**
     * Moves the mouse onto the RSModel.
     */
    public void hover() {
        ctx.mouse.move(getPoint());
    }

    /**
     * Returns true if the provided object is an RSModel with the same x, y and
     * z points as this model. This method compares all of the values in the
     * three vertex arrays.
     *
     * @return <code>true</code> if the provided object is a model with the same
     *         points as this.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof RSModel) {
            RSModel m = (RSModel) o;
            return Arrays.equals(indices1, m.indices1)
                    && Arrays.equals(xPoints, m.xPoints)
                    && Arrays.equals(yPoints, m.yPoints)
                    && Arrays.equals(zPoints, m.zPoints);
        }
        return false;
    }

    protected Point getPointInRange(int start, int end) {
        int locX = getLocalX();
        int locY = getLocalY();
        int height = ctx.calc.tileHeight(locX, locY);
        Polygon[] triangles = this.getTriangles();
        if (triangles == null) {
            Polygon tilePoly = Perspective.getCanvasTilePoly(ctx.proxy,
                                                             new LocalPoint(getLocalX(), getLocalY()));
            int minX = 0, maxX = 0, minY = 0, maxY = 0;
            for (int i = 0; i < tilePoly.xpoints.length; i++) {
                if (i == 0) {
                    minX = tilePoly.xpoints[i];
                    maxX = tilePoly.xpoints[i];
                    minY = tilePoly.ypoints[i];
                    maxY = tilePoly.ypoints[i];
                }

                minX = (minX > tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : minX;
                maxX = (maxX < tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : maxX;
                minY = (minY > tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : minY;
                maxY = (maxY < tilePoly.xpoints[i]) ? tilePoly.xpoints[i] : maxY;
            }

            // XXX this needs to centre, and then return normally distrubted.
            return (new Point(ctx.random(minX, maxX),
                              ctx.random(minY, maxY)));
        }

        // choose a number between start and end, set i
        int t = ctx.random(start, end);
        if (t < triangles.length) {
            Polygon tri = triangles[t];
            if (tri.npoints == 3) {
                int minX = 0, maxX = 0, minY = 0, maxY = 0;
                for (int i = 0; i < tri.npoints; i++) {
                    int x = tri.xpoints[i];
                    int y = tri.ypoints[i];

                    if (i == 0) {
                        minX = maxX = x;
                        minY = maxY = y;
                    }

                    minX = Math.min(x, minX);
                    maxX = Math.max(x, maxX);
                    minY = Math.min(y, minY);
                    maxY = Math.max(y, maxY);
                }

                //log.info(String.format("HERE3 tri (# = %d, i = %d)", triangles.length, t));
                return new Point((minX + maxX) / 2, (minY + maxY) / 2);
            }

            int n = ctx.random(0, tri.npoints);
            if (n < tri.npoints) {
                //log.info(String.format("HERE2 tri (t# = %d, v# = %d, t = %d, n = %d)",
                //                       triangles.length, tri.npoints, t, n));
                return new Point(tri.xpoints[n], tri.ypoints[n]);
            }
        }

        return null;
    }

    public int getOrientation() {
        return 0;
    }

    public int getIndexCount() {
        return (model != null) ? model.getFaceCount() : 0;
    }

    public int getVertexCount() {
        return (model != null) ? model.getVerticesCount() : 0;
    }

    public Model getModel() {
        return model;
    }

    public Polygon xgetConvexHull() {
        // int ex = model.getExtremeX();
        // if (ex == -1)
        // {
        //  // dynamic models don't get stored when they render where this normally happens
        //  model.calculateBoundsCylinder();
        //  model.calculateExtreme(0);
        //  ex = model.getExtremeX();
        // }

        // int x1 = model.getCenterX();
        // int y1 = model.getCenterZ();
        // int z1 = model.getCenterY();

        // int ey = model.getExtremeZ();
        // int ez = model.getExtremeY();

        // int x2 = x1 + ex;
        // int y2 = y1 + ey;
        // int z2 = z1 + ez;

        // x1 -= ex;
        // y1 -= ey;
        // z1 -= ez;

        // int[] xa = new int[]{
        //      x1, x2, x1, x2,
        //      x1, x2, x1, x2
        // };
        // int[] ya = new int[]{
        //      y1, y1, y2, y2,
        //      y1, y1, y2, y2
        // };
        // int[] za = new int[]{
        //      z1, z1, z1, z1,
        //      z2, z2, z2, z2
        // };

        // int[] x2d = new int[8];
        // int[] y2d = new int[8];

        // var h = Perspective.getTileHeight(ctx.proxy,
        //                                new LocalPoint(getLocalX(), getLocalY()),
        //                                ctx.proxy.getPlane());
        // Perspective.modelToCanvas(ctx.proxy, 8, getLocalX(),
        //                        getLocalY(), h, getOrientation(), xa, ya, za, x2d, y2d);
        // SimplePolygon simplePolygon = Jarvis.convexHull(x2d, y2d);
        // return new Polygon(simplePolygon.getX(),
        //                 simplePolygon.getY(),
        //                 simplePolygon.size());
        return null;
    }
}
