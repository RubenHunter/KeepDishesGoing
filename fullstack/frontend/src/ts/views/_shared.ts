import { getCart } from "../api/orderApi";
import { getOrCreateCustomerId } from "../state/session";

export async function refreshCartBadge(inline = false) {
    try {
        const cart = await getCart(getOrCreateCustomerId());
        const n = cart.items.reduce((a, b) => a + b.quantity, 0);
        const el = document.getElementById("cart-badge");
        if (el) el.textContent = String(n);
        if (inline) {
            const el2 = document.getElementById("cart-badge-inline");
            if (el2) el2.textContent = String(n);
        }
    } catch {
        // ignore
    }
}
