import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Activity, ArrowRight, Bell, CarFront,
  Check, CheckCircle2, ChevronDown, CircleHelp, Clock3, CreditCard, Gauge,
  LayoutDashboard, LifeBuoy, LogOut, MapPin, Menu, Navigation, ReceiptText,
  RefreshCw, Route, Search, Settings2, ShieldCheck, UserRound,
  Wallet, X, XCircle,
} from 'lucide-react';
import { api, auth, errorMessage } from './lib/api';
import type { LocationInput, MatchResult, Page, Payment, Quote, Role, Trip, TripStatus } from './types';

const statusText: Record<TripStatus, string> = {
  Requested: 'Đã yêu cầu', Quoted: 'Đang báo giá', DriverOffered: 'Đã mời tài xế',
  DriverAccepted: 'Tài xế đã nhận', EnRoute: 'Đang đến điểm đón', PickedUp: 'Đang di chuyển',
  DroppedOff: 'Đã trả khách', PaymentPending: 'Chờ thanh toán', Paid: 'Hoàn tất',
  Cancelled: 'Đã hủy', Failed: 'Không thành công',
};
const activeStatuses: TripStatus[] = ['Requested', 'Quoted', 'DriverOffered', 'DriverAccepted', 'EnRoute', 'PickedUp', 'DroppedOff', 'PaymentPending'];
const numberFormat = new Intl.NumberFormat('vi-VN');

function money(value?: number, currency = 'VND') {
  if (value === undefined || value === null) return '—';
  return `${numberFormat.format(value)} ${currency === 'VND' ? '₫' : currency}`;
}
function dateTime(value?: string) {
  if (!value) return 'Chưa có thời gian';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}
function shortId(id: string) { return id.length > 12 ? `#${id.slice(0, 8).toUpperCase()}` : `#${id.toUpperCase()}`; }
function statusTone(status: string) {
  if (['Paid', 'Succeeded'].includes(status)) return 'bg-emerald-50 text-emerald-700 ring-emerald-100';
  if (['Failed', 'Cancelled'].includes(status)) return 'bg-rose-50 text-rose-700 ring-rose-100';
  if (status === 'Refunded') return 'bg-violet-50 text-violet-700 ring-violet-100';
  return 'bg-blue-50 text-blue-700 ring-blue-100';
}

type Notice = { kind: 'success' | 'error'; text: string };
type Booking = { pickup: string; dropoff: string; pickupLat: string; pickupLng: string; dropoffLat: string; dropoffLng: string };
const initialBooking: Booking = { pickup: '', dropoff: '', pickupLat: '', pickupLng: '', dropoffLat: '', dropoffLng: '' };

function location(address: string, lat: string, lng: string): LocationInput {
  const result: LocationInput = { address: address.trim() };
  if (lat.trim() && lng.trim()) {
    result.latitude = Number(lat);
    result.longitude = Number(lng);
  }
  return result;
}

