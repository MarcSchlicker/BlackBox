package net.mcreator.blackbox.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.mcreator.blackbox.init.BlackboxModBlocks;
import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.world.inventory.HandbookMenu;

public class HandbookScreen extends AbstractContainerScreen<HandbookMenu> {
	private static final int GUIDE_PAGE_COUNT = 7;
	private static final int PAGE_COUNT = 15;
	private int page;
	private Button previousButton;
	private Button nextButton;

	public HandbookScreen(HandbookMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 248;
		this.imageHeight = 180;
	}

	@Override
	protected void init() {
		super.init();
		this.previousButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1)).bounds(this.leftPos + 12, this.topPos + 148, 28, 20).build());
		this.nextButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1)).bounds(this.leftPos + this.imageWidth - 40, this.topPos + 148, 28, 20).build());
		updateButtons();
	}

	private void changePage(int change) {
		this.page = Math.max(0, Math.min(PAGE_COUNT - 1, this.page + change));
		updateButtons();
	}

	private void updateButtons() {
		this.previousButton.active = this.page > 0;
		this.nextButton.active = this.page < PAGE_COUNT - 1;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF26352D);
		graphics.fill(this.leftPos + 7, this.topPos + 7, this.leftPos + this.imageWidth - 7, this.topPos + this.imageHeight - 7, 0xFFF2F4EF);
		graphics.fill(this.leftPos + 123, this.topPos + 8, this.leftPos + 125, this.topPos + this.imageHeight - 8, 0xFFD1D7D0);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		Component heading = Component.translatable("gui.blackbox.handbook.page." + this.page + ".title");
		graphics.drawCenteredString(this.font, heading, this.imageWidth / 2, 17, 0xFF183322);
		if (this.page < GUIDE_PAGE_COUNT) {
			Component body = Component.translatable("gui.blackbox.handbook.page." + this.page + ".body");
			graphics.drawWordWrap(this.font, body, 19, 39, this.imageWidth - 38, 0xFF26312A);
		} else {
			renderRecipePage(graphics, this.page - GUIDE_PAGE_COUNT);
		}
		graphics.drawCenteredString(this.font, Component.literal((this.page + 1) + " / " + PAGE_COUNT), this.imageWidth / 2, 154, 0xFF5D675F);
	}

	private void renderRecipePage(GuiGraphics graphics, int recipePage) {
		RecipeView[] recipes = recipesForPage(recipePage);
		for (int index = 0; index < recipes.length; index++) {
			renderRecipe(graphics, index == 0 ? 8 : 132, recipes[index]);
		}
	}

	private void renderRecipe(GuiGraphics graphics, int x, RecipeView recipe) {
		graphics.drawCenteredString(this.font, recipe.title(), x + 52, 35, 0xFF26312A);
		for (int slot = 0; slot < 9; slot++) {
			int slotX = x + (slot % 3) * 18;
			int slotY = 50 + (slot / 3) * 18;
			graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF89958D);
			graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFFE3E7E1);
			ItemStack ingredient = recipe.ingredients()[slot];
			if (!ingredient.isEmpty()) {
				graphics.renderItem(ingredient, slotX + 1, slotY + 1);
			}
		}
		graphics.drawString(this.font, Component.literal("->"), x + 59, 69, 0xFF41614D, false);
		graphics.fill(x + 79, 67, x + 99, 87, 0xFF4F9A6C);
		graphics.fill(x + 81, 69, x + 97, 85, 0xFFF4F6F2);
		graphics.renderItem(recipe.result(), x + 81, 69);
	}

	private RecipeView[] recipesForPage(int recipePage) {
		return switch (recipePage) {
			case 0 -> new RecipeView[]{
				shaped("gui.blackbox.handbook.recipe.workbench", stack(BlackboxModBlocks.DIMENSIONAL_WORKBENCH), Items.COPPER_INGOT, Items.REDSTONE, Items.COPPER_INGOT, Items.IRON_INGOT, Items.CRAFTING_TABLE, Items.IRON_INGOT, Items.COPPER_INGOT, Items.COPPER_INGOT, Items.COPPER_INGOT),
				shaped("gui.blackbox.handbook.recipe.core", stack(BlackboxModItems.DIMENSION_CORE), Items.COPPER_INGOT, Items.REDSTONE, Items.COPPER_INGOT, Items.REDSTONE, Items.EMERALD, Items.REDSTONE, Items.COPPER_INGOT, Items.REDSTONE, Items.COPPER_INGOT)
			};
			case 1 -> new RecipeView[]{
				shaped("gui.blackbox.handbook.recipe.input", stack(BlackboxModBlocks.INPUTBLOCK), null, Items.COPPER_INGOT, null, Items.BLUE_DYE, Items.HOPPER, Items.BLUE_DYE, null, Items.COPPER_INGOT, null),
				shaped("gui.blackbox.handbook.recipe.output", stack(BlackboxModBlocks.OUTPUT_BLOCK), null, Items.COPPER_INGOT, null, Items.GREEN_DYE, Items.HOPPER, Items.GREEN_DYE, null, Items.COPPER_INGOT, null)
			};
			case 2 -> new RecipeView[]{
				shaped("gui.blackbox.handbook.recipe.blackbox", stack(BlackboxModBlocks.BLACKBOX_BLOCK), Items.IRON_INGOT, Items.REDSTONE, Items.IRON_INGOT, Items.OBSIDIAN, Items.CHEST, Items.OBSIDIAN, Items.IRON_INGOT, Items.REDSTONE, Items.IRON_INGOT),
				shaped("gui.blackbox.handbook.recipe.stability", stack(BlackboxModItems.STABILITY_UPGRADE), null, Items.IRON_INGOT, null, Items.CLOCK, Items.COMPARATOR, Items.CLOCK, null, Items.IRON_INGOT, null)
			};
			case 3 -> new RecipeView[]{
				shaped("gui.blackbox.handbook.recipe.medium", stack(BlackboxModItems.MEDIUM_CELL_UPGRADE), Items.COPPER_INGOT, null, Items.COPPER_INGOT, null, Items.REDSTONE, null, Items.COPPER_INGOT, null, Items.COPPER_INGOT),
				shapeless("gui.blackbox.handbook.recipe.large", stack(BlackboxModItems.LARGE_CELL_UPGRADE), BlackboxModItems.MEDIUM_CELL_UPGRADE, BlackboxModItems.MEDIUM_CELL_UPGRADE)
			};
			case 4 -> new RecipeView[]{
				shapeless("gui.blackbox.handbook.recipe.standard", stack(BlackboxModItems.STANDARD_ENVIRONMENT_UPGRADE), Items.COMPASS, Items.DEEPSLATE, Items.EMERALD),
				shapeless("gui.blackbox.handbook.recipe.overworld", stack(BlackboxModItems.OVERWORLD_ENVIRONMENT_UPGRADE), Items.COMPASS, Items.GRASS_BLOCK, Items.OAK_SAPLING)
			};
			case 5 -> new RecipeView[]{
				shapeless("gui.blackbox.handbook.recipe.nether", stack(BlackboxModItems.NETHER_ENVIRONMENT_UPGRADE), Items.COMPASS, Items.NETHERRACK, Items.NETHER_WART),
				shapeless("gui.blackbox.handbook.recipe.end", stack(BlackboxModItems.END_ENVIRONMENT_UPGRADE), Items.COMPASS, Items.END_STONE, Items.ENDER_PEARL)
			};
			case 6 -> new RecipeView[]{
				shapeless("gui.blackbox.handbook.recipe.mob_spawn", stack(BlackboxModItems.MOB_SPAWN_UPGRADE), Items.EMERALD, Items.ROTTEN_FLESH, Items.BONE, Items.GUNPOWDER, Items.SPIDER_EYE),
				shaped("gui.blackbox.handbook.recipe.blueprint", stack(BlackboxModItems.BLUEPRINT), Items.PAPER, Items.PAPER, Items.PAPER, Items.PAPER, Items.INK_SAC, Items.PAPER, Items.PAPER, Items.PAPER, Items.PAPER)
			};
			default -> new RecipeView[]{
				shapeless("gui.blackbox.handbook.recipe.handbook", stack(BlackboxModItems.HANDBOOK), Items.BOOK, Items.EMERALD),
				shaped("gui.blackbox.handbook.recipe.sword", stack(BlackboxModItems.OPFERSCHWERT), Items.EMERALD, Items.IRON_INGOT, Items.EMERALD, Items.EMERALD, Items.IRON_INGOT, Items.EMERALD, Items.EMERALD, Items.STICK, Items.EMERALD)
			};
		};
	}

	private RecipeView shaped(String titleKey, ItemStack result, net.minecraft.world.level.ItemLike... ingredients) {
		ItemStack[] stacks = new ItemStack[9];
		for (int index = 0; index < stacks.length; index++) {
			stacks[index] = ingredients[index] == null ? ItemStack.EMPTY : new ItemStack(ingredients[index]);
		}
		return new RecipeView(Component.translatable(titleKey), stacks, result);
	}

	private RecipeView shapeless(String titleKey, ItemStack result, net.minecraft.world.level.ItemLike... ingredients) {
		ItemStack[] stacks = new ItemStack[9];
		for (int index = 0; index < stacks.length; index++) {
			stacks[index] = index < ingredients.length ? new ItemStack(ingredients[index]) : ItemStack.EMPTY;
		}
		return new RecipeView(Component.translatable(titleKey), stacks, result);
	}

	private ItemStack stack(net.minecraft.world.level.ItemLike item) {
		return new ItemStack(item);
	}

	private record RecipeView(Component title, ItemStack[] ingredients, ItemStack result) {
	}
}
