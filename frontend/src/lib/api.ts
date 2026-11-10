import type { AccountSummary, DriverWallet, LocationInput, MatchResult, Payment, Quote, Role, Trip } from '../types';

const baseUrl = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');
const TOKEN_KEY = 'swiftride.accessToken';

export const auth = {
  getToken: () => sessionStorage.getItem(TOKEN_KEY) || '',
  setToken: (value: string) => value ? sessionStorage.setItem(TOKEN_KEY, value) : sessionStorage.removeItem(TOKEN_KEY),
};

type RequestOptions = { method?: string; body?: unknown; idempotencyKey?: string; signal?: AbortSignal };

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 12000);
  const token = auth.getToken();
  const headers: Record<string, string> = { 'Accept': 'application/json', 'X-Correlation-Id': crypto.randomUUID() };
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;
  if (options.idempotencyKey) headers['Idempotency-Key'] = options.idempotencyKey;
  try {
    const response = await fetch(`${baseUrl}${path}`, {
      method: options.method || 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal || controller.signal,
    });
    const contentType = response.headers.get('content-type') || '';
    const data = response.status === 204 ? null : contentType.includes('application/json') ? await response.json() : await response.text();
    if (!response.ok) {
      const message = typeof data === 'object' && data ? (data.message || data.detail || data.title) : data;
      const error = new Error(message || `Yêu cầu thất bại (${response.status}).`) as Error & { status: number };
      error.status = response.status;
      throw error;
    }
    return data as T;
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw new Error('Kết nối quá thời gian. Vui lòng thử lại.');
    if (error instanceof TypeError) throw new Error('Không kết nối được API Gateway. Kiểm tra backend và cấu hình URL.');
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

const post = <T>(path: string, body?: unknown, idempotencyKey?: string) => request<T>(path, { method: 'POST', body, idempotencyKey });

export const api = {
  accounts: {
    list: (role: Role) => request<AccountSummary[]>(`/users?role=${encodeURIComponent(role)}`),
  },
  trips: {
    list: (role: 'rider' | 'driver', userId: string) => request<Trip[]>(`/trips?${role}Id=${encodeURIComponent(userId)}`),
    get: (id: string) => request<Trip>(`/trips/${encodeURIComponent(id)}`),
    create: (body: { riderId: string; pickup: LocationInput; dropoff: LocationInput }) => post<Trip>('/trips', body),
    confirmQuote: (id: string, quote: Quote) => post<Trip>(`/trips/${encodeURIComponent(id)}/confirm-quote`, { quote }),
    cancel: (id: string) => post<Trip>(`/trips/${encodeURIComponent(id)}/cancel`),
    driverAccept: (id: string, driverId: string) => post<Trip>(`/trips/${encodeURIComponent(id)}/driver-accept`, { driverId }),
    driverReject: (id: string, driverId: string) => post<Trip>(`/trips/${encodeURIComponent(id)}/driver-reject`, { driverId }),
    enRoute: (id: string) => post<Trip>(`/trips/${encodeURIComponent(id)}/en-route`),
    pickup: (id: string) => post<Trip>(`/trips/${encodeURIComponent(id)}/pickup`),
    dropoff: (id: string) => post<Trip>(`/trips/${encodeURIComponent(id)}/dropoff`),
  },
  matching: {
    quote: (tripId: string) => post<Quote>('/quote', { tripId }),
    match: (tripId: string, excludedDriverIds: string[] = []) => post<MatchResult>('/match', { tripId, excludedDriverIds }),
    offers: (driverId: string) => request<Trip[]>(`/match/offers?driverId=${encodeURIComponent(driverId)}`),
  },
  payments: {
    list: (userId: string) => request<Payment[]>(`/payments?riderId=${encodeURIComponent(userId)}`),
    get: (id: string) => request<Payment>(`/payments/${encodeURIComponent(id)}`),
    create: (body: { tripId: string; riderId: string; amount: number; currency: string; method: 'WALLET' | 'CARD_TOKEN'; paymentToken?: string }, idempotencyKey: string) => post<Payment>('/payments', body, idempotencyKey),
    refund: (id: string, reason: string, idempotencyKey: string) => post<Payment>(`/payments/${encodeURIComponent(id)}/refund`, { reason }, idempotencyKey),
  },
  wallet: {
    get: (driverId: string) => request<DriverWallet>(`/drivers/${encodeURIComponent(driverId)}/wallet`),
  },
};

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Đã có lỗi xảy ra. Vui lòng thử lại.';
}
