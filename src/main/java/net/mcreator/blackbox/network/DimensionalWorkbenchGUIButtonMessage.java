package net.mcreator.blackbox.network;

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

import net.mcreator.blackbox.block.entity.DimensionalWorkbenchBlockEntity;
import net.mcreator.blackbox.config.BlackboxConfig;
import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.procedures.TeleporttoDimensionProcedure;
import net.mcreator.blackbox.util.FarmAccessMode;
import net.mcreator.blackbox.util.FarmCoreData;
import net.mcreator.blackbox.util.FarmDimensionRuntime;
import net.mcreator.blackbox.BlackboxMod;

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
		// security measure to prevent arbitrary chunk generation
		if (!world.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)))
			return;
		if (buttonID == 0) {
			TeleporttoDimensionProcedure.execute(x, y, z, entity);
		} else if (buttonID == 1 && entity instanceof net.minecraft.server.level.ServerPlayer player
				&& new BlockPos(x, y, z).closerToCenterThan(player.position(), 8.0D)
				&& world.getBlockEntity(new BlockPos(x, y, z)) instanceof DimensionalWorkbenchBlockEntity workbench) {
			ItemStack core = workbench.getItem(0);
			if (!core.is(BlackboxModItems.DIMENSION_CORE.get()) || !FarmCoreData.canManage(core, player)) {
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
