package com.github.steanky.proxima.solid;

import com.github.steanky.vector.Bounds3D;
import com.github.steanky.vector.Vec3D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {
    @Test
    void closeCollision() {
        Vec3D origin = Vec3D.immutable(-83.16250001192093, 12, -62.600000011920926);
        Vec3D lengths = Vec3D.immutable(0.6, 3.05, 0.6);
        Vec3D direction = Vec3D.immutable(0.36250001192092896, 0.0, -0.19999998807907104);
        Bounds3D childBounds = Bounds3D.immutable(0.437, 0.0, 0.0, 0.12500000000000006, 1.0, 1.0);

        Solid solid = Solid.of(childBounds);
        long result = Util.minMaxCollision(solid, -83, 12, -63, origin.x(), origin.y(), origin.z(), lengths.x(),
                lengths.y(),
                lengths.z(), direction.x(), direction.y(), direction.z(), 1E-6);

        float lowest = Solid.lowest(result);
        float highest = Solid.highest(result);

        assertEquals(0, lowest);
        assertEquals(1, highest);
    }

}