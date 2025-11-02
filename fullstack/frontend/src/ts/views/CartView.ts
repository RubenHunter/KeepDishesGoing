import { clearCart, getCart, removeFromCart, updateCartQuantity } from "../api/orderApi";
import { getOrCreateCustomerId } from "../state/session";
import { money, setAppContent, showToast } from "../ui";
import { navigate } from "../router";
import { refreshCartBadge } from "./_shared";

export async function CartView() {
    setAppContent(`
    <div class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Your Cart</div>
          <div class="card-sub">Review items and proceed to checkout</div>
        </div>
        <div class="row">
          <button id="continue" class="btn btn-outline">← Continue shopping</button>
          <button id="clear" class="btn btn-danger">Clear cart</button>
        </div>
      </div>
      <div id="cart-body"></div>
    </div>
  `);

    document.getElementById("continue")!.addEventListener("click", () => navigate("/"));

    await render();

    async function render() {
        try {
            const cart = await getCart(getOrCreateCustomerId());
            const body = document.getElementById("cart-body")!;
            if (!cart.items.length) {
                body.innerHTML = `<div class="muted">Your cart is empty.</div>`;
                refreshCartBadge(true);
                document.getElementById("clear")!.setAttribute("disabled", "true");
                return;
            }
            body.innerHTML = `
        <div class="list">
          ${cart.items
                .map(
                    (i) => `
            <div class="item">
              <div>
                <div class="item-title">${escapeHtml(i.itemName)}</div>
                <div class="item-sub">${money(i.unitPrice, i.currency)} • ${i.quantity}× = ${money(
                        i.lineTotal,
                        i.currency
                    )}</div>
              </div>
              <div class="row">
                <div class="inline-qty">
                  <button data-dec="${i.menuItemId}">−</button>
                  <input type="number" min="1" value="${i.quantity}" id="qty-${i.menuItemId}" />
                  <button data-inc="${i.menuItemId}">+</button>
                </div>
                <button class="btn btn-outline" data-update="${i.menuItemId}">Update</button>
                <button class="btn btn-danger" data-remove="${i.menuItemId}">Remove</button>
              </div>
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
        <div class="space"></div>
        <div class="row" style="justify-content: flex-end;">
          <button id="checkout" class="btn btn-primary">Proceed to checkout →</button>
        </div>
      `;

            document.getElementById("checkout")!.addEventListener("click", () => navigate("/checkout"));

            document.getElementById("clear")!.addEventListener("click", async () => {
                await clearCart(getOrCreateCustomerId());
                await render();
                showToast("Cart cleared");
            });

            body.querySelectorAll<HTMLButtonElement>("button[data-inc]").forEach((b) =>
                b.addEventListener("click", () => {
                    const id = b.dataset.inc!;
                    const input = document.getElementById(`qty-${id}`) as HTMLInputElement;
                    input.value = String(Math.max(1, Number(input.value || 1) + 1));
                })
            );
            body.querySelectorAll<HTMLButtonElement>("button[data-dec]").forEach((b) =>
                b.addEventListener("click", () => {
                    const id = b.dataset.dec!;
                    const input = document.getElementById(`qty-${id}`) as HTMLInputElement;
                    input.value = String(Math.max(1, Number(input.value || 1) - 1));
                })
            );
            body.querySelectorAll<HTMLButtonElement>("button[data-update]").forEach((b) =>
                b.addEventListener("click", async () => {
                    const id = b.dataset.update!;
                    const qty = Math.max(1, Number((document.getElementById(`qty-${id}`) as HTMLInputElement).value || 1));
                    b.disabled = true;
                    try {
                        await updateCartQuantity({ menuItemId: id, quantity: qty, customerId: getOrCreateCustomerId() });
                        await render();
                        showToast("Quantity updated");
                    } finally {
                        b.disabled = false;
                    }
                })
            );
            body.querySelectorAll<HTMLButtonElement>("button[data-remove]").forEach((b) =>
                b.addEventListener("click", async () => {
                    const id = b.dataset.remove!;
                    b.disabled = true;
                    try {
                        await removeFromCart({ menuItemId: id, customerId: getOrCreateCustomerId() });
                        await render();
                        showToast("Item removed");
                    } finally {
                        b.disabled = false;
                    }
                })
            );

            refreshCartBadge(true);
        } catch (e: any) {
            document.getElementById("cart-body")!.innerHTML = `
        <div class="card" style="border-color: rgba(255,0,0,.2)">
          <div class="row"><span class="muted">Failed to load cart:</span> ${e?.message ?? "Error"}</div>
        </div>
      `;
        }
    }
}

function escapeHtml(s: string) {
    return s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]!));
}

