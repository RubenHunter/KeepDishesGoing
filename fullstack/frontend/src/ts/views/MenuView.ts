import { fetchMenu } from "../api/restaurantApi";
import { addItemToCart } from "../api/orderApi";
import { getOrCreateCustomerId } from "../state/session";
import { navigate } from "../router";
import { money, setAppContent, showToast, spinner } from "../ui";
import { refreshCartBadge } from "./_shared";

export async function MenuView(params: { id: string }) {
    const restaurantId = params.id;
    setAppContent(`
    <div class="row" style="justify-content: space-between; margin-bottom:12px;">
      <button class="btn btn-outline" id="back">← Back</button>
      <a class="btn btn-primary" href="#/cart">Cart <span id="cart-badge-inline" class="badge">0</span></a>
    </div>
    <div class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Menu</div>
          <div class="card-sub">Add your favorite dishes to cart</div>
        </div>
        <div class="row">
          <input class="input" placeholder="Search dishes..." id="search" />
        </div>
      </div>
      <div id="menu-body" class="grid">
        <div class="row muted">${spinner()} Loading menu...</div>
      </div>
    </div>
  `);

    document.getElementById("back")!.addEventListener("click", () => navigate("/"));

    try {
        const items = await fetchMenu(restaurantId);
        renderMenu(items);

        const search = document.getElementById("search") as HTMLInputElement;
        search.addEventListener("input", () => {
            const q = search.value.toLowerCase().trim();
            const filtered = items.filter(
                (d) =>
                    d.name.toLowerCase().includes(q) ||
                    (d.description ?? "").toLowerCase().includes(q) ||
                    (d.category ?? "").toLowerCase().includes(q)
            );
            renderMenu(filtered);
        });
    } catch (e: any) {
        document.getElementById("menu-body")!.innerHTML = `
      <div class="card" style="grid-column: 1/-1; border-color: rgba(255,0,0,.2)">
        <div class="row"><span class="muted">Failed to load menu:</span> ${e?.message ?? "Error"}</div>
      </div>
    `;
    }

    refreshCartBadge(true);

    function renderMenu(arr: any[]) {
        const html = arr
            .map(
                (d) => `
      <div class="card">
        <div class="card-header">
          <div>
            <div class="card-title">${escapeHtml(d.name)}</div>
            <div class="card-sub">${escapeHtml(d.description ?? "")}</div>
          </div>
          <span class="badge">${escapeHtml(d.category ?? "Dish")}</span>
        </div>
        <div class="row" style="justify-content: space-between;">
          <div class="row" style="gap:18px;">
            <div class="item-title">${money(d.price, d.currency)}</div>
          </div>
          <div class="row">
            <div class="inline-qty">
              <button data-dec="${d.id}">−</button>
              <input type="number" min="1" value="1" id="qty-${d.id}" />
              <button data-inc="${d.id}">+</button>
            </div>
            <button class="btn btn-primary" data-add="${d.id}">Add</button>
          </div>
        </div>
      </div>
    `
            )
            .join("");
        const body = document.getElementById("menu-body")!;
        body.innerHTML = html || `<div class="muted">No dishes found.</div>`;

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
        body.querySelectorAll<HTMLButtonElement>("button[data-add]").forEach((b) =>
            b.addEventListener("click", async () => {
                const id = b.dataset.add!;
                const input = document.getElementById(`qty-${id}`) as HTMLInputElement;
                const qty = Math.max(1, Number(input.value || 1));
                const dish = arr.find((x) => x.id === id);
                if (!dish) return;

                b.disabled = true;
                try {
                    await addItemToCart({
                        restaurantId,
                        menuItemId: dish.id,
                        itemName: dish.name,
                        quantity: qty,
                        unitPrice: dish.price,
                        customerId: getOrCreateCustomerId(),
                    });
                    showToast(`Added ${qty}× ${dish.name}`);
                    refreshCartBadge(true);
                } catch (e: any) {
                    showToast(`Failed: ${e?.message ?? "Error"}`);
                } finally {
                    b.disabled = false;
                }
            })
        );
    }
}

function escapeHtml(s: string) {
    return s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]!));
}

