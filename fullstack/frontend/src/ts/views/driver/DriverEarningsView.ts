import { getPayouts, registerDriver } from "../../api/deliveryApi.ts";
import { getSession } from "../../state/session.ts";
import { emptyState, skeletonLines } from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import { dateTime, money } from "../../presenter/format.ts";
import type { View } from "../View.ts";

type SortKey = "deliveredAt" | "deliveryId" | "billableMinutes" | "amount" | "running";

/** US35 - completed deliveries + payouts with running total, sortable table. */
export class DriverEarningsView implements View {
	private destroyed = false;
	private rowsWithRunning: Array<{ deliveredAt: string; deliveryId: string; billableMinutes: number; amount: number; running: number }> = [];
	private sortKey: SortKey = "deliveredAt";
	private sortDir: "asc" | "desc" = "asc";

	async render(root: HTMLElement): Promise<void> {
		mount(root, h("div", { class: "view" }, skeletonLines(4)));
		const session = getSession();
		const driverId = session?.sub;
		if (!driverId) return;

		try {
			await registerDriver(session!.username).catch(() => {}); // idempotent
			const summary = await getPayouts(driverId);
			if (this.destroyed) return;

			// Running total accumulates chronologically; display order can differ.
			const rows = [...summary.rows].sort(
				(a, b) => new Date(a.deliveredAt).getTime() - new Date(b.deliveredAt).getTime(),
			);
			let running = 0;
			this.rowsWithRunning = rows.map((r) => {
				running += r.amount;
				return {
					deliveredAt: r.deliveredAt,
					deliveryId: r.deliveryId,
					billableMinutes: r.billableMinutes,
					amount: r.amount,
					running,
				};
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
							h("p", { class: "subtitle" }, "Completed deliveries and payouts. Click a column header to sort."),
						),
						h(
							"div",
							{ class: "card stat-tile", style: "min-width:200px" },
							h("div", { class: "stat-value price" }, money(summary.totalAmount)),
							h("div", { class: "stat-label" }, "Total earned"),
						),
					),
					this.rowsWithRunning.length === 0
						? emptyState(
								"No completed deliveries yet",
								"Claim a delivery and complete it to earn a payout.",
								h("a", { class: "btn btn-primary", href: "#/driver" }, "Find deliveries"),
							)
						: this.table(),
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

	private table(): HTMLElement {
		const th = (key: SortKey, label: string, numeric: boolean): HTMLElement => {
			const active = this.sortKey === key;
			const arrow = active ? (this.sortDir === "asc" ? " ↑" : " ↓") : "";
			return h(
				"th",
				{
					class: `sortable${numeric ? " num" : ""}`,
					"aria-sort": active ? (this.sortDir === "asc" ? "ascending" : "descending") : "none",
					onclick: () => this.sortBy(key),
				},
				h("span", { class: "th-label" }, label),
				h("span", { class: "th-arrow", "aria-hidden": "true" }, arrow),
			);
		};

		const sorted = this.sortedRows();
		return h(
			"div",
			{ class: "table-wrap", id: "earnings-table" },
			h(
				"table",
				{ class: "table" },
				h(
					"thead",
					{},
					h(
						"tr",
						{},
						th("deliveredAt", "Delivered", false),
						th("deliveryId", "Delivery", false),
						th("billableMinutes", "Billed min", true),
						th("amount", "Payout", true),
						th("running", "Running total", true),
					),
				),
				h(
					"tbody",
					{},
					...sorted.map((r) =>
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
		);
	}

	private sortedRows(): typeof this.rowsWithRunning {
		const dir = this.sortDir === "asc" ? 1 : -1;
		return [...this.rowsWithRunning].sort((a, b) => {
			switch (this.sortKey) {
				case "deliveryId":
					return a.deliveryId.localeCompare(b.deliveryId) * dir;
				case "billableMinutes":
					return (a.billableMinutes - b.billableMinutes) * dir;
				case "amount":
					return (a.amount - b.amount) * dir;
				case "running":
					return (a.running - b.running) * dir;
				default:
					return (new Date(a.deliveredAt).getTime() - new Date(b.deliveredAt).getTime()) * dir;
			}
		});
	}

	private sortBy(key: SortKey): void {
		if (this.sortKey === key) {
			this.sortDir = this.sortDir === "asc" ? "desc" : "asc";
		} else {
			this.sortKey = key;
			this.sortDir = "asc";
		}
		const wrap = document.getElementById("earnings-table");
		if (wrap && wrap.isConnected) wrap.replaceWith(this.table());
	}

	destroy(): void {
		this.destroyed = true;
	}
}
