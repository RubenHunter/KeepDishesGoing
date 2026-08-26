import "./css/tokens.css";
import "./css/base.css";
import "./css/components.css";
import "./css/views.css";

import { renderShell } from "./ts/presenter/layout.ts";
import { Router } from "./ts/router.ts";
import { AdminPayoutReportView } from "./ts/views/admin/AdminPayoutReportView.ts";
import { CartView } from "./ts/views/customer/CartView.ts";
import { CheckoutView } from "./ts/views/customer/CheckoutView.ts";
import { OrderConfirmationView } from "./ts/views/customer/OrderConfirmationView.ts";
import { OrderTrackingView } from "./ts/views/customer/OrderTrackingView.ts";
import { OrdersListView } from "./ts/views/customer/OrdersListView.ts";
import { RestaurantListView } from "./ts/views/customer/RestaurantListView.ts";
import { RestaurantMenuView } from "./ts/views/customer/RestaurantMenuView.ts";
import { AccountView } from "./ts/views/customer/AccountView.ts";
import { DeliveryDetailView } from "./ts/views/driver/DeliveryDetailView.ts";
import { DriverDeliveriesView } from "./ts/views/driver/DriverDeliveriesView.ts";
import { DriverEarningsView } from "./ts/views/driver/DriverEarningsView.ts";
import { LoginView } from "./ts/views/LoginView.ts";
import { OwnerDashboardView } from "./ts/views/owner/OwnerDashboardView.ts";
import { OwnerMenuEditorView } from "./ts/views/owner/OwnerMenuEditorView.ts";
import { OwnerOrdersView } from "./ts/views/owner/OwnerOrdersView.ts";

const page = renderShell();

new Router(page)
	// Customer (public)
	.register({ pattern: "/", view: () => new RestaurantListView() })
	.register({ pattern: "/restaurants/:id", view: () => new RestaurantMenuView() })
	.register({ pattern: "/cart", view: () => new CartView() })
	.register({ pattern: "/checkout", view: () => new CheckoutView() })
	.register({ pattern: "/orders/:id/confirmation", view: () => new OrderConfirmationView() })
	.register({ pattern: "/orders", view: () => new OrdersListView() })
	.register({ pattern: "/orders/:id/track", view: () => new OrderTrackingView() })
	// Owner (role=owner)
	.register({ pattern: "/owner/login", view: () => new LoginView("owner", "#/owner") })
	.register({ pattern: "/owner", view: () => new OwnerDashboardView(), role: "owner", loginRedirect: "#/owner/login" })
	.register({ pattern: "/owner/menu", view: () => new OwnerMenuEditorView(), role: "owner", loginRedirect: "#/owner/login" })
	.register({ pattern: "/owner/orders", view: () => new OwnerOrdersView(), role: "owner", loginRedirect: "#/owner/login" })
	// Driver (role=driver)
	.register({ pattern: "/driver/login", view: () => new LoginView("driver", "#/driver") })
	.register({ pattern: "/driver", view: () => new DriverDeliveriesView(), role: "driver", loginRedirect: "#/driver/login" })
	.register({ pattern: "/driver/deliveries/:id", view: () => new DeliveryDetailView(), role: "driver", loginRedirect: "#/driver/login" })
	.register({ pattern: "/driver/earnings", view: () => new DriverEarningsView(), role: "driver", loginRedirect: "#/driver/login" })
	// Admin (role=admin)
	.register({ pattern: "/admin/login", view: () => new LoginView("admin", "#/admin/payouts") })
	.register({ pattern: "/admin/payouts", view: () => new AdminPayoutReportView(), role: "admin", loginRedirect: "#/admin/login" })
	// User (role=user)
	.register({ pattern: "/user/login", view: () => new LoginView("user", "#/account") })
	.register({ pattern: "/account", view: () => new AccountView(), role: "user", loginRedirect: "#/user/login" })
	.start();
