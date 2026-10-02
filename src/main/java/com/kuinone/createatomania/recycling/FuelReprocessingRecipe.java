package com.kuinone.createatomania.recycling;

import java.util.ArrayList;
import java.util.List;

import com.kuinone.createatomania.content.ContentRegistry;
import com.kuinone.createatomania.content.fuel.AtomaniaDataComponents;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Reprocesses a spent fuel block into recoverable uranium, plutonium and waste.
 * <p>
 * This is a real recipe with a real recipe type, so it appears in recipe viewers and can be
 * datapack-controlled, but its output is computed at craft time from the input stack's
 * composition component rather than being fixed in JSON. That is the only way to honour the
 * requirement that yields depend on what is actually in the block: crushing a fresh block
 * gives you mostly uranium, while crushing one that has bred plutonium gives you plutonium
 * and a pile of waste.
 */
public class FuelReprocessingRecipe implements Recipe<SingleRecipeInput> {

	/** The item this recipe accepts. */
	private final Ingredient input;

	/** Ticks the process takes, purely informational for Create's machines. */
	private final int processingTime;

	public FuelReprocessingRecipe(Ingredient input, int processingTime) {
		this.input = input;
		this.processingTime = processingTime;
	}

	public int processingTime() {
		return processingTime;
	}

	@Override
	public boolean matches(SingleRecipeInput recipeInput, Level level) {
		return input.test(recipeInput.getItem(0));
	}

	/**
	 * Produces the recovered stacks for a given fuel block item.
	 * <p>
	 * The composition travels on the stack's data component, so this reads whatever the block
	 * actually contained rather than assuming a fresh charge.
	 */
	public List<ItemStack> assembleOutputs(ItemStack fuelStack) {
		ReprocessingYield.Yields yields =
			ReprocessingYield.compute(AtomaniaDataComponents.readComposition(fuelStack));

		List<ItemStack> outputs = new ArrayList<>(5);
		addIfAny(outputs, ContentRegistry.URANIUM_235.get(), yields.uranium235());
		addIfAny(outputs, ContentRegistry.URANIUM_238.get(), yields.uranium238());
		addIfAny(outputs, ContentRegistry.PLUTONIUM_239.get(), yields.plutonium239());
		addIfAny(outputs, ContentRegistry.HIGH_LEVEL_WASTE.get(), yields.highLevelWaste());
		addIfAny(outputs, ContentRegistry.LOW_LEVEL_WASTE.get(), yields.lowLevelWaste());
		return outputs;
	}

	private static void addIfAny(List<ItemStack> outputs, net.minecraft.world.item.Item item, int count) {
		if (count > 0) {
			outputs.add(new ItemStack(item, count));
		}
	}

	@Override
	public ItemStack assemble(SingleRecipeInput recipeInput, HolderLookup.Provider registries) {
		List<ItemStack> outputs = assembleOutputs(recipeInput.getItem(0));
		return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	/** @return every possible output, used by recipe viewers to display the recipe. */
	@Override
	public NonNullList<ItemStack> getRemainingItems(SingleRecipeInput recipeInput) {
		return NonNullList.create();
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return new ItemStack(ContentRegistry.URANIUM_238.get());
	}

	/** @return a representative output list for display, based on fresh natural uranium. */
	public List<ItemStack> displayOutputs() {
		return assembleOutputs(new ItemStack(ContentRegistry.FUEL_BLOCK_ITEM.get()));
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ReprocessingRecipes.FUEL_REPROCESSING_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return ReprocessingRecipes.FUEL_REPROCESSING_TYPE.get();
	}

	/** Datapack form. */
	public static class Serializer implements RecipeSerializer<FuelReprocessingRecipe> {

		private static final MapCodec<FuelReprocessingRecipe> CODEC =
			RecordCodecBuilder.mapCodec(instance -> instance.group(
				Ingredient.CODEC_NONEMPTY.fieldOf("ingredient")
					.forGetter(recipe -> recipe.input),
				com.mojang.serialization.Codec.INT.optionalFieldOf("processing_time", 200)
					.forGetter(recipe -> recipe.processingTime)
			).apply(instance, FuelReprocessingRecipe::new));

		private static final StreamCodec<RegistryFriendlyByteBuf, FuelReprocessingRecipe> STREAM_CODEC =
			StreamCodec.composite(
				Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.input,
				net.minecraft.network.codec.ByteBufCodecs.VAR_INT, recipe -> recipe.processingTime,
				FuelReprocessingRecipe::new);

		@Override
		public MapCodec<FuelReprocessingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, FuelReprocessingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}