package com.github.steanky.proxima.path;

import com.github.steanky.proxima.Navigator;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BasicNavigator implements Navigator {
    private final Pathfinder pathfinder;
    private final PathSettings pathSettings;

    private Future<PathResult> result;

    public BasicNavigator(@NotNull Pathfinder pathfinder, @NotNull PathSettings pathSettings) {
        this.pathfinder = Objects.requireNonNull(pathfinder);
        this.pathSettings = Objects.requireNonNull(pathSettings);
    }

    @Override
    public void navigate(double x, double y, double z, @NotNull PathTarget target) {
        if (result != null && !result.isDone()) {
            result.cancel(true);
            result = null;
        }

        result = pathfinder.pathfind(x, y, z, target, pathSettings);
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
