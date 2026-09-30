package com.kuinone.createatomania.content.fuel;

import com.kuinone.createatomania.CreateAtomania;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block entity types for the reactor blocks. */
public final class AtomaniaBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
		DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateAtomania.MODID);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelBlockEntity>> FUEL_BLOCK =
		BLOCK_ENTITIES.register("fuel_block",
			() -> BlockEntityType.Builder.of(FuelBlockEntity::new, AtomaniaBlocks.FUEL_BLOCK.get())
				.build(null));

	private AtomaniaBlockEntities() {}
}