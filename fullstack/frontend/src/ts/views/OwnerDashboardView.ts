import { setAppContent, showToast } from "../ui";
import { getOwnerSession, logoutOwner } from "../state/ownerSession";
import { navigate } from "../router";

export function OwnerDashboardView() {
    const s = getOwnerSession();
    if (!s) {
        navigate("/owner/login");
        return;
    }
    setAppContent(`
    <div class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Owner dashboard</div>
          <div class="card-sub">Signed in as ${escapeHtml(s.email)} • Restaurant: ${escapeHtml(s.restaurantId)}</div>
        </div>
        <button id="logout" class="btn btn-outline">Logout</button>
      </div>

      <div class="grid" style="grid-template-columns: 1fr 1fr;">
        <div class="card">
          <div class="card-header">
            <div class="card-title">Menu management</div>
            <div class="card-sub">Edit dishes, manage drafts, publish</div>
          </div>
          <button id="go-menu" class="btn btn-primary">Open</button>
        </div>

        <div class="card">
          <div class="card-header">
            <div class="card-title">Deliveries</div>
            <div class="card-sub">See due deliveries (empty until MQ is implemented)</div>
          </div>
          <button id="go-deliveries" class="btn btn-primary">Open</button>
        </div>
      </div>
    </div>
  `);

    document.getElementById("logout")!.addEventListener("click", () => {
        logoutOwner();
        showToast("Logged out");
        navigate("/");
    });
    document.getElementById("go-menu")!.addEventListener("click", () => navigate("/owner/menu"));
    document.getElementById("go-deliveries")!.addEventListener("click", () => navigate("/owner/deliveries"));
}

function escapeHtml(s: string) {
    return s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]!));
}
