package com.github.steanky.proxima.node;

import com.github.steanky.vector.Vec3I;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

public class Node implements Comparable<Node> {
    public final int x;
    public final int y;
    public final int z;

    public float g;
    public final float h;

    public Node parent;

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

    public @NotNull Vec3I vector() {
        return Vec3I.immutable(x, y, z);
    }

    public int reversedAddAll(@NotNull Vec3I[] vectors) {
        Node prev = null;
        Node current = this;

        while (current != null) {
            Node next = current.parent;
            current.parent = prev;

            prev = current;
            current = next;
        }

        int i = 0;
        for (; i < vectors.length && prev != null; i++, prev = prev.parent) {
            vectors[i] = prev.vector();
        }

        return i;
    }

    public int size() {
        Node current = this;
        int size = 0;
        do {
            size++;
            current = current.parent;
        }
        while (current != null);

        return size;
    }

    public @NotNull @Unmodifiable Set<Vec3I> reverseToVectorSet() {
        if (parent == null) {
            return Set.of(vector());
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

        ObjectSet<Vec3I> set = new ObjectLinkedOpenHashSet<>(size);
        do {
            set.add(prev.vector());
            prev = prev.parent;
        }
        while (prev != null);

        return ObjectSets.unmodifiable(set);
    }

    public @NotNull Vec3I[] asVectorArray() {
        if (parent == null) {
            return new Vec3I[] { vector() };
        }

        Node current = this;
        int size = size();

        Vec3I[] array = new Vec3I[size];
        for (int i = 0; i < size && current != null; i++) {
            array[i] = current.vector();
            current = current.parent;
        }

        return array;
    }

    public boolean onHeap() {
        return heapIndex > -1;
    }

    @Override
    public int compareTo(@NotNull Node o) {
        return Float.compare(g + h, o.g + o.h);
    }
}
