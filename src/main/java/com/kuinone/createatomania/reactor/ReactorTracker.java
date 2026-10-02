package com.kuinone.createatomania.reactor;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Remembers which blocks belong to a reactor, per level.
 * <p>
 * The simulation does not need this to be correct, but it does need it to be cheap: without
 * tracking, the only alternative would be sweeping every loaded chunk every tick. Instead,
 * blocks announce themselves when placed or loaded, and the tracker hands the simulator a
 * small set of seed positions to expand from.
 * <p>
 * The set is deliberately conservative. A stale entry costs one wasted flood fill; a missing
 * entry would stall a reactor, so entries are only dropped when a chunk unloads.
 */
public final class ReactorTracker {

	private static final Map<ServerLevel, Set<BlockPos>> TRACKED = new WeakHashMap<>();

	private ReactorTracker() {}

	/** Records a position as part of some reactor. */
	public static void track(ServerLevel level, BlockPos pos) {
		TRACKED.computeIfAbsent(level, ignored -> ConcurrentHashMap.newKeySet())
			.add(pos.immutable());
	}

	/** Forgets a position, called when a reactor block is removed. */
	public static void untrack(ServerLevel level, BlockPos pos) {
		Set<BlockPos> positions = TRACKED.get(level);
		if (positions != null) {
			positions.remove(pos);
		}
	}

	/** Drops all state for a level. */
	public static void clear(ServerLevel level) {
		TRACKED.remove(level);
	}

	/**
	 * Runs one simulation step for every distinct reactor in the level.
	 * <p>
	 * Seeds that turn out to belong to the same reactor are expanded by the scan anyway, so
	 * the sweep simply skips a seed once it has been visited by an earlier one.
	 */
	public static void tickAll(ServerLevel level) {
		Set<BlockPos> positions = TRACKED.get(level);
		if (positions == null || positions.isEmpty()) {
			return;
		}

		Set<BlockPos> visited = ConcurrentHashMap.newKeySet();
		for (BlockPos seed : positions) {
			if (visited.contains(seed) || !level.isLoaded(seed)) {
				continue;
			}
			// The scanner walks the whole connected reactor, so everything it reaches can be
			// skipped as a future seed.
			visited.addAll(ReactorScanner.collectBlocks(level, seed));
			ReactorSimulator.tick(level, seed);
		}

		// Seeds whose chunks are gone are no longer useful.
		positions.removeIf(pos -> !level.isLoaded(pos));
	}
}