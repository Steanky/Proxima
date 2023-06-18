package com.github.steanky.proxima.resolver;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.snapper.NodeSnapper;
import com.github.steanky.proxima.space.Space;
import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@FunctionalInterface
public interface PositionResolver {
    PositionResolver FLOORED = Vec3I::immutableFloored;

    static @NotNull PositionResolver seekBelow(@NotNull Space space, int searchHeight, double width, double epsilon) {
        return new ClosestBelow(space, searchHeight, width, epsilon);
    }

    static @NotNull PositionResolver asIfByInitial(@NotNull NodeSnapper snapper) {
        Objects.requireNonNull(snapper, "snapper");

        return new PositionResolver() {
            private static final Direction[] DIRECTIONS =
                    new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

            @Override
            public @Nullable Vec3I resolve(double x, double y, double z) {
                int isx = (int) Math.floor(x);
                int isy = (int) Math.floor(y);
                int isz = (int) Math.floor(z);

                long result = snapper.checkInitial(x, y, z, isx, isy, isz);
                if (result != NodeSnapper.FAIL) {
                    //if we can reach the starting node, don't explore any other options
                    return Vec3I.immutable(isx, isy, isz);
                }

                Vec3I closestVector = null;
                double closestVectorDistance = Double.POSITIVE_INFINITY;
                for (Direction direction : DIRECTIONS) {
                    int inx = (int) Math.floor(x + direction.x);
                    int iny = (int) Math.floor(y + direction.y);
                    int inz = (int) Math.floor(z + direction.z);

                    result = snapper.checkInitial(x, y, z, inx, iny, inz);
                    if (result != NodeSnapper.FAIL) {
                        double thisDistance = Vec3D.distanceSquared(x, y, z, inx + 0.5, iny, inz + 0.5);
                        if (thisDistance < closestVectorDistance) {
                            closestVectorDistance = thisDistance;
                            closestVector = Vec3I.immutable(inx, iny, inz);
                        }
                    }
                }

                return closestVector;
            }
        };
    }

    @Nullable Vec3I resolve(double x, double y, double z);

    default @Nullable Vec3I resolve(@NotNull Vec3D vec) {
        return resolve(vec.x(), vec.y(), vec.z());
    }
}
