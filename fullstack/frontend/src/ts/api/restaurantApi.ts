export interface Restaurant {
    id: string;
    name: string;
    status?: string;
    openingHours?: string;
    logo?: string | null;
    fullAddress?: string;
    avgPrice?: number;     // optional, may be supplied by backend
    priceLevel?: string;   // optional, € ... €€€€
}

export function priceLevelForAverage(avg: number | undefined | null): string | undefined {
    if (avg == null || !isFinite(avg)) return undefined;
    if (avg < 10) return "€";
    if (avg <= 30) return "€€";
    if (avg <= 60) return "€€€";
    return "€€€€";
}


export interface Dish {
    id: string;
    name: string;
    description?: string;
    price: number;
    currency: string;
    status?: string;
    category?: string | null;
}

const REST_BASE = "/restaurant-api/api";

function mapDishDto(dto: any): Dish {
    const price = dto.price?.amount ?? dto.price?.number ?? dto.price ?? 0;
    const currency = dto.price?.currency ?? "EUR";
    return {
        id: dto.id?.id ?? dto.id ?? dto.dishId ?? "",
        name: dto.name?.name ?? dto.name ?? dto.title ?? "Dish",
        description: dto.description?.text ?? dto.description ?? "",
        price: Number(price),
        currency,
        status: dto.status ?? dto.dishStatus ?? undefined,
        category: dto.category ?? null,
    };
}

export async function fetchRestaurants(): Promise<Restaurant[]> {
    const res = await fetch(`${REST_BASE}/restaurants`);
    if (!res.ok) throw new Error("Failed to fetch restaurants");
    const arr = await res.json();
    return (arr as any[]).map((r: any) => ({
        id: r.id?.id ?? r.id ?? "",
        name: r.name ?? r.title ?? "Restaurant",
        status: r.status ?? r.restaurantStatus ?? undefined,
        openingHours: r.openingHours ?? undefined,
        logo: r.logo ?? null,
        fullAddress: r.fullAddress ?? undefined,
    }));
}

export async function fetchMenu(restaurantId: string): Promise<Dish[]> {
    const res = await fetch(`${REST_BASE}/restaurants/${restaurantId}/menu`);
    if (!res.ok) throw new Error("Failed to fetch menu");
    const arr = await res.json();
    return (arr as any[]).map(mapDishDto);
}


