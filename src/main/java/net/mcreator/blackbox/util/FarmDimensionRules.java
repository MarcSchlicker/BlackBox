package net.mcreator.blackbox.util;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.mcreator.blackbox.config.BlackboxConfig;
import net.mcreator.blackbox.world.dimension.LimitedFlatLevelSource;

@EventBusSubscriber
public final class FarmDimensionRules {
	private FarmDimensionRules() {
	}

	@SubscribeEvent
	public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level) || !FarmEnvironment.isFarmDimension(level.dimension())) {
			return;
		}
		ServerPlayer player = event.getEntity() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
		if (!LimitedFlatLevelSource.isFarmChunk(event.getPos().getX() >> 4, event.getPos().getZ() >> 4)) {
			event.setCanceled(true);
			if (player != null) {
				player.displayClientMessage(Component.translatable("message.blackbox.farm.outside_cell").withStyle(ChatFormatting.RED), true);
			}
			return;
		}
		if (BlackboxConfig.deniedBlocks().contains(event.getPlacedBlock().getBlock())) {
			event.setCanceled(true);
			if (player != null) {
				player.displayClientMessage(Component.translatable("message.blackbox.farm.block_denied").withStyle(ChatFormatting.RED), true);
			}
			return;
		}
		if (player == null) {
			return;
		}
		FarmCell cell = FarmDimensionRuntime.getAssignedCell(player).orElse(null);
		if (cell == null || !cell.contains(event.getPos())) {
			event.setCanceled(true);
			player.displayClientMessage(Component.translatable("message.blackbox.farm.outside_cell").withStyle(ChatFormatting.RED), true);
			return;
		}
	}

	@SubscribeEvent
	public static void onBlockBroken(BlockEvent.BreakEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level) || !FarmEnvironment.isFarmDimension(level.dimension()) || !(event.getPlayer() instanceof ServerPlayer player)) {
			return;
		}
		FarmCell cell = FarmDimensionRuntime.getAssignedCell(player).orElse(null);
		if (cell == null || !cell.contains(event.getPos())) {
			event.setCanceled(true);
			player.displayClientMessage(Component.translatable("message.blackbox.farm.outside_cell").withStyle(ChatFormatting.RED), true);
		}
	}
}
