import { hasRole, type Role } from "./state/session.ts";
import { refreshNav } from "./presenter/layout.ts";
import type { View } from "./views/View.ts";

type Route = {
	pattern: string;
	view: () => View;
	role?: Role;
	loginRedirect?: string;
};

/** Convert "/restaurants/:id" to a regex capturing named params. */
function compile(pattern: string): RegExp {
	const regex = pattern.replace(/:[^/]+/g, "([^/]+)");
	return new RegExp(`^${regex}$`);
}

function paramNames(pattern: string): string[] {
	return [...pattern.matchAll(/:([^/]+)/g)].map((m) => m[1]);
}

export class Router {
	private routes: Route[] = [];
	private current: View | null = null;
	private root: HTMLElement;

	constructor(root: HTMLElement) {
		this.root = root;
		window.addEventListener("hashchange", () => void this.resolve());
	}

	register(route: Route): Router {
		this.routes.push(route);
		return this;
	}

	start(): void {
		void this.resolve();
	}

	private async resolve(): Promise<void> {
		const hash = location.hash.slice(1) || "/";
		const [path] = hash.split("?");

		for (const route of this.routes) {
			const match = compile(route.pattern).exec(path);
			if (!match) continue;

			if (route.role && !hasRole(route.role)) {
				location.hash = route.loginRedirect ?? "#/";
				return;
			}

			const params: Record<string, string> = {};
			paramNames(route.pattern).forEach((name, i) => {
				params[name] = decodeURIComponent(match[i + 1]);
			});

			this.current?.destroy?.();
			const view = route.view();
			this.current = view;
			this.root.replaceChildren();
			refreshNav();
			window.scrollTo(0, 0);
			await view.render(this.root, params);
			return;
		}

		// Unknown route → home
		location.hash = "#/";
	}
}
