package io.github.marcschlicker.blackbox.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.network.CoreDistributionMatrixMessage;
import io.github.marcschlicker.blackbox.util.FarmCoreData;
import io.github.marcschlicker.blackbox.world.inventory.CoreDistributionMatrixMenu;

import java.util.ArrayList;
import java.util.List;

/** Client presentation of the server-owned Core Distribution Matrix session. */
public class CoreDistributionMatrixScreen extends AbstractContainerScreen<CoreDistributionMatrixMenu> {
	private static final int LIST_X = 8;
	private static final int LIST_Y = 64;
	private static final int ROW_HEIGHT = 18;
	private static final int VISIBLE_ROWS = 5;
	private static final int PREVIEW_X = 244;
	private Button splitButton;
	private Button resetButton;
	private int scroll;
	private ItemStack cachedCore = ItemStack.EMPTY;
	private List<Route> cachedRoutes = List.of();

	public CoreDistributionMatrixScreen(CoreDistributionMatrixMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 386;
		this.imageHeight = 250;
	}

	@Override
	protected void init() {
		super.init();
		this.splitButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> send(
				this.menu.isAwaitingConfirmation() ? CoreDistributionMatrixMenu.CONFIRM_SPLIT : CoreDistributionMatrixMenu.ARM_SPLIT, -1))
				.bounds(this.leftPos + PREVIEW_X, this.topPos + 101, 134, 20).build());
		this.resetButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.core_distribution_matrix.reset"), button ->
				send(CoreDistributionMatrixMenu.RESET_ASSIGNMENTS, -1)).bounds(this.leftPos + PREVIEW_X, this.topPos + 125, 134, 18).build());
	}

	@Override
	public void containerTick() {
		super.containerTick();
		int itemOutputCount = (int) routes().stream().filter(route -> route.kind() == Kind.ITEM).count();
		boolean ready = this.menu.core().is(BlackboxModItems.DIMENSION_CORE.get()) && itemOutputCount >= 2 && this.menu.resultCoreCount() > 0;
		this.splitButton.active = ready;
		this.resetButton.active = ready;
		this.splitButton.setMessage(Component.translatable(this.menu.isAwaitingConfirmation()
				? "gui.blackbox.core_distribution_matrix.confirm" : "gui.blackbox.core_distribution_matrix.prepare"));
		this.scroll = Math.clamp(this.scroll, 0, Math.max(0, routes().size() - VISIBLE_ROWS));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderRouteTooltip(graphics, mouseX, mouseY);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF171B21);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 2,
				this.menu.isAwaitingConfirmation() ? 0xFFB94D4D : 0xFF4C9B70);
		graphics.fill(this.leftPos + 236, this.topPos + 10, this.leftPos + 237, this.topPos + 162, 0xFF53616F);
		drawSlot(graphics, this.leftPos + 89, this.topPos + 25, 0xFFF0C15B);
		drawSlot(graphics, this.leftPos + 125, this.topPos + 25, 0xFF65B5DB);
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 173 + row * 18, 0xFF68727D);
			}
		}
		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 231, 0xFF68727D);
		}
		for (int row = 0; row < VISIBLE_ROWS; row++) {
			int routeIndex = this.scroll + row;
			int y = this.topPos + LIST_Y + row * ROW_HEIGHT;
			graphics.fill(this.leftPos + LIST_X, y, this.leftPos + 228, y + 17, routeIndex < routes().size() ? 0xFF10151B : 0xFF0C0F13);
			if (routeIndex < routes().size()) {
				Route route = routes().get(routeIndex);
				int accent = switch (route.kind()) {
					case ITEM -> 0xFF76B97B;
					case FLUID -> 0xFF62B9E6;
					case ENERGY -> 0xFFE1B95F;
				};
				graphics.fill(this.leftPos + LIST_X, y, this.leftPos + LIST_X + 2, y + 17, accent);
			}
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, Component.translatable("gui.blackbox.core_distribution_matrix.title"), 8, 8, 0xFFE8EDF2, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.core_distribution_matrix.source"), 62, 30, 0xFFAAB4C0, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.core_distribution_matrix.upgrade"), 116, 51, 0xFFAAB4C0, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.core_distribution_matrix.assignments"), LIST_X, 51, 0xFF8DDB9B, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.core_distribution_matrix.preview"), PREVIEW_X, 51, 0xFF73C7FF, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.core_distribution_matrix.result_cores", this.menu.resultCoreCount()), PREVIEW_X, 66, 0xFFE8EDF2, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.core_distribution_matrix.input_warning"), PREVIEW_X, 80, 0xFFFFC866, false);
		if (this.menu.isAwaitingConfirmation()) {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.core_distribution_matrix.delete_warning"), PREVIEW_X, 151, 0xFFFF7B7B, false);
		} else {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.core_distribution_matrix.click_hint"), PREVIEW_X, 151, 0xFF9AA4B0, false);
		}
		for (int row = 0; row < VISIBLE_ROWS; row++) {
			int routeIndex = this.scroll + row;
			if (routeIndex >= routes().size()) {
				continue;
			}
			Route route = routes().get(routeIndex);
			int y = LIST_Y + row * ROW_HEIGHT + 4;
			graphics.renderItem(route.display(), LIST_X + 5, y - 4);
			String name = this.font.plainSubstrByWidth(route.label().getString(), 137);
			graphics.drawString(this.font, name, LIST_X + 25, y, route.color(), false);
			String groupLabel = Component.translatable("gui.blackbox.core_distribution_matrix.core_number", group(route) + 1).getString();
			graphics.fill(LIST_X + 169, y - 3, LIST_X + 223, y + 10, 0xFF273B50);
			graphics.drawString(this.font, groupLabel, LIST_X + 172, y, 0xFFB9E0FA, false);
		}
		if (routes().size() > VISIBLE_ROWS) {
			graphics.drawString(this.font, (this.scroll + 1) + "-" + Math.min(this.scroll + VISIBLE_ROWS, routes().size()) + "/" + routes().size(),
					162, 51, 0xFF9AA4B0, false);
		}
		graphics.drawString(this.font, Component.translatable("container.inventory"), 8, 163, 0xFFAAB4C0, false);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && inside(mouseX, mouseY, this.leftPos + LIST_X, this.topPos + LIST_Y, 220, VISIBLE_ROWS * ROW_HEIGHT)) {
			int row = ((int) mouseY - (this.topPos + LIST_Y)) / ROW_HEIGHT;
			int index = this.scroll + row;
			if (index >= 0 && index < routes().size()) {
				Route route = routes().get(index);
				send(switch (route.kind()) {
					case ITEM -> CoreDistributionMatrixMenu.CYCLE_ITEM_GROUP;
					case FLUID -> CoreDistributionMatrixMenu.CYCLE_FLUID_GROUP;
					case ENERGY -> CoreDistributionMatrixMenu.CYCLE_ENERGY_GROUP;
				}, route.index());
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (inside(mouseX, mouseY, this.leftPos + LIST_X, this.topPos + LIST_Y, 220, VISIBLE_ROWS * ROW_HEIGHT) && scrollY != 0) {
			this.scroll = Math.clamp(this.scroll + (scrollY > 0 ? -1 : 1), 0, Math.max(0, routes().size() - VISIBLE_ROWS));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void renderRouteTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		if (!inside(mouseX, mouseY, this.leftPos + LIST_X, this.topPos + LIST_Y, 220, VISIBLE_ROWS * ROW_HEIGHT)) {
			return;
		}
		int row = (mouseY - (this.topPos + LIST_Y)) / ROW_HEIGHT;
		int index = this.scroll + row;
		if (index < 0 || index >= routes().size()) {
			return;
		}
		Route route = routes().get(index);
		List<Component> tooltip = new ArrayList<>();
		tooltip.add(route.label());
		tooltip.add(Component.translatable("gui.blackbox.core_distribution_matrix.route_hint", group(route) + 1));
		graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
	}

	private List<Route> routes() {
		ItemStack currentCore = this.menu.core();
		if (ItemStack.isSameItemSameComponents(this.cachedCore, currentCore)) {
			return this.cachedRoutes;
		}
		this.cachedCore = currentCore.copy();
		List<Route> routes = new ArrayList<>();
		FarmCoreData.Recipe recipe = this.menu.recipe();
		List<FarmCoreData.StackAmount> itemOutputs = recipe.outputTemplates();
		for (int index = 0; index < itemOutputs.size(); index++) {
			FarmCoreData.StackAmount output = itemOutputs.get(index);
			routes.add(new Route(Kind.ITEM, index, output.stack(), output.stack().getHoverName(), 0xFFBEEEC1));
		}
		for (int index = 0; index < recipe.fluidOutputs().size(); index++) {
			FluidStack output = recipe.fluidOutputs().get(index).stack();
			ItemStack display = output.getFluid().getBucket().equals(Items.AIR) ? new ItemStack(Items.WATER_BUCKET) : new ItemStack(output.getFluid().getBucket());
			routes.add(new Route(Kind.FLUID, index, display, Component.literal(output.getHoverName().getString() + " (" + recipe.fluidOutputs().get(index).amount() + " mB)"),
					0xFF73C7FF));
		}
		if (recipe.energyOutput() > 0) {
			routes.add(new Route(Kind.ENERGY, 0, new ItemStack(Items.REDSTONE), Component.literal(recipe.energyOutput() + " FE"), 0xFFE2BE62));
		}
		this.cachedRoutes = List.copyOf(routes);
		return this.cachedRoutes;
	}

	private int group(Route route) {
		return switch (route.kind()) {
			case ITEM -> this.menu.itemGroup(route.index());
			case FLUID -> this.menu.fluidGroup(route.index());
			case ENERGY -> this.menu.energyGroup();
		};
	}

	private void send(int action, int index) {
		PacketDistributor.sendToServer(new CoreDistributionMatrixMessage(action, this.menu.x, this.menu.y, this.menu.z, index));
	}

	private void drawSlot(GuiGraphics graphics, int x, int y, int borderColor) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, borderColor);
		graphics.fill(x, y, x + 16, y + 16, 0xFF0C0F13);
	}

	private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private enum Kind {
		ITEM, FLUID, ENERGY
	}

	private record Route(Kind kind, int index, ItemStack display, Component label, int color) {
	}
}
