package io.github.marcschlicker.blackbox.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;

import io.github.marcschlicker.blackbox.config.BlackboxConfig;
import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.procedures.TeleporttoDimensionProcedure;
import io.github.marcschlicker.blackbox.util.FarmAccessMode;
import io.github.marcschlicker.blackbox.util.FarmCoreData;
import io.github.marcschlicker.blackbox.util.FarmDimensionRuntime;
import io.github.marcschlicker.blackbox.util.FarmMeasurementHost;
import io.github.marcschlicker.blackbox.BlackboxMod;

@EventBusSubscriber
public record DimensionalWorkbenchGUIButtonMessage(int buttonID, int x, int y, int z) implements CustomPacketPayload {
	public static final Type<DimensionalWorkbenchGUIButtonMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "dimensional_workbench_gui_buttons"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DimensionalWorkbenchGUIButtonMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, DimensionalWorkbenchGUIButtonMessage message) -> {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}, (RegistryFriendlyByteBuf buffer) -> new DimensionalWorkbenchGUIButtonMessage(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));

	@Override
	public Type<DimensionalWorkbenchGUIButtonMessage> type() {
		return TYPE;
	}

	public static void handleData(final DimensionalWorkbenchGUIButtonMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> handleButtonAction(context.player(), message.buttonID, message.x, message.y, message.z)).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		Level world = entity.level();
		BlockPos pos = new BlockPos(x, y, z);
		// security measure to prevent arbitrary chunk generation
		if (!world.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)))
			return;
		if (!pos.closerToCenterThan(entity.position(), 8.0D) || !(world.getBlockEntity(pos) instanceof FarmMeasurementHost workbench)) {
			return;
		}
		if (buttonID == 0) {
			TeleporttoDimensionProcedure.execute(x, y, z, entity);
		} else if (buttonID == 1 && entity instanceof net.minecraft.server.level.ServerPlayer player) {
			ItemStack core = workbench.getItem(0);
			if (!core.is(BlackboxModItems.DIMENSION_CORE.get()) || FarmCoreData.isVillageArchiveCore(core) || !FarmCoreData.canManage(core, player)) {
				player.sendSystemMessage(Component.translatable("message.blackbox.farm.access_denied"));
				return;
			}
			FarmCoreData.ensureOwner(core, player);
			FarmAccessMode access = FarmCoreData.getAccessMode(core).next(BlackboxConfig.ALLOW_PUBLIC_FARMS.get());
			FarmCoreData.setAccessMode(core, access);
			workbench.setChanged();
			FarmDimensionRuntime.refreshFarmRecord(player.server, core);
			player.sendSystemMessage(Component.translatable("message.blackbox.farm.access_changed", Component.translatable("gui.blackbox.access." + access.id())));
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		BlackboxMod.addNetworkMessage(DimensionalWorkbenchGUIButtonMessage.TYPE, DimensionalWorkbenchGUIButtonMessage.STREAM_CODEC, DimensionalWorkbenchGUIButtonMessage::handleData);
	}
}
