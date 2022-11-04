package com.github.steanky.proxima.path;

import com.github.steanky.proxima.node.Node;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;

public interface PathOperation {
    void init(int startX, int startY, int startZ, int destinationX, int destinationY, int destinationZ,
            @NotNull PathSettings settings);

    boolean step();

    @NotNull State state();

    int startX();

    int startY();

    int startZ();

    @NotNull Node current();

    @NotNull Vec3I2ObjectMap<Node> graph();

    @NotNull PathResult makeResult();

    void cleanup();

    enum State {
        UNINITIALIZED,
        INITIALIZED,
        COMPLETE
    }
}
