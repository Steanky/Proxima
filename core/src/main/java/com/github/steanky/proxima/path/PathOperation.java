package com.github.steanky.proxima.path;

import org.jetbrains.annotations.NotNull;

public interface PathOperation {
    void init(double startX, double startY, double startZ, int destinationX, int destinationY, int destinationZ,
            @NotNull PathSettings settings);

    boolean step();

    @NotNull PathResult makeResult();

    void cleanup();

    enum State {
        UNINITIALIZED,
        INITIALIZED,
        COMPLETE
    }
}
