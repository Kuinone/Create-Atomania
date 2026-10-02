package com.kuinone.createatomania.radiation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Applies radiation to living entities on a fixed interval.
 * <p>
 * Dosing every tick would be wasteful and would make the numbers twitchy, so it runs once a
 * second and is scaled by the elapsed time. The interval is fixed rather than random so that
 * dose rates are reproducible, which matters when a player is trying to work out how long
 * they can safely stay near a reactor.
 */
public final class RadiationEvents {

	/** Dosing interval in ticks. Twenty ticks is one second. */
	private static final int INTERVAL = 20;

	/** Elapsed time per dosing pulse, in seconds. */
	private static final double SECONDS_PER_PULSE = INTERVAL / 20.0D;

	private RadiationEvents() {}

	/** Registers the radiation tick hook. */
	public static void register() {
		NeoForge.EVENT_BUS.addListener(RadiationEvents::onLevelTick);
		NeoForge.EVENT_BUS.addListener(RadiationEvents::onEntityJoin);
	}

	private static void onLevelTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel level)) {
			return;
		}
		if (level.getGameTime() % INTERVAL != 0L) {
			return;
		}

		// getAllEntities walks the level's entity index, which is far cheaper than building
		// an AABB over the whole world border every pulse.
		for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				continue;
			}
			// Skip entities whose chunk is not loaded or which cannot plausibly be dosed.
			if (!level.isLoaded(living.blockPosition())) {
				continue;
			}
			RadiationSystem.irradiate(living, SECONDS_PER_PULSE);
		}
	}

	/** Gives entities the attachment lazily so old saves pick it up without a migration. */
	private static void onEntityJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
		if (event.getEntity() instanceof LivingEntity living) {
			living.getData(RadiationAttachments.RADIATION.get());
		}
	}
}