package com.kuinone.createatomania.content.fuel;

import com.kuinone.createatomania.reactor.FuelComposition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Carries a fuel block's nuclide inventory and its accumulated heat.
 * <p>
 * All state is persisted to NBT. That persistence is what makes breaking and replacing a
 * fuel block lossless: the composition travels with the block item and comes back when it
 * is placed down again.
 */
public class FuelBlockEntity extends BlockEntity {

	/** The default inventory of a freshly crafted block: natural uranium. */
	private FuelComposition composition = FuelComposition.NATURAL_URANIUM;

	/** Accumulated heat, which drives transfer into adjacent water. */
	private double heat;

	public FuelBlockEntity(BlockPos pos, BlockState state) {
		super(AtomaniaBlockEntities.FUEL_BLOCK.get(), pos, state);
	}

	public FuelComposition getComposition() {
		return composition;
	}

	public void setComposition(FuelComposition composition) {
		this.composition = composition;
		setChanged();
	}

	public double getHeat() {
		return heat;
	}

	public void setHeat(double heat) {
		this.heat = Math.max(0.0D, heat);
	}

	public void addHeat(double delta) {
		setHeat(this.heat + delta);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		FuelComposition.CODEC
			.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), composition)
			.resultOrPartial(error -> com.kuinone.createatomania.CreateAtomania.LOGGER
				.warn("Failed to save fuel composition: {}", error))
			.ifPresent(encoded -> tag.put("Composition", encoded));
		tag.putDouble("Heat", heat);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		Tag encoded = tag.get("Composition");
		if (encoded != null) {
			FuelComposition.CODEC
				.parse(registries.createSerializationContext(NbtOps.INSTANCE), encoded)
				.resultOrPartial(error -> com.kuinone.createatomania.CreateAtomania.LOGGER
					.warn("Failed to load fuel composition: {}", error))
				.ifPresent(loaded -> composition = loaded);
		}
		heat = tag.getDouble("Heat").orElse(0.0D);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}