import { RESTAURANT_TYPE_LABELS, isOpen } from "../../../domain/Restaurant.ts";
import { logoImg, tag } from "../../../presenter/components.ts";
import { h } from "../../../presenter/dom.ts";
import { money } from "../../../presenter/format.ts";
import type { CardModel } from "./filters.ts";

/** Landscape banner per restaurant (cards only; menu header keeps the logo). */
const BANNERS: Record<string, string> = {
	"a6a52c73-9070-4128-a988-255383b941bc": "/img/banners/fries.jpg",
	"b7b62d84-1234-5678-9101-112131415161": "/img/banners/pizza.jpg",
	"c8c73e95-2345-6789-1011-121314151617": "/img/banners/sushi.jpg",
	"d9d84f06-3456-7891-0222-324252637485": "/img/banners/finedining.jpg",
	"e0ea1785-4567-8910-0333-435363748596": "/img/banners/kfc.jpg",
	"f1fa2896-5678-9012-0444-546474859607": "/img/banners/pizzahut.jpg",
	"a1ab3078-6789-0123-0555-657585960718": "/img/banners/dominos.jpg",
	"b2bc4189-7890-1234-0666-768696071829": "/img/banners/fiveguys.jpg",
	"c3cd5290-8901-2345-0777-8797a7182930": "/img/banners/pasta.jpg",
};

const DELIVERY_BASE_FEE = 2.99;
const DELIVERY_FEE_PER_KM = 0.49;

function deliveryFee(km: number): number {
	return Math.round((DELIVERY_BASE_FEE + DELIVERY_FEE_PER_KM * km) * 100) / 100;
}

/** Uber Eats style card: full-width image, floating chips, info below. */
export function restaurantCard(r: CardModel, km: Map<string, number>): HTMLElement {
	const open = isOpen(r);
	const banner = BANNERS[r.id] ?? r.logoUrl;
	const distance = km.get(r.id);

	return h(
		"a",
		{
			class: "card card-interactive restaurant-card",
			href: `#/restaurants/${r.id}`,
			"aria-label": `${r.name}, ${open ? "open" : "closed"}`,
		},
		h(
			"div",
			{ class: "restaurant-media" },
			logoImg(banner, r.name, "restaurant-banner"),
			r.priceCategory
				? h(
						"span",
						{ class: "price-chip", "aria-label": `Price category ${r.priceCategory}` },
						r.priceCategory,
					)
				: null,
			h(
				"span",
				{ class: `status-chip ${open ? "open" : "closed"}` },
				open ? "Open now" : "Closed",
			),
		),
		h(
			"div",
			{ class: "restaurant-body" },
			h(
				"div",
				{ class: "restaurant-name-row" },
				h("span", { class: "name" }, r.name),
				r.restaurantType ? tag(RESTAURANT_TYPE_LABELS[r.restaurantType]) : null,
			),
			r.fullAddress ? h("div", { class: "address" }, r.fullAddress) : null,
			h(
				"div",
				{ class: "meta-row" },
				r.openingHours ? h("span", { class: "meta" }, r.openingHours) : null,
				h(
					"span",
					{ class: "meta", id: `dist-${r.id}` },
					distance !== undefined ? `${distance.toFixed(1)} km` : "",
				),
				h(
					"span",
					{ class: "meta", id: `fee-${r.id}` },
					distance !== undefined ? `delivery fee ~${money(deliveryFee(distance))}` : "",
				),
			),
		),
	);
}
