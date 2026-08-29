package net.mcreator.blackbox.util;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;

import net.mcreator.blackbox.BlackboxMod;
import net.mcreator.blackbox.config.BlackboxConfig;
import net.mcreator.blackbox.init.BlackboxModBlocks;
import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.init.BlackboxModVillagers;

@EventBusSubscriber(modid = BlackboxMod.MODID)
public final class VillageArchiveRuntime {
	private VillageArchiveRuntime() {
	}

	@SubscribeEvent
	public static void onWorkbenchPlaced(BlockEvent.EntityPlaceEvent event) {
		if (!BlackboxConfig.isVillageArchiveEnabled() || !event.getPlacedBlock().is(BlackboxModBlocks.DIMENSIONAL_WORKBENCH.get())
				|| !(event.getLevel() instanceof ServerLevel level)) {
			return;
		}
		Villager candidate = level.getEntitiesOfClass(Villager.class, new AABB(event.getPos()).inflate(4.0D), villager ->
				villager.isAlive() && villager.getVillagerData().getProfession() == VillagerProfession.NITWIT)
				.stream().min(java.util.Comparator.comparingDouble(villager -> villager.distanceToSqr(event.getPos().getX() + 0.5D, event.getPos().getY() + 0.5D, event.getPos().getZ() + 0.5D))).orElse(null);
		if (candidate == null) {
			return;
		}
		candidate.setVillagerData(candidate.getVillagerData().setProfession(BlackboxModVillagers.DIMENSIONAL_ARCHIVIST.get()).setLevel(1));
		candidate.setVillagerXp(0);
		level.broadcastEntityEvent(candidate, (byte) 14);
		if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
			player.sendSystemMessage(Component.translatable("message.blackbox.village_archive.archivist_created").withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}

	@SubscribeEvent
	public static void onServerStarted(ServerStartedEvent event) {
		if (BlackboxConfig.isVillageArchiveEnabled()) {
			reloadArchiveData(event.getServer());
		}
	}

	@SubscribeEvent
	public static void onVillagerTrades(VillagerTradesEvent event) {
		if (!BlackboxConfig.isVillageArchiveEnabled() || event.getType() != BlackboxModVillagers.DIMENSIONAL_ARCHIVIST.get()) {
			return;
		}
		addTrade(event, 1, 2, 12, FarmCoreData.createArchiveIronFarmCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get())), 3, 4);
		addTrade(event, 1, 0, 8, new ItemStack(BlackboxModItems.DIMENSION_CORE.get()), 8, 2);
		addTrade(event, 2, 3, 18, FarmCoreData.createArchiveSugarCaneFarmCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get())), 2, 10);
		addTrade(event, 2, 1, 16, new ItemStack(BlackboxModItems.MEDIUM_CELL_UPGRADE.get()), 3, 8);
		addTrade(event, 3, 4, 24, FarmCoreData.createArchiveNetherWartFarmCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get())), 2, 15);
		addTrade(event, 3, 2, 24, new ItemStack(BlackboxModItems.STABILITY_UPGRADE.get()), 2, 12);
		addTrade(event, 4, 6, 32, FarmCoreData.createArchiveBlazeFarmCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get())), 1, 20);
		addTrade(event, 4, 3, 30, new ItemStack(BlackboxModItems.MOB_SPAWN_UPGRADE.get()), 2, 18);
		addTrade(event, 5, 8, 48, enchantedSacrificialSword(event), 1, 30);
	}

	@SubscribeEvent
	public static void onArchivistInteract(PlayerInteractEvent.EntityInteract event) {
		if (BlackboxConfig.isVillageArchiveEnabled() || !(event.getTarget() instanceof Villager villager)
				|| villager.getVillagerData().getProfession() != BlackboxModVillagers.DIMENSIONAL_ARCHIVIST.get()) {
			return;
		}
		event.setCancellationResult(InteractionResult.FAIL);
		event.setCanceled(true);
		if (!event.getLevel().isClientSide()) {
			event.getEntity().sendSystemMessage(Component.translatable("message.blackbox.village_archive.disabled").withStyle(ChatFormatting.RED));
		}
	}

	public static void onModuleStateChanged(MinecraftServer server, boolean enabled) {
		reloadArchiveData(server);
	}

	private static void reloadArchiveData(MinecraftServer server) {
		server.reloadResources(server.getPackRepository().getSelectedIds()).exceptionally(error -> {
			BlackboxMod.LOGGER.error("Could not reload BlackBox Village Archive recipes and trades", error);
			return null;
		});
	}

	private static void addTrade(VillagerTradesEvent event, int level, int heads, int emeralds, ItemStack result, int maxTrades, int xp) {
		ItemStack emeraldPrice = new ItemStack(Items.EMERALD, emeralds);
		if (heads == 0) {
			event.getTrades().get(level).add(new BasicItemListing(emeraldPrice, result, maxTrades, xp, 0.0F));
			return;
		}
		event.getTrades().get(level).add(new BasicItemListing(new ItemStack(BlackboxModItems.VILLAGER_HEAD.get(), heads), emeraldPrice, result, maxTrades, xp, 0.0F));
	}

	private static ItemStack enchantedSacrificialSword(VillagerTradesEvent event) {
		ItemStack sword = new ItemStack(BlackboxModItems.OPFERSCHWERT.get());
		var enchantments = event.getRegistryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		sword.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 3);
		sword.enchant(enchantments.getOrThrow(Enchantments.LOOTING), 2);
		return sword;
	}
}
