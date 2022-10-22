package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {
    @Test
    void invertToList() {
        Node second = new Node(0, 0, 0, 0, 0, null);
        Node first = new Node(0, 0, 1, 0, 0, second);

        List<Vec3I> positions = first.reverseToVectorList();
        assertEquals(Vec3I.ORIGIN, positions.get(0));
        assertEquals(Vec3I.immutable(0, 0, 1), positions.get(1));
    }
}