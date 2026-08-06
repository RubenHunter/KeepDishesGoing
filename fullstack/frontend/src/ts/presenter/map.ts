import { geocode, haversineKm, homeAddressLine, homeCoordinates } from "./geo.ts";

let leafletLoaded = false;

function ensureLeaflet(): Promise<void> {
	if (leafletLoaded) return Promise.resolve();
	return new Promise((resolve) => {
		const link = document.createElement("link");
		link.rel = "stylesheet";
		link.href = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css";
		document.head.appendChild(link);

		const script = document.createElement("script");
		script.src = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js";
		script.onload = () => { leafletLoaded = true; resolve(); };
		script.onerror = () => resolve();
		document.head.appendChild(script);
	});
}

interface LatLon { lat: number; lon: number }

/**
 * Restaurant detail map with 2 pins (restaurant + home) and distance line
 * when the user has a saved home address. Falls back to single-pin OSM embed.
 */
export async function restaurantMap(
	address: string,
	container: HTMLElement,
): Promise<void> {
	const homeLine = homeAddressLine();

	try {
		const [restCoord, homeCoord] = await Promise.all([
			geocode(address),
			homeLine ? homeCoordinates() : Promise.resolve(null),
		]);
		if (!restCoord) return;

		if (!homeCoord) {
			renderSimpleEmbed(restCoord, container);
			return;
		}

		await ensureLeaflet();
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const L = (window as any).L;
		if (!L) {
			renderSimpleEmbed(restCoord, container);
			return;
		}

		renderDualMap(restCoord, homeCoord, container, L);
	} catch {
		/* offline */
	}
}

function renderSimpleEmbed(coord: LatLon, container: HTMLElement): void {
	const dLat = 0.006, dLon = 0.009;
	const bbox = `${coord.lon - dLon},${coord.lat - dLat},${coord.lon + dLon},${coord.lat + dLat}`;
	const src = `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${coord.lat},${coord.lon}`;

	const frame = document.createElement("iframe");
	frame.src = src;
	frame.loading = "lazy";
	frame.setAttribute("referrerpolicy", "no-referrer");

	const wrap = document.createElement("div");
	wrap.className = "map-embed";
	wrap.appendChild(frame);

	const existing = container.firstChild;
	if (existing) container.replaceChild(wrap, existing);
	else container.appendChild(wrap);
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function renderDualMap(rest: LatLon, home: LatLon, container: HTMLElement, L: any): void {
	const midLat = (rest.lat + home.lat) / 2;
	const midLon = (rest.lon + home.lon) / 2;
	const km = haversineKm(rest, home);

	const wrapper = document.createElement("div");

	const mapDiv = document.createElement("div");
	mapDiv.className = "map-embed";
	mapDiv.style.cssText = "height:300px;width:100%;border-radius:var(--radius-lg);overflow:hidden";
	wrapper.appendChild(mapDiv);

	const distLabel = document.createElement("p");
	distLabel.className = "help muted";
	distLabel.style.cssText = "margin-top:var(--space-2);font-size:var(--text-sm)";
	distLabel.textContent = `${km.toFixed(1)} km from your home`;
	wrapper.appendChild(distLabel);

	container.replaceChildren(wrapper);

	const map = L.map(mapDiv).setView([midLat, midLon], 13);
	L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
		attribution: "&copy; OpenStreetMap",
	}).addTo(map);

	L.marker([rest.lat, rest.lon]).addTo(map)
		.bindPopup("<strong>Restaurant</strong>").openPopup();

	L.marker([home.lat, home.lon]).addTo(map)
		.bindPopup("<strong>Your home</strong>");

	L.polyline([[rest.lat, rest.lon], [home.lat, home.lon]], {
		color: "#C2410C", weight: 2, dashArray: "6 4",
	}).addTo(map);

	const latPad = Math.abs(rest.lat - home.lat) * 2 + 0.008;
	const lonPad = Math.abs(rest.lon - home.lon) * 2 + 0.008;
	map.fitBounds([
		[Math.min(rest.lat, home.lat) - latPad, Math.min(rest.lon, home.lon) - lonPad],
		[Math.max(rest.lat, home.lat) + latPad, Math.max(rest.lon, home.lon) + lonPad],
	]);
}
