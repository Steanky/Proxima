package com.github.steanky.proxima;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class BasicPathfinder implements Pathfinder {
    private final Supplier<? extends PathHandler> handlerSupplier;
    private final Map<Object, PathHandler> handlerMap;

    public BasicPathfinder(@NotNull Supplier<? extends PathHandler> handlerSupplier) {
        this.handlerSupplier = Objects.requireNonNull(handlerSupplier);
        this.handlerMap = new ConcurrentHashMap<>();
    }

    @Override
    public @NotNull Future<PathResult> pathfind(int x, int y, int z, int destX, int destY, int destZ,
            @NotNull PathSettings settings, @NotNull Object regionKey) {

        PathHandler handler = handlerMap.get(Objects.requireNonNull(regionKey));
        if (handler == null) {
            throw new IllegalArgumentException("Region '" + regionKey + "' not registered");
        }

        return handler.pathfind(x, y, z, destX, destY, destZ, settings);
    }

    @Override
    public void registerRegion(@NotNull Object regionKey) {
        handlerMap.put(Objects.requireNonNull(regionKey), handlerSupplier.get());
    }

    @Override
    public void deregisterRegion(@NotNull Object regionKey) {
        handlerMap.remove(Objects.requireNonNull(regionKey));
    }
}
