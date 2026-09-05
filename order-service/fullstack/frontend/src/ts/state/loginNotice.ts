import { load, remove, save } from "../infrastructure/storage.ts";

const KEY = "kdg.login-notice";

export type LoginNotice = {
	message: string;
	/** Hash the login view should send the user to after a successful login. */
	returnHash?: string;
};

/** Queue a notice shown on the next login view (e.g. guest tries to add to cart). */
export function setLoginNotice(message: string, returnHash?: string): void {
	save(KEY, { message, returnHash } satisfies LoginNotice);
}

/** Read once and clear. */
export function takeLoginNotice(): LoginNotice | null {
	const notice = load<LoginNotice>(KEY);
	remove(KEY);
	return notice;
}
