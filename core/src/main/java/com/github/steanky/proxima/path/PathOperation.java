package com.github.steanky.proxima.path;

import org.jetbrains.annotations.NotNull;

public interface PathOperation {
    void init(int startX, int startY, int startZ, int destinationX, int destinationY, int destinationZ,
            @NotNull PathSettings settings, double yOffset);

    boolean step();

    @NotNull PathResult makeResult();

    void cleanup();

    enum State {
        UNINITIALIZED,
        INITIALIZED,
        COMPLETE
    }
}
