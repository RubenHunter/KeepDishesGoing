import { downloadPayoutReport } from "../../api/deliveryApi.ts";
import { busyButton, field, toast } from "../../presenter/components.ts";
import { h, mount } from "../../presenter/dom.ts";
import type { View } from "../View.ts";

function isoDaysAgo(days: number): string {
	const d = new Date();
	d.setDate(d.getDate() - days);
	return d.toISOString().slice(0, 10);
}

/** US38 - payout report PDF for a date range, per-courier summary included. */
export class AdminPayoutReportView implements View {
	render(root: HTMLElement): void {
		const from = h("input", {
			class: "input",
			type: "date",
			required: true,
			value: isoDaysAgo(7),
		});
		const to = h("input", {
			class: "input",
			type: "date",
			required: true,
			value: isoDaysAgo(0),
		});
		const downloadBtn = h(
			"button",
			{ class: "btn btn-primary", type: "submit" },
			"Download PDF",
		);

		const form = h(
			"form",
			{
				onsubmit: (e: Event) => {
					e.preventDefault();
					void this.run(downloadBtn, from.value, to.value);
				},
			},
			field("From", from),
			field("To", to),
			downloadBtn,
		);

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
						h("h1", {}, "Payout report"),
						h(
							"p",
							{ class: "subtitle" },
							"PDF with every delivery payout plus a per-courier summary for the period.",
						),
					),
				),
				h("div", { class: "card form-column" }, form),
			),
		);
	}

	private async run(btn: HTMLButtonElement, from: string, to: string): Promise<void> {
		if (from > to) {
			toast('"From" must be before "to".', "error");
			return;
		}
		busyButton(btn, true);
		try {
			await downloadPayoutReport(`${from}T00:00:00`, `${to}T23:59:59`);
			toast("Report downloaded", "success");
		} catch (error) {
			toast(error instanceof Error ? error.message : "Download failed", "error");
		} finally {
			busyButton(btn, false);
		}
	}
}
