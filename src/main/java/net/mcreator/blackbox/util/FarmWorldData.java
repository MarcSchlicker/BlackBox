package net.mcreator.blackbox.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FarmWorldData extends SavedData {
	private static final String DATA_NAME = "blackbox_farms";
	private final Map<UUID, FarmRecord> farms = new LinkedHashMap<>();
	private ListTag measurements = new ListTag();

	public static FarmWorldData get(MinecraftServer server) {
		return server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(
				new SavedData.Factory<>(FarmWorldData::new, FarmWorldData::load), DATA_NAME);
	}

	public static FarmWorldData load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		FarmWorldData data = new FarmWorldData();
		ListTag farmList = tag.getList("Farms", Tag.TAG_COMPOUND);
		for (int index = 0; index < farmList.size(); index++) {
			FarmRecord record = FarmRecord.load(farmList.getCompound(index));
			if (record != null) {
				data.farms.put(record.coreId(), record);
			}
		}
		data.measurements = tag.getList("Measurements", Tag.TAG_COMPOUND).copy();
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		ListTag farmList = new ListTag();
		for (FarmRecord record : this.farms.values()) {
			farmList.add(record.save());
		}
		tag.put("Farms", farmList);
		tag.put("Measurements", this.measurements.copy());
		return tag;
	}

	public ListTag measurements() {
		return this.measurements.copy();
	}

	public void setMeasurements(ListTag measurements) {
		this.measurements = measurements.copy();
		setDirty();
	}

	public void upsert(FarmRecord record) {
		this.farms.put(record.coreId(), record);
		setDirty();
	}

	public FarmRecord find(UUID coreId) {
		return this.farms.get(coreId);
	}

	public List<FarmRecord> farms() {
		List<FarmRecord> result = new ArrayList<>(this.farms.values());
		result.sort(Comparator.comparing(FarmRecord::name, String.CASE_INSENSITIVE_ORDER).thenComparing(FarmRecord::coreId));
		return List.copyOf(result);
	}

	public boolean remove(UUID coreId) {
		boolean removed = this.farms.remove(coreId) != null;
		if (removed) {
			setDirty();
		}
		return removed;
	}

	public record FarmRecord(UUID coreId, int sizeChunks, String dimension, String name, UUID owner, String ownerName, long lastUsed) {
		public FarmRecord {
			sizeChunks = Math.max(1, Math.min(3, sizeChunks));
			dimension = dimension == null ? FarmEnvironment.STANDARD.dimension().location().toString() : dimension;
			name = name == null || name.isBlank() ? "Unnamed farm" : name.trim();
			ownerName = ownerName == null || ownerName.isBlank() ? "Unknown" : ownerName.trim();
		}

		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("CoreId", this.coreId.toString());
			tag.putInt("Size", this.sizeChunks);
			tag.putString("Dimension", this.dimension);
			tag.putString("Name", this.name);
			tag.putString("Owner", this.owner == null ? "" : this.owner.toString());
			tag.putString("OwnerName", this.ownerName);
			tag.putLong("LastUsed", this.lastUsed);
			return tag;
		}

		private static FarmRecord load(CompoundTag tag) {
			try {
				UUID coreId = UUID.fromString(tag.getString("CoreId"));
				UUID owner = tag.getString("Owner").isBlank() ? null : UUID.fromString(tag.getString("Owner"));
				return new FarmRecord(coreId, tag.getInt("Size"), tag.getString("Dimension"), tag.getString("Name"), owner,
						tag.getString("OwnerName"), tag.getLong("LastUsed"));
			} catch (IllegalArgumentException exception) {
				return null;
			}
		}
	}
}
