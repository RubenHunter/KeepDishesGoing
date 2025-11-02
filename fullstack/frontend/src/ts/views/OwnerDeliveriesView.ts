import { setAppContent } from "../ui";
import { requireOwner } from "../state/ownerSession";
import { navigate } from "../router";

export function OwnerDeliveriesView() {
    const s = requireOwner();
    setAppContent(`
    <div class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Due deliveries</div>
          <div class="card-sub">Restaurant: ${escapeHtml(s.restaurantId)}</div>
        </div>
        <button id="back" class="btn btn-outline">← Back</button>
      </div>

      <div class="space"></div>
      <div class="card" style="align-items:center; text-align:center;">
        <div class="card-title">Nothing here yet</div>
        <div class="card-sub">This page will show incoming orders / deliveries once RabbitMQ is integrated.</div>
      </div>
    </div>
  `);

    document.getElementById("back")!.addEventListener("click", () => navigate("/owner"));
}

function escapeHtml(s: string) {
    return s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]!));
}
