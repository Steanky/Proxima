package com.github.steanky.proxima.snapper;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.proxima.space.Space;
import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class BasicFlightNodeSnapper implements FlightNodeSnapper {
    private final double width;
    private final double halfWidth;
    private final double wDiff;
    private final double height;

    private final int ceilHeight;
    private final int searchHeight;

    private final boolean fullWidth;
    private final boolean fullHeight;

    private final int halfBlockWidth;

    private final Space space;

    public BasicFlightNodeSnapper(double width, double height, @NotNull Space space, double epsilon) {
        validate(width, height, epsilon);

        int rWidth = (int) Math.rint(width);

        this.fullWidth = width == rWidth && (rWidth & 1) != 0;
        this.fullHeight = height == Math.rint(height);

        //silly, totally unnecessary way to add 1 to a number only if it is even
        int blockWidth = ((int) Math.ceil(width)) | 1;

        this.width = width - epsilon;
        this.halfWidth = width / 2;
        this.wDiff = ((blockWidth - width) / 2) - (epsilon / 2);
        this.height = height - epsilon;
        this.ceilHeight = (int) Math.ceil(height);
        this.searchHeight = (int) Math.ceil(height);

        this.halfBlockWidth = blockWidth >> 1;
        this.space = Objects.requireNonNull(space);
    }

    private static void validate(double width, double height, double epsilon) {
        //width must be non-negative and finite
        if (width < 0 || !Double.isFinite(width)) {
            throw new IllegalArgumentException("Invalid width: " + width);
        }

        //height must be non-negative and finite
        if (height < 0 || !Double.isFinite(height)) {
            throw new IllegalArgumentException("Invalid height: " + height);
        }

        //epsilon must be positive and finite
        if (epsilon < 0 || !Double.isFinite(epsilon)) {
            throw new IllegalArgumentException("Invalid epsilon: " + epsilon);
        }

        //epsilon can't reduce width to 0 or lower
        if (epsilon >= width) {
            throw new IllegalArgumentException("Epsilon must not be larger than or equal to width");
        }

        //epsilon can't reduce height to 0 or lower
        if (epsilon >= height) {
            throw new IllegalArgumentException("Epsilon must not be larger than or equal to height");
        }
    }

    @Override
    public boolean snap(@NotNull Direction direction, int nodeX, int nodeY, int nodeZ, double nodeOffset) {
        int dx = direction.x;
        int dy = direction.y;
        int dz = direction.z;

        int nx = nodeX + dx;
        int nz = nodeZ + dz;

        double exactY = nodeY + nodeOffset;

        if (dy == 0) {
            int actualSearchHeight = nodeOffset == 0 ? searchHeight : ((int)Math.ceil(nodeOffset + height) - nodeY) + 1;
            for (int i = 0; i < actualSearchHeight; i++) {
                int y = nodeY + i;

                for (int dh = -halfBlockWidth; dh <= halfBlockWidth; dh++) {
                    if (!fullWidth) {
                        //first block we may encounter
                        int x = dx == 0 ? nx + dh : nodeX + (dx * halfBlockWidth);
                        int z = dz == 0 ? nz + dh : nodeZ + (dz * halfBlockWidth);

                        Solid solid = space.solidAt(x, y, z);

                        if (!solid.isEmpty() && !solid.isFull()) {
                            double ax = (((nodeX + 0.5) - x) - halfWidth) + (dx > 0 ? width : wDiff * dx);
                            double ay = exactY - y;
                            double az = (((nodeZ + 0.5) - z) - halfWidth) + (dz > 0 ? width : wDiff * dz);

                            double lx = dx == 0 ? width : wDiff;
                            double lz = dz == 0 ? width : wDiff;

                            List<Bounds3D> children = solid.children();
                            for (int j = 0; j < children.size(); j++) {
                                if (children.get(j).overlaps(ax, ay, az, lx, height, lz)) {
                                    return false;
                                }
                            }
                        }
                    }

                    int x = dx == 0 ? nx + dh : nx + (dx * halfBlockWidth);
                    int z = dz == 0 ? nz + dh : nz + (dz * halfBlockWidth);

                    Solid solid = space.solidAt(x, y, z);
                    if (solid.isEmpty()) {
                        continue;
                    }

                    if (solid.isFull()) {
                        return false;
                    }

                    double ax = ((nodeX + 0.5) - x) - halfWidth;
                    double ay = exactY - y;
                    double az = ((nodeZ + 0.5) - z) - halfWidth;

                    double lx = width + Math.abs(dx);
                    double lz = width + Math.abs(dz);

                    if (dx < 0) {
                        ax += dx;
                    }

                    if (dz < 0) {
                        az += dz;
                    }

                    List<Bounds3D> children = solid.children();
                    for (int j = 0; j < children.size(); j++) {
                        if (children.get(j).overlaps(ax, ay, az, lx, height, lz)) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }

        int y = dy == 1 ? ceilHeight + nodeY : nodeY - 1;
        boolean checkOverlap = (dy == 1 && !fullHeight) || nodeOffset != 0;

        for (int dex = -halfBlockWidth; dex <= halfBlockWidth; dex++) {
            for (int dez = -halfBlockWidth; dez <= halfBlockWidth; dez++) {
                int x = nodeX + dex;
                int z = nodeZ + dez;

                if (checkOverlap) {
                    int py = y - dy;
                    Solid solid = space.solidAt(x, py, z);

                    if (!solid.isEmpty() && !solid.isFull() && collidesVertically(nodeX, nodeZ, x, py, z, dy, exactY,
                            solid)) {
                        //partial solid above or below us
                        return false;
                    }
                }

                Solid solid = space.solidAt(x, y, z);
                if (solid.isEmpty()) {
                    continue;
                }

                if (solid.isFull() || collidesVertically(nodeX, nodeZ, x, y, z, dy, exactY, solid)) {
                    //always collide with full solids
                    return false;
                }
            }
        }

        return true;
    }

    private boolean collidesVertically(int nodeX, int nodeZ, int x, int y, int z, int dy, double exactY, Solid solid) {
        double ax = (nodeX + 0.5) - halfWidth;
        double az = (nodeZ + 0.5) - halfWidth;

        double mx = (nodeX + 0.5) + halfWidth;
        double mz = (nodeZ + 0.5) + halfWidth;

        List<Bounds3D> children = solid.children();
        for (int i = 0; i < children.size(); i++) {
            Bounds3D child = children.get(i);

            if (dy == 1 ? (exactY + height + 1 < y + child.originY()) :
                    (exactY - 1 > y + child.maxY())) {
                //we won't collide with this child, it's too far below or above us
                continue;
            }

            if (x + child.originX() < mx && z + child.originZ() < mz && x + child.maxX() > ax &&
                    z + child.maxZ() > az) {
                //collision found
                return true;
            }
        }

        return false;
    }
}