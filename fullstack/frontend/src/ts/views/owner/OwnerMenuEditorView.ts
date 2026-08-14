import {
	createDish,
	listDishes,
	publishDish,
	publishMenu,
	schedulePublish,
	setAvailability,
	unpublishDish,
	updateDish,
} from "../../api/restaurantApi.ts";
import { DISH_CATEGORY_LABELS, type Dish, type DishCategory } from "../../domain/Dish.ts";
import { resolveOwnerRestaurantId } from "../../state/ownerRestaurant.ts";
import {
	breadcrumb,
	busyButton,
	dishBadge,
	emptyState,
	field,
	modal,
	skeletonLines,
	toast,
} from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { money } from "../../presenter/format.ts";
import { getScheduledPublish, setScheduledPublish, clearScheduledPublish } from "../../state/scheduledPublish.ts";
import type { View } from "../View.ts";

const MAX_PUBLISHED = 10;

/**
 * US4 (drafts), US5 (live vs pending + counter), US6 (publish/unpublish),
 * US7 (apply all), US8 (schedule), US9 (stock toggle, immediate), US10 (max 10 live).
 */
export class OwnerMenuEditorView implements View {
	private destroyed = false;
	private restaurantId: string | null = null;

	async render(root: HTMLElement): Promise<void> {
		this.restaurantId = await resolveOwnerRestaurantId();
		if (!this.restaurantId) {
			mount(
				root,
				h(
					"div",
					{ class: "view" },
					emptyState(
						"Create your restaurant first",
						"You need a restaurant before you can edit a menu.",
						h("a", { class: "btn btn-primary", href: "#/owner" }, "Go to dashboard"),
					),
				),
			);
			return;
		}
		mount(root, h("div", { class: "view" }, skeletonLines(5)));
		await this.reload(root);
	}

	private async reload(root: HTMLElement): Promise<void> {
		try {
			const dishes = await listDishes(this.restaurantId!);
			if (this.destroyed) return;
			this.paint(root, dishes);
		} catch (error) {
			if (this.destroyed) return;
			mount(
				root,
				h(
					"div",
					{ class: "view" },
					emptyState(
						"Could not load your dishes",
						error instanceof Error ? error.message : "Unknown error",
						h(
							"button",
							{ class: "btn btn-primary", onclick: () => void this.render(root) },
							"Retry",
						),
					),
				),
			);
		}
	}

	private paint(root: HTMLElement, dishes: Dish[]): void {
		const liveCount = dishes.filter((d) => d.status === "PUBLISHED").length;
		const pendingCount = dishes.filter((d) => d.status === "DRAFT").length;

		mount(
			root,
			h(
				"div",
				{ class: "view" },
				breadcrumb([
					{ label: "Dashboard", href: "#/owner" },
					{ label: "Menu editor" },
				]),
				h(
					"div",
					{ class: "page-header" },
					h(
						"div",
						{},
						h("h1", {}, "Menu editor"),
						h(
							"p",
							{ class: "subtitle" },
							`${liveCount}/${MAX_PUBLISHED} dishes live · ${pendingCount} pending drafts`,
						),
					),
					h(
						"button",
						{ class: "btn btn-primary", onclick: () => this.dishForm(root, null) },
						"New dish",
					),
				),
				pendingCount > 0 ? this.pendingBanner(root, pendingCount) : null,
				this.scheduledBanner(),
				dishes.length === 0
					? emptyState(
							"No dishes yet",
							"Create a dish - it starts as a draft and does not affect your live menu.",
							h(
								"button",
								{ class: "btn btn-primary", onclick: () => this.dishForm(root, null) },
								"Create first dish",
							),
						)
					: h(
							"div",
							{ class: "table-wrap" },
							h(
								"table",
								{ class: "table" },
								h(
									"thead",
									{},
									h(
										"tr",
										{},
										h("th", {}, "Dish"),
										h("th", {}, "Category"),
										h("th", { class: "num" }, "Price"),
										h("th", {}, "State"),
										h("th", { class: "actions" }, "Actions"),
									),
								),
								h("tbody", {}, ...dishes.map((d) => this.row(root, d))),
							),
						),
			),
		);
	}

	private pendingBanner(root: HTMLElement, count: number): HTMLElement {
		return h(
			"div",
			{ class: "pending-banner" },
			h(
				"span",
				{},
				h("strong", {}, `${count} draft${count === 1 ? "" : "s"} pending`),
				" - not visible on the live menu yet.",
			),
			h(
				"div",
				{ class: "cluster" },
				h(
					"button",
					{
						class: "btn btn-primary btn-sm",
						onclick: (e: Event) =>
							void this.run(
								e.target as HTMLButtonElement,
								async () => {
									await publishMenu(this.restaurantId!);
									clearScheduledPublish(this.restaurantId!);
								},
								"All drafts published",
								root,
							),
					},
					"Apply all now",
				),
				h(
					"button",
					{
						class: "btn btn-secondary btn-sm",
						onclick: () => this.scheduleDialog(root),
					},
					"Schedule…",
				),
			),
		);
	}

	private scheduledBanner(): HTMLElement | null {
		const iso = getScheduledPublish(this.restaurantId!);
		if (!iso) return null;
		const date = new Date(iso);
		const formatted = date.toLocaleString(undefined, {
			dateStyle: "medium",
			timeStyle: "short",
		});
		return h(
			"div",
			{ class: "pending-banner", style: "background:var(--info-soft);border-color:var(--info)" },
			h("span", {}, h("strong", {}, "Scheduled"), ` - pending drafts go live ${formatted}.`),
		);
	}

