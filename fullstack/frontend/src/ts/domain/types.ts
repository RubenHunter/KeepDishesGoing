/** Shared value types. Backend speaks EUR decimals - money is a plain number here. */

export type Eur = number;

/** restaurant-service price value object: {"amount": 10.0, "currency": "EUR"} */
export type Priced = {
	amount: Eur;
	currency: string;
};

export type ApiProblem = {
	status: number;
	message: string;
};
