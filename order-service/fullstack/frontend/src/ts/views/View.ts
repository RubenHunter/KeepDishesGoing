/** Base view contract. Views clean up timers/listeners in destroy(). */
export interface View {
	render(root: HTMLElement, params: Record<string, string>): void | Promise<void>;
	destroy?(): void;
}
