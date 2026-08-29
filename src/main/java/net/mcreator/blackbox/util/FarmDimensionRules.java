package net.mcreator.blackbox.util;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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
		FarmCell cell = player == null ? FarmDimensionRuntime.registeredCellAt(level, event.getPos()).orElse(null)
				: FarmDimensionRuntime.getAssignedCell(player).orElse(null);
		if (!LimitedFlatLevelSource.isFarmChunk(event.getPos().getX() >> 4, event.getPos().getZ() >> 4) || cell == null || !cell.contains(event.getPos())) {
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

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || !FarmEnvironment.isFarmDimension(player.level().dimension())) {
			return;
		}
		FarmCell cell = FarmDimensionRuntime.getAssignedCell(player).orElse(null);
		if (cell == null || cell.contains(player.blockPosition())) {
			return;
		}
		double x = Math.max(cell.minBlockX() + 0.5D, Math.min(cell.maxBlockX() + 0.5D, player.getX()));
		double z = Math.max(cell.minBlockZ() + 0.5D, Math.min(cell.maxBlockZ() + 0.5D, player.getZ()));
		player.teleportTo((ServerLevel) player.level(), x, player.getY(), z, player.getYRot(), player.getXRot());
		player.displayClientMessage(Component.translatable("message.blackbox.farm.outside_cell").withStyle(ChatFormatting.RED), true);
	}
}
