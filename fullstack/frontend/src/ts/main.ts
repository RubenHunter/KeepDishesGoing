import { registerRoute, startRouter } from "./router";
import { RestaurantsView } from "./views/RestaurantsView";
import { MenuView } from "./views/MenuView";
import { CartView } from "./views/CartView";
import { CheckoutView } from "./views/CheckoutView";
import { OwnerLoginView } from "./views/OwnerLoginView";
import { OwnerDashboardView } from "./views/OwnerDashboardView";
import { OwnerMenuView } from "./views/OwnerMenuView";
import { OwnerDeliveriesView } from "./views/OwnerDeliveriesView";

function ensureAppRoot() {
    if (!document.getElementById("app")) {
        const d = document.createElement("div");
        d.id = "app";
        document.body.appendChild(d);
    }
}
ensureAppRoot();

registerRoute("/", () => RestaurantsView());
registerRoute("/restaurants/:id", ({ id }) => MenuView({ id }));
registerRoute("/cart", () => CartView());
registerRoute("/checkout", () => CheckoutView());

// Owner routes
registerRoute("/owner/login", () => OwnerLoginView());
registerRoute("/owner", () => OwnerDashboardView());
registerRoute("/owner/menu", () => OwnerMenuView());
registerRoute("/owner/deliveries", () => OwnerDeliveriesView());

startRouter();