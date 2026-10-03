package com.kuinone.createatomania;

import com.kuinone.createatomania.content.ContentRegistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Smoke tests that run on a real server.
 * <p>
 * These exist because compiling proves nothing about whether registration actually
 * succeeded: the earlier failure reproduced only at runtime, when a block entity type tried
 * to resolve a block while the item register had already been closed. The tests therefore
 * assert that every entry the mod declares is genuinely present in its registry.
 */
@GameTestHolder(CreateAtomania.MODID)
@PrefixGameTestTemplate(false)
public final class AtomaniaGameTests {

	/**
	 * Template placed for every test.
	 * <p>
	 * A GameTest always needs a structure to place, even when it only inspects registries, so
	 * the mod ships a minimal empty platform at this path. The value is a bare path: the
	 * holder's namespace is prepended automatically, so writing the namespace here would
	 * double it up.
	 */
	private static final String TEMPLATE = "empty";

	private AtomaniaGameTests() {}

	/** Every block the mod declares must be registered. */
	@GameTest(template = TEMPLATE)
	public static void blocksAreRegistered(GameTestHelper helper) {
		check(helper, "block", ContentRegistry.FUEL_BLOCK.getId(),
			BuiltInRegistries.BLOCK.containsKey(ContentRegistry.FUEL_BLOCK.getId()));
		check(helper, "block", ContentRegistry.CONTROL_ROD.getId(),
			BuiltInRegistries.BLOCK.containsKey(ContentRegistry.CONTROL_ROD.getId()));
		check(helper, "block", ContentRegistry.GEIGER_COUNTER.getId(),
			BuiltInRegistries.BLOCK.containsKey(ContentRegistry.GEIGER_COUNTER.getId()));
		check(helper, "block", ContentRegistry.NEUTRON_FLUX_COUNTER.getId(),
			BuiltInRegistries.BLOCK.containsKey(ContentRegistry.NEUTRON_FLUX_COUNTER.getId()));
		helper.succeed();
	}

	/** Every item the mod declares must be registered. */
	@GameTest(template = TEMPLATE)
	public static void itemsAreRegistered(GameTestHelper helper) {
		check(helper, "item", ContentRegistry.RAW_URANIUM.getId(),
			BuiltInRegistries.ITEM.containsKey(ContentRegistry.RAW_URANIUM.getId()));
		check(helper, "item", ContentRegistry.URANIUM_INGOT.getId(),
			BuiltInRegistries.ITEM.containsKey(ContentRegistry.URANIUM_INGOT.getId()));
		check(helper, "item", ContentRegistry.URANIUM_235.getId(),
			BuiltInRegistries.ITEM.containsKey(ContentRegistry.URANIUM_235.getId()));
		check(helper, "item", ContentRegistry.URANIUM_238.getId(),
			BuiltInRegistries.ITEM.containsKey(ContentRegistry.URANIUM_238.getId()));
		check(helper, "item", ContentRegistry.PLUTONIUM_239.getId(),
			BuiltInRegistries.ITEM.containsKey(ContentRegistry.PLUTONIUM_239.getId()));
		check(helper, "item", ContentRegistry.HIGH_LEVEL_WASTE.getId(),
			BuiltInRegistries.ITEM.containsKey(ContentRegistry.HIGH_LEVEL_WASTE.getId()));
		check(helper, "item", ContentRegistry.LOW_LEVEL_WASTE.getId(),
			BuiltInRegistries.ITEM.containsKey(ContentRegistry.LOW_LEVEL_WASTE.getId()));
		helper.succeed();
	}

	/** The block entity types must be registered. */
	@GameTest(template = TEMPLATE)
	public static void blockEntitiesAreRegistered(GameTestHelper helper) {
		check(helper, "block_entity_type", AtomaniaBlockEntities.FUEL_BLOCK.getId(),
			BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(AtomaniaBlockEntities.FUEL_BLOCK.getId()));
		check(helper, "block_entity_type", AtomaniaBlockEntities.CONTROL_ROD.getId(),
			BuiltInRegistries.BLOCK_ENTITY_TYPE
				.containsKey(AtomaniaBlockEntities.CONTROL_ROD.getId()));
		check(helper, "block_entity_type", AtomaniaBlockEntities.GEIGER_COUNTER.getId(),
			BuiltInRegistries.BLOCK_ENTITY_TYPE
				.containsKey(AtomaniaBlockEntities.GEIGER_COUNTER.getId()));
		check(helper, "block_entity_type", AtomaniaBlockEntities.NEUTRON_FLUX_COUNTER.getId(),
			BuiltInRegistries.BLOCK_ENTITY_TYPE
				.containsKey(AtomaniaBlockEntities.NEUTRON_FLUX_COUNTER.getId()));
		helper.succeed();
	}

	/** The data component and the reprocessing recipe type must be registered. */
	@GameTest(template = TEMPLATE)
	public static void componentsAndRecipeTypesAreRegistered(GameTestHelper helper) {
		var component = com.kuinone.createatomania.content.fuel.AtomaniaDataComponents.FUEL_COMPOSITION
			.getId();
		check(helper, "data_component_type", component,
			BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(component));

		var recipeType = com.kuinone.createatomania.recycling.ReprocessingRecipes
			.FUEL_REPROCESSING_TYPE.getId();
		check(helper, "recipe_type", recipeType,
			BuiltInRegistries.RECIPE_TYPE.containsKey(recipeType));
		helper.succeed();
	}

	/** Fails the test with a useful message when an entry is missing. */
	private static void check(GameTestHelper helper, String registry,
		net.minecraft.resources.ResourceLocation id, boolean present) {

		if (!present) {
			helper.fail("missing " + registry + " entry: " + id);
		}
	}
}