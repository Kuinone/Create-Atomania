package com.kuinone.createatomania;

import com.kuinone.createatomania.content.ControlRodBlockEntity;
import com.kuinone.createatomania.content.ContentRegistry;
import com.kuinone.createatomania.content.detector.DetectorBlockEntity;
import com.kuinone.createatomania.content.detector.DetectorKind;
import com.kuinone.createatomania.content.fuel.FuelBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block entity types for every reactor block. */
public final class AtomaniaBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
		DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateAtomania.MODID);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelBlockEntity>> FUEL_BLOCK =
		BLOCK_ENTITIES.register("fuel_block",
			() -> BlockEntityType.Builder.of(FuelBlockEntity::new,
					ContentRegistry.FUEL_BLOCK.get())
				.build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ControlRodBlockEntity>> CONTROL_ROD =
		BLOCK_ENTITIES.register("control_rod",
			() -> BlockEntityType.Builder.of(ControlRodBlockEntity::new, ContentRegistry.CONTROL_ROD.get())
				.build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DetectorBlockEntity>> GEIGER_COUNTER =
		BLOCK_ENTITIES.register("geiger_counter",
			() -> BlockEntityType.Builder.of(
					(pos, state) -> new DetectorBlockEntity(pos, state, DetectorKind.RADIATION),
					ContentRegistry.GEIGER_COUNTER.get())
				.build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DetectorBlockEntity>> NEUTRON_FLUX_COUNTER =
		BLOCK_ENTITIES.register("neutron_flux_counter",
			() -> BlockEntityType.Builder.of(
					(pos, state) -> new DetectorBlockEntity(pos, state, DetectorKind.NEUTRON_FLUX),
					ContentRegistry.NEUTRON_FLUX_COUNTER.get())
				.build(null));

	private AtomaniaBlockEntities() {}
}