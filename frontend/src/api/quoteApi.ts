import api from './config';

export interface QuoteRequest {
  pol: string;
  pod: string;
  equipment: string;
  commodity?: string;
  readyDate?: string;
  weightKg?: number;
}

export interface QuoteCharge {
  code: string;
  description: string;
  amount: number;
  currency: string;
  basis: string;
}

export interface QuoteResponse {
  quoteId: number | null;
  found: boolean;
  pol: string;
  pod: string;
  equipment: string;
  commodity: string;
  readyDate: string;
  carrierScac: string;
  carrierName: string;
  oceanFreight: number;
  markupAmount: number;
  totalAmount: number;
  currency: string;
  validUntil: string;
  transitDays: number | null;
  candidateCount: number;
  charges: QuoteCharge[];
  createdAt: string;
  message: string | null;
}

export interface PortItem {
  unloc: string;
  name: string;
  country: string;
}

export interface CarrierItem {
  id: number;
  scac: string;
  name: string;
}

export interface QuoteOption {
  tag: string;           // "CHEAPEST" | "FASTEST" | "BEST VALUE" | "RECOMMENDED" | ""
  carrierScac: string;
  carrierName: string;
  oceanFreight: number;
  markupAmount: number;
  totalAmount: number;
  currency: string;
  transitDays: number | null;
  rateLineId: number;
  tags: string[];
}

export interface QuoteCompareResponse {
  pol: string;
  pod: string;
  equipment: string;
  commodity: string;
  readyDate: string;
  totalCandidates: number;
  options: QuoteOption[];
}

export const quoteApi = {
  create:      (req: QuoteRequest) => api.post<QuoteResponse>('/quotes', req),
  compare:     (req: QuoteRequest) => api.post<QuoteCompareResponse>('/quotes/compare', req),
  get:         (id: number)        => api.get<QuoteResponse>(`/quotes/${id}`),
  list:        (page = 0, size = 20) => api.get('/quotes', { params: { page, size } }),
  searchPorts: (q: string)         => api.get<PortItem[]>('/ports', { params: { q } }),
  carriers:    ()                  => api.get<CarrierItem[]>('/carriers'),
};


