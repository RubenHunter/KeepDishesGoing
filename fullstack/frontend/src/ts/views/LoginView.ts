import { login } from "../api/authApi.ts";
import { KEYCLOAK } from "../config.ts";
import { hasRole, type Role } from "../state/session.ts";
import { takeLoginNotice } from "../state/loginNotice.ts";
import { busyButton, field, toast } from "../presenter/components.ts";
import { h, mount } from "../presenter/dom.ts";
import type { View } from "./View.ts";

/** Keycloak self-registration page (realm has registration allowed; new accounts get the `user` role via default roles). */
function registrationUrl(): string {
	const params = new URLSearchParams({
		client_id: KEYCLOAK.clientId,
		response_type: "code",
		scope: "openid",
		redirect_uri: location.origin + "/",
	});
	return `${KEYCLOAK.url}/realms/${KEYCLOAK.realm}/protocol/openid-connect/registrations?${params}`;
}

/** Shared login for owner/driver/admin - Keycloak direct grant, role checked after login. */
export class LoginView implements View {
	private readonly role: Role;
	private readonly redirect: string;
	private returnHash: string | null = null;

	constructor(role: Role, redirect: string) {
		this.role = role;
		this.redirect = redirect;
	}

	render(root: HTMLElement): void {
		if (hasRole(this.role)) {
			location.hash = this.redirect;
			return;
		}

		const notice = takeLoginNotice();
		if (notice?.returnHash) this.returnHash = notice.returnHash;

		const usernameInput = h("input", {
			class: "input",
			required: true,
			autocomplete: "username",
		});
		const passwordInput = h("input", {
			class: "input",
			type: "password",
			required: true,
			autocomplete: "current-password",
			style: "padding-right:40px",
		});

		const eyeBtn = h(
			"button",
			{
				type: "button",
				"aria-label": "Show password",
				style: "position:absolute;right:4px;top:50%;transform:translateY(-50%);width:32px;height:32px;background:none;border:none;cursor:pointer;display:grid;place-items:center;font-size:16px;line-height:1;padding:0;color:var(--text-muted)",
				onclick: () => {
					const show = passwordInput.type === "password";
					passwordInput.type = show ? "text" : "password";
					const icon = eyeBtn.firstChild as HTMLElement;
					if (show) {
						icon.innerHTML = `<line x1="1" y1="1" x2="23" y2="23"/>`;
						icon.setAttribute("stroke", "var(--text)");
					} else {
						icon.textContent = "";
						icon.removeAttribute("stroke");
					}
					eyeBtn.setAttribute("aria-label", show ? "Hide password" : "Show password");
				},
			},
			h("span", {}, "👁"),
		);

		const pwdWrapper = h("div", { style: "position:relative" }, passwordInput, eyeBtn);
		const submitBtn = h(
			"button",
			{ class: "btn btn-primary", type: "submit", style: "width:100%" },
			"Log in",
		);

		const form = h(
			"form",
			{
				onsubmit: (e: Event) => {
					e.preventDefault();
					void this.submit(submitBtn, usernameInput.value, passwordInput.value);
				},
			},
			field("Username", usernameInput),
			field("Password", pwdWrapper),
			submitBtn,
		);

		mount(
			root,
			h(
				"div",
				{ class: "view" },
				h(
					"div",
					{ class: "login-wrap" },
					h(
						"div",
						{ class: "card" },
						h("h1", {}, "Log in"),
						notice?.message
							? h("p", { class: "pending-banner", role: "status" }, notice.message)
							: null,
						h(
							"p",
							{ class: "role-note" },
							"Sign in with your Keycloak account.",
						),
						form,
						h(
							"p",
							{ class: "help muted", style: "margin-top:var(--space-4)" },
							"No account yet? ",
							h(
								"a",
								{ href: registrationUrl(), target: "_blank", rel: "noopener" },
								"Create one",
							),
							" - new accounts get customer access; owner/courier roles are assigned by the platform admin (demo setup).",
						),
					),
				),
			),
		);
	}

	private async submit(
		btn: HTMLButtonElement,
		username: string,
		password: string,
	): Promise<void> {
		busyButton(btn, true);
		try {
			await login(username, password);
			if (!hasRole(this.role)) {
				toast(`This account does not have the "${this.role}" role.`, "error");
				busyButton(btn, false);
				return;
			}
			toast(`Welcome, ${username}`, "success");
			location.hash = this.returnHash ?? this.redirect;
		} catch (error) {
			busyButton(btn, false);
			toast(error instanceof Error ? error.message : "Login failed", "error");
		}
	}
}
