package net.mcreator.blackbox.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;

import net.mcreator.blackbox.BlackboxMod;
import net.mcreator.blackbox.config.BlackboxConfig;
import net.mcreator.blackbox.init.BlackboxModBlocks;
import net.mcreator.blackbox.item.BlueprintItem;
import net.mcreator.blackbox.item.BlueprintItem.StorageScope;
import net.mcreator.blackbox.network.BlueprintTransferMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public final class BlueprintLibrary {
	private static final int DATA_VERSION = 1;
	public static final int MAX_TRANSFER_BYTES = 2_000_000;

	private BlueprintLibrary() {
	}

	public static boolean save(ServerPlayer player, ServerLevel level, ItemStack blueprint, StorageScope storageScope, String requestedName) {
		FarmCell cell = FarmDimensionRuntime.getAssignedCell(player).orElse(null);
		if (cell == null) {
			return false;
		}
		ListTag blocks = new ListTag();
		for (int x = cell.minBlockX(); x <= cell.maxBlockX(); x++) {
			for (int z = cell.minBlockZ(); z <= cell.maxBlockZ(); z++) {
				int top = Math.min(level.getMaxBuildHeight(), level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1);
				for (int y = Math.max(1, level.getMinBuildHeight()); y < top; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = level.getBlockState(pos);
					if (!canStore(state, pos, cell)) {
						continue;
					}
					if (blocks.size() >= BlackboxConfig.MAX_BLUEPRINT_BLOCKS.get()) {
						player.sendSystemMessage(Component.translatable("message.blackbox.blueprint.too_large", BlackboxConfig.MAX_BLUEPRINT_BLOCKS.get()).withStyle(ChatFormatting.RED));
						return false;
					}
					CompoundTag block = new CompoundTag();
					block.putInt("x", x - cell.minBlockX());
					block.putInt("y", y);
					block.putInt("z", z - cell.minBlockZ());
					block.put("state", NbtUtils.writeBlockState(state));
					blocks.add(block);
				}
			}
		}

		String fallbackId = UUID.randomUUID().toString();
		String cleanName = requestedName == null ? "" : requestedName.trim();
		if (cleanName.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.blackbox.blueprint.name_required").withStyle(ChatFormatting.RED), true);
			return false;
		}
		if (cleanName.length() > 48) {
			cleanName = cleanName.substring(0, 48).trim();
		}
		String name = cleanName;
		String author = player.getGameProfile().getName();
		BlueprintSummary existing = storageScope == StorageScope.SERVER ? list(player.server).stream()
					.filter(summary -> summary.name().equalsIgnoreCase(name) && summary.author().equalsIgnoreCase(author))
					.findFirst()
					.orElse(null) : null;
		String id = existing == null ? fallbackId : existing.id();
		CompoundTag root = new CompoundTag();
		root.putInt("version", DATA_VERSION);
		root.putInt("revision", existing == null ? 1 : existing.revision() + 1);
		root.putString("id", id);
		root.putString("name", name);
		root.putString("author", author);
		root.putInt("block_count", blocks.size());
		root.put("blocks", blocks);
		if (storageScope == StorageScope.LOCAL) {
			byte[] data = encode(root);
			if (data.length == 0 || data.length > MAX_TRANSFER_BYTES) {
				player.sendSystemMessage(Component.translatable("message.blackbox.blueprint.save_failed").withStyle(ChatFormatting.RED));
				return false;
			}
			BlueprintTransferMessage.sendLocalSave(player, id, name, data);
			return true;
		}
		try {
			Path directory = blueprintDirectory(player.server);
			Files.createDirectories(directory);
			NbtIo.writeCompressed(root, directory.resolve(id + ".nbt"));
			player.sendSystemMessage(Component.translatable("message.blackbox.blueprint.saved", name, blocks.size()).withStyle(ChatFormatting.GREEN));
			return true;
		} catch (IOException exception) {
			BlackboxMod.LOGGER.error("Could not save farm blueprint {}", id, exception);
			player.sendSystemMessage(Component.translatable("message.blackbox.blueprint.save_failed").withStyle(ChatFormatting.RED));
			return false;
		}
	}

	public static byte[] loadCompressed(MinecraftServer server, String blueprintId) {
		if (find(server, blueprintId) == null) {
			return new byte[0];
		}
		try {
			byte[] data = Files.readAllBytes(blueprintDirectory(server).resolve(blueprintId + ".nbt"));
			return data.length <= MAX_TRANSFER_BYTES ? data : new byte[0];
		} catch (IOException exception) {
			BlackboxMod.LOGGER.error("Could not read farm blueprint {}", blueprintId, exception);
			return new byte[0];
		}
	}

	public static boolean storeUploaded(ServerPlayer player, byte[] data) {
		if (!player.hasPermissions(2)) {
			player.displayClientMessage(Component.translatable("message.blackbox.blueprint.publish_denied").withStyle(ChatFormatting.RED), true);
			return false;
		}
		CompoundTag root = decode(data);
		BlueprintSummary summary = summarize(root);
		if (summary == null || root.getList("blocks", Tag.TAG_COMPOUND).size() > BlackboxConfig.MAX_BLUEPRINT_BLOCKS.get()
				|| list(player.server).size() >= BlackboxConfig.MAX_SERVER_BLUEPRINTS.get() && find(player.server, summary.id()) == null) {
			player.displayClientMessage(Component.translatable("message.blackbox.blueprint.load_failed").withStyle(ChatFormatting.RED), true);
			return false;
		}
		try {
			Path directory = blueprintDirectory(player.server);
			Files.createDirectories(directory);
			NbtIo.writeCompressed(root, directory.resolve(summary.id() + ".nbt"));
			player.displayClientMessage(Component.translatable("message.blackbox.blueprint.published", summary.name()).withStyle(ChatFormatting.GREEN), true);
			return true;
		} catch (IOException exception) {
			BlackboxMod.LOGGER.error("Could not publish farm blueprint {}", summary.id(), exception);
			return false;
		}
	}

	public static List<BlueprintSummary> list(MinecraftServer server) {
		Path directory = blueprintDirectory(server);
		if (!Files.isDirectory(directory)) {
			return List.of();
		}
		try (Stream<Path> paths = Files.list(directory)) {
			return paths.filter(path -> path.getFileName().toString().endsWith(".nbt"))
					.limit(BlackboxConfig.MAX_SERVER_BLUEPRINTS.get())
					.map(BlueprintLibrary::readSummary)
					.filter(Objects::nonNull)
					.sorted((left, right) -> left.name().compareToIgnoreCase(right.name()))
					.toList();
		} catch (IOException exception) {
			BlackboxMod.LOGGER.error("Could not list farm blueprints", exception);
			return List.of();
		}
	}

	public static BlueprintSummary find(MinecraftServer server, String blueprintId) {
		if (blueprintId == null || !blueprintId.matches("[0-9a-fA-F-]{36}")) {
			return null;
		}
		Path file = blueprintDirectory(server).resolve(blueprintId + ".nbt");
		return Files.isRegularFile(file) ? readSummary(file) : null;
	}

	public static boolean rename(ServerPlayer player, String blueprintId, String requestedName) {
		BlueprintSummary summary = find(player.server, blueprintId);
		String name = cleanName(requestedName);
		if (summary == null || name.isEmpty() || !canManage(player, summary)) {
			return false;
		}
		Path file = blueprintDirectory(player.server).resolve(blueprintId + ".nbt");
		try {
			CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.create(16L * 1024L * 1024L));
			root.putString("name", name);
			root.putInt("revision", Math.max(1, summary.revision() + 1));
			NbtIo.writeCompressed(root, file);
			player.displayClientMessage(Component.translatable("message.blackbox.blueprint.renamed", name), true);
			return true;
		} catch (IOException exception) {
			BlackboxMod.LOGGER.error("Could not rename farm blueprint {}", blueprintId, exception);
			return false;
		}
	}

	public static boolean delete(ServerPlayer player, String blueprintId) {
		BlueprintSummary summary = find(player.server, blueprintId);
		if (summary == null || !canManage(player, summary)) {
			return false;
		}
		try {
			boolean removed = Files.deleteIfExists(blueprintDirectory(player.server).resolve(blueprintId + ".nbt"));
			if (removed) {
				player.displayClientMessage(Component.translatable("message.blackbox.blueprint.deleted", summary.name()), true);
			}
			return removed;
		} catch (IOException exception) {
			BlackboxMod.LOGGER.error("Could not delete farm blueprint {}", blueprintId, exception);
			return false;
		}
	}

	private static BlueprintSummary readSummary(Path file) {
		try {
			CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.create(16L * 1024L * 1024L));
			return summarize(root);
		} catch (IOException exception) {
			BlackboxMod.LOGGER.warn("Ignoring unreadable farm blueprint {}", file.getFileName(), exception);
			return null;
		}
	}

	public static boolean apply(ServerPlayer player, ServerLevel level, String blueprintId) {
		if (!blueprintId.matches("[0-9a-fA-F-]{36}")) {
			return false;
		}
		CompoundTag root;
		try {
			Path file = blueprintDirectory(player.server).resolve(blueprintId + ".nbt");
			if (!Files.isRegularFile(file)) {
				player.sendSystemMessage(Component.translatable("message.blackbox.blueprint.not_on_server").withStyle(ChatFormatting.RED));
				return false;
			}
			root = NbtIo.readCompressed(file, NbtAccounter.create(16L * 1024L * 1024L));
		} catch (IOException exception) {
			BlackboxMod.LOGGER.error("Could not load farm blueprint {}", blueprintId, exception);
			player.sendSystemMessage(Component.translatable("message.blackbox.blueprint.load_failed").withStyle(ChatFormatting.RED));
			return false;
		}

		return applyRoot(player, level, root);
	}

	public static boolean applyUploaded(ServerPlayer player, ServerLevel level, String expectedId, byte[] data) {
		CompoundTag root = decode(data);
		BlueprintSummary summary = summarize(root);
		if (summary == null || !summary.id().equals(expectedId)) {
			player.displayClientMessage(Component.translatable("message.blackbox.blueprint.load_failed").withStyle(ChatFormatting.RED), true);
			return false;
		}
		return applyRoot(player, level, root);
	}

	private static boolean applyRoot(ServerPlayer player, ServerLevel level, CompoundTag root) {
		FarmCell cell = FarmDimensionRuntime.getAssignedCell(player).orElse(null);
		if (cell == null || root.getList("blocks", Tag.TAG_COMPOUND).size() > BlackboxConfig.MAX_BLUEPRINT_BLOCKS.get()) {
			return false;
		}
		List<Placement> placements = new ArrayList<>();
		int conflicts = 0;
		ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
		for (int index = 0; index < blocks.size() && placements.size() < BlackboxConfig.MAX_BLUEPRINT_BLOCKS.get(); index++) {
			CompoundTag block = blocks.getCompound(index);
			BlockPos target = new BlockPos(cell.minBlockX() + block.getInt("x"), block.getInt("y"), cell.minBlockZ() + block.getInt("z"));
			if (!cell.contains(target) || target.getY() <= 0 || target.equals(cell.inputPos()) || target.equals(cell.outputPos())) {
				continue;
			}
			BlockState state = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), block.getCompound("state"));
			if (!canStore(state, target, cell)) {
				continue;
			}
			BlockState existing = level.getBlockState(target);
			if (existing.equals(state)) {
				continue;
			}
			if (!existing.isAir()) {
				conflicts++;
				continue;
			}
			placements.add(new Placement(target, state));
		}

		Map<Item, Integer> required = requiredMaterials(placements);
		if (!player.getAbilities().instabuild) {
			Map.Entry<Item, Integer> missing = firstMissing(player, required);
			if (missing != null) {
				player.sendSystemMessage(Component.translatable("message.blackbox.blueprint.missing_material", missing.getValue(), missing.getKey().getDescription()).withStyle(ChatFormatting.RED));
				return false;
			}
			consumeMaterials(player, required);
		}
		for (Placement placement : placements) {
			level.setBlock(placement.pos, placement.state, 3);
		}
		player.sendSystemMessage(Component.translatable("message.blackbox.blueprint.applied", placements.size(), conflicts).withStyle(ChatFormatting.GREEN));
		return true;
	}

	public static BlueprintSummary summarize(CompoundTag root) {
		if (root == null || root.getInt("version") != DATA_VERSION) {
			return null;
		}
		String id = root.getString("id");
		String name = root.getString("name").trim();
		String author = root.getString("author").trim();
		int blockCount = root.getInt("block_count");
		int revision = Math.max(1, root.getInt("revision"));
		if (!id.matches("[0-9a-fA-F-]{36}") || name.isEmpty() || name.length() > 64 || author.length() > 32 || blockCount < 0
				|| blockCount > BlackboxConfig.MAX_BLUEPRINT_BLOCKS.get()) {
			return null;
		}
		return new BlueprintSummary(id, name, author, blockCount, revision);
	}

	public static byte[] encode(CompoundTag root) {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			NbtIo.writeCompressed(root, output);
			return output.toByteArray();
		} catch (IOException exception) {
			BlackboxMod.LOGGER.error("Could not encode farm blueprint", exception);
			return new byte[0];
		}
	}

	public static CompoundTag decode(byte[] data) {
		if (data == null || data.length == 0 || data.length > MAX_TRANSFER_BYTES) {
			return null;
		}
		try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
			return NbtIo.readCompressed(input, NbtAccounter.create(16L * 1024L * 1024L));
		} catch (IOException exception) {
			BlackboxMod.LOGGER.warn("Could not decode transferred farm blueprint", exception);
			return null;
		}
	}

	private static boolean canStore(BlockState state, BlockPos pos, FarmCell cell) {
		return !state.isAir() && state.getFluidState().isEmpty() && !state.is(BlackboxModBlocks.EMERALD_BEDROCK.get())
				&& !pos.equals(cell.inputPos()) && !pos.equals(cell.outputPos()) && !BlackboxConfig.deniedBlocks().contains(state.getBlock())
				&& state.getBlock().asItem() != Items.AIR;
	}

	private static Map<Item, Integer> requiredMaterials(List<Placement> placements) {
		Map<Item, Integer> required = new HashMap<>();
		for (Placement placement : placements) {
			required.merge(placement.state.getBlock().asItem(), 1, Integer::sum);
		}
		return required;
	}

	private static Map.Entry<Item, Integer> firstMissing(ServerPlayer player, Map<Item, Integer> required) {
		for (Map.Entry<Item, Integer> entry : required.entrySet()) {
			int available = 0;
			for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
				ItemStack stack = player.getInventory().getItem(slot);
				if (stack.is(entry.getKey())) {
					available += stack.getCount();
				}
			}
			if (available < entry.getValue()) {
				return Map.entry(entry.getKey(), entry.getValue() - available);
			}
		}
		return null;
	}

	private static void consumeMaterials(ServerPlayer player, Map<Item, Integer> required) {
		for (Map.Entry<Item, Integer> entry : required.entrySet()) {
			int remaining = entry.getValue();
			for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
				ItemStack stack = player.getInventory().getItem(slot);
				if (stack.is(entry.getKey())) {
					int removed = Math.min(remaining, stack.getCount());
					stack.shrink(removed);
					remaining -= removed;
				}
			}
		}
		player.getInventory().setChanged();
	}

	private static Path blueprintDirectory(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("blackbox_blueprints");
	}

	private static String cleanName(String requestedName) {
		String clean = requestedName == null ? "" : requestedName.trim();
		return clean.length() > 48 ? clean.substring(0, 48).trim() : clean;
	}

	private static boolean canManage(ServerPlayer player, BlueprintSummary summary) {
		return player.hasPermissions(2) || summary.author().equalsIgnoreCase(player.getGameProfile().getName());
	}

	private record Placement(BlockPos pos, BlockState state) {
	}

	public record BlueprintSummary(String id, String name, String author, int blockCount, int revision) {
		public BlueprintSummary {
			revision = Math.max(1, revision);
		}
	}
}
