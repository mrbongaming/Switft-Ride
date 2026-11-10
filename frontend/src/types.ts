export type Role = 'rider' | 'driver';
export type Page = 'overview' | 'trips' | 'payments' | 'wallet';

export interface AccountSummary {
  id: string;
  displayName: string;
  role: Role;
  phone?: string;
  vehicle?: string;
  licensePlate?: string;
}

export interface LocationInput {
  address: string;
  latitude?: number;
  longitude?: number;
}

export type TripStatus = 'Requested' | 'Quoted' | 'DriverOffered' | 'DriverAccepted' | 'EnRoute' | 'PickedUp' | 'DroppedOff' | 'PaymentPending' | 'Paid' | 'Cancelled' | 'Failed';

export interface Trip {
  id: string;
  riderId: string;
  driverId?: string;
  pickup: LocationInput;
  dropoff: LocationInput;
  status: TripStatus;
  quotedFare?: number;
  finalFare?: number;
  currency?: string;
  createdAt?: string;
  updatedAt?: string;
  paymentId?: string;
}

export interface Quote {
  tripId: string;
  amount: number;
  currency: string;
  distanceKm?: number;
  durationMinutes?: number;
  surgeMultiplier?: number;
  expiresAt?: string;
}

export interface MatchResult {
  tripId: string;
  driverId?: string;
  driverName?: string;
  vehicle?: string;
  etaMinutes?: number;
  attempt?: number;
  maxAttempts?: number;
}

export interface Payment {
  id: string;
  tripId: string;
  amount: number;
  currency: string;
  status: 'Pending' | 'Succeeded' | 'Failed' | 'Refunded';
  createdAt?: string;
  method?: 'WALLET' | 'CARD_TOKEN';
}

export interface WalletEntry {
  id: string;
  tripId?: string;
  amount: number;
  type: 'EARNING' | 'WITHDRAWAL' | 'ADJUSTMENT';
  status: 'Pending' | 'Succeeded' | 'Failed';
  createdAt?: string;
}

export interface DriverWallet {
  driverId: string;
  balance: number;
  pendingBalance: number;
  currency: string;
  entries: WalletEntry[];
}

export interface ApiError extends Error { status?: number }
