export interface Dish {
    id: string;
    name: string;
    description?: string;
    priceAmount: number;
    currency: string;
    status?: string;
}