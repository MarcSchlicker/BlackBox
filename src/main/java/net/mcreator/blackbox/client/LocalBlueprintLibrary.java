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

	public static BlueprintSummary save(byte[] data) {
		CompoundTag root = BlueprintLibrary.decode(data);
		BlueprintSummary summary = BlueprintLibrary.summarize(root);
		if (summary == null) {
			return null;
		}
		try {
			Path directory = directory();
			Files.createDirectories(directory);
			for (BlueprintSummary existing : list()) {
				if (!existing.id().equals(summary.id()) && existing.name().equalsIgnoreCase(summary.name()) && existing.author().equalsIgnoreCase(summary.author())) {
					root.putString("id", existing.id());
					root.putInt("revision", existing.revision() + 1);
					data = BlueprintLibrary.encode(root);
					summary = BlueprintLibrary.summarize(root);
					break;
				}
			}
			Files.write(directory.resolve(summary.id() + ".nbt"), data);
			return summary;
		} catch (IOException exception) {
			BlackboxMod.LOGGER.error("Could not save local farm blueprint {}", summary.id(), exception);
			return null;
		}
	}

	public static BlueprintSummary rename(String blueprintId, String requestedName) {
		String name = requestedName == null ? "" : requestedName.trim();
		if (name.isEmpty() || name.length() > 48) {
			return null;
		}
		byte[] data = load(blueprintId);
		CompoundTag root = BlueprintLibrary.decode(data);
		BlueprintSummary summary = BlueprintLibrary.summarize(root);
		if (summary == null) {
			return null;
		}
		root.putString("name", name);
		root.putInt("revision", summary.revision() + 1);
		byte[] encoded = BlueprintLibrary.encode(root);
		try {
			Files.write(directory().resolve(blueprintId + ".nbt"), encoded);
			return BlueprintLibrary.summarize(root);
		} catch (IOException exception) {
			BlackboxMod.LOGGER.error("Could not rename local farm blueprint {}", blueprintId, exception);
			return null;
		}
	}

	public static boolean delete(String blueprintId) {
		if (blueprintId == null || !blueprintId.matches("[0-9a-fA-F-]{36}")) {
			return false;
		}
		try {
			return Files.deleteIfExists(directory().resolve(blueprintId + ".nbt"));
		} catch (IOException exception) {
			BlackboxMod.LOGGER.error("Could not delete local farm blueprint {}", blueprintId, exception);
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
