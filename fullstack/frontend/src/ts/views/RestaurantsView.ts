import { fetchRestaurants, fetchMenu, priceLevelForAverage } from "../api/restaurantApi";
import { navigate } from "../router";
import { setAppContent, spinner } from "../ui";
import { refreshCartBadge } from "./_shared";

export async function RestaurantsView() {
    setAppContent(`
    <div class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Restaurants</div>
          <div class="card-sub">Pick a restaurant to browse its menu</div>
        </div>
        <div class="row">
          <span class="kbd">Tip</span>
          <span class="muted">Press <b>R</b> to refresh</span>
        </div>
      </div>
      <div id="restaurants-body">
        <div class="row muted">${spinner()} Loading restaurants...</div>
      </div>
    </div>
    <div class="space"></div>
  `);

    try {
        const data = await fetchRestaurants();
        const grid = data.map((r) => `
      <div class="card">
        <div class="card-header">
          <div>
            <div class="card-title">${escapeHtml(r.name)}</div>
            <div class="card-sub">${escapeHtml(r.fullAddress ?? "No address")}</div>
          </div>
          <div class="row">
            <span class="badge">${r.status ?? "UNKNOWN"}</span>
            <span class="badge" id="price-${r.id}">${r.priceLevel ?? ""}</span>
          </div>
        </div>
        <div class="row" style="justify-content: space-between;">
          <div class="muted">${escapeHtml(r.openingHours ?? "")}</div>
          <button class="btn btn-primary" data-open="${r.id}">View menu</button>
        </div>
      </div>
    `).join("");

        const body = document.getElementById("restaurants-body")!;
        body.innerHTML = `<div class="grid">${grid}</div>`;

        // Lazy load price level per restaurant using average menu price (US39)
        data.forEach(async (r) => {
            try {
                const menu = await fetchMenu(r.id);
                if (!menu.length) return;
                const avg = menu.reduce((a, b) => a + (b.price || 0), 0) / menu.length;
                const level = priceLevelForAverage(avg);
                const el = document.getElementById(`price-${r.id}`);
                if (el) el.textContent = level ?? "";
            } catch {
                // ignore
            }
        });

        body.querySelectorAll<HTMLButtonElement>("button[data-open]").forEach((btn) => {
            btn.addEventListener("click", () => navigate(`/restaurants/${btn.dataset.open}`));
        });
    } catch (e: any) {
        document.getElementById("restaurants-body")!.innerHTML = `
      <div class="card" style="border-color: rgba(255,0,0,.2)">
        <div class="row"><span class="muted">Failed to load:</span> ${escapeHtml(e?.message ?? "Error")}</div>
      </div>
    `;
    }

    refreshCartBadge();
}

function escapeHtml(s: string) {
    return s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]!));
}

window.addEventListener("keydown", (e) => {
    if (e.key.toLowerCase() === "r") RestaurantsView();
});