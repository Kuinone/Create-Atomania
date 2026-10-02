package com.kuinone.createatomania.radiation;

import com.kuinone.createatomania.AtomaniaConfig;
import com.kuinone.createatomania.content.RadioactiveItem;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Applies radiation to entities and turns accumulated dose into illness.
 * <p>
 * Two sources are modelled. The environment doses an entity through the flux the reactor
 * simulation already computed, which means standing behind a wall of concrete genuinely
 * shields you. Carried and dropped radioactive items dose an entity directly from their
 * activity in becquerels, which is why a pocket full of plutonium is a bad idea even with
 * the reactor shut down.
 */
public final class RadiationSystem {

	private RadiationSystem() {}

	/** Applies one pulse of environmental and carried dose to an entity. */
	public static void irradiate(LivingEntity entity, double seconds) {
		RadiationData data = entity.getData(RadiationAttachments.RADIATION.get());
		AtomaniaConfig config = AtomaniaConfig.INSTANCE;

		double dose = 0.0D;

		// Environmental dose, from the flux at the entity's position.
		dose += RadiationField.doseAt(entity.level(), entity.blockPosition(), config);

		// Carried dose, summed over the whole inventory.
		double activity = carriedActivity(entity);
		dose += activity * config.dosePerHeldItem.get() * seconds;

		data.addDose(dose * seconds);
		data.decay(seconds);

		applySickness(entity, data);
	}

	/** Sums the activity in becquerels of every radioactive item an entity carries. */
	public static double carriedActivity(LivingEntity entity) {
		double total = 0.0D;
		if (entity instanceof Player player) {
			total += activityOf(player.getInventory().items);
			total += activityOf(player.getInventory().armor);
			total += activityOf(player.getInventory().offhand);
		} else {
			total += activityOf(entity.getHandSlots());
		}
		return total;
	}

	private static double activityOf(Iterable<ItemStack> stacks) {
		double total = 0.0D;
		for (ItemStack stack : stacks) {
			if (!stack.isEmpty() && stack.getItem() instanceof RadioactiveItem radioactive) {
				total += radioactive.becquerels() * stack.getCount();
			}
		}
		return total;
	}

	/** Sums the activity of dropped radioactive items near a position. */
	public static double groundActivity(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
		double radius = AtomaniaConfig.INSTANCE.itemRadiationRadius.get();
		if (radius <= 0.0D) {
			return 0.0D;
		}
		double total = 0.0D;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
			new AABB(pos).inflate(radius))) {
			ItemStack stack = item.getItem();
			if (stack.getItem() instanceof RadioactiveItem radioactive) {
				total += radioactive.becquerels() * stack.getCount();
			}
		}
		return total;
	}

	/**
	 * Translates the accumulated dose into status effects.
	 * <p>
	 * The staging is deliberately cumulative rather than random: a mild dose gives nausea,
	 * a moderate one adds weakness and slowness, and a severe one adds withering damage. At
	 * the lethal threshold the entity dies outright, which is the only outcome that cannot be
	 * waited out.
	 */
	public static void applySickness(LivingEntity entity, RadiationData data) {
		int severity = data.getSeverity();

		if (data.isLethal()) {
			// Acute radiation syndrome at a lethal dose. Bypass armour and enchantments.
			entity.hurt(entity.damageSources().genericKill(), Float.MAX_VALUE);
			return;
		}
		if (severity <= 0) {
			return;
		}

		int duration = 100;

		// Nausea from the first stage onward: the classic early symptom.
		apply(entity, MobEffects.CONFUSION, duration, 0);

		if (severity >= 2) {
			apply(entity, MobEffects.WEAKNESS, duration, Math.min(severity - 1, 3));
		}
		if (severity >= 3) {
			apply(entity, MobEffects.MOVEMENT_SLOWDOWN, duration, Math.min(severity - 2, 3));
		}
		if (severity >= 4) {
			apply(entity, MobEffects.DIG_SLOWDOWN, duration, Math.min(severity - 3, 2));
		}
		if (severity >= 5) {
			// Withering tissue damage: the dose is now actively killing cells.
			apply(entity, MobEffects.WITHER, duration, Math.min(severity - 4, 2));
		}
		if (severity >= 6) {
			apply(entity, MobEffects.HUNGER, duration, Math.min(severity - 5, 2));
		}
	}

	private static void apply(LivingEntity entity,
		net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int duration,
		int amplifier) {

		MobEffectInstance existing = entity.getEffect(effect);
		// Only refresh when the current instance is weaker, so stronger effects are not
		// downgraded by a later weaker pulse.
		if (existing == null || existing.getAmplifier() <= amplifier) {
			entity.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true));
		}
	}
}