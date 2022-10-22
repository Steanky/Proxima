package com.github.steanky.proxima;

import org.jetbrains.annotations.NotNull;

public interface PathOperation {
    void init(int startX, int startY, int startZ, int destinationX, int destinationY, int destinationZ);

    void step();

    @NotNull State state();

    @NotNull PathResult result();

    int startX();

    int startY();

    int startZ();

    int currentX();

    int currentY();

    int currentZ();

    enum State {
        UNINITIALIZED,
        INITIALIZED,
        COMPLETE
    }
}