export default function App() {
  const [role, setRole] = useState<Role>('rider');
  const [page, setPage] = useState<Page>('overview');
  const [menuOpen, setMenuOpen] = useState(false);
  const [riderId, setRiderId] = useState(localStorage.getItem('swiftride.riderId') || 'rider-001');
  const [driverId, setDriverId] = useState(localStorage.getItem('swiftride.driverId') || 'driver-001');
  const [token, setToken] = useState(auth.getToken());
  const [booking, setBooking] = useState<Booking>(initialBooking);
  const [advanced, setAdvanced] = useState(false);
  const [quote, setQuote] = useState<Quote | null>(null);
  const [match, setMatch] = useState<MatchResult | null>(null);
  const [createdTrip, setCreatedTrip] = useState<Trip | null>(null);
  const [trips, setTrips] = useState<Trip[]>([]);
  const [offers, setOffers] = useState<Trip[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [selectedTrip, setSelectedTrip] = useState<Trip | null>(null);
  const [notice, setNotice] = useState<Notice | null>(null);
  const [busy, setBusy] = useState('');
  const [loading, setLoading] = useState(false);
  const currentId = role === 'rider' ? riderId : driverId;

  const refresh = useCallback(async (currentRole: Role, id: string, showError = true) => {
    if (!id.trim()) return;
    setLoading(true);
    try {
      const calls = currentRole === 'rider'
        ? await Promise.allSettled([api.trips.list('rider', id), api.payments.list(id)])
        : await Promise.allSettled([api.trips.list('driver', id), api.matching.offers(id)]);
      const first = calls[0];
      if (first.status === 'fulfilled') setTrips(first.value);
      if (calls[1].status === 'fulfilled') {
        if (currentRole === 'rider') setPayments(calls[1].value as Payment[]);
        else setOffers(calls[1].value as Trip[]);
      }
      const failed = calls.find(result => result.status === 'rejected');
      if (failed?.status === 'rejected' && showError) setNotice({ kind: 'error', text: errorMessage(failed.reason) });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void refresh(role, currentId); }, [role, currentId, refresh]);
  useEffect(() => { localStorage.setItem('swiftride.riderId', riderId); }, [riderId]);
  useEffect(() => { localStorage.setItem('swiftride.driverId', driverId); }, [driverId]);

  async function run(key: string, action: () => Promise<void>, success?: string) {
    setBusy(key); setNotice(null);
    try {
      await action();
      if (success) setNotice({ kind: 'success', text: success });
      await refresh(role, currentId, false);
    } catch (error) {
      setNotice({ kind: 'error', text: errorMessage(error) });
    } finally { setBusy(''); }
  }

  async function getQuote() {
    if (!booking.pickup.trim() || !booking.dropoff.trim()) {
      setNotice({ kind: 'error', text: 'Vui lòng nhập điểm đón và điểm đến.' }); return;
    }
    for (const [lat, lng] of [[booking.pickupLat, booking.pickupLng], [booking.dropoffLat, booking.dropoffLng]]) {
      if ((lat && !lng) || (!lat && lng) || (lat && lng && (!Number.isFinite(Number(lat)) || !Number.isFinite(Number(lng)) || Math.abs(Number(lat)) > 90 || Math.abs(Number(lng)) > 180))) {
        setNotice({ kind: 'error', text: 'Tọa độ cần đủ vĩ độ và kinh độ hợp lệ.' }); return;
      }
    }
    await run('quote', async () => {
      const trip = await api.trips.create({ riderId, pickup: location(booking.pickup, booking.pickupLat, booking.pickupLng), dropoff: location(booking.dropoff, booking.dropoffLat, booking.dropoffLng) });
      setCreatedTrip(trip); setQuote(null); setMatch(null);
      const nextQuote = await api.matching.quote(trip.id);
      setQuote(nextQuote);
      try { setMatch(await api.matching.match(trip.id)); }
      catch (error) { setNotice({ kind: 'error', text: `Đã có báo giá, nhưng tìm tài xế chưa xong: ${errorMessage(error)}` }); }
    });
  }

  function selectTrip(trip: Trip) { setSelectedTrip(trip); setPage('trips'); setMenuOpen(false); }
  const activeTrip = useMemo(() => trips.find(t => activeStatuses.includes(t.status)) || null, [trips]);
  const completedCount = trips.filter(t => t.status === 'Paid').length;
  const totalSpent = payments.filter(p => p.status === 'Succeeded').reduce((sum, p) => sum + p.amount, 0);

  async function mutateTrip(key: string, action: () => Promise<Trip>, message: string) {
    await run(key, async () => {
      const updated = await action();
      setSelectedTrip(updated);
      if (createdTrip?.id === updated.id) {
        setCreatedTrip(updated);
        if (!['Requested', 'Quoted'].includes(updated.status)) setQuote(null);
      }
    }, message);
  }

  async function refund(payment: Payment) {
    const reason = window.prompt('Lý do hoàn tiền:', 'Yêu cầu hỗ trợ từ khách hàng');
    if (!reason?.trim()) return;
    const keyName = `swiftride.refundKey.${payment.id}`;
    let key = localStorage.getItem(keyName);
    if (!key) { key = crypto.randomUUID(); localStorage.setItem(keyName, key); }
    await run(`refund-${payment.id}`, async () => {
      await api.payments.refund(payment.id, reason, key!);
      localStorage.removeItem(keyName);
    }, 'Đã gửi yêu cầu hoàn tiền.');
  }

  const nav = role === 'rider' ? [
    { id: 'overview' as Page, title: 'Tổng quan', icon: LayoutDashboard },
    { id: 'trips' as Page, title: 'Chuyến đi', icon: Route },
    { id: 'payments' as Page, title: 'Thanh toán', icon: Wallet },
    { id: 'settings' as Page, title: 'Cài đặt kết nối', icon: Settings2 },
  ] : [
    { id: 'overview' as Page, title: 'Bảng điều khiển', icon: LayoutDashboard },
    { id: 'trips' as Page, title: 'Chuyến đi', icon: Route },
    { id: 'settings' as Page, title: 'Cài đặt kết nối', icon: Settings2 },
  ];

  return <div className="min-h-screen bg-canvas text-ink lg:flex">
    {menuOpen && <button className="fixed inset-0 z-30 bg-ink/40 lg:hidden" onClick={() => setMenuOpen(false)} aria-label="Đóng menu" />}
    <aside className={`fixed inset-y-0 left-0 z-40 flex w-[256px] flex-col border-r border-slate-100 bg-white transition-transform lg:sticky lg:top-0 lg:h-screen lg:translate-x-0 ${menuOpen ? 'translate-x-0' : '-translate-x-full'}`}>
      <div className="flex h-[88px] items-center justify-between px-7">
        <button className="flex items-center gap-2.5" onClick={() => { setPage('overview'); setMenuOpen(false); }}>
          <span className="flex h-10 w-10 items-center justify-center rounded-[14px] bg-brand text-white shadow-lg shadow-blue-200"><Navigation size={21} strokeWidth={2.8} className="-rotate-45" /></span>
          <span className="font-display text-[21px] font-extrabold tracking-[-1.3px]">Swift<span className="text-brand">Ride</span></span>
        </button>
        <button className="lg:hidden" onClick={() => setMenuOpen(false)} aria-label="Đóng"><X size={20} /></button>
      </div>
      <div className="px-4 pt-8"><p className="px-3 pb-3 text-[10px] font-bold uppercase tracking-[.18em] text-slate-400">Điều hướng</p>
        <nav className="space-y-1">{nav.map(item => <button key={item.id} onClick={() => { setPage(item.id); setMenuOpen(false); }} className={`nav-item ${page === item.id ? 'nav-active' : ''}`}><item.icon size={19} strokeWidth={page === item.id ? 2.3 : 1.9} /><span>{item.title}</span>{page === item.id && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-brand" />}</button>)}</nav>
      </div>
      <div className="mt-auto px-4 pb-5">
        <div className="mb-5 rounded-[18px] bg-[#f1f6ff] p-4"><div className="mb-2 flex h-8 w-8 items-center justify-center rounded-lg bg-white text-brand"><LifeBuoy size={17} /></div><p className="font-display text-sm font-bold">Cần hỗ trợ?</p><p className="mt-1 text-xs leading-relaxed text-muted">Kiểm tra kết nối và hợp đồng API khi gặp lỗi.</p><button className="mt-3 flex items-center gap-1 text-xs font-bold text-brand" onClick={() => setPage('settings')}>Xem cài đặt <ArrowRight size={13} /></button></div>
        <div className="flex items-center gap-3 border-t border-slate-100 px-2 pt-5"><div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-100 text-brand"><UserRound size={19} /></div><div className="min-w-0"><p className="truncate text-sm font-bold">{role === 'rider' ? 'Khách hàng' : 'Tài xế'}</p><p className="truncate text-[11px] text-muted">{currentId}</p></div><LogOut size={16} className="ml-auto text-slate-300" /></div>
      </div>
    </aside>

    <main className="min-w-0 flex-1">
      <header className="flex h-[76px] items-center justify-between border-b border-slate-100 bg-white px-5 md:px-9 xl:px-12">
        <div className="flex items-center gap-3"><button className="lg:hidden" onClick={() => setMenuOpen(true)} aria-label="Mở menu"><Menu size={22} /></button><div className="hidden text-xs font-medium text-muted sm:block">Trang chủ <span className="mx-2 text-slate-300">/</span> <span className="font-semibold text-ink">{nav.find(n => n.id === page)?.title}</span></div></div>
        <div className="flex items-center gap-3"><span className="hidden items-center gap-2 rounded-full bg-emerald-50 px-3 py-1.5 text-[11px] font-bold text-emerald-700 sm:flex"><span className="h-1.5 w-1.5 rounded-full bg-emerald-500" /> Giao diện sẵn sàng</span><button onClick={() => void refresh(role, currentId)} className="header-icon" title="Làm mới dữ liệu" aria-label="Làm mới dữ liệu"><RefreshCw size={18} className={loading ? 'animate-spin' : ''} /></button><button className="header-icon" aria-label="Thông báo" onClick={() => setNotice({ kind: 'success', text: 'Thông báo sẽ hiển thị khi backend cung cấp dữ liệu.' })}><Bell size={18} /></button><div className="flex h-9 w-9 items-center justify-center rounded-full bg-[#e9efff] font-display text-xs font-extrabold text-brand">{role === 'rider' ? 'KH' : 'TX'}</div></div>
      </header>
      <div className="mx-auto max-w-[1560px] px-5 pb-16 pt-8 md:px-9 xl:px-12">
        {notice && <div role="alert" className={`mb-6 flex items-start gap-3 rounded-xl border p-3.5 text-sm ${notice.kind === 'error' ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-emerald-200 bg-emerald-50 text-emerald-700'}`}>{notice.kind === 'error' ? <XCircle size={18} className="mt-0.5 shrink-0" /> : <CheckCircle2 size={18} className="mt-0.5 shrink-0" />}<span className="flex-1">{notice.text}</span><button aria-label="Đóng thông báo" onClick={() => setNotice(null)}><X size={16} /></button></div>}

        {page === 'overview' && <>
          <div className="mb-7 flex flex-wrap items-end justify-between gap-4"><div><p className="mb-2 text-xs font-bold uppercase tracking-[.16em] text-brand">{role === 'rider' ? 'Hành trình của bạn' : 'Không gian tài xế'}</p><h1 className="font-display text-[27px] font-extrabold tracking-[-1.3px] md:text-[32px]">{role === 'rider' ? 'Chào mừng trở lại 👋' : 'Sẵn sàng lên đường?'}</h1><p className="mt-1.5 text-sm text-muted">{role === 'rider' ? 'Đặt chuyến tiếp theo chỉ trong vài bước.' : 'Theo dõi lời mời và cập nhật trạng thái chuyến đi.'}</p></div><div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white p-1 shadow-card"><button onClick={() => { setRole('rider'); setSelectedTrip(null); }} className={`role-btn ${role === 'rider' ? 'role-active' : ''}`}><UserRound size={15} /> Khách hàng</button><button onClick={() => { setRole('driver'); setSelectedTrip(null); }} className={`role-btn ${role === 'driver' ? 'role-active' : ''}`}><CarFront size={16} /> Tài xế</button></div></div>
          <div className="mb-7 grid gap-4 md:grid-cols-3"><StatCard icon={<Route size={20} />} label={role === 'rider' ? 'Tổng chuyến đi' : 'Chuyến đã nhận'} value={trips.length.toString()} note="Dữ liệu từ Trip Service" tone="blue" /><StatCard icon={<CheckCircle2 size={20} />} label="Chuyến hoàn tất" value={completedCount.toString()} note="Đã thanh toán" tone="green" /><StatCard icon={role === 'rider' ? <Wallet size={20} /> : <Activity size={20} />} label={role === 'rider' ? 'Đã thanh toán' : 'Lời mời mới'} value={role === 'rider' ? money(totalSpent) : offers.length.toString()} note={role === 'rider' ? 'Dữ liệu từ Payment Service' : 'Đang chờ phản hồi'} tone="orange" /></div>
          {role === 'rider' ? <div className="grid gap-6 xl:grid-cols-[minmax(380px,510px)_minmax(0,1fr)]">
            <section className="card p-5 md:p-7"><div className="mb-6 flex items-center justify-between"><div><p className="section-eyebrow">ĐẶT CHUYẾN MỚI</p><h2 className="section-title mt-1">Bạn muốn đi đâu?</h2></div><span className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-brand"><Navigation size={20} /></span></div>
              <div className="relative space-y-4"><div className="absolute left-[19px] top-[39px] h-[67px] border-l-2 border-dashed border-slate-200" /><AddressInput label="Điểm đón" icon="dot" placeholder="Nhập địa chỉ đón" value={booking.pickup} onChange={value => setBooking(b => ({ ...b, pickup: value }))} /><AddressInput label="Điểm đến" icon="pin" placeholder="Bạn muốn đến đâu?" value={booking.dropoff} onChange={value => setBooking(b => ({ ...b, dropoff: value }))} /></div>
              <button type="button" className="mt-5 flex items-center gap-1.5 text-xs font-bold text-brand" onClick={() => setAdvanced(v => !v)}><ChevronDown size={15} className={advanced ? 'rotate-180' : ''} /> Nhập tọa độ thủ công {advanced ? '(ẩn)' : '(tùy chọn)'}</button>
              {advanced && <div className="mt-3 grid grid-cols-2 gap-3"><CoordinateInput label="Vĩ độ đón" value={booking.pickupLat} onChange={value => setBooking(b => ({ ...b, pickupLat: value }))} /><CoordinateInput label="Kinh độ đón" value={booking.pickupLng} onChange={value => setBooking(b => ({ ...b, pickupLng: value }))} /><CoordinateInput label="Vĩ độ đến" value={booking.dropoffLat} onChange={value => setBooking(b => ({ ...b, dropoffLat: value }))} /><CoordinateInput label="Kinh độ đến" value={booking.dropoffLng} onChange={value => setBooking(b => ({ ...b, dropoffLng: value }))} /></div>}
              <div className="mt-5 flex gap-3 rounded-xl bg-[#f7f9fd] p-3.5"><ShieldCheck size={18} className="shrink-0 text-brand" /><p className="text-xs leading-relaxed text-muted">Giá và tài xế do backend tính toán. Bạn sẽ xác nhận giá trước khi gửi lời mời cho tài xế.</p></div>
              <button className="primary-btn mt-6 w-full" disabled={!!busy} onClick={() => void getQuote()}>{busy === 'quote' ? <RefreshCw size={18} className="animate-spin" /> : <Search size={18} />} Tìm xe và báo giá <ArrowRight size={17} className="ml-auto" /></button>
              {quote && createdTrip && <div className="mt-5 rounded-2xl border border-blue-100 bg-[#f5f8ff] p-5"><div className="flex items-start justify-between"><div><p className="text-xs font-bold uppercase tracking-widest text-brand">Giá dự kiến</p><p className="mt-1 font-display text-[30px] font-extrabold tracking-tight">{money(quote.amount, quote.currency)}</p></div><span className="rounded-lg bg-white px-2.5 py-1 text-[11px] font-bold text-brand">{shortId(createdTrip.id)}</span></div><div className="mt-3 flex flex-wrap gap-3 text-xs text-muted">{quote.distanceKm !== undefined && <span><Route size={13} className="mr-1 inline" />{quote.distanceKm} km</span>}{quote.durationMinutes !== undefined && <span><Clock3 size={13} className="mr-1 inline" />{quote.durationMinutes} phút</span>}{match?.etaMinutes !== undefined && <span><CarFront size={13} className="mr-1 inline" />Tài xế: {match.etaMinutes} phút</span>}</div>{match?.driverName && <p className="mt-3 text-xs text-muted">Tài xế gợi ý: <strong className="text-ink">{match.driverName}</strong>{match.vehicle ? ` · ${match.vehicle}` : ''}</p>}<div className="mt-5 flex gap-2"><button className="primary-btn flex-1 justify-center" disabled={!!busy} onClick={() => void mutateTrip('confirm', () => api.trips.confirmQuote(createdTrip.id, quote), 'Đã xác nhận giá. Đang chờ tài xế nhận chuyến.')}><Check size={17} /> Xác nhận giá</button><button className="secondary-btn" disabled={!!busy} onClick={() => void mutateTrip('cancel', () => api.trips.cancel(createdTrip.id), 'Đã hủy chuyến đi.')}>Từ chối</button></div></div>}
            </section>
            <div className="space-y-6"><MapSlot /><section className="card p-5 md:p-6"><div className="flex items-center justify-between"><div><p className="section-eyebrow">CHUYẾN HIỆN TẠI</p><h2 className="section-title mt-1">Theo dõi hành trình</h2></div><button className="text-xs font-bold text-brand" onClick={() => setPage('trips')}>Xem tất cả <ArrowRight size={13} className="inline" /></button></div>{activeTrip ? <TripRow trip={activeTrip} onClick={() => selectTrip(activeTrip)} /> : <EmptyState icon={<CarFront size={23} />} title="Chưa có chuyến đang diễn ra" description="Chuyến mới sẽ xuất hiện ở đây sau khi bạn đặt xe." />}</section></div>
          </div> : <div className="grid gap-6 xl:grid-cols-[minmax(0,1.3fr)_minmax(300px,.7fr)]"><section className="card p-6"><div className="mb-5 flex items-start justify-between"><div><p className="section-eyebrow">LỜI MỜI MỚI</p><h2 className="section-title mt-1">Chuyến đang chờ bạn</h2></div><span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-bold text-brand">{offers.length} lời mời</span></div>{offers.length ? <div className="space-y-3">{offers.map(trip => <div key={trip.id} className="rounded-2xl border border-slate-100 p-4"><TripRoute trip={trip} /><div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-4"><span className="font-display text-lg font-extrabold">{money(trip.quotedFare, trip.currency)}</span><div className="flex gap-2"><button className="secondary-btn" disabled={!!busy} onClick={() => void mutateTrip(`reject-${trip.id}`, () => api.trips.driverReject(trip.id, driverId), 'Đã từ chối lời mời.')}>Từ chối</button><button className="primary-btn" disabled={!!busy} onClick={() => void mutateTrip(`accept-${trip.id}`, () => api.trips.driverAccept(trip.id, driverId), 'Đã nhận chuyến đi.')}><Check size={16} /> Nhận chuyến</button></div></div></div>)}</div> : <EmptyState icon={<Bell size={23} />} title="Chưa có lời mời" description="Lời mời từ Matching và Pricing Service sẽ xuất hiện tại đây." />}</section><div className="space-y-6"><MapSlot compact /><section className="card p-6"><p className="section-eyebrow">CHUYẾN ĐANG THỰC HIỆN</p><h2 className="section-title mt-1">Tiến độ của bạn</h2>{activeTrip ? <TripRow trip={activeTrip} onClick={() => selectTrip(activeTrip)} /> : <EmptyState icon={<Gauge size={23} />} title="Chưa có chuyến hiện tại" description="Nhận một lời mời để bắt đầu hành trình." />}</section></div></div>}
        </>}

        {page === 'trips' && <><PageHeading eyebrow="LỊCH SỬ HOẠT ĐỘNG" title="Chuyến đi" description="Xem trạng thái và các bước tiếp theo của từng chuyến." action={<button className="secondary-btn" onClick={() => void refresh(role, currentId)}><RefreshCw size={16} className={loading ? 'animate-spin' : ''} /> Làm mới</button>} /><div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_370px]"><section className="card overflow-hidden"><div className="border-b border-slate-100 px-6 py-5"><h2 className="font-display text-base font-extrabold">Tất cả chuyến đi <span className="ml-1 text-sm font-medium text-muted">({trips.length})</span></h2></div>{trips.length ? <div className="divide-y divide-slate-100">{trips.map(trip => <TripRow key={trip.id} trip={trip} onClick={() => setSelectedTrip(trip)} selected={selectedTrip?.id === trip.id} />)}</div> : <EmptyState icon={<Route size={25} />} title="Chưa có chuyến đi" description="Dữ liệu từ Trip Service sẽ hiển thị ở đây." />}</section><section className="card h-fit p-6"><p className="section-eyebrow">CHI TIẾT CHUYẾN</p>{selectedTrip ? <><div className="mt-2 flex items-center justify-between"><h2 className="section-title">{shortId(selectedTrip.id)}</h2><StatusBadge status={selectedTrip.status} /></div><p className="mt-1 text-xs text-muted">{dateTime(selectedTrip.createdAt)}</p><div className="my-6 border-t border-slate-100" /><TripRoute trip={selectedTrip} /><div className="my-6 border-t border-slate-100" /><div className="space-y-3 text-sm"><Detail label="Khách hàng" value={selectedTrip.riderId} /><Detail label="Tài xế" value={selectedTrip.driverId || 'Chưa có'} /><Detail label="Giá chuyến" value={money(selectedTrip.finalFare ?? selectedTrip.quotedFare, selectedTrip.currency)} /></div><div className="my-6 border-t border-slate-100" /><h3 className="mb-4 text-sm font-bold">Tiến trình</h3><TripProgress status={selectedTrip.status} /><div className="mt-6 space-y-2">{role === 'driver' && selectedTrip.status === 'DriverAccepted' && <button className="primary-btn w-full justify-center" disabled={!!busy} onClick={() => void mutateTrip('enroute', () => api.trips.enRoute(selectedTrip.id), 'Đã cập nhật: đang đến điểm đón.')}><Navigation size={16} /> Bắt đầu đến điểm đón</button>}{role === 'driver' && selectedTrip.status === 'EnRoute' && <button className="primary-btn w-full justify-center" disabled={!!busy} onClick={() => void mutateTrip('pickup', () => api.trips.pickup(selectedTrip.id), 'Đã xác nhận đón khách.')}><Check size={16} /> Xác nhận đã đón khách</button>}{role === 'driver' && selectedTrip.status === 'PickedUp' && <button className="primary-btn w-full justify-center" disabled={!!busy} onClick={() => void mutateTrip('dropoff', () => api.trips.dropoff(selectedTrip.id), 'Đã xác nhận trả khách.')}><MapPin size={16} /> Xác nhận đã trả khách</button>}{role === 'rider' && ['DroppedOff', 'PaymentPending'].includes(selectedTrip.status) && <div className="rounded-xl bg-blue-50 p-3 text-xs leading-relaxed text-brand"><ShieldCheck size={16} className="mr-2 inline" />Trip Service sẽ phát sự kiện hoàn tất chuyến đi. Payment Service xử lý thanh toán tự động; hãy làm mới để xem kết quả.</div>}{role === 'rider' && ['Requested', 'Quoted', 'DriverOffered'].includes(selectedTrip.status) && <button className="secondary-btn w-full justify-center text-rose-600" disabled={!!busy} onClick={() => void mutateTrip('cancel', () => api.trips.cancel(selectedTrip.id), 'Đã hủy chuyến đi.')}>Hủy chuyến</button>}</div></> : <EmptyState icon={<CircleHelp size={24} />} title="Chọn một chuyến đi" description="Chi tiết và các thao tác sẽ hiện tại đây." />}</section></div></>}

        {page === 'payments' && role === 'rider' && <><PageHeading eyebrow="GIAO DỊCH" title="Thanh toán" description="Theo dõi giao dịch và yêu cầu hoàn tiền khi cần." action={<button className="secondary-btn" onClick={() => void refresh(role, currentId)}><RefreshCw size={16} className={loading ? 'animate-spin' : ''} /> Làm mới</button>} /><div className="mb-6 grid gap-4 md:grid-cols-3"><StatCard icon={<Wallet size={20} />} label="Tổng đã thanh toán" value={money(totalSpent)} note="Giao dịch thành công" tone="blue" /><StatCard icon={<ReceiptText size={20} />} label="Số giao dịch" value={payments.length.toString()} note="Tất cả trạng thái" tone="orange" /><StatCard icon={<CheckCircle2 size={20} />} label="Thành công" value={payments.filter(p => p.status === 'Succeeded').length.toString()} note="Dữ liệu từ Payment Service" tone="green" /></div><section className="card overflow-hidden"><div className="border-b border-slate-100 px-6 py-5"><h2 className="font-display font-extrabold">Lịch sử giao dịch</h2></div>{payments.length ? <div className="divide-y divide-slate-100">{payments.map(payment => <div key={payment.id} className="flex flex-wrap items-center gap-4 px-6 py-5"><span className="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-brand"><CreditCard size={20} /></span><div className="min-w-0 flex-1"><p className="text-sm font-bold">Thanh toán {shortId(payment.tripId)}</p><p className="mt-1 text-xs text-muted">{dateTime(payment.createdAt)} · {payment.method === 'CARD_TOKEN' ? 'Thẻ token' : 'Ví điện tử'}</p></div><div className="text-right"><p className="font-display text-base font-extrabold">{money(payment.amount, payment.currency)}</p><div className="mt-1"><StatusBadge status={payment.status} /></div></div>{payment.status === 'Succeeded' && <button className="secondary-btn ml-auto text-xs" disabled={!!busy} onClick={() => void refund(payment)}>Yêu cầu hoàn tiền</button>}</div>)}</div> : <EmptyState icon={<ReceiptText size={25} />} title="Chưa có giao dịch" description="Sau khi thanh toán, lịch sử sẽ hiển thị tại đây." />}</section></>}

        {page === 'settings' && <><PageHeading eyebrow="TÍCH HỢP" title="Cài đặt kết nối" description="Thông tin client dùng để gửi yêu cầu tới API Gateway." /><div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]"><section className="card p-6 md:p-8"><div className="flex items-center gap-3"><span className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-brand"><Settings2 size={20} /></span><div><h2 className="font-display font-extrabold">Cấu hình phiên làm việc</h2><p className="text-xs text-muted">Thay đổi được lưu trong trình duyệt này.</p></div></div><div className="mt-7 grid gap-5 sm:grid-cols-2"><label className="field-label">Rider ID<input className="text-input mt-2" value={riderId} onChange={event => setRiderId(event.target.value)} placeholder="rider-001" /></label><label className="field-label">Driver ID<input className="text-input mt-2" value={driverId} onChange={event => setDriverId(event.target.value)} placeholder="driver-001" /></label></div><label className="field-label mt-5 block">JWT access token<textarea className="text-input mt-2 min-h-28 resize-y" value={token} onChange={event => setToken(event.target.value)} placeholder="Dán JWT do hệ thống xác thực cấp" /></label><div className="mt-5 flex flex-wrap gap-3"><button className="primary-btn" onClick={() => { auth.setToken(token.trim()); setNotice({ kind: 'success', text: 'Đã lưu token trên trình duyệt.' }); }}><Check size={16} /> Lưu token</button><button className="secondary-btn" onClick={() => { auth.setToken(''); setToken(''); setNotice({ kind: 'success', text: 'Đã xóa token khỏi trình duyệt.' }); }}>Xóa token</button></div><p className="mt-4 text-xs leading-relaxed text-muted">Các ID này chỉ phục vụ quá trình tích hợp trước khi có đăng nhập. Backend vẫn phải kiểm tra quyền truy cập tài nguyên theo JWT.</p></section><div className="space-y-6"><section className="card p-6"><p className="section-eyebrow">API GATEWAY</p><h2 className="section-title mt-1">Điểm kết nối</h2><code className="mt-4 block break-all rounded-xl bg-slate-50 p-3 text-xs text-brand">{import.meta.env.VITE_API_BASE_URL || '/api'}</code><p className="mt-3 text-xs leading-relaxed text-muted">Vite chuyển tiếp /api đến Gateway khi phát triển. Đổi URL trong .env khi triển khai.</p><button className="secondary-btn mt-5" onClick={() => void refresh(role, currentId)}><RefreshCw size={15} /> Kiểm tra bằng cách tải dữ liệu</button></section><section className="card p-6"><div className="flex items-start gap-3"><ShieldCheck size={20} className="text-emerald-600" /><div><h3 className="text-sm font-bold">An toàn dữ liệu</h3><p className="mt-1 text-xs leading-relaxed text-muted">Không lưu số thẻ hoặc CVV. Chỉ sử dụng token từ nhà cung cấp thanh toán.</p></div></div></section></div></div></>}
      </div>
    </main>
  </div>;
}

function StatCard({ icon, label, value, note, tone }: { icon: React.ReactNode; label: string; value: string; note: string; tone: 'blue' | 'green' | 'orange' }) {
  const colors = { blue: 'bg-[#eef4ff] text-brand', green: 'bg-[#eaf8f2] text-emerald-600', orange: 'bg-[#fff4e9] text-orange-500' };
  return <div className="card flex items-center gap-4 p-5"><span className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-[15px] ${colors[tone]}`}>{icon}</span><div className="min-w-0"><p className="text-xs font-medium text-muted">{label}</p><p className="mt-0.5 truncate font-display text-[23px] font-extrabold tracking-tight">{value}</p><p className="mt-0.5 text-[11px] text-slate-400">{note}</p></div></div>;
}
function AddressInput({ label, icon, placeholder, value, onChange }: { label: string; icon: 'dot' | 'pin'; placeholder: string; value: string; onChange: (value: string) => void }) {
  return <label className="relative flex items-center gap-3 rounded-[14px] border border-slate-200 bg-white px-3 py-3.5 transition focus-within:border-brand focus-within:ring-2 focus-within:ring-blue-100"><span className="z-10 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-white text-brand">{icon === 'dot' ? <span className="h-3 w-3 rounded-full border-[3px] border-brand" /> : <MapPin size={17} fill="#2463eb" strokeWidth={1.5} />}</span><span className="min-w-0 flex-1"><span className="block text-[10px] font-bold uppercase tracking-wider text-muted">{label}</span><input className="w-full bg-transparent text-sm font-semibold outline-none placeholder:font-normal placeholder:text-slate-400" value={value} onChange={event => onChange(event.target.value)} placeholder={placeholder} /></span></label>;
}
function CoordinateInput({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return <label className="field-label">{label}<input type="number" step="any" className="text-input mt-1.5" value={value} onChange={event => onChange(event.target.value)} placeholder="0.000000" /></label>;
}
function MapSlot({ compact = false }: { compact?: boolean }) {
  return <div className={`card relative overflow-hidden bg-[#edf2f8] ${compact ? 'h-[265px]' : 'h-[365px] md:h-[415px]'}`} aria-label="Khu vực dành cho bản đồ, chưa tích hợp"><div className="absolute left-5 top-5 rounded-lg bg-white/90 px-3 py-2 text-[11px] font-bold text-muted shadow-sm">Khu vực bản đồ</div><div className="absolute bottom-5 left-5 rounded-lg bg-white/90 px-3 py-2 text-[11px] text-muted shadow-sm">Để trống để tích hợp map sau</div></div>;
}
function EmptyState({ icon, title, description }: { icon: React.ReactNode; title: string; description: string }) {
  return <div className="flex min-h-[170px] flex-col items-center justify-center px-6 py-8 text-center"><span className="mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-50 text-slate-400">{icon}</span><p className="text-sm font-bold">{title}</p><p className="mt-1 max-w-[260px] text-xs leading-relaxed text-muted">{description}</p></div>;
}
function StatusBadge({ status }: { status: string }) { return <span className={`inline-flex rounded-full px-2.5 py-1 text-[10px] font-bold ring-1 ${statusTone(status)}`}>{statusText[status as TripStatus] || ({ Pending: 'Đang xử lý', Succeeded: 'Thành công', Refunded: 'Đã hoàn tiền' } as Record<string, string>)[status] || status}</span>; }
function TripRoute({ trip }: { trip: Trip }) { return <div className="relative space-y-5"><div className="absolute left-[7px] top-[13px] h-[48px] border-l border-dashed border-slate-300" /><div className="flex items-start gap-3"><span className="z-10 mt-1.5 h-4 w-4 shrink-0 rounded-full border-[4px] border-brand bg-white" /><div><p className="text-[10px] font-bold uppercase tracking-wider text-muted">Điểm đón</p><p className="mt-0.5 text-sm font-semibold">{trip.pickup?.address || 'Chưa có địa chỉ'}</p></div></div><div className="flex items-start gap-3"><MapPin size={17} fill="#18243b" className="z-10 mt-1 shrink-0 text-ink" /><div><p className="text-[10px] font-bold uppercase tracking-wider text-muted">Điểm đến</p><p className="mt-0.5 text-sm font-semibold">{trip.dropoff?.address || 'Chưa có địa chỉ'}</p></div></div></div>; }
function TripRow({ trip, onClick, selected = false }: { trip: Trip; onClick: () => void; selected?: boolean }) { return <button onClick={onClick} className={`flex w-full flex-wrap items-center gap-4 px-6 py-5 text-left transition hover:bg-slate-50 ${selected ? 'bg-blue-50/60' : ''}`}><span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#eef4ff] text-brand"><CarFront size={21} /></span><div className="min-w-[160px] flex-1"><div className="flex items-center gap-2"><p className="text-sm font-bold">{shortId(trip.id)}</p><StatusBadge status={trip.status} /></div><p className="mt-1 truncate text-xs text-muted">{trip.pickup?.address || 'Điểm đón'} → {trip.dropoff?.address || 'Điểm đến'}</p><p className="mt-1 text-[11px] text-slate-400">{dateTime(trip.createdAt)}</p></div><div className="ml-auto flex items-center gap-3"><p className="font-display text-sm font-extrabold">{money(trip.finalFare ?? trip.quotedFare, trip.currency)}</p><ArrowRight size={16} className="text-slate-300" /></div></button>; }
function Detail({ label, value }: { label: string; value: string }) { return <div className="flex items-start justify-between gap-3"><span className="text-muted">{label}</span><span className="max-w-[210px] break-all text-right font-semibold">{value}</span></div>; }
function TripProgress({ status }: { status: TripStatus }) { const steps: TripStatus[] = ['Requested', 'DriverAccepted', 'EnRoute', 'PickedUp', 'DroppedOff', 'PaymentPending', 'Paid']; const current = steps.indexOf(status); if (['Cancelled', 'Failed'].includes(status)) return <p className="rounded-xl bg-rose-50 p-3 text-xs font-semibold text-rose-700">{statusText[status]}</p>; return <div className="space-y-0">{steps.map((step, index) => <div key={step} className="flex gap-3"><div className="flex flex-col items-center"><span className={`flex h-5 w-5 items-center justify-center rounded-full ${index <= current ? 'bg-brand text-white' : 'border-2 border-slate-200 bg-white'}`}>{index <= current && <Check size={12} />}</span>{index < steps.length - 1 && <span className={`h-7 w-px ${index < current ? 'bg-brand' : 'bg-slate-200'}`} />}</div><span className={`pt-0.5 text-xs ${index <= current ? 'font-semibold text-ink' : 'text-muted'}`}>{statusText[step]}</span></div>)}</div>; }
function PageHeading({ eyebrow, title, description, action }: { eyebrow: string; title: string; description: string; action?: React.ReactNode }) { return <div className="mb-7 flex flex-wrap items-end justify-between gap-4"><div><p className="mb-2 text-xs font-bold uppercase tracking-[.16em] text-brand">{eyebrow}</p><h1 className="font-display text-[30px] font-extrabold tracking-tight">{title}</h1><p className="mt-1.5 text-sm text-muted">{description}</p></div>{action}</div>; }
