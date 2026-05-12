import api from './config';

export interface DashboardStats {
  carriers: number;
  activeRatesheets: number;
  totalRatesheets: number;
  totalRateLines: number;
  ports: number;
}

export interface RatesheetSummary {
  id: number;
  carrierScac: string;
  carrierName: string;
  source: string;
  sourceFile: string;
  type: string;
  currency: string;
  validFrom: string;
  validTo: string;
  status: string;
  rateLineCount: number;
  uploadedAt: string;
}

export interface RateLine {
  id: number;
  pol: string;
  polName: string;
  pod: string;
  podName: string;
  via: string;
  equipment: string;
  commodity: string;
  baseAmount: number;
  currency: string;
  transitDays: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface ImportResult {
  ratesheetId: number;
  parser: string;
  linesImported: number;
  message: string;
}

export const ratesheetApi = {
  stats: () => api.get<DashboardStats>('/dashboard/stats'),
  list: () => api.get<RatesheetSummary[]>('/ratesheets'),
  get: (id: number) => api.get<RatesheetSummary>(`/ratesheets/${id}`),
  lines: (id: number, page = 0, size = 50) =>
    api.get<Page<RateLine>>(`/ratesheets/${id}/lines`, { params: { page, size } }),
  search: (pol?: string, pod?: string, equipment?: string, page = 0, size = 50) =>
    api.get<Page<RateLine>>('/rates/search', { params: { pol, pod, equipment, page, size } }),
  import: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.post<ImportResult>('/ratesheets/import', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

