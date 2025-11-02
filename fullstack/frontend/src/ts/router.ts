type RouteHandler = (params: Record<string, string>) => void;

const routes: { pattern: RegExp; keys: string[]; handler: RouteHandler }[] = [];

export function registerRoute(path: string, handler: RouteHandler) {
    const keys: string[] = [];
    const pattern = new RegExp(
        "^" +
        path.replace(/\/:([^/]+)/g, (_, k) => {
            keys.push(k);
            return "/([^/]+)";
        }) +
        "$"
    );
    routes.push({ pattern, keys, handler });
}

function parseLocation(): { path: string; params: Record<string, string> } {
    const h = location.hash || "#/";
    const path = h.replace(/^#/, "");
    for (const r of routes) {
        const m = path.match(r.pattern);
        if (m) {
            const params: Record<string, string> = {};
            r.keys.forEach((k, i) => (params[k] = decodeURIComponent(m[i + 1])));
            return { path, params };
        }
    }
    return { path, params: {} };
}

export function navigate(path: string) {
    if (!path.startsWith("#")) path = "#" + path;
    if (location.hash === path) window.dispatchEvent(new HashChangeEvent("hashchange"));
    else location.hash = path;
}

export function startRouter() {
    const handler = () => {
        const { path, params } = parseLocation();
        for (const r of routes) {
            if (r.pattern.test(path)) {
                r.handler(params);
                return;
            }
        }
    };
    window.addEventListener("hashchange", handler);
    handler();
}

