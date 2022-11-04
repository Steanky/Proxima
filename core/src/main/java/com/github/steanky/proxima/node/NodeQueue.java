package com.github.steanky.proxima.node;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class NodeQueue implements PriorityQueue<Node> {
    private static final int DEFAULT_INITIAL_CAPACITY = 32;

    private Node[] heap;
    private int size;

    /**
     * Creates a new NodeQueue using the default initial capacity (32).
     */
    public NodeQueue() {
        this.heap = new Node[DEFAULT_INITIAL_CAPACITY];
    }

    /**
     * Creates a new NodeQueue with the provided initial capacity.
     *
     * @param initialCapacity the initial capacity
     * @throws IllegalArgumentException if initialCapacity is negative
     */
    public NodeQueue(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Invalid capacity " + initialCapacity);
        }

        if (initialCapacity == 0) {
            heap = (Node[]) ObjectArrays.EMPTY_ARRAY;
        }
        else {
            heap = new Node[initialCapacity];
        }
    }

    @Override
    public void enqueue(@NotNull Node node) {
        Objects.requireNonNull(node, "node");
        if (size == heap.length) {
            heap = ObjectArrays.grow(heap, size + 1);
        }

        heap[size++] = node;
        NodeHeaps.upHeap(heap, size - 1);
    }

    @Override
    public @NotNull Node dequeue() {
        if (size == 0) {
            throw new NoSuchElementException();
        }

        Node result = heap[0];
        heap[0] = heap[--size];
        heap[size] = null;
        if (size != 0) {
            NodeHeaps.downHeap(heap, size, 0);
        }

        result.heapIndex = -1;
        return result;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            heap[i].heapIndex = -1;
            heap[i] = null;
        }
        size = 0;
    }

    @Override
    public Node first() {
        if (size == 0) {
            throw new NoSuchElementException();
        }

        return heap[0];
    }

    @Override
    public void changed() {
        NodeHeaps.downHeap(heap, size, 0);
    }

    @Override
    public Comparator<? super Node> comparator() {
        return null;
    }

    /**
     * If the given node is already in the heap, update its position, otherwise enqueue it.
     *
     * @param node the node to update or enqueue
     */
    public void enqueueOrUpdate(@NotNull Node node) {
        Objects.requireNonNull(node, "node");
        if (node.onHeap()) {
            //if we're already on the heap, call changed to update its position in-place
            changed(node);
        }
        else {
            //otherwise, add it to the heap
            enqueue(node);
        }
    }

    public void changed(@NotNull Node node) {
        int pos = node.heapIndex;
        if (pos < 0 || pos > size) {
            throw new IllegalArgumentException("Node " + node + " does not belong to the queue");
        }

        int newPos = NodeHeaps.upHeap(heap, pos);
        NodeHeaps.downHeap(heap, size, newPos);
    }

    /**
     * Trims the internal array to size.
     */
    public void trim() {
        heap = ObjectArrays.trim(heap, size);
    }

    /**
     * Trims the internal array to either the current size, or n, whichever is larger. Can only reduce the capacity of
     * the array.
     *
     * @param n the size to trim to if larger than this queue's size
     */
    public void trim(int n) {
        heap = ObjectArrays.trim(heap, Math.max(size, n));
    }
}
