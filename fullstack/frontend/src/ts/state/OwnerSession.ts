const OWNER_SESSION_KEY = "owner.session";

export interface OwnerSession {
    email: string;
    restaurantId: string;
}

export function getOwnerSession(): OwnerSession | null {
    try {
        const raw = localStorage.getItem(OWNER_SESSION_KEY);
        return raw ? (JSON.parse(raw) as OwnerSession) : null;
    } catch {
        return null;
    }
}

export function isOwnerLoggedIn(): boolean {
    return !!getOwnerSession();
}

export function loginOwner(email: string, restaurantId: string) {
    const session: OwnerSession = { email, restaurantId };
    localStorage.setItem(OWNER_SESSION_KEY, JSON.stringify(session));
}

export function logoutOwner() {
    localStorage.removeItem(OWNER_SESSION_KEY);
}

export function requireOwner(): OwnerSession {
    const s = getOwnerSession();
    if (!s) throw new Error("Not logged in as owner");
    return s;
}
