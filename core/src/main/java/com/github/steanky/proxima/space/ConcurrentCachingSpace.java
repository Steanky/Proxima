package com.github.steanky.proxima.space;

import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.vector.Vec3I;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.locks.StampedLock;

/**
 * A thread-safe {@link Space} implementation. Must be subclassed by a class which provides actual {@link Solid}
 * instances from some source, for example a Minecraft chunk.
 * <p>
 * This class is designed to minimize lock contention in the common case of heavily concurrent reads and relatively few
 * writes. It is often the case that simply <i>reading</i> {@link Solid} objects is poorly performant or not conducive
 * to massively parallel reads. The {@link ConcurrentCachingSpace#loadSolid(int, int, int)} method is expected to
 * utilize algorithms like this; it may be slow to read from, or the data may be synchronized in such a way that
 * concurrent reads are not possible. Solids returned from this method are cached in a parallelism-friendly data
 * structure that allows as much concurrent access as possible.
 */
public abstract class ConcurrentCachingSpace implements Space {
    private static final int CHUNK_READ_ATTEMPTS = 5;
    private static final int BLOCK_READ_ATTEMPTS = 5;

    private final StampedLock lock;
    private final Long2ObjectOpenHashMap<Chunk> cache;

    private record Chunk(Int2ObjectMap<Solid> map, StampedLock lock) {
        @SuppressWarnings("DuplicatedCode")
        private Solid read(int key) {
            long read;
            Solid solid;

            boolean valid;
            int i = 0;
            do {
                //often, we can grab the map without ever needing to read-lock it
                //if a writer thread wants access, it will break our optimistic read, and we retry
                //retries are limited to BLOCK_READ_ATTEMPTS, after which a proper read lock is used
                read = lock.tryOptimisticRead();

                try {
                    solid = map.get(key);
                    valid = lock.validate(read);
                }
                catch (Throwable ignored) {
                    /*
                    theoretically, a table rehash or trim could occur in the map at exactly the wrong time, which would
                    cause get(int) to throw an exception; this means our read was definitely broken
                     */
                    solid = null;
                    valid = false;
                }
            }
            while (!valid && i++ < BLOCK_READ_ATTEMPTS);

            if (valid) {
                //we were able to successfully read the solid without locking on the map
                return solid;
            }

            read = lock.readLock();
            try {
                //we tried to optimistically read too many times; we need to do a full read
                return map.get(key);
            }
            finally {
                lock.unlockRead(read);
            }
        }

        private void write(int key, Solid solid) {
            long write = lock.writeLock();
            try {
                map.put(key, solid);
            }
            finally {
                lock.unlockWrite(write);
            }
        }

        private boolean remove(int key) {
            long write = lock.writeLock();
            try {
                map.remove(key);
                return map.isEmpty();
            }
            finally {
                lock.unlockWrite(write);
            }
        }

        private Chunk() {
            this(new Int2ObjectOpenHashMap<>(), new StampedLock());
        }

        private static long key(int x, int z) {
            return (((long) x << 4) << 32) | ((long) z << 4);
        }

        private static int relative(int x, int y, int z) {
            return ((x & 15) << 15) | ((y & 2047) << 4) | (z & 15);
        }
    }

    //works identically to Chunk#read(int), but for the chunk cache rather than individual blocks
    //methods are duplicated because they effectively differ by primitive type (long vs int)
    @SuppressWarnings("DuplicatedCode")
    private Chunk getChunk(long chunkKey) {
        long read;
        Chunk chunk;

        boolean valid;
        int i = 0;
        do {
            read = lock.tryOptimisticRead();

            try {
                chunk = cache.get(chunkKey);
                valid = lock.validate(read);
            }
            catch (Throwable ignored) {
                chunk = null;
                valid = false;
            }
        }
        while (!valid && i++ < CHUNK_READ_ATTEMPTS);

        if (valid) {
            return chunk;
        }

        read = lock.readLock();
        try {
            return cache.get(chunkKey);
        }
        finally {
            lock.unlockRead(read);
        }
    }

