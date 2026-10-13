package io.github.marcschlicker.blackbox.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import io.github.marcschlicker.blackbox.init.BlackboxModBlockEntities;
import io.github.marcschlicker.blackbox.util.OutputFilterData;
import io.github.marcschlicker.blackbox.util.SpawnerIntegration;

import java.util.ArrayList;
import java.util.List;

/**
 * A normal Blackbox inventory and routing machine with five dedicated Apothic
 * Spawner sources. The first five input slots are spawners; the other four are
 * Masher or category-filter modules.
 */
public class SpawnerBlackboxBlockEntity extends BlackboxBlockBlockEntity {
	public static final int SPAWNER_START = CORE_SLOT;
	public static final int SPAWNER_END = SPAWNER_START + 5;
	public static final int MODULE_START = SPAWNER_END;
	public static final int MODULE_END = INPUT_END;
	// Kept as aliases so existing saves and the legacy menu class remain readable.
	public static final int UPGRADE_START = MODULE_START;
	public static final int UPGRADE_END = MODULE_END;
	private static final String SOURCE_STATS_TAG = "SpawnerSourceStats";
	private static final String LEGACY_DROP_STATS_TAG = "SpawnerDropStats";
	private static final String LEGACY_TOTAL_ROLLS_TAG = "SpawnerLootRolls";
	private static final String PENDING_XP_FLUID_TAG = "SpawnerPendingExperienceFluid";
	private static final String NEXT_TICKS_TAG = "SpawnerNextTicks";
	private static final String RANDOM_TAG = "SpawnerRandomState";
	private final long[] nextSpawnTicks = new long[SPAWNER_END - SPAWNER_START];
	private final List<SpawnerSourceStatistic> sourceStatistics = new ArrayList<>();
	private List<DropStatistic> observedDropsCache = List.of();
	private long observedLootRollsCache;
	private boolean observedDropsCacheDirty = true;
	private long pendingExperienceFluid;
	private long randomState;

