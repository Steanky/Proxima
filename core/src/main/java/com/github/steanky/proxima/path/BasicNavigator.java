package com.github.steanky.proxima.path;

import com.github.steanky.proxima.Navigator;
import com.github.steanky.proxima.PositionResolver;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BasicNavigator implements Navigator {
    private final Pathfinder pathfinder;
    private final PositionResolver originResolver;
    private final PositionResolver destinationResolver;
    private final PathSettings pathSettings;

    private Future<PathResult> result;

    public BasicNavigator(@NotNull Pathfinder pathfinder, @NotNull PositionResolver originResolver,
            @NotNull PositionResolver destinationResolver,
            @NotNull PathSettings pathSettings) {
        this.pathfinder = Objects.requireNonNull(pathfinder);
        this.originResolver = Objects.requireNonNull(originResolver);
        this.destinationResolver = Objects.requireNonNull(destinationResolver);
        this.pathSettings = Objects.requireNonNull(pathSettings);
    }

    @Override
    public void navigate(double x, double y, double z, double toX, double toY, double toZ) {
        Vec3I origin = originResolver.resolve(x, y, z);
        Vec3I destination = destinationResolver.resolve(toX, toY, toZ);
        if (result != null && !result.isDone()) {
            result.cancel(true);
            result = null;
        }

        result = pathfinder.pathfind(origin.x(), origin.y(), origin.z(), destination.x(), destination.y(),
                destination.z(), pathSettings);
    }

    @Override
    public boolean navigationComplete() {
        return result != null && result.isDone();
    }

    @Override
    public @NotNull PathResult getResult() {
        if (result == null || !result.isDone()) {
            throw new IllegalStateException("Result not yet complete");
        }

        try {
            PathResult pathResult = result.get();
            result = null;

            return pathResult;
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void cancel() {
        if (result != null && !result.isDone() && !result.isCancelled()) {
            result.cancel(true);
        }

        result = null;
    }
}
