package net.mcreator.blackbox.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import net.mcreator.blackbox.BlackboxMod;
import net.mcreator.blackbox.util.BlueprintLibrary;
import net.mcreator.blackbox.util.BlueprintLibrary.BlueprintSummary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class LocalBlueprintLibrary {
	private static final int MAX_ENTRIES = 256;

	private LocalBlueprintLibrary() {
	}

	public static List<BlueprintSummary> list() {
		Path directory = directory();
		if (!Files.isDirectory(directory)) {
			return List.of();
		}
		try (Stream<Path> paths = Files.list(directory)) {
			return paths.filter(path -> path.getFileName().toString().endsWith(".nbt"))
					.limit(MAX_ENTRIES)
					.map(LocalBlueprintLibrary::readSummary)
					.filter(Objects::nonNull)
					.sorted((left, right) -> left.name().compareToIgnoreCase(right.name()))
					.toList();
		} catch (IOException exception) {
			BlackboxMod.LOGGER.error("Could not list local farm blueprints", exception);
			return List.of();
		}
	}

	public static boolean save(byte[] data) {
		CompoundTag root = BlueprintLibrary.decode(data);
		BlueprintSummary summary = BlueprintLibrary.summarize(root);
		if (summary == null) {
			return false;
		}
		try {
			Path directory = directory();
			Files.createDirectories(directory);
			for (BlueprintSummary existing : list()) {
				if (!existing.id().equals(summary.id()) && existing.name().equalsIgnoreCase(summary.name()) && existing.author().equalsIgnoreCase(summary.author())) {
					Files.deleteIfExists(directory.resolve(existing.id() + ".nbt"));
				}
			}
			Files.write(directory.resolve(summary.id() + ".nbt"), data);
			return true;
		} catch (IOException exception) {
			BlackboxMod.LOGGER.error("Could not save local farm blueprint {}", summary.id(), exception);
			return false;
		}
	}

	public static byte[] load(String blueprintId) {
		if (blueprintId == null || !blueprintId.matches("[0-9a-fA-F-]{36}")) {
			return new byte[0];
		}
		try {
			byte[] data = Files.readAllBytes(directory().resolve(blueprintId + ".nbt"));
			return data.length <= BlueprintLibrary.MAX_TRANSFER_BYTES ? data : new byte[0];
		} catch (IOException exception) {
			return new byte[0];
		}
	}

	private static BlueprintSummary readSummary(Path path) {
		try {
			byte[] data = Files.readAllBytes(path);
			return data.length <= BlueprintLibrary.MAX_TRANSFER_BYTES ? BlueprintLibrary.summarize(BlueprintLibrary.decode(data)) : null;
		} catch (IOException exception) {
			return null;
		}
	}

	private static Path directory() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("blackbox_blueprints").resolve("local");
	}
}
