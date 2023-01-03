package com.github.steanky.proxima.space;

import com.github.steanky.vector.Vec3IFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.locks.StampedLock;

public class ConcurrentChunkedSpatialCache<T> implements SpatialCache<T> {
    private final StampedLock stampedLock;
    private final Long2ObjectLinkedOpenHashMap<Chunk<T>> chunkMap;

    private record Chunk<T>(Int2ObjectMap<T> map, StampedLock lock) {

    }

    public ConcurrentChunkedSpatialCache() {
        this.stampedLock = new StampedLock();
        this.chunkMap = new Long2ObjectLinkedOpenHashMap<>();
    }

    private long chunk(int x, int z) {
        return (long) x << 32 | z;
    }

    private int chunkRelative(int x, int y, int z) {
        return ((x & 15) << 15) | ((y & 2047) << 4) | (z & 15);
    }

    private Chunk<T> getLocal(long chunk) {
        long readStamp = stampedLock.readLock();
        try {
            return chunkMap.get(chunk);
        }
        finally {
            stampedLock.unlockRead(readStamp);
        }
    }

    private Chunk<T> computeLocalIfAbsent(long chunk) {
        //get the chunk,
        Chunk<T> local = getLocal(chunk);

        if (local == null) {
            long writeStamp = stampedLock.writeLock();

            try {
                if (chunkMap.containsKey(chunk)) {
                    //ChunkLocal may have been added by another thread after we checked, but before we got the lock
                    local = chunkMap.get(chunk);
                }
                else {
                    local = new Chunk<>(new Int2ObjectOpenHashMap<>(), new StampedLock());
                    chunkMap.put(chunk, local);
                }
            }
            finally {
                stampedLock.unlockWrite(writeStamp);
            }
        }

        return local;
    }

    @Override
    public T get(int x, int y, int z) {
        Chunk<T> local = getLocal(chunk(x, z));
        if (local != null) {
            int chunkRelative = chunkRelative(x, y, z);

            long localReadStamp = local.lock.readLock();
            try {
                return local.map.get(chunkRelative);
            }
            finally {
                local.lock.unlockRead(localReadStamp);
            }
        }

        return null;
    }

    @Override
    public void put(int x, int y, int z, @NotNull T item) {
        Objects.requireNonNull(item);
        Chunk<T> local = computeLocalIfAbsent(chunk(x, z));

        int relative = chunkRelative(x, y, z);
        long writeStamp = local.lock.writeLock();
        try {
            local.map.put(relative, item);
        }
        finally {
            local.lock.unlockWrite(writeStamp);
        }
    }

    @Override
    public @NotNull T computeIfAbsent(int x, int y, int z, @NotNull Vec3IFunction<T> supplier) {
        Objects.requireNonNull(supplier);
        Chunk<T> local = computeLocalIfAbsent(chunk(x, z));

        int relative = chunkRelative(x, y, z);

        long readStamp = local.lock.readLock();
        T value;
        try {
            value = local.map.get(relative);
        }
        finally {
            local.lock.unlockRead(readStamp);
        }

        if (value != null) {
            //the value already exists, return it (we only needed to briefly acquire a read lock)
            return value;
        }

        T newValue = Objects.requireNonNull(supplier.apply(x, y, z), "supplier return value");

        //if the value is null, acquire write lock
        long writeStamp = local.lock.writeLock();
        try {
            //known race condition: other thread may have put its own value while we were generating ours
            local.map.put(relative, newValue);
        }
        finally {
            local.lock.unlockWrite(writeStamp);
        }

        return newValue;
    }

    @Override
    public void remove(int x, int y, int z) {
        long chunk = chunk(x, z);
        Chunk<T> local = getLocal(chunk);

        if (local != null) {
            int relative = chunkRelative(x, y, z);

            long localWriteStamp = local.lock.writeLock();
            try {
                local.map.remove(relative);
                if (local.map.isEmpty()) {
                    long globalWriteStamp = stampedLock.writeLock();
                    try {
                        chunkMap.remove(chunk);
                    }
                    finally {
                        stampedLock.unlockWrite(globalWriteStamp);
                    }
                }
            }
            finally {
                local.lock.unlockWrite(localWriteStamp);
            }
        }
    }

    @Override
    public void clear() {
        long writeStamp = stampedLock.writeLock();
        try {
            chunkMap.clear();
            chunkMap.trim(32);
        }
        finally {
            stampedLock.unlockWrite(writeStamp);
        }
    }
}
