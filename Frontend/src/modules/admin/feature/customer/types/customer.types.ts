export interface CustomerResponse {
  userId: number;

  name: string;
  email: string;
  phone: string | null;
  address: string | null;
  avatarUrl: string | null;

  customerRank: string | null;
  loyaltyPoints: number | null;

  dateOfBirth: string | null;
  gender: number | null;
  genderText: string | null;

  status: number;
  statusText: string | null;

  createdAt: string | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface CustomerSearchParams {
  keyword?: string;
  status?: number | null;
  page?: number;
  size?: number;
}