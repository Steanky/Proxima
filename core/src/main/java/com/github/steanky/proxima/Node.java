package com.github.steanky.proxima;

import com.github.steanky.toolkit.collection.Iterators;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

final class Node implements Comparable<Node> {
    final int x;
    final int y;
    final int z;

    float g;
    float h;

    final Movement movement;
    Node parent;

    int heapIndex;

    public Node(int x, int y, int z, float g, float h, @NotNull Movement movement, @Nullable Node parent) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.g = g;
        this.h = h;

        this.movement = movement;
        this.parent = parent;
        this.heapIndex = -1;
    }

    @NotNull @Unmodifiable List<Vec3I> reverseToVectorList() {
        Node prev = null;
        Node current = this;

        int size = 0;
        while (current != null) {
            //invert the linked list represented by this node, calculate size while we do this
            Node next = current.parent;
            current.parent = prev;

            prev = current;
            current = next;
            size++;
        }

        Vec3I[] vectors = new Vec3I[size];
        int i = 0;
        do {
            vectors[i++] = Vec3I.immutable(prev.x, prev.y, prev.z);
            prev = prev.parent;
        }
        while (prev != null);

        //faster than List.of, does not perform a defensive copy
        return Iterators.arrayView(vectors);
    }

    boolean onHeap() {
        return heapIndex > -1;
    }

    @Override
    public int compareTo(@NotNull Node o) {
        return Float.compare(g + h, o.g + o.h);
    }
}