    private void addToExistingOrNewChunk(Chunk chunk, long chunkKey, int blockKey, Solid solidToWrite) {
        if (chunk != null) {
            //in most cases, we can easily add the solid
            chunk.write(blockKey, solidToWrite);
            return;
        }

        boolean addedSolid = false;
        long write = lock.writeLock();
        try {
            //may be non-null if another thread quickly added a chunk to the cache
            //if so, don't create a new chunk, remove write lock, and add the solid to the chunk
            //otherwise, if the chunk is null, create a new chunk, add our initial solid to it, and put it in the map
            Chunk otherChunk = cache.get(chunkKey);
            if (otherChunk != null) {
                //another thread added a chunk - use it
                chunk = otherChunk;
            }
            else {
                //create a new chunk, add our solid to it, and put it in the cache
                //we don't need to write-lock on the newly-created chunk at all this way
                chunk = new Chunk();
                chunk.map.put(blockKey, solidToWrite);

                cache.put(chunkKey, chunk);

                //indicate that we already put the solid in the chunk
                addedSolid = true;
            }
        }
        finally {
            lock.unlockWrite(write);
        }

        if (!addedSolid) {
            chunk.write(blockKey, solidToWrite);
        }
    }

    public ConcurrentCachingSpace() {
        this.lock = new StampedLock();
        this.cache = new Long2ObjectOpenHashMap<>();
    }

    @Override
    public final @NotNull Solid solidAt(int x, int y, int z) {
        long chunkKey = Chunk.key(x, z);
        int blockKey = Chunk.relative(x, y, z);

        Chunk chunk = getChunk(chunkKey);
        Solid solid;
        if (chunk == null) {
            addToExistingOrNewChunk(null, chunkKey, blockKey, solid = loadSolid(x, y, z));
            return solid;
        }

        solid = chunk.read(blockKey);
        if (solid != null) {
            return solid;
        }

        chunk.write(blockKey, solid = loadSolid(x, y, z));
        return solid;
    }

    @Override
    public final @NotNull Solid solidAt(@NotNull Vec3I vec) {
        return solidAt(vec.x(), vec.y(), vec.z());
    }

    /**
     * Updates the solid at the given position. If the solid is null, any solid that was cached at that location will
     * be removed, to be re-computed when needed.
     *
     * @param x the x-coordinate of the solid to update
     * @param y the y-coordinate of the solid to update
     * @param z the z-coordinate of the solid to update
     * @param solid the new solid, or null to remove any cached solid (if present)
     */
    public void updateSolid(int x, int y, int z, @Nullable Solid solid) {
        long chunkKey = Chunk.key(x, z);

        //null solid means we need to remove from a chunk
        if (solid == null) {
            Chunk chunk = getChunk(chunkKey);
            if (chunk != null) {
                //returns true when we empty the chunk
                if (chunk.remove(Chunk.relative(x, y, z))) {
                    long write = lock.writeLock();
                    try {
                        cache.remove(chunkKey);
                    }
                    finally {
                        lock.unlockWrite(write);
                    }
                }
            }
        }
        else {
            //if the chunk does not exist (is null); creates it and adds the solid
            //otherwise, adds the solid to the existing chunk
            addToExistingOrNewChunk(getChunk(chunkKey), chunkKey, Chunk.relative(x, y, z), solid);
        }
    }

    /**
     * Clears the cache, reducing it to a state similar to when it was first initialized.
     */
    public void clearCache() {
        long write = lock.writeLock();
        try {
            cache.clear();
            cache.trim();
        }
        finally {
            lock.unlockWrite(write);
        }
    }

    /**
     * Loads a solid, which will be cached in this space until it is invalidated. This method is called by
     * {@link ConcurrentCachingSpace#solidAt(int, int, int)} when it encounters a cache miss.
     * <p>
     * Threads calling this method will typically not hold any write locks. Therefore, implementations should expect
     * that this method will be called concurrently by two or more threads.
     *
     * @param x the x-coordinate of the solid to load
     * @param y the y-coordinate of the solid to load
     * @param z the z-coordinate of the solid to load
     * @return a Solid object
     */
    public abstract @NotNull Solid loadSolid(int x, int y, int z);
}
