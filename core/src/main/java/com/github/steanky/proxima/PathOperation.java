package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import com.github.steanky.vector.Vec3I2ObjectMap;
import com.github.steanky.vector.Vec3IPredicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PathOperation {
    void init(int startX, int startY, int startZ, int destinationX, int destinationY, int destinationZ,
            @NotNull PathSettings settings);

    @Nullable PathResult step();

    @NotNull State state();

    int startX();

    int startY();

    int startZ();

    int currentX();

    int currentY();

    int currentZ();

    @NotNull Vec3I2ObjectMap<Node> graph();

    @NotNull Object syncTarget();

    enum State {
        UNINITIALIZED,
        INITIALIZED,
        COMPLETE;

        public boolean running() {
            return this == INITIALIZED;
        }
    }
}
