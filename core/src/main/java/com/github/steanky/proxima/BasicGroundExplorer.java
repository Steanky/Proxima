package com.github.steanky.proxima;

import org.jetbrains.annotations.NotNull;

public class BasicGroundExplorer implements Explorer {
    private static final Direction[] WALK_DIRECTIONS = new Direction[] {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST
    };

    public BasicGroundExplorer() {

    }

    @Override
    public void exploreEach(@NotNull Node node, @NotNull NodeHandler handler) {

    }
}
