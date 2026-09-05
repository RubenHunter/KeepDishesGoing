import { DISH_CATEGORY_LABELS, type Dish, type DishCategory } from "../../../domain/Dish.ts";
import { h } from "../../../presenter/dom.ts";

export type MenuSort = "none" | "asc" | "desc";

export function filterMenu(
	dishes: Dish[],
	category: DishCategory | null,
	sort: MenuSort,
): Dish[] {
	let list = dishes;
	if (category) {
		list = list.filter((d) => d.category === category);
	}
	if (sort !== "none") {
		list = [...list].sort((a, b) =>
			sort === "asc" ? a.price.amount - b.price.amount : b.price.amount - a.price.amount,
		);
	}
	return list;
}

export function menuFilterBar(
	category: DishCategory | null,
	sort: MenuSort,
	onCategory: (category: DishCategory | null) => void,
	onSort: (sort: MenuSort) => void,
): HTMLElement {
	const chips: HTMLElement[] = [
		h(
			"button",
			{
				class: `filter-chip ${category === null ? "active" : ""}`,
				"aria-pressed": String(category === null),
				onclick: () => onCategory(null),
			},
			"All",
		),
	];
	for (const [cat, label] of Object.entries(DISH_CATEGORY_LABELS)) {
		chips.push(
			h(
				"button",
				{
					class: `filter-chip ${category === cat ? "active" : ""}`,
					"aria-pressed": String(category === cat),
					onclick: () => onCategory(category === cat ? null : (cat as DishCategory)),
				},
				label,
			),
		);
	}
	const sortSelect = h(
		"select",
		{
			class: "select",
			style: "max-width:160px;height:32px;font-size:var(--text-sm)",
			onchange: (e: Event) => onSort((e.target as HTMLSelectElement).value as MenuSort),
		},
		h("option", { value: "none", selected: sort === "none" }, "Sort price"),
		h("option", { value: "asc", selected: sort === "asc" }, "Lowest first"),
		h("option", { value: "desc", selected: sort === "desc" }, "Highest first"),
	);
	return h(
		"div",
		{ class: "filter-bar", role: "group", "aria-label": "Menu filters" },
		...chips,
		sortSelect,
	);
}
