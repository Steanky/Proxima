package com.github.steanky.proxima;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

final class Node implements Comparable<Node> {
    final int x;
    final int y;
    final int z;

    float g;
    float h;

    Node parent;

    int heapIndex;

    public Node(int x, int y, int z, float g, float h, @Nullable Node parent) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.g = g;
        this.h = h;

        this.parent = parent;
        this.heapIndex = -1;
    }

    @NotNull Node reverse() {
        Node prev = null;
        Node current = this;
        while (current != null) {
            Node next = current.parent;
            current.parent = prev;

            prev = current;
            current = next;
        }

        return Objects.requireNonNull(prev);
    }

    boolean onHeap() {
        return heapIndex > 0;
    }

    @Override
    public int compareTo(@NotNull Node o) {
        return Float.compare(g + h, o.g + o.h);
    }
}
