package com.kuinone.createatomania.radiation;

import com.kuinone.createatomania.CreateAtomania;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registers the radiation dose as an entity data attachment.
 * <p>
 * An attachment is the right container here rather than a capability: the dose is a single
 * number that any living entity may carry, it must survive saving and loading, and it does
 * not need to be synced to every client every tick. It is marked as copying on death so that
 * respawning does not conveniently wipe away a lethal dose.
 */
public final class RadiationAttachments {

	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
		DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CreateAtomania.MODID);

	/** Accumulated absorbed dose, in sieverts, on any living entity. */
	public static final DeferredHolder<AttachmentType<?>, AttachmentType<RadiationData>> RADIATION =
		ATTACHMENTS.register("radiation", () -> AttachmentType.builder(RadiationData::new)
			.serialize(RadiationData.CODEC)
			.copyOnDeath()
			.build());

	private RadiationAttachments() {}
}