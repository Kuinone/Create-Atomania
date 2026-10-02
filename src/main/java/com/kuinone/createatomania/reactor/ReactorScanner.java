package com.kuinone.createatomania.reactor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.kuinone.createatomania.content.fuel.FuelBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Finds the blocks that make up one reactor.
 * <p>
 * A flood fill from the starting block, stepping only through positions that either hold
 * fuel or are a moderator or control rod. That way a reactor can be shaped freely: a fuel
 * block only couples to another if there is a path of reactor material between them, and a
 * wall of stone genuinely shields one reactor from the next.
 */
public final class ReactorScanner {

	/** Upper bound on a single reactor, so a pathological build cannot stall the server. */
	private static final int MAX_BLOCKS = 2048;

	private ReactorScanner() {}

	/** @return every reactor block reachable from {@code origin}, including moderators. */
	public static Set<BlockPos> collectBlocks(Level level, BlockPos origin) {
		Set<BlockPos> visited = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();

		queue.add(origin);
		visited.add(origin);

		while (!queue.isEmpty() && visited.size() < MAX_BLOCKS) {
			BlockPos current = queue.poll();

			for (Direction direction : Direction.values()) {
				BlockPos neighbour = current.relative(direction);
				if (visited.contains(neighbour) || !level.isLoaded(neighbour)) {
					continue;
				}
				if (!ReactorMedium.conducts(level, neighbour)) {
					continue;
				}
				visited.add(neighbour);
				queue.add(neighbour);
			}
		}
		return visited;
	}

	/**
	 * Flood fills from {@code origin} and returns every fuel block found.
	 *
	 * @param level  the level
	 * @param origin the block to start from
	 * @return the fuel blocks belonging to this reactor, never null
	 */
	public static List<FuelBlockEntity> collectFuelBlocks(Level level, BlockPos origin) {
		List<FuelBlockEntity> fuels = new ArrayList<>();
		for (BlockPos pos : collectBlocks(level, origin)) {
			if (level.getBlockEntity(pos) instanceof FuelBlockEntity fuel) {
				fuels.add(fuel);
			}
		}
		return fuels;
	}
}