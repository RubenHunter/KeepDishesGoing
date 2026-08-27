/** Tiny DOM builder - `h("div", {class: "card"}, child1, child2)`. */

type Attrs = {
	[key: string]: string | number | boolean | EventListener | undefined;
};

type Child = Node | string | number | null | undefined;

export function h<K extends keyof HTMLElementTagNameMap>(
	tag: K,
	attrs: Attrs = {},
	...children: Child[]
): HTMLElementTagNameMap[K] {
	const el = document.createElement(tag);
	for (const [key, value] of Object.entries(attrs)) {
		if (value === undefined || value === false) continue;
		if (key.startsWith("on") && typeof value === "function") {
			el.addEventListener(key.slice(2).toLowerCase(), value);
		} else if (key === "type" && tag === "input") {
			// The `type` IDL property is the only reliable way to set an input's type —
			// some mobile browsers ignore setAttribute("type", "date"/"email"/…).
			(el as HTMLInputElement).type = String(value);
		} else if (value === true) {
			el.setAttribute(key, "");
		} else {
			el.setAttribute(key, String(value));
		}
	}
	el.append(...children.filter((c) => c !== null && c !== undefined).map(toNode));
	return el;
}

export function clear(el: HTMLElement): void {
	el.replaceChildren();
}

export function mount(el: HTMLElement, ...children: Child[]): void {
	el.replaceChildren(...children.filter((c) => c !== null && c !== undefined).map(toNode));
}

function toNode(child: Node | string | number): Node {
	return typeof child === "object" ? child : document.createTextNode(String(child));
}
