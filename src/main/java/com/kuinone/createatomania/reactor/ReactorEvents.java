package com.kuinone.createatomania.reactor;

import com.kuinone.createatomania.AtomaniaConfig;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Hooks the reactor simulation into the game tick.
 * <p>
 * The simulation runs per level rather than per block, on a fixed interval, so that a large
 * reactor costs one sweep every {@code simulationInterval} ticks instead of one tick handler
 * per fuel block. Blocks simply register their position when they are placed and the sweep
 * discovers the rest.
 */
public final class ReactorEvents {

	private ReactorEvents() {}

	/** Registers the level tick and unload hooks. */
	public static void register() {
		NeoForge.EVENT_BUS.addListener(ReactorEvents::onLevelTick);
		NeoForge.EVENT_BUS.addListener(ReactorEvents::onLevelUnload);
	}

	private static void onLevelTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel level)) {
			return;
		}
		int interval = AtomaniaConfig.INSTANCE.simulationInterval.get();
		if (level.getGameTime() % interval != 0L) {
			return;
		}
		ReactorTracker.tickAll(level);
	}

	private static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel level) {
			ReactorTracker.clear(level);
		}
	}
}