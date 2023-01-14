package com.github.steanky.proxima.node;

import com.github.steanky.toolkit.collection.Containers;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class Node implements Comparable<Node> {
    public final int x;
    public final int y;
    public final int z;
    public final float h;
    public final float blockOffset;
    public final float jumpOffset;
    public float g;
    public Node parent;

    int heapIndex;

    public Node(int x, int y, int z, float g, float h, @Nullable Node parent, float blockOffset, float jumpOffset) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.g = g;
        this.h = h;

        this.blockOffset = blockOffset;

        this.parent = parent;
        this.heapIndex = -1;

        this.jumpOffset = jumpOffset;
    }

    public Node(int x, int y, int z, float g, float h, @Nullable Node parent, float blockOffset) {
        this(x, y, z, g, h, parent, blockOffset, 0F);
    }

    public @NotNull @Unmodifiable List<Node> reverseToNavigationList() {
        if (parent == null) {
            return List.of(this);
        }

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

        Node[] nodes = new Node[size];
        int i = 0;
        do {
            nodes[i++] = prev;
            prev = prev.parent;
        } while (prev != null);

        //Containers.arrayView instead of List.of so we don't need to copy the array
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
