package io.github.marcschlicker.blackbox.util;

import com.mojang.authlib.GameProfile;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Runtime-only bridge for the optional Apothic Spawners and Mob Grinding Utils content. */
public final class SpawnerIntegration {
	/**
	 * Apothic Spawners replaces the vanilla spawner's block item in-place.  Its
	 * registry name remains minecraft:spawner, rather than becoming an item in
	 * the apothic_spawners namespace.
	 */
	public static final ResourceLocation VANILLA_SPAWNER = ResourceLocation.withDefaultNamespace("spawner");
	public static final ResourceLocation LEGACY_APOTHIC_SPAWNER = ResourceLocation.fromNamespaceAndPath("apothic_spawners", "spawner");
	public static final ResourceLocation MOB_FAN = ResourceLocation.fromNamespaceAndPath("mob_grinding_utils", "fan");
	public static final ResourceLocation MOB_MASHER = ResourceLocation.fromNamespaceAndPath("mob_grinding_utils", "saw");
	public static final ResourceLocation FLUID_XP = ResourceLocation.fromNamespaceAndPath("mob_grinding_utils", "fluid_xp");
	private static final ResourceLocation APOTHEOSIS_RARITY_COMPONENT = ResourceLocation.fromNamespaceAndPath("apotheosis", "rarity");
	public static final int MILLIBUCKETS_PER_EXPERIENCE_POINT = 20;
	private static final String MGU_ID = "mob_grinding_utils";
	private static final String APOTHEOSIS_ID = "apotheosis";
	private static final String APOTHIC_SPAWNERS_ID = "apothic_spawners";
	private static final UUID KILLER_ID = UUID.nameUUIDFromBytes("blackbox:spawner_blackbox".getBytes(java.nio.charset.StandardCharsets.UTF_8));
	private static final int DEFAULT_MASHER_UPGRADE_LIMIT = 10;

	private SpawnerIntegration() {
	}

	public static boolean isAvailable() {
		return ModList.get().isLoaded(MGU_ID) && ModList.get().isLoaded(APOTHEOSIS_ID) && ModList.get().isLoaded(APOTHIC_SPAWNERS_ID);
	}

	public static boolean isApothicSpawner(ItemStack stack) {
		if (!isAvailable() || stack.isEmpty()) {
			return false;
		}
		ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
		return VANILLA_SPAWNER.equals(itemId) || LEGACY_APOTHIC_SPAWNER.equals(itemId);
	}

	/**
	 * A source remains the same farm source when its spawner data is unchanged.
	 * Optional mods can add cosmetic or transient item components while a spawner
	 * sits in an inventory; those must not hide its already observed drops.
	 */
	public static boolean hasSameSpawnerConfiguration(ItemStack first, ItemStack second) {
		if (!isApothicSpawner(first) || !isApothicSpawner(second) || first.getItem() != second.getItem()) {
			return false;
		}
		CompoundTag firstData = first.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
		CompoundTag secondData = second.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
		return firstData.equals(secondData);
	}

