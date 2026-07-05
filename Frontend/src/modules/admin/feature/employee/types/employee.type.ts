export type EmployeeRole = "MANAGER" | "CASHIER";

export type EmployeeDisplayRole =
  | "OWNER"
  | "MANAGER"
  | "CASHIER"
  | "USER"
  | string;

export type EmployeeStatus = 0 | 1;

export interface EmployeeResponse {
  userId: number;
  employeeCode: string;
  roleName: EmployeeDisplayRole;
  name: string;
  email: string;
  phone: string;
  address: string | null;
  avatarUrl: string | null;
  status: EmployeeStatus;
  isDeleted: boolean;
  createdAt: string;
  citizenId: string;
  jobTitle: string;
  salary: number;
  hireDate: string;
}

export interface EmployeeCreateRequest {
  role: EmployeeRole;
  name: string;
  email: string;
  password: string;
  phone: string;
  address?: string | null;
  avatarUrl?: string | null;
  citizenId: string;
  jobTitle: string;
  salary: number;
  hireDate: string;
}

export interface EmployeeUpdateRequest {
  role: EmployeeRole;
  name: string;
  email: string;
  password?: string;
  phone: string;
  address?: string | null;
  avatarUrl?: string | null;
  citizenId: string;
  jobTitle: string;
  salary: number;
  hireDate: string;
}

export interface EmployeeSearchParams {
  keyword?: string;
  status?: EmployeeStatus | null;
  page?: number;
  size?: number;
}

export interface UploadAvatarResponse {
  url: string;
  publicId: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
}