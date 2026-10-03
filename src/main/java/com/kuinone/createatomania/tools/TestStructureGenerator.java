package com.kuinone.createatomania.tools;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

/**
 * Writes the empty gametest structure the mod's tests are placed into.
 * <p>
 * Hand-writing this file as raw bytes is needlessly error-prone, so the structure is built
 * with Minecraft's own NBT tags and written with its own compressed writer, which guarantees
 * exactly the schema its reader expects.
 */
public final class TestStructureGenerator {

	private TestStructureGenerator() {}

	public static void main(String[] args) throws Exception {
		Path output = Path.of(args[0]);

		CompoundTag root = new CompoundTag();

		// A 3x3x3 volume. The size field is a list of three ints.
		root.put("size", list(IntTag.valueOf(3), IntTag.valueOf(3), IntTag.valueOf(3)));

		// No entities and no blocks: the tests only inspect registries.
		root.put("entities", new ListTag());
		root.put("blocks", new ListTag());

		Path parent = output.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		NbtIo.writeCompressed(root, output);
		System.out.println("wrote " + output.toAbsolutePath());
	}

	private static ListTag list(Tag... tags) {
		ListTag list = new ListTag();
		list.addAll(List.of(tags));
		return list;
	}
}
