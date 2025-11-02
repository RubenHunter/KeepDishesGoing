import { createOrder, getCart, placeOrderValidated } from "../api/orderApi";
import { getOrCreateCustomerId } from "../state/session";
import { money, setAppContent, showToast } from "../ui";
import { navigate } from "../router";
import { refreshCartBadge } from "./_shared";

export async function CheckoutView() {
    const customerId = getOrCreateCustomerId();
    const cart = await safeCart(customerId);

    if (!cart || !cart.items.length || !cart.restaurantId) {
        setAppContent(`
      <div class="card">
        <div class="card-header">
          <div class="card-title">Checkout</div>
          <div class="card-sub">Your cart is empty.</div>
        </div>
        <button class="btn btn-primary" id="go">Browse restaurants</button>
      </div>
    `);
        document.getElementById("go")!.addEventListener("click", () => navigate("/"));
        refreshCartBadge(true);
        return;
    }

    setAppContent(`
    <div class="grid" style="grid-template-columns: 1.1fr .9fr;">
      <div class="card">
        <div class="card-header">
          <div>
            <div class="card-title">Delivery details</div>
            <div class="card-sub">We will validate availability and prices before placing the order</div>
          </div>
        </div>
        <div class="space"></div>
        <label class="card-sub">Email</label>
        <input class="input" id="email" type="email" placeholder="you@example.com" />
        <div class="space"></div>
        <label class="card-sub">Delivery address</label>
        <input class="input" id="address" type="text" placeholder="Street, number, city" />
        <div class="space"></div>
        <div class="row" style="justify-content:flex-end;">
          <button class="btn btn-outline" id="back">← Back</button>
          <button class="btn btn-primary" id="place">Place order →</button>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="card-title">Order summary</div>
          <span class="badge">${cart.items.length} items</span>
        </div>
        <div class="list">
          ${cart.items
        .map(
            (i) => `
            <div class="item">
              <div>
                <div class="item-title">${escapeHtml(i.itemName)}</div>
                <div class="item-sub">${i.quantity}× ${money(i.unitPrice, i.currency)}</div>
              </div>
              <div class="item-title">${money(i.lineTotal, i.currency)}</div>
            </div>
          `
        )
        .join("")}
        </div>
        <hr class="divider" />
        <div class="row" style="justify-content: space-between;">
          <div class="card-sub">Total</div>
          <div class="item-title">${money(cart.totalAmount, cart.currency)}</div>
        </div>
      </div>
    </div>
  `);

    document.getElementById("back")!.addEventListener("click", () => navigate("/cart"));
    document.getElementById("place")!.addEventListener("click", async () => {
        const email = (document.getElementById("email") as HTMLInputElement).value.trim();
        const address = (document.getElementById("address") as HTMLInputElement).value.trim();
        if (!email || !address) {
            showToast("Please fill in email and address");
            return;
        }

        const btn = document.getElementById("place") as HTMLButtonElement;
        btn.disabled = true;

        try {
            const payload = {
                customerId,
                restaurantId: cart.restaurantId!,
                customerEmail: email,
                deliveryAddress: address,
                items: cart.items.map((i) => ({
                    menuItemId: i.menuItemId,
                    itemName: i.itemName,
                    quantity: i.quantity,
                    unitPrice: i.unitPrice,
                })),
            };
            const created = await createOrder(payload);
            const placed = await placeOrderValidated(created.orderId);
            showToast(placed.message || "Order placed");
            navigate("/"); // Delivery MQ would take over next
        } catch (e: any) {
            showToast(e?.message ?? "Failed to place order");
        } finally {
            btn.disabled = false;
            refreshCartBadge(true);
        }
    });

    refreshCartBadge(true);
}

async function safeCart(customerId: string) {
    try {
        const { getCart } = await import("../api/orderApi");
        return await getCart(customerId);
    } catch {
        return null;
    }
}

function escapeHtml(s: string) {
    return s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]!));
}

