import { h } from "./dom.ts";

/**
 * Weekly opening-hours editor. Serializes to a compact string the backend
 * stores verbatim (openingHours is a free-text field): "Mon–Fri 09:00–22:00; Sat–Sun 10:00–23:00".
 */

const DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"] as const;

type DayRow = {
	enabled: HTMLInputElement;
	open: HTMLInputElement;
	close: HTMLInputElement;
};

export function weeklyHoursEditor(): { el: HTMLElement; value: () => string; valid: () => boolean } {
	const rows: DayRow[] = DAYS.map(() => {
		const enabled = h("input", { type: "checkbox" });
		const open = h("input", { class: "input", type: "time", value: "09:00", disabled: true });
		const close = h("input", { class: "input", type: "time", value: "22:00", disabled: true });
		enabled.addEventListener("change", () => {
			open.disabled = !enabled.checked;
			close.disabled = !enabled.checked;
		});
		return { enabled, open, close };
	});

	const el = h(
		"div",
		{ class: "table-wrap" },
		h(
			"table",
			{ class: "table" },
			h(
				"thead",
				{},
				h("tr", {}, h("th", {}, "Day"), h("th", {}, "Opens"), h("th", {}, "Closes")),
			),
			h(
				"tbody",
				{},
				...rows.map((row, i) =>
					h(
						"tr",
						{},
						h(
							"td",
							{},
							h(
								"label",
								{ class: "cluster", style: "gap:var(--space-2);cursor:pointer" },
								row.enabled,
								DAYS[i],
							),
						),
						h("td", {}, row.open),
						h("td", {}, row.close),
					),
				),
			),
		),
	);

	/** Group consecutive days with identical hours: "Mon–Fri 09:00–22:00; Sat 10:00–16:00". */
	const value = (): string => {
		const active = rows
			.map((row, i) => ({ row, day: DAYS[i] }))
			.filter(({ row }) => row.enabled.checked);
		const groups: { from: string; to: string; hours: string }[] = [];
		for (const { row, day } of active) {
			const hours = `${row.open.value}–${row.close.value}`;
			const last = groups[groups.length - 1];
			if (last && last.hours === hours) {
				last.to = day;
			} else {
				groups.push({ from: day, to: day, hours });
			}
		}
		return groups
			.map((g) => (g.from === g.to ? `${g.from} ${g.hours}` : `${g.from}–${g.to} ${g.hours}`))
			.join("; ");
	};

	const valid = (): boolean => rows.some((r) => r.enabled.checked);

	return { el, value, valid };
}
