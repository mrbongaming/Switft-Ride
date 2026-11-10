import { useEffect, useMemo, useState } from 'react';
import { latLngBounds } from 'leaflet';
import { CircleMarker, MapContainer, Polyline, Popup, TileLayer, useMap, useMapEvents } from 'react-leaflet';
import { Loader2, MapPin } from 'lucide-react';
import { fetchRoute, type MapPoint, type RoutePreview } from '../lib/map';
import type { Trip } from '../types';

const defaultCenter: [number, number] = [10.7769, 106.7009];

function MapClickHandler({ enabled, onPoint }: { enabled: boolean; onPoint?: (point: MapPoint) => void }) {
  useMapEvents({
    click(event) {
      if (enabled) onPoint?.({ latitude: event.latlng.lat, longitude: event.latlng.lng });
    },
  });
  return null;
}

function FitRoute({ pickup, dropoff, route }: { pickup: MapPoint | null; dropoff: MapPoint | null; route: RoutePreview | null }) {
  const map = useMap();
  useEffect(() => {
    const positions = route?.positions.length ? route.positions : [pickup, dropoff].filter(Boolean).map(point => [point!.latitude, point!.longitude] as [number, number]);
    if (positions.length >= 2) map.fitBounds(latLngBounds(positions), { padding: [38, 38], maxZoom: 16 });
    else if (positions.length === 1) map.setView(positions[0], 16);
  }, [dropoff, map, pickup, route]);
  return null;
}

type RideMapProps = {
  pickup: MapPoint | null;
  dropoff: MapPoint | null;
  route: RoutePreview | null;
  routeLoading?: boolean;
  interactive?: boolean;
  compact?: boolean;
  onPoint?: (point: MapPoint) => void;
};

export function RideMap({ pickup, dropoff, route, routeLoading, interactive = false, compact = false, onPoint }: RideMapProps) {
  const center = pickup ? [pickup.latitude, pickup.longitude] as [number, number] : defaultCenter;
  const instruction = !pickup ? 'Bấm lần 1 để chọn điểm đón' : !dropoff ? 'Bấm lần 2 để chọn điểm đến' : routeLoading ? 'Đang tìm đường đi…' : route ? `${route.distanceKm} km · khoảng ${route.durationMinutes} phút` : 'Đã chọn đủ hai điểm';

  return <div className={`card relative overflow-hidden bg-[#edf2f8] ${compact ? 'h-[285px]' : 'h-[365px] md:h-[415px]'}`}>
    <MapContainer center={center} zoom={13} className="h-full w-full" zoomControl>
      <TileLayer attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors' url="https://tile.openstreetmap.org/{z}/{x}/{y}.png" />
      <MapClickHandler enabled={interactive && !dropoff} onPoint={onPoint} />
      <FitRoute pickup={pickup} dropoff={dropoff} route={route} />
      {route?.positions.length && <Polyline positions={route.positions} pathOptions={{ color: '#2463eb', weight: 6, opacity: .9 }} />}
      {pickup && <CircleMarker center={[pickup.latitude, pickup.longitude]} radius={8} pathOptions={{ color: '#2463eb', fillColor: '#ffffff', fillOpacity: 1, weight: 4 }}><Popup>Điểm đón</Popup></CircleMarker>}
      {dropoff && <CircleMarker center={[dropoff.latitude, dropoff.longitude]} radius={9} pathOptions={{ color: '#18243b', fillColor: '#18243b', fillOpacity: 1, weight: 2 }}><Popup>Điểm đến</Popup></CircleMarker>}
    </MapContainer>
    <div className="pointer-events-none absolute left-4 top-4 z-[500] flex items-center gap-2 rounded-xl bg-white/95 px-3 py-2 text-[11px] font-bold text-ink shadow-lg backdrop-blur"><MapPin size={14} className="text-brand" />{routeLoading && <Loader2 size={13} className="animate-spin text-brand" />}{instruction}</div>
  </div>;
}

function toPoint(location: Trip['pickup']): MapPoint | null {
  return Number.isFinite(location?.latitude) && Number.isFinite(location?.longitude)
    ? { latitude: location.latitude!, longitude: location.longitude! }
    : null;
}

export function DriverTripMap({ trip }: { trip: Trip }) {
  const pickup = useMemo(() => toPoint(trip.pickup), [trip.pickup]);
  const dropoff = useMemo(() => toPoint(trip.dropoff), [trip.dropoff]);
  const [route, setRoute] = useState<RoutePreview | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!pickup || !dropoff) return;
    const controller = new AbortController();
    setLoading(true);
    setError('');
    fetchRoute(pickup, dropoff, controller.signal)
      .then(setRoute)
      .catch(reason => { if (reason instanceof DOMException && reason.name === 'AbortError') return; setError(reason instanceof Error ? reason.message : 'Không tải được tuyến đường.'); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [dropoff, pickup]);

  if (!pickup || !dropoff) return <div className="card flex h-[285px] flex-col items-center justify-center px-6 text-center"><MapPin size={26} className="mb-3 text-slate-400" /><p className="text-sm font-bold">Chưa có tọa độ tuyến đường</p><p className="mt-1 text-xs text-muted">Backend cần trả latitude và longitude sau khi tài xế nhận chuyến.</p></div>;

  return <div className="space-y-2"><RideMap pickup={pickup} dropoff={dropoff} route={route} routeLoading={loading} compact />{error && <p className="rounded-lg bg-rose-50 px-3 py-2 text-xs text-rose-700">{error}</p>}</div>;
}
