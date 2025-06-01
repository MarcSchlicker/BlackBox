
package net.mcreator.blackbox.block.entity;

import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.mcreator.blackbox.world.inventory.BlackBoxGuiMenu;
import net.mcreator.blackbox.init.BlackboxModBlockEntities;

import javax.annotation.Nullable;

import java.util.stream.IntStream;

import io.netty.buffer.Unpooled;

public class BlackboxBlockBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	private NonNullList<ItemStack> stacks = NonNullList.<ItemStack>withSize(256, ItemStack.EMPTY);
	private final SidedInvWrapper handler = new SidedInvWrapper(this, null);

	public BlackboxBlockBlockEntity(BlockPos position, BlockState state) {
		super(BlackboxModBlockEntities.BLACKBOX_BLOCK.get(), position, state);
	}

	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(compound, lookupProvider);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);
	}

	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(compound, lookupProvider);
		if (!this.trySaveLootTable(compound)) {
			ContainerHelper.saveAllItems(compound, this.stacks, lookupProvider);
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
		return this.saveWithFullMetadata(lookupProvider);
	}

	@Override
	public int getContainerSize() {
		return stacks.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.stacks)
			if (!itemstack.isEmpty())
				return false;
		return true;
	}

	@Override
	public Component getDefaultName() {
		return Component.literal("blackbox_block");
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new BlackBoxGuiMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Blackbox Block");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		if (index == 1)
			return false;
		if (index == 2)
			return false;
		if (index == 3)
			return false;
		if (index == 4)
			return false;
		if (index == 5)
			return false;
		if (index == 6)
			return false;
		if (index == 7)
			return false;
		if (index == 8)
			return false;
		if (index == 9)
			return false;
		if (index == 10)
			return false;
		if (index == 11)
			return false;
		if (index == 12)
			return false;
		if (index == 13)
			return false;
		if (index == 14)
			return false;
		if (index == 15)
			return false;
		if (index == 16)
			return false;
		if (index == 17)
			return false;
		if (index == 18)
			return false;
		if (index == 19)
			return false;
		if (index == 20)
			return false;
		if (index == 21)
			return false;
		if (index == 22)
			return false;
		if (index == 23)
			return false;
		if (index == 24)
			return false;
		if (index == 25)
			return false;
		if (index == 26)
			return false;
		if (index == 27)
			return false;
		if (index == 28)
			return false;
		if (index == 29)
			return false;
		if (index == 30)
			return false;
		if (index == 31)
			return false;
		if (index == 32)
			return false;
		if (index == 33)
			return false;
		if (index == 34)
			return false;
		if (index == 35)
			return false;
		if (index == 36)
			return false;
		if (index == 37)
			return false;
		if (index == 38)
			return false;
		if (index == 39)
			return false;
		if (index == 40)
			return false;
		if (index == 41)
			return false;
		if (index == 42)
			return false;
		if (index == 43)
			return false;
		if (index == 44)
			return false;
		if (index == 45)
			return false;
		if (index == 46)
			return false;
		if (index == 47)
			return false;
		if (index == 48)
			return false;
		if (index == 49)
			return false;
		if (index == 50)
			return false;
		if (index == 51)
			return false;
		if (index == 52)
			return false;
		if (index == 53)
			return false;
		if (index == 54)
			return false;
		if (index == 55)
			return false;
		if (index == 56)
			return false;
		if (index == 57)
			return false;
		if (index == 58)
			return false;
		if (index == 59)
			return false;
		if (index == 60)
			return false;
		if (index == 61)
			return false;
		if (index == 62)
			return false;
		if (index == 63)
			return false;
		if (index == 64)
			return false;
		if (index == 65)
			return false;
		if (index == 66)
			return false;
		if (index == 67)
			return false;
		if (index == 68)
			return false;
		if (index == 69)
			return false;
		if (index == 70)
			return false;
		if (index == 71)
			return false;
		if (index == 72)
			return false;
		if (index == 73)
			return false;
		if (index == 74)
			return false;
		if (index == 75)
			return false;
		if (index == 76)
			return false;
		if (index == 77)
			return false;
		if (index == 78)
			return false;
		if (index == 79)
			return false;
		if (index == 80)
			return false;
		if (index == 81)
			return false;
		if (index == 82)
			return false;
		if (index == 83)
			return false;
		if (index == 84)
			return false;
		if (index == 85)
			return false;
		if (index == 86)
			return false;
		if (index == 87)
			return false;
		if (index == 88)
			return false;
		if (index == 89)
			return false;
		if (index == 90)
			return false;
		if (index == 91)
			return false;
		if (index == 92)
			return false;
		if (index == 93)
			return false;
		if (index == 94)
			return false;
		if (index == 95)
			return false;
		if (index == 96)
			return false;
		if (index == 97)
			return false;
		if (index == 98)
			return false;
		if (index == 99)
			return false;
		if (index == 100)
			return false;
		if (index == 101)
			return false;
		if (index == 102)
			return false;
		if (index == 103)
			return false;
		if (index == 104)
			return false;
		if (index == 105)
			return false;
		if (index == 106)
			return false;
		if (index == 107)
			return false;
		if (index == 108)
			return false;
		if (index == 109)
			return false;
		if (index == 110)
			return false;
		if (index == 111)
			return false;
		if (index == 112)
			return false;
		if (index == 113)
			return false;
		if (index == 114)
			return false;
		if (index == 115)
			return false;
		if (index == 116)
			return false;
		if (index == 117)
			return false;
		if (index == 118)
			return false;
		if (index == 119)
			return false;
		if (index == 120)
			return false;
		if (index == 121)
			return false;
		if (index == 122)
			return false;
		if (index == 123)
			return false;
		if (index == 124)
			return false;
		if (index == 125)
			return false;
		if (index == 126)
			return false;
		if (index == 127)
			return false;
		if (index == 128)
			return false;
		if (index == 129)
			return false;
		if (index == 130)
			return false;
		if (index == 131)
			return false;
		if (index == 132)
			return false;
		if (index == 133)
			return false;
		if (index == 134)
			return false;
		if (index == 135)
			return false;
		if (index == 136)
			return false;
		if (index == 137)
			return false;
		if (index == 138)
			return false;
		if (index == 139)
			return false;
		if (index == 140)
			return false;
		if (index == 141)
			return false;
		if (index == 142)
			return false;
		if (index == 143)
			return false;
		if (index == 144)
			return false;
		if (index == 145)
			return false;
		if (index == 146)
			return false;
		if (index == 147)
			return false;
		if (index == 148)
			return false;
		if (index == 149)
			return false;
		if (index == 150)
			return false;
		if (index == 151)
			return false;
		if (index == 152)
			return false;
		if (index == 153)
			return false;
		if (index == 154)
			return false;
		if (index == 155)
			return false;
		if (index == 156)
			return false;
		if (index == 157)
			return false;
		if (index == 158)
			return false;
		if (index == 159)
			return false;
		if (index == 160)
			return false;
		if (index == 161)
			return false;
		if (index == 162)
			return false;
		if (index == 163)
			return false;
		if (index == 164)
			return false;
		if (index == 165)
			return false;
		if (index == 166)
			return false;
		if (index == 167)
			return false;
		if (index == 168)
			return false;
		if (index == 169)
			return false;
		if (index == 170)
			return false;
		if (index == 171)
			return false;
		if (index == 172)
			return false;
		if (index == 173)
			return false;
		if (index == 174)
			return false;
		if (index == 175)
			return false;
		if (index == 176)
			return false;
		if (index == 177)
			return false;
		if (index == 178)
			return false;
		if (index == 179)
			return false;
		if (index == 180)
			return false;
		if (index == 181)
			return false;
		if (index == 182)
			return false;
		if (index == 183)
			return false;
		if (index == 184)
			return false;
		if (index == 185)
			return false;
		if (index == 186)
			return false;
		if (index == 187)
			return false;
		if (index == 188)
			return false;
		if (index == 189)
			return false;
		if (index == 190)
			return false;
		if (index == 191)
			return false;
		if (index == 192)
			return false;
		if (index == 193)
			return false;
		if (index == 194)
			return false;
		if (index == 195)
			return false;
		if (index == 196)
			return false;
		if (index == 197)
			return false;
		if (index == 198)
			return false;
		if (index == 199)
			return false;
		if (index == 200)
			return false;
		if (index == 201)
			return false;
		if (index == 202)
			return false;
		if (index == 203)
			return false;
		if (index == 204)
			return false;
		if (index == 205)
			return false;
		if (index == 206)
			return false;
		if (index == 207)
			return false;
		if (index == 208)
			return false;
		if (index == 209)
			return false;
		if (index == 210)
			return false;
		if (index == 211)
			return false;
		if (index == 212)
			return false;
		if (index == 213)
			return false;
		if (index == 214)
			return false;
		if (index == 215)
			return false;
		if (index == 216)
			return false;
		if (index == 217)
			return false;
		if (index == 218)
			return false;
		if (index == 219)
			return false;
		if (index == 220)
			return false;
		if (index == 221)
			return false;
		if (index == 222)
			return false;
		if (index == 223)
			return false;
		if (index == 224)
			return false;
		if (index == 225)
			return false;
		if (index == 226)
			return false;
		if (index == 227)
			return false;
		if (index == 228)
			return false;
		if (index == 229)
			return false;
		if (index == 230)
			return false;
		if (index == 231)
			return false;
		if (index == 232)
			return false;
		if (index == 233)
			return false;
		if (index == 234)
			return false;
		if (index == 235)
			return false;
		if (index == 236)
			return false;
		if (index == 237)
			return false;
		if (index == 238)
			return false;
		if (index == 239)
			return false;
		if (index == 240)
			return false;
		if (index == 241)
			return false;
		if (index == 242)
			return false;
		if (index == 243)
			return false;
		if (index == 244)
			return false;
		if (index == 245)
			return false;
		if (index == 246)
			return false;
		if (index == 247)
			return false;
		if (index == 248)
			return false;
		if (index == 249)
			return false;
		if (index == 250)
			return false;
		if (index == 251)
			return false;
		if (index == 252)
			return false;
		if (index == 253)
			return false;
		if (index == 254)
			return false;
		if (index == 255)
			return false;
		if (index == 256)
			return false;
		return true;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return IntStream.range(0, this.getContainerSize()).toArray();
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack itemstack, @Nullable Direction direction) {
		return this.canPlaceItem(index, itemstack);
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack itemstack, Direction direction) {
		if (index == 0)
			return false;
		return true;
	}

	public SidedInvWrapper getItemHandler() {
		return handler;
	}
}