	/**
	 * Affix values, sockets and generated names change for every Apotheosis loot
	 * roll.  A filter should therefore identify such gear by its base item and
	 * loot rarity instead of treating every generated variation as a new entry.
	 */
	public static String apothicFilterGroup(ItemStack stack) {
		if (!isAvailable() || stack.isEmpty() || !BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(APOTHEOSIS_RARITY_COMPONENT)) {
			return "";
		}
		DataComponentType<?> component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(APOTHEOSIS_RARITY_COMPONENT);
		Object rarityHolder = getComponent(stack, component);
		ResourceLocation rarityId = dynamicHolderId(rarityHolder);
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return rarityId == null || itemId == null ? "" : "apotheosis/" + itemId + "/" + rarityId;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Object getComponent(ItemStack stack, DataComponentType<?> component) {
		return component == null ? null : stack.get((DataComponentType) component);
	}

	private static ResourceLocation dynamicHolderId(Object holder) {
		if (holder == null) {
			return null;
		}
		try {
			Object id = holder.getClass().getMethod("getId").invoke(holder);
			return id instanceof ResourceLocation location ? location : null;
		} catch (ReflectiveOperationException ignored) {
			return null;
		}
	}

	public static boolean isMobMasherUpgrade(ItemStack stack) {
		if (!isAvailable() || stack.isEmpty()) {
			return false;
		}
		ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id != null && MGU_ID.equals(id.getNamespace()) && id.getPath().startsWith("saw_upgrade_");
	}

	public static int lootingLevel(List<ItemStack> upgrades) {
		return upgradeCount(upgrades, "saw_upgrade_looting");
	}

	/** Uses Mob Grinding Utils' server setting so the Blackbox matches a real Masher. */
	public static int masherUpgradeLimit() {
		if (!ModList.get().isLoaded(MGU_ID)) {
			return DEFAULT_MASHER_UPGRADE_LIMIT;
		}
		try {
			Object configValue = Class.forName("mob_grinding_utils.config.ServerConfig").getField("MASHER_MAX_UPGRADES").get(null);
			Object value = configValue.getClass().getMethod("get").invoke(configValue);
			if (value instanceof Number number) {
				return Math.clamp(number.intValue(), 0, DEFAULT_MASHER_UPGRADE_LIMIT);
			}
		} catch (ReflectiveOperationException ignored) {
			// The optional integration must also work against a compatible MGU release.
		}
		return DEFAULT_MASHER_UPGRADE_LIMIT;
	}

	public static boolean hasFireUpgrade(List<ItemStack> upgrades) {
		return upgradeCount(upgrades, "saw_upgrade_fire") > 0;
	}

	private static int upgradeCount(List<ItemStack> upgrades, String path) {
		int count = 0;
		int limit = masherUpgradeLimit();
		for (ItemStack upgrade : upgrades) {
			ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(upgrade.getItem());
			if (id != null && MGU_ID.equals(id.getNamespace()) && path.equals(id.getPath())) {
				count = Math.min(limit, count + upgrade.getCount());
			}
		}
		return count;
	}

	public static SpawnerProfile readProfile(ItemStack stack) {
		if (!isApothicSpawner(stack)) {
			return null;
		}
		CompoundTag data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
		List<SpawnChoice> choices = new ArrayList<>();
		collectSpawnPotentialChoices(data.getList("SpawnPotentials", Tag.TAG_COMPOUND), choices);
		if (choices.isEmpty()) {
			SpawnChoice choice = readChoice(data.getCompound("SpawnData"), 1);
			if (choice != null) {
				choices.add(choice);
			}
		}
		if (choices.isEmpty()) {
			SpawnChoice choice = findChoice(data, 0);
			if (choice != null) {
				choices.add(choice);
			}
		}
		if (choices.isEmpty()) {
			return null;
		}
		int minDelay = positiveOr(data, "MinSpawnDelay", 200);
		int maxDelay = Math.max(minDelay, positiveOr(data, "MaxSpawnDelay", 800));
		int spawnCount = Math.clamp(positiveOr(data, "SpawnCount", 4), 1, 64);
		return new SpawnerProfile(choices, minDelay, maxDelay, spawnCount);
	}

	public static LootRoll rollMobLoot(ServerLevel level, SpawnerProfile profile, int choiceRoll, List<ItemStack> upgrades, BlockPos origin) {
		SpawnChoice choice = profile.choose(choiceRoll);
		if (choice == null) {
			return LootRoll.EMPTY;
		}
		Entity entity = choice.type().create(level);
		if (entity == null) {
			return LootRoll.EMPTY;
		}
		entity.moveTo(origin.getX() + 0.5D, origin.getY() + 1.0D, origin.getZ() + 0.5D, 0.0F, 0.0F);
		if (entity instanceof Mob mob) {
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(origin), MobSpawnType.SPAWNER, null);
		}
		if (!choice.entityData().isEmpty()) {
			entity.load(choice.entityData().copy());
			entity.moveTo(origin.getX() + 0.5D, origin.getY() + 1.0D, origin.getZ() + 0.5D, 0.0F, 0.0F);
		}
		if (hasFireUpgrade(upgrades)) {
			entity.setRemainingFireTicks(40);
		}
		FakePlayer killer = createKiller(level, origin, upgrades);
		DamageSource damage = level.damageSources().playerAttack(killer);
		LootParams.Builder builder = new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
				.withParameter(LootContextParams.THIS_ENTITY, entity)
				.withParameter(LootContextParams.DAMAGE_SOURCE, damage)
				.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer)
				.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
				.withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
				.withLuck(killer.getLuck());
		LootTable table = level.getServer().reloadableRegistries().getLootTable(entity.getType().getDefaultLootTable());
		int experience = entity instanceof Mob mob ? mob.getExperienceReward(level, killer) : 0;
		return new LootRoll(table.getRandomItems(builder.create(LootContextParamSets.ENTITY)), Math.max(0, experience));
	}

	public static boolean isGem(ItemStack stack) {
		ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id != null && id.getPath().toLowerCase(java.util.Locale.ROOT).contains("gem");
	}

	public static net.neoforged.neoforge.fluids.FluidStack experienceFluid(long millibuckets) {
		if (millibuckets <= 0 || !net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(FLUID_XP)) {
			return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
		}
		Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(FLUID_XP);
		return new net.neoforged.neoforge.fluids.FluidStack(fluid, (int) Math.min(Integer.MAX_VALUE, millibuckets));
	}

	private static FakePlayer createKiller(ServerLevel level, BlockPos origin, List<ItemStack> upgrades) {
		FakePlayer killer = new FakePlayer(level, new GameProfile(KILLER_ID, "Spawner Blackbox"));
		killer.moveTo(origin.getX() + 0.5D, origin.getY() + 1.0D, origin.getZ() + 0.5D, 0.0F, 0.0F);
		ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
		applyEnchantments(level, sword, upgrades);
		killer.setItemInHand(InteractionHand.MAIN_HAND, sword);
		return killer;
	}

	private static void applyEnchantments(ServerLevel level, ItemStack sword, List<ItemStack> upgrades) {
		applyEnchantment(level, sword, Enchantments.SHARPNESS, upgradeCount(upgrades, "saw_upgrade_sharpness") * 10);
		applyEnchantment(level, sword, Enchantments.LOOTING, lootingLevel(upgrades));
		applyEnchantment(level, sword, Enchantments.FIRE_ASPECT, upgradeCount(upgrades, "saw_upgrade_fire"));
		applyEnchantment(level, sword, Enchantments.SMITE, upgradeCount(upgrades, "saw_upgrade_smite") * 10);
		applyEnchantment(level, sword, Enchantments.BANE_OF_ARTHROPODS, upgradeCount(upgrades, "saw_upgrade_arthropod") * 10);
		applyBeheadingComponent(sword, upgradeCount(upgrades, "saw_upgrade_beheading"));
	}

	private static void applyEnchantment(ServerLevel level, ItemStack stack,
			net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantment, int levelValue) {
		if (levelValue > 0) {
			stack.enchant(level.holderOrThrow(enchantment), levelValue);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void applyBeheadingComponent(ItemStack sword, int beheading) {
		if (beheading <= 0) {
			return;
		}
		try {
			Object value = Class.forName("mob_grinding_utils.components.MGUComponents").getField("BEHEADING").get(null);
			if (value instanceof Supplier<?> supplier && supplier.get() instanceof DataComponentType component) {
				sword.set(component, beheading);
			}
		} catch (ReflectiveOperationException ignored) {
			// The entire bridge is optional; a pack without the component must still load normally.
		}
	}

	private static void collectSpawnPotentialChoices(ListTag potentials, List<SpawnChoice> choices) {
		for (int index = 0; index < potentials.size(); index++) {
			CompoundTag potential = potentials.getCompound(index);
			SpawnChoice choice = readChoice(potential.getCompound("data"), Math.max(1, potential.getInt("weight")));
			if (choice == null) {
				choice = readChoice(potential, Math.max(1, potential.getInt("Weight")));
			}
			if (choice != null) {
				choices.add(choice);
			}
		}
	}

	private static SpawnChoice findChoice(CompoundTag data, int depth) {
		if (depth > 4) {
			return null;
		}
		SpawnChoice direct = readChoice(data, 1);
		if (direct != null) {
			return direct;
		}
		for (String key : data.getAllKeys()) {
			if (data.contains(key, Tag.TAG_COMPOUND)) {
				SpawnChoice nested = findChoice(data.getCompound(key), depth + 1);
				if (nested != null) {
					return nested;
				}
			}
		}
		return null;
	}

	private static SpawnChoice readChoice(CompoundTag data, int weight) {
		CompoundTag entityData = data.contains("entity", Tag.TAG_COMPOUND) ? data.getCompound("entity")
				: data.contains("Entity", Tag.TAG_COMPOUND) ? data.getCompound("Entity") : data;
		ResourceLocation id = ResourceLocation.tryParse(entityData.getString("id"));
		if (id == null) {
			id = ResourceLocation.tryParse(entityData.getString("Id"));
		}
		if (id == null || !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
			return null;
		}
		return new SpawnChoice(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(id), entityData.copy(), Math.max(1, weight));
	}

	private static int positiveOr(CompoundTag data, String key, int fallback) {
		return data.contains(key, Tag.TAG_ANY_NUMERIC) ? Math.max(1, data.getInt(key)) : fallback;
	}

	public record SpawnerProfile(List<SpawnChoice> choices, int minDelay, int maxDelay, int spawnCount) {
		public SpawnerProfile {
			choices = List.copyOf(choices);
		}

		public SpawnChoice choose(int roll) {
			int totalWeight = this.choices.stream().mapToInt(SpawnChoice::weight).sum();
			if (totalWeight <= 0) {
				return null;
			}
			int target = Math.floorMod(roll, totalWeight);
			for (SpawnChoice choice : this.choices) {
				target -= choice.weight();
				if (target < 0) {
					return choice;
				}
			}
			return this.choices.getLast();
		}
	}

	public record SpawnChoice(EntityType<?> type, CompoundTag entityData, int weight) {
	}

	public record LootRoll(List<ItemStack> drops, int experience) {
		private static final LootRoll EMPTY = new LootRoll(List.of(), 0);
	}
}
