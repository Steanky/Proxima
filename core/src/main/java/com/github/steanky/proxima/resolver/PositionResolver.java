package com.github.steanky.proxima.resolver;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.snapper.BasicNodeSnapper;
import com.github.steanky.proxima.snapper.NodeSnapper;
import com.github.steanky.proxima.space.Space;
import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PositionResolver {
    @NotNull Vec3I resolve(double x, double y, double z);

    default @NotNull Vec3I resolve(@NotNull Vec3D vec) {
        return resolve(vec.x(), vec.y(), vec.z());
    }

    PositionResolver FLOORED = Vec3I::immutableFloored;

    static @NotNull PositionResolver snapping(@NotNull Space space, double width, double height, double jumpHeight,
            double fallTolerance, double epsilon) {
        return new SnappingResolver(new BasicNodeSnapper(space, width, height, jumpHeight, fallTolerance, epsilon),
                new Direction[] {
                        Direction.NORTH,
                        Direction.SOUTH,
                        Direction.EAST,
                        Direction.WEST
                });
    }

    static @NotNull PositionResolver snapping(@NotNull Space space, double width, double height, double epsilon) {
        return new SnappingResolver(new BasicNodeSnapper(space, width, height, epsilon),
                new Direction[] {
                        Direction.NORTH,
                        Direction.SOUTH,
                        Direction.EAST,
                        Direction.WEST,
                        Direction.UP,
                        Direction.DOWN
                });
    }

    static @NotNull PositionResolver snapping(@NotNull NodeSnapper nodeSnapper,
            @NotNull Direction @NotNull ... directions) {
        return new SnappingResolver(nodeSnapper, directions);
    }
}
