import { getPayouts, registerDriver } from "../../api/deliveryApi.ts";
import { getSession } from "../../state/session.ts";
import { emptyState, skeletonLines } from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { dateTime, money } from "../../presenter/format.ts";
import type { View } from "../View.ts";

/** US35 - completed deliveries + payouts with running total. */
export class DriverEarningsView implements View {
	private destroyed = false;

	async render(root: HTMLElement): Promise<void> {
		mount(root, h("div", { class: "view" }, skeletonLines(4)));
		const session = getSession();
		const driverId = session?.sub;
		if (!driverId) return;

		try {
			await registerDriver(session!.username).catch(() => {}); // idempotent
			const summary = await getPayouts(driverId);
			if (this.destroyed) return;

			// Oldest first so the running total accumulates naturally.
			const rows = [...summary.rows].sort(
				(a, b) => new Date(a.deliveredAt).getTime() - new Date(b.deliveredAt).getTime(),
			);
			let running = 0;
			const rowsWithRunning = rows.map((r) => {
				running += r.amount;
				return { ...r, running };
			});

			mount(
				root,
				h(
					"div",
					{ class: "view" },
					h(
						"div",
						{ class: "page-header" },
						h(
							"div",
							{},
							h("h1", {}, "Earnings"),
							h("p", { class: "subtitle" }, "Completed deliveries and payouts."),
						),
						h(
							"div",
							{ class: "card stat-tile", style: "min-width:200px" },
							h("div", { class: "stat-value price" }, money(summary.totalAmount)),
							h("div", { class: "stat-label" }, "Total earned"),
						),
					),
					rowsWithRunning.length === 0
						? emptyState(
								"No completed deliveries yet",
								"Claim a delivery and complete it to earn a payout.",
								h("a", { class: "btn btn-primary", href: "#/driver" }, "Find deliveries"),
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
											h("th", {}, "Delivered"),
											h("th", {}, "Delivery"),
											h("th", { class: "num" }, "Billed min"),
											h("th", { class: "num" }, "Payout"),
											h("th", { class: "num" }, "Running total"),
										),
									),
									h(
										"tbody",
										{},
										...rowsWithRunning.map((r) =>
											h(
												"tr",
												{},
												h("td", { class: "mono" }, dateTime(r.deliveredAt)),
												h("td", { class: "mono" }, r.deliveryId.slice(0, 8)),
												h("td", { class: "num" }, String(r.billableMinutes)),
												h("td", { class: "num" }, money(r.amount)),
												h("td", { class: "num" }, h("strong", {}, money(r.running))),
											),
										),
									),
								),
							),
				),
			);
		} catch (error) {
			if (this.destroyed) return;
			mount(
				root,
				h(
					"div",
					{ class: "view" },
					emptyState(
						"Could not load earnings",
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

	destroy(): void {
		this.destroyed = true;
	}
}
