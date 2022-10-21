package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

public interface PathOperation {
    void step();

    @NotNull State state();

    @NotNull Vec3I start();

    int currentX();

    int currentY();

    int currentZ();

    enum State {
        IN_PROGRESS,
        SUCCEEDED,
        FAILED;

        public boolean complete() {
            return this == SUCCEEDED || this == FAILED;
        }
    }
}