	public SpawnerBlackboxBlockEntity(BlockPos position, BlockState state) {
		super(BlackboxModBlockEntities.SPAWNER_BLACKBOX.get(), position, state);
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.blackbox.spawner_blackbox");
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(tag, lookupProvider);
		this.pendingExperienceFluid = Math.max(0L, tag.getLong(PENDING_XP_FLUID_TAG));
		if (this.pendingExperienceFluid == 0L && tag.contains("SpawnerExperience")) {
			this.pendingExperienceFluid = Math.max(0L, tag.getLong("SpawnerExperience")) * SpawnerIntegration.MILLIBUCKETS_PER_EXPERIENCE_POINT;
		}
		this.randomState = tag.getLong(RANDOM_TAG);
		long[] savedTicks = tag.getLongArray(NEXT_TICKS_TAG);
		System.arraycopy(savedTicks, 0, this.nextSpawnTicks, 0, Math.min(savedTicks.length, this.nextSpawnTicks.length));
		this.sourceStatistics.clear();
		ListTag savedSources = tag.getList(SOURCE_STATS_TAG, Tag.TAG_COMPOUND);
		for (int index = 0; index < savedSources.size(); index++) {
			CompoundTag saved = savedSources.getCompound(index);
			int sourceSlot = saved.getInt("Slot");
			ItemStack spawner = ItemStack.parseOptional(lookupProvider, saved.getCompound("Spawner"));
			if (sourceSlot < SPAWNER_START || sourceSlot >= SPAWNER_END || spawner.isEmpty()) {
				continue;
			}
			SpawnerSourceStatistic source = new SpawnerSourceStatistic(sourceSlot, spawner.copyWithCount(1), Math.max(0L, saved.getLong("Rolls")));
			ListTag savedDrops = saved.getList("Drops", Tag.TAG_COMPOUND);
			for (int dropIndex = 0; dropIndex < savedDrops.size(); dropIndex++) {
				CompoundTag savedDrop = savedDrops.getCompound(dropIndex);
				ItemStack stack = ItemStack.parseOptional(lookupProvider, savedDrop.getCompound("Stack"));
				if (!stack.isEmpty()) {
					String group = savedDrop.getString("FilterGroup");
					mergeDrop(source.drops(), stack, group.isEmpty() ? SpawnerIntegration.apothicFilterGroup(stack) : group,
							Math.max(0L, savedDrop.getLong("Amount")), Math.max(0L, savedDrop.getLong("Occurrences")));
				}
			}
			this.sourceStatistics.add(source);
		}
		invalidateObservedDrops();
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(tag, lookupProvider);
		tag.putLong(PENDING_XP_FLUID_TAG, this.pendingExperienceFluid);
		tag.putLong(RANDOM_TAG, this.randomState);
		tag.putLongArray(NEXT_TICKS_TAG, this.nextSpawnTicks);
		tag.remove(LEGACY_DROP_STATS_TAG);
		tag.remove(LEGACY_TOTAL_ROLLS_TAG);
		ListTag savedSources = new ListTag();
		for (SpawnerSourceStatistic source : this.sourceStatistics) {
			CompoundTag saved = new CompoundTag();
			saved.putInt("Slot", source.slot());
			saved.put("Spawner", source.spawner().copyWithCount(1).saveOptional(lookupProvider));
			saved.putLong("Rolls", source.rolls());
			ListTag savedDrops = new ListTag();
			for (DropStatistic statistic : source.drops()) {
				CompoundTag savedDrop = new CompoundTag();
				savedDrop.put("Stack", statistic.stack().copyWithCount(1).saveOptional(lookupProvider));
				if (!statistic.filterGroup().isEmpty()) {
					savedDrop.putString("FilterGroup", statistic.filterGroup());
				}
				savedDrop.putLong("Amount", statistic.amount());
				savedDrop.putLong("Occurrences", statistic.occurrences());
				savedDrops.add(savedDrop);
			}
			saved.put("Drops", savedDrops);
			savedSources.add(saved);
		}
		tag.put(SOURCE_STATS_TAG, savedSources);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot >= SPAWNER_START && slot < SPAWNER_END) {
			return SpawnerIntegration.isApothicSpawner(stack);
		}
		if (slot >= MODULE_START && slot < MODULE_END) {
			return SpawnerIntegration.isMobMasherUpgrade(stack);
		}
		return super.canPlaceItem(slot, stack);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		boolean sourceChanged = slot >= SPAWNER_START && slot < SPAWNER_END
				&& !ItemStack.isSameItemSameComponents(getItem(slot), stack);
		super.setItem(slot, stack);
		if (sourceChanged) {
			invalidateObservedDrops();
		}
	}

	public boolean isReady() {
		return SpawnerIntegration.isAvailable() && spawnerCount() > 0;
	}

	public int spawnerCount() {
		int count = 0;
		for (int slot = SPAWNER_START; slot < SPAWNER_END; slot++) {
			if (SpawnerIntegration.isApothicSpawner(getItem(slot))) {
				count++;
			}
		}
		return count;
	}

	public List<ItemStack> masherUpgrades() {
		List<ItemStack> upgrades = new ArrayList<>();
		for (int slot = MODULE_START; slot < MODULE_END; slot++) {
			ItemStack module = getItem(slot);
			if (SpawnerIntegration.isMobMasherUpgrade(module)) {
				upgrades.add(module.copy());
			}
		}
		return upgrades;
	}

	public boolean isDropAllowed(ItemStack drop) {
		if (drop.isEmpty()) {
			return false;
		}
		ItemStack filter = getItem(FILTER_UPGRADE_SLOT);
		if (!OutputFilterData.isSpawnerArmorEnabled(filter) && drop.getItem() instanceof ArmorItem) {
			return false;
		}
		if (!OutputFilterData.isSpawnerGemsEnabled(filter) && SpawnerIntegration.isGem(drop)) {
			return false;
		}
		return this.level == null || OutputFilterData.isSpawnerDropEnabled(filter, drop, this.level.registryAccess());
	}

	public long nextSpawnTick(int index) {
		return index < 0 || index >= this.nextSpawnTicks.length ? 0L : this.nextSpawnTicks[index];
	}

	public void setNextSpawnTick(int index, long tick) {
		if (index >= 0 && index < this.nextSpawnTicks.length && this.nextSpawnTicks[index] != tick) {
			this.nextSpawnTicks[index] = Math.max(0L, tick);
			setChanged();
		}
	}

	public int nextRandomInt(int bound) {
		if (bound <= 1) {
			return 0;
		}
		return (int) Math.floorMod(nextRandomLong(), bound);
	}

	private long nextRandomLong() {
		if (this.randomState == 0L) {
			this.randomState = mix64(this.worldPosition.asLong() ^ 0xB10CB0A5L);
		}
		this.randomState += 0x9E3779B97F4A7C15L;
		setChanged();
		return mix64(this.randomState);
	}

	public void recordLootRoll(int sourceSlot, ItemStack spawner, int experience) {
		sourceStatistic(sourceSlot, spawner).incrementRolls();
		long produced = (long) Math.max(0, experience) * SpawnerIntegration.MILLIBUCKETS_PER_EXPERIENCE_POINT;
		this.pendingExperienceFluid = Math.min(Long.MAX_VALUE - produced, this.pendingExperienceFluid) + produced;
		invalidateObservedDrops();
		setChanged();
	}

	/** XP remains pending while the normal fluid output tank is full. */
	public void flushExperienceFluid() {
		if (this.pendingExperienceFluid <= 0L) {
			return;
		}
		long moved = resources().outputFluids().fillLong(SpawnerIntegration.experienceFluid(this.pendingExperienceFluid), this.pendingExperienceFluid, IFluidHandler.FluidAction.EXECUTE);
		if (moved > 0L) {
			this.pendingExperienceFluid -= moved;
			setChanged();
		}
	}

	public void recordObservedDrop(int sourceSlot, ItemStack spawner, ItemStack drop) {
		if (drop.isEmpty()) {
			return;
		}
		mergeDrop(sourceStatistic(sourceSlot, spawner).drops(), drop, SpawnerIntegration.apothicFilterGroup(drop), drop.getCount(), 1L);
		invalidateObservedDrops();
		setChanged();
	}

	private SpawnerSourceStatistic sourceStatistic(int sourceSlot, ItemStack spawner) {
		for (int index = this.sourceStatistics.size() - 1; index >= 0; index--) {
			SpawnerSourceStatistic current = this.sourceStatistics.get(index);
			if (current.slot() != sourceSlot) {
				continue;
			}
			if (SpawnerIntegration.hasSameSpawnerConfiguration(current.spawner(), spawner)) {
				return current;
			}
			this.sourceStatistics.remove(index);
			invalidateObservedDrops();
		}
		SpawnerSourceStatistic source = new SpawnerSourceStatistic(sourceSlot, spawner.copyWithCount(1), 0L);
		this.sourceStatistics.add(source);
		invalidateObservedDrops();
		return source;
	}

	private static void mergeDrop(List<DropStatistic> drops, ItemStack drop, String group, long amount, long occurrences) {
		for (int index = 0; index < drops.size(); index++) {
			DropStatistic current = drops.get(index);
			boolean sameEntry = !group.isEmpty()
					? group.equals(current.filterGroup())
					: current.filterGroup().isEmpty() && ItemStack.isSameItemSameComponents(current.stack(), drop);
			if (sameEntry) {
				drops.set(index, new DropStatistic(current.stack(), current.filterGroup(), current.amount() + amount, current.occurrences() + occurrences));
				return;
			}
		}
		drops.add(new DropStatistic(drop.copyWithCount(1), group, amount, occurrences));
	}

	public List<DropStatistic> observedDrops() {
		rebuildObservedDropsCache();
		return this.observedDropsCache;
	}

	public long totalLootRolls() {
		rebuildObservedDropsCache();
		return this.observedLootRollsCache;
	}

	private void invalidateObservedDrops() {
		this.observedDropsCacheDirty = true;
	}

	private void rebuildObservedDropsCache() {
		if (!this.observedDropsCacheDirty) {
			return;
		}
		List<DropStatistic> aggregated = new ArrayList<>();
		long totalRolls = 0L;
		for (SpawnerSourceStatistic source : this.sourceStatistics) {
			if (!isSourceActive(source)) {
				continue;
			}
			totalRolls = Math.min(Long.MAX_VALUE - source.rolls(), totalRolls) + source.rolls();
			for (DropStatistic statistic : source.drops()) {
				mergeDrop(aggregated, statistic.stack(), statistic.filterGroup(), statistic.amount(), statistic.occurrences());
			}
		}
		this.observedDropsCache = List.copyOf(aggregated);
		this.observedLootRollsCache = totalRolls;
		this.observedDropsCacheDirty = false;
	}

	private boolean isSourceActive(SpawnerSourceStatistic source) {
		return source.slot() >= SPAWNER_START && source.slot() < SPAWNER_END
				&& SpawnerIntegration.hasSameSpawnerConfiguration(source.spawner(), getItem(source.slot()));
	}

	private static long mix64(long value) {
		long mixed = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
		mixed = (mixed ^ mixed >>> 27) * 0x94D049BB133111EBL;
		return mixed ^ mixed >>> 31;
	}

	public record DropStatistic(ItemStack stack, String filterGroup, long amount, long occurrences) {
	}

	private static final class SpawnerSourceStatistic {
		private final int slot;
		private final ItemStack spawner;
		private long rolls;
		private final List<DropStatistic> drops = new ArrayList<>();

		private SpawnerSourceStatistic(int slot, ItemStack spawner, long rolls) {
			this.slot = slot;
			this.spawner = spawner;
			this.rolls = rolls;
		}

		private int slot() {
			return this.slot;
		}

		private ItemStack spawner() {
			return this.spawner;
		}

		private long rolls() {
			return this.rolls;
		}

		private void incrementRolls() {
			this.rolls = Math.min(Long.MAX_VALUE, this.rolls + 1L);
		}

		private List<DropStatistic> drops() {
			return this.drops;
		}
	}
}