	private row(root: HTMLElement, dish: Dish): HTMLElement {
		const isPublished = dish.status === "PUBLISHED";
		const isOutOfStock = dish.status === "OUT_OF_STOCK";

		return h(
			"tr",
			{},
			h(
				"td",
				{},
				h("div", { style: "font-weight:600" }, dish.name),
				h("div", { class: "muted", style: "max-width:36ch" }, dish.description),
			),
			h(
				"td",
				{},
				dish.category
					? h("span", { class: "badge badge-neutral" }, DISH_CATEGORY_LABELS[dish.category])
					: "-",
			),
			h("td", { class: "num" }, money(dish.price.amount)),
			h("td", {}, dishBadge(dish.status)),
			h(
				"td",
				{ class: "actions" },
				h(
					"div",
					{ class: "cluster", style: "justify-content:flex-end" },
					h(
						"button",
						{ class: "btn btn-ghost btn-sm", onclick: () => this.dishForm(root, dish) },
						"Edit",
					),
					isPublished || isOutOfStock
						? h(
								"button",
								{
									class: "btn btn-ghost btn-sm",
									onclick: (e: Event) =>
										void this.run(
											e.target as HTMLButtonElement,
											() => unpublishDish(this.restaurantId!, dish.id),
											"Unpublished",
											root,
										),
								},
								"Unpublish",
							)
						: h(
								"button",
								{
									class: "btn btn-ghost btn-sm",
									onclick: (e: Event) =>
										void this.run(
											e.target as HTMLButtonElement,
											() => publishDish(this.restaurantId!, dish.id),
											"Published",
											root,
										),
								},
								"Publish",
							),
					isPublished
						? h(
								"button",
								{
									class: "btn btn-ghost btn-sm",
									onclick: (e: Event) =>
										void this.run(
											e.target as HTMLButtonElement,
											() => setAvailability(this.restaurantId!, dish.id, false),
											"Marked out of stock",
											root,
										),
								},
								"Out of stock",
							)
						: isOutOfStock
							? h(
									"button",
									{
										class: "btn btn-ghost btn-sm",
										onclick: (e: Event) =>
											void this.run(
												e.target as HTMLButtonElement,
												() => setAvailability(this.restaurantId!, dish.id, true),
												"Back in stock",
												root,
											),
									},
									"In stock",
								)
							: null,
				),
			),
		);
	}

	private dishForm(root: HTMLElement, dish: Dish | null): void {
		const name = h("input", { class: "input", required: true, value: dish?.name ?? "" });
		const description = h(
			"textarea",
			{ class: "textarea", required: true },
			dish?.description ?? "",
		);
		const price = h("input", {
			class: "input",
			type: "number",
			min: "0.01",
			step: "0.01",
			required: true,
			value: dish ? dish.price.amount.toFixed(2) : "",
		});
		const category = h(
			"select",
			{ class: "select", required: true },
			...Object.entries(DISH_CATEGORY_LABELS).map(([value, label]) =>
				h(
					"option",
					{ value, selected: dish?.category === value },
					label,
				),
			),
		);
		const imageUrl = h("input", {
			class: "input",
			type: "url",
			placeholder: "https://example.com/dish-image.jpg",
			value: dish?.imageUrl ?? "",
		});

		const saveBtn = h(
			"button",
			{ class: "btn btn-primary", type: "submit" },
			dish ? "Save changes" : "Save as draft",
		);

		const form = h(
			"form",
			{
				onsubmit: (e: Event) => {
					e.preventDefault();
					dialog.close();
					const body = {
						name: name.value,
						description: description.value,
						price: { amount: Number.parseFloat(price.value), currency: "EUR" },
						category: category.value as DishCategory,
						imageUrl: imageUrl.value.trim() || null,
					};
					void (dish
						? this.run(saveBtn, () => updateDish(this.restaurantId!, dish.id, body), "Dish updated", root)
						: this.run(saveBtn, () => createDish(this.restaurantId!, body), "Draft saved", root));
				},
			},
			field("Name", name),
			field("Description", description),
			field("Price (€)", price, {
				help: dish
					? "Saved as draft - live menu unaffected until you publish."
					: "Starts as a draft, invisible to customers.",
			}),
			field("Category", category),
			field("Image URL", imageUrl, { help: "Optional - shown on the customer menu." }),
			saveBtn,
		);

		const dialog = modal(dish ? `Edit ${dish.name}` : "New dish", form, []);
	}

	private scheduleDialog(root: HTMLElement): void {
		const when = h("input", { class: "input", type: "datetime-local", required: true });
		const scheduleBtn = h("button", { class: "btn btn-primary", type: "submit" }, "Schedule");
		const form = h(
			"form",
			{
				onsubmit: (e: Event) => {
					e.preventDefault();
					dialog.close();
					const iso = toLocalDateTime(when.value);
					void this.run(
						scheduleBtn,
						async () => {
							await schedulePublish(this.restaurantId!, iso);
							setScheduledPublish(this.restaurantId!, iso);
						},
						"Changes scheduled",
						root,
					);
				},
			},
			field("Go live at", when, {
				help: "All pending drafts go live together at this time.",
			}),
			scheduleBtn,
		);
		const dialog = modal("Schedule pending changes", form, []);
	}

	private async run(
		btn: HTMLButtonElement,
		action: () => Promise<unknown>,
		successMessage: string,
		root: HTMLElement,
	): Promise<void> {
		busyButton(btn, true);
		try {
			await action();
			toast(successMessage, "success");
			await this.reload(root);
		} catch (error) {
			busyButton(btn, false);
			toast(error instanceof Error ? error.message : "Action failed", "error");
		}
	}

	destroy(): void {
		this.destroyed = true;
	}
}

/** datetime-local ("2026-07-27T14:30") → LocalDateTime ("2026-07-27T14:30:00") */
function toLocalDateTime(value: string): string {
	return value.length === 16 ? `${value}:00` : value;
}
