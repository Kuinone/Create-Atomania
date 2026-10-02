package com.kuinone.createatomania.recycling;

import com.kuinone.createatomania.CreateAtomania;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The reprocessing recipe type.
 * <p>
 * This exists as its own recipe type because reprocessing output depends on the input's
 * data components, which a normal datapack recipe cannot express: the yields are a function
 * of the fuel's nuclide inventory at the moment it is crushed. The recipe is still a real
 * registered recipe so it shows in recipe viewers and can be gated or removed by datapacks.
 */
public final class ReprocessingRecipes {

	public static final DeferredRegister<RecipeType<?>> TYPES =
		DeferredRegister.create(Registries.RECIPE_TYPE, CreateAtomania.MODID);

	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
		DeferredRegister.create(Registries.RECIPE_SERIALIZER, CreateAtomania.MODID);

	public static final DeferredHolder<RecipeType<?>, RecipeType<FuelReprocessingRecipe>> FUEL_REPROCESSING_TYPE =
		TYPES.register("fuel_reprocessing", () -> RecipeType.simple(
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
				CreateAtomania.MODID, "fuel_reprocessing")));

	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FuelReprocessingRecipe>>
		FUEL_REPROCESSING_SERIALIZER = SERIALIZERS.register("fuel_reprocessing",
			FuelReprocessingRecipe.Serializer::new);

	private ReprocessingRecipes() {}
}