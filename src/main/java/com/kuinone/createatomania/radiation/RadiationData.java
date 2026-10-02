package com.kuinone.createatomania.radiation;

import com.kuinone.createatomania.AtomaniaConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.CompoundTag;

/**
 * The accumulated radiation dose carried by a living entity.
 * <p>
 * Dose is stored in sieverts and persists across death and dimension changes, because
 * radiation damage is cumulative in the body rather than reset by respawning. The value is
 * what the severity staging reads, and what eventually kills the entity outright.
 * <p>
 * Dose also clears slowly over time, modelling biological clearance of radionuclides, so a
 * player who leaves the reactor does eventually recover rather than being permanently doomed.
 */
public class RadiationData {

	/** Persistent form, so the accumulated dose survives a save and a respawn. */
	public static final Codec<RadiationData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.DOUBLE.optionalFieldOf("dose", 0.0D).forGetter(data -> data.dose)
	).apply(instance, dose -> {
		RadiationData data = new RadiationData();
		data.dose = Math.max(0.0D, dose);
		return data;
	}));

	/** Total absorbed dose in sieverts. */
	private double dose;

	public double getDose() {
		return dose;
	}

	public void setDose(double dose) {
		this.dose = Math.max(0.0D, dose);
	}

	/** Adds to the accumulated dose. */
	public void addDose(double sieverts) {
		if (sieverts > 0.0D) {
			this.dose += sieverts;
		}
	}

	/**
	 * Clears a fraction of the accumulated dose.
	 *
	 * @param seconds elapsed time in seconds
	 */
	public void decay(double seconds) {
		double clearance = AtomaniaConfig.INSTANCE.doseDecayPerSecond.get() * seconds;
		if (clearance > 0.0D) {
			dose = Math.max(0.0D, dose - clearance);
		}
	}

	/** @return the current radiation sickness severity, from 0 (healthy) upward. */
	public int getSeverity() {
		AtomaniaConfig config = AtomaniaConfig.INSTANCE;
		double threshold = config.sicknessThreshold.get();
		if (dose < threshold) {
			return 0;
		}
		double perStage = Math.max(1.0E-6D, config.dosePerStage.get());
		int severity = 1 + (int) Math.floor((dose - threshold) / perStage);
		return Math.min(severity, config.maxSeverity.get());
	}

	/** @return true if the accumulated dose is now lethal. */
	public boolean isLethal() {
		return dose >= AtomaniaConfig.INSTANCE.lethalDose.get();
	}

	/** Serialises this data to NBT. */
	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putDouble("Dose", dose);
		return tag;
	}

	/** Reads this data from NBT. */
	public static RadiationData load(CompoundTag tag) {
		RadiationData data = new RadiationData();
		data.dose = Math.max(0.0D, tag.getDouble("Dose"));
		return data;
	}
}