import { setAppContent, showToast } from "../ui";
import { loginOwner } from "../state/OwnerSession";
import { navigate } from "../router";

export function OwnerLoginView() {
    setAppContent(`
    <div class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Owner login</div>
          <div class="card-sub">Mock login for owner tools</div>
        </div>
      </div>
      <label class="card-sub">Email</label>
      <input class="input" id="email" type="email" placeholder="owner@example.com" />
      <div class="space"></div>
      <label class="card-sub">Restaurant ID</label>
      <input class="input" id="rid" type="text" placeholder="restaurant-uuid" />
      <div class="space"></div>
      <div class="row" style="justify-content:flex-end;">
        <button class="btn btn-primary" id="login">Login</button>
      </div>
    </div>
  `);

    document.getElementById("login")!.addEventListener("click", () => {
        const email = (document.getElementById("email") as HTMLInputElement).value.trim();
        const rid = (document.getElementById("rid") as HTMLInputElement).value.trim();
        if (!email || !rid) {
            showToast("Fill in email and restaurant id");
            return;
        }
        loginOwner(email, rid);
        showToast("Logged in");
        navigate("/owner");
    });
}