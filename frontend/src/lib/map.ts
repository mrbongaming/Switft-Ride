export type MapPoint = {
  latitude: number;
  longitude: number;
};

export type PlaceResult = MapPoint & {
  id: string;
  address: string;
};

export type RoutePreview = {
  positions: [number, number][];
  distanceKm: number;
  durationMinutes: number;
};

const geocodingBaseUrl = (import.meta.env.VITE_GEOCODING_URL || 'https://nominatim.openstreetmap.org').replace(/\/$/, '');
const routingBaseUrl = (import.meta.env.VITE_ROUTING_URL || 'https://router.project-osrm.org').replace(/\/$/, '');

let nominatimQueue: Promise<unknown> = Promise.resolve();
let lastNominatimRequest = 0;

function queuedNominatimRequest<T>(url: string): Promise<T> {
  const request = async () => {
    const waitMs = Math.max(0, 1100 - (Date.now() - lastNominatimRequest));
    if (waitMs) await new Promise(resolve => window.setTimeout(resolve, waitMs));
    lastNominatimRequest = Date.now();
    const response = await fetch(url, {
      headers: { Accept: 'application/json', 'Accept-Language': 'vi' },
    });
    if (!response.ok) throw new Error(`Dịch vụ tìm kiếm bản đồ đang bận (${response.status}).`);
    return response.json() as Promise<T>;
  };

  const result = nominatimQueue.then(request, request);
  nominatimQueue = result.then(() => undefined, () => undefined);
  return result;
}

export async function searchPlaces(query: string): Promise<PlaceResult[]> {
  const params = new URLSearchParams({
    q: query.trim(),
    format: 'jsonv2',
    countrycodes: 'vn',
    addressdetails: '1',
    limit: '5',
  });
  const data = await queuedNominatimRequest<Array<{ place_id: number; display_name: string; lat: string; lon: string }>>(
    `${geocodingBaseUrl}/search?${params}`,
  );
  return data.map(place => ({
    id: String(place.place_id),
    address: place.display_name,
    latitude: Number(place.lat),
    longitude: Number(place.lon),
  }));
}

export async function reverseGeocode(point: MapPoint): Promise<string> {
  const params = new URLSearchParams({
    lat: String(point.latitude),
    lon: String(point.longitude),
    format: 'jsonv2',
    zoom: '18',
  });
  const data = await queuedNominatimRequest<{ display_name?: string }>(`${geocodingBaseUrl}/reverse?${params}`);
  return data.display_name || `Vị trí ${point.latitude.toFixed(6)}, ${point.longitude.toFixed(6)}`;
}

export async function fetchRoute(pickup: MapPoint, dropoff: MapPoint, signal?: AbortSignal): Promise<RoutePreview> {
  const coordinates = `${pickup.longitude},${pickup.latitude};${dropoff.longitude},${dropoff.latitude}`;
  const params = new URLSearchParams({ overview: 'full', geometries: 'geojson', steps: 'false' });
  const response = await fetch(`${routingBaseUrl}/route/v1/driving/${coordinates}?${params}`, { signal });
  if (!response.ok) throw new Error(`Không tải được tuyến đường (${response.status}).`);

  const data = await response.json() as {
    code: string;
    routes?: Array<{
      distance: number;
      duration: number;
      geometry: { coordinates: [number, number][] };
    }>;
  };
  const route = data.routes?.[0];
  if (data.code !== 'Ok' || !route) throw new Error('Không tìm thấy đường đi phù hợp giữa hai điểm.');

  return {
    positions: route.geometry.coordinates.map(([longitude, latitude]) => [latitude, longitude]),
    distanceKm: Math.round((route.distance / 1000) * 10) / 10,
    durationMinutes: Math.max(1, Math.round(route.duration / 60)),
  };
}
