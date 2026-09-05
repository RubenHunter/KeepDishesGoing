import type { Priced } from "./types.ts";

/**
 * Dish lifecycle (restaurant-service DishStatus):
 * - PUBLISHED: visible + orderable
 * - OUT_OF_STOCK: visible, not addable to cart (US9, immediate - not schedulable)
 * - DRAFT: invisible to customers, pending publish (US4)
 */
export type DishState = "PUBLISHED" | "OUT_OF_STOCK" | "DRAFT";

export type DishCategory = "APPETIZER" | "MAIN_COURSE" | "DESSERT" | "BEVERAGE";

export const DISH_CATEGORY_LABELS: Record<DishCategory, string> = {
	APPETIZER: "Appetizer",
	MAIN_COURSE: "Main course",
	DESSERT: "Dessert",
	BEVERAGE: "Beverage",
};

/** Shared DTO shape for menu (public) and owner dish list */
export type Dish = {
	id: string;
	name: string;
	description: string;
	price: Priced;
	category: DishCategory | null;
	status: DishState;
	imageUrl: string | null;
};

export type DishInput = {
	name: string;
	description: string;
	price: Priced;
	category: DishCategory;
	imageUrl?: string | null;
};
