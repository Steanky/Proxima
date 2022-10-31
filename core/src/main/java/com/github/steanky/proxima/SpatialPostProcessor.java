package com.github.steanky.proxima;

import com.github.steanky.toolkit.collection.Iterators;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class SpatialPostProcessor implements PathPostProcessor {
    private final Space space;

    public SpatialPostProcessor(@NotNull Space space) {
        this.space = Objects.requireNonNull(space);
    }

    @Override
    public @NotNull NavigationResult process(@NotNull PathResult pathResult) {
        Set<Vec3I> vectors = pathResult.vectors();
        Iterator<Vec3I> iterator = vectors.iterator();
        NavigationNode[] navigationNodes = new NavigationNode[vectors.size()];

        for (int i = 0; i < navigationNodes.length; i++) {
            navigationNodes[i] = processNode(iterator.next());
        }

        return new NavigationResult(pathResult, Iterators.arrayView(navigationNodes));
    }

    private NavigationNode processNode(Vec3I position) {
        Vec3I below = position.sub(Direction.DOWN.vector());
        Solid solid = space.solidAt(below);

        Vec3I newPosition;
        double offset;
        if (!solid.isFull()) {
            if (!solid.isEmpty()) {
                newPosition = position.sub(below);
                offset = solid.bounds().lengthY();
            }
            else {
                newPosition = position;
                offset = 0;
            }
        }
        else {
            newPosition = position;
            offset = 0;
        }

        return new NavigationNode(newPosition, offset);
    }
}
