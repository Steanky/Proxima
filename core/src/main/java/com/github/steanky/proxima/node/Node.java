package com.github.steanky.proxima.node;

import com.github.steanky.toolkit.collection.Containers;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class Node implements Comparable<Node> {
    public final int x;
    public final int y;
    public final int z;
    public final float h;
    public final float blockOffset;
    public final float jumpOffset;
    public float g;

    public Node parent;
    public int length;

    int heapIndex;

    public Node(int x, int y, int z, float g, float h, float blockOffset, float jumpOffset) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.g = g;
        this.h = h;

        this.blockOffset = blockOffset;

        this.heapIndex = -1;

        this.jumpOffset = jumpOffset;
        this.length = 1;
    }

    public Node(int x, int y, int z, float g, float h, float blockOffset) {
        this(x, y, z, g, h, blockOffset, 0F);
    }

    public @NotNull Node reverse() {
        if (parent == null) {
            return this;
        }

        Node prev = null;
        Node current = this;

        while (current != null) {
            //invert the linked list represented by this node, calculate size while we do this
            Node next = current.parent;
            current.parent = prev;

            prev = current;
            current = next;
        }

        return prev;
    }

    public void forEach(@NotNull Consumer<? super Node> nodeConsumer) {
        Objects.requireNonNull(nodeConsumer);

        Node current = this;
        while (current != null) {
            nodeConsumer.accept(current);
            current = current.parent;
        }
    }

    /**
     * Converts the linked list represented by this {@link Node} into an unmodifiable random access {@link List}. This
     * operation is "slow", operating in {@code O(2n)} time, where {@code n} is the size of the linked list. It is
     * intended for debugging only.
     *
     * @return an unmodifiable list of nodes
     */
    @ApiStatus.Internal
    public @NotNull @Unmodifiable List<Node> toList() {
        Node current = this;
        int size = 0;
        while (current != null) {
            current = current.parent;
            size++;
        }

        Node[] nodes = new Node[size];
        current = this;
        int i = 0;
        while (current != null) {
            nodes[i++] = current;
            current = current.parent;
        }

        return Containers.arrayView(nodes);
    }

    public boolean onHeap() {
        return heapIndex > -1;
    }

    public boolean positionEquals(@NotNull Node other) {
        return positionEquals(other.x, other.y, other.z);
    }

    public boolean positionEquals(@NotNull Vec3I other) {
        return positionEquals(other.x(), other.y(), other.z());
    }

    public boolean positionEquals(int x, int y, int z) {
        return this.x == x && this.y == y && this.z == z;
    }

    @Override
    public int compareTo(@NotNull Node o) {
        return Float.compare(g + h, o.g + o.h);
    }

    @Override
    public String toString() {
        return "Node{x=" + x + ", y=" + y + ", z=" + z + ", g=" + g + ", h=" + h + ", yOffset=" + blockOffset +
                ", jumpOffset=" + jumpOffset + ", heapIndex=" + heapIndex + "}";
    }
}
