package com.kuinone.createatomania.content.fuel;

import com.kuinone.createatomania.CreateAtomania;
import com.kuinone.createatomania.reactor.FuelComposition;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The data components that let a fuel block's contents ride along on the item form.
 * <p>
 * Using a component rather than NBT keeps the composition a first-class, typed part of the
 * stack, so it survives crafting, hoppers, Create contraptions and any recipe that copies
 * components.
 */
public final class AtomaniaDataComponents {

	public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
		DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CreateAtomania.MODID);

	/** The nuclide inventory stored on a fuel block item. */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<FuelComposition>> FUEL_COMPOSITION =
		COMPONENTS.register("fuel_composition", () -> DataComponentType.<FuelComposition>builder()
			.persistent(FuelComposition.CODEC)
			.networkSynchronized(FuelComposition.STREAM_CODEC)
			.build());

	private AtomaniaDataComponents() {}

	/** Reads the composition from a stack, falling back to natural uranium. */
	public static FuelComposition readComposition(ItemStack stack) {
		FuelComposition stored = stack.get(FUEL_COMPOSITION.get());
		return stored == null ? FuelComposition.NATURAL_URANIUM : stored;
	}

	/** Writes a composition onto a stack. */
	public static void writeComposition(ItemStack stack, FuelComposition composition) {
		stack.set(FUEL_COMPOSITION.get(), composition);
	}
}