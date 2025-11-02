import { setAppContent, showToast, money, spinner } from "../ui";
import { requireOwner } from "../state/OwnerSession";
import { fetchMenu } from "../api/restaurantApi";
import { navigate } from "../router";

type DraftChange =
    | { type: "create"; id: string; name: string; price: number; currency: string; description?: string | null }
    | { type: "update"; id: string; name?: string; price?: number; description?: string | null }
    | { type: "delete"; id: string };

function draftsKey(rid: string) {
    return `owner.drafts.${rid}`;
}
function loadDrafts(rid: string): DraftChange[] {
    try {
        const raw = localStorage.getItem(draftsKey(rid));
        return raw ? (JSON.parse(raw) as DraftChange[]) : [];
    } catch {
        return [];
    }
}
function saveDrafts(rid: string, drafts: DraftChange[]) {
    localStorage.setItem(draftsKey(rid), JSON.stringify(drafts));
}

export async function OwnerMenuView() {
    const s = requireOwner();
    setAppContent(`
    <div class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Menu management</div>
          <div class="card-sub">Restaurant: ${escapeHtml(s.restaurantId)}</div>
        </div>
        <div class="row">
          <button id="back" class="btn btn-outline">← Back</button>
          <button id="add" class="btn">+ Add dish (draft)</button>
          <button id="discard" class="btn btn-danger">Discard drafts</button>
          <button id="publish" class="btn btn-primary">Publish all</button>
        </div>
      </div>
      <div id="menu-body" class="row muted">${spinner()} Loading...</div>
    </div>
  `);

    document.getElementById("back")!.addEventListener("click", () => navigate("/owner"));
    document.getElementById("add")!.addEventListener("click", async () => {
        const name = prompt("Dish name?");
        if (!name) return;
        const priceStr = prompt("Price (e.g. 12.50)?", "9.99");
        const price = Number(priceStr ?? "");
        if (!isFinite(price) || price <= 0) {
            showToast("Invalid price");
            return;
        }
        const drafts = loadDrafts(s.restaurantId);
        drafts.push({ type: "create", id: crypto.randomUUID(), name, price, currency: "EUR", description: "" });
        saveDrafts(s.restaurantId, drafts);
        await render();
        showToast("Draft created");
    });
    document.getElementById("discard")!.addEventListener("click", async () => {
        saveDrafts(s.restaurantId, []);
        await render();
        showToast("Drafts discarded");
    });
    document.getElementById("publish")!.addEventListener("click", async () => {
        // No backend yet: simulate publish
        saveDrafts(s.restaurantId, []);
        await render();
        showToast("Published (simulated). Connect backend to persist.");
    });

    await render();

    async function render() {
        const body = document.getElementById("menu-body")!;
        body.innerHTML = `<div class="row muted">${spinner()} Loading menu...</div>`;
        try {
            const live = await fetchMenu(s.restaurantId);
            const drafts = loadDrafts(s.restaurantId);
            const pendingCount = drafts.length;

            // Apply overlay to mark items with pending updates or deletes
            const pendingUpdateIds = new Set(drafts.filter(d => d.type === "update").map(d => (d as any).id));
            const pendingDeleteIds = new Set(drafts.filter(d => d.type === "delete").map(d => (d as any).id));

            const liveList = live.map(d => {
                const flagged = pendingUpdateIds.has(d.id) || pendingDeleteIds.has(d.id);
                const tag = flagged ? `<span class="badge">pending</span>` : "";
                return `
          <div class="item">
            <div>
              <div class="item-title">${escapeHtml(d.name)} ${tag}</div>
              <div class="item-sub">${escapeHtml(d.description ?? "")}</div>
            </div>
            <div class="row">
              <div class="item-title">${money(d.price, d.currency)}</div>
              <button class="btn btn-outline" data-edit="${d.id}">Edit</button>
              <button class="btn btn-danger" data-del="${d.id}">Delete</button>
            </div>
          </div>
        `;
            }).join("");

            const draftsList = drafts.length
                ? drafts.map(dr => {
                    if (dr.type === "create") {
                        return `
                <div class="item">
                  <div>
                    <div class="item-title">[new] ${escapeHtml(dr.name)}</div>
                    <div class="item-sub">${money(dr.price, dr.currency)}</div>
                  </div>
                  <div class="row">
                    <button class="btn btn-danger" data-rm="${dr.id}">Remove</button>
                  </div>
                </div>
              `;
                    }
                    if (dr.type === "update") {
                        const parts = [
                            dr.name ? `name → ${escapeHtml(dr.name)}` : null,
                            isFinite(Number(dr.price)) && dr.price! > 0 ? `price → ${money(dr.price!, "EUR")}` : null,
                            dr.description ? `desc → ${escapeHtml(dr.description!)}` : null,
                        ].filter(Boolean).join(", ");
                        return `
                <div class="item">
                  <div class="item-title">[update] #${dr.id}</div>
                  <div class="item-sub">${parts || "no changes"}</div>
                  <button class="btn btn-danger" data-rm="${dr.id}">Remove</button>
                </div>
              `;
                    }
                    return `
              <div class="item">
                <div class="item-title">[delete] #${dr.id}</div>
                <button class="btn btn-danger" data-rm="${dr.id}">Undo</button>
              </div>
            `;
                }).join("")
                : `<div class="muted">No pending changes</div>`;

            body.innerHTML = `
        <div class="grid" style="grid-template-columns: 1fr 1fr;">
          <div class="card">
            <div class="card-header">
              <div class="card-title">Live</div>
              <span class="badge">${live.length}</span>
            </div>
            <div class="list">${liveList}</div>
          </div>

          <div class="card">
            <div class="card-header">
              <div class="card-title">Pending</div>
              <span class="badge" id="pending-badge">${pendingCount}</span>
            </div>
            <div class="list">${draftsList}</div>
          </div>
        </div>
      `;

            // Wire live actions to create drafts
            body.querySelectorAll<HTMLButtonElement>('button[data-edit]').forEach(btn => {
                btn.addEventListener("click", () => {
                    const id = btn.dataset.edit!;
                    const newName = prompt("New name (leave empty to keep)?") || undefined;
                    const priceStr = prompt("New price (leave empty to keep)?") || "";
                    const newPrice = priceStr.trim() ? Number(priceStr) : undefined;
                    if (newPrice !== undefined && (!isFinite(newPrice) || newPrice <= 0)) {
                        showToast("Invalid price");
                        return;
                    }
                    const newDesc = prompt("New description (leave empty to keep)?") || undefined;
                    const drafts = loadDrafts(s.restaurantId);
                    drafts.push({ type: "update", id, name: newName || undefined, price: newPrice, description: newDesc });
                    saveDrafts(s.restaurantId, drafts);
                    render();
                    showToast("Change added to pending");
                });
            });
            body.querySelectorAll<HTMLButtonElement>('button[data-del]').forEach(btn => {
                btn.addEventListener("click", () => {
                    const id = btn.dataset.del!;
                    const drafts = loadDrafts(s.restaurantId);
                    drafts.push({ type: "delete", id });
                    saveDrafts(s.restaurantId, drafts);
                    render();
                    showToast("Delete marked as pending");
                });
            });
            body.querySelectorAll<HTMLButtonElement>('button[data-rm]').forEach(btn => {
                btn.addEventListener("click", () => {
                    const id = btn.dataset.rm!;
                    const drafts = loadDrafts(s.restaurantId).filter(d => d.id !== id);
                    saveDrafts(s.restaurantId, drafts);
                    render();
                    showToast("Pending change removed");
                });
            });
        } catch (e: any) {
            body.innerHTML = `<div class="muted">Failed to load: ${escapeHtml(e?.message ?? "Error")}</div>`;
        }
    }
}

function escapeHtml(s: string) {
    return s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]!));
}

