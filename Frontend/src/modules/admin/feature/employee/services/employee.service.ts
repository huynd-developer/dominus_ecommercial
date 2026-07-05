import api from "@/common/api";
import type {
  EmployeeCreateRequest,
  EmployeeResponse,
  EmployeeSearchParams,
  EmployeeUpdateRequest,
  PageResponse,
  UploadAvatarResponse,
} from "../types/employee.type";

const BASE_URL = "/admin/employees";

export const employeeService = {
  getAll(params: EmployeeSearchParams) {
    return api.get<PageResponse<EmployeeResponse>>(BASE_URL, {
      params: {
        keyword: params.keyword || undefined,
        status: params.status ?? undefined,
        page: params.page ?? 0,
        size: params.size ?? 10,
      },
    });
  },

  getById(userId: number) {
    return api.get<EmployeeResponse>(`${BASE_URL}/${userId}`);
  },

  create(payload: EmployeeCreateRequest) {
    return api.post<EmployeeResponse>(BASE_URL, payload);
  },

  update(userId: number, payload: EmployeeUpdateRequest) {
    return api.put<EmployeeResponse>(`${BASE_URL}/${userId}`, payload);
  },

  lock(userId: number) {
    return api.patch<EmployeeResponse>(`${BASE_URL}/${userId}/lock`);
  },

  unlock(userId: number) {
    return api.patch<EmployeeResponse>(`${BASE_URL}/${userId}/unlock`);
  },

  deleteSoft(userId: number) {
    return api.delete<void>(`${BASE_URL}/${userId}`);
  },

  uploadAvatar(file: File) {
    const formData = new FormData();
    formData.append("file", file);

    return api.post<UploadAvatarResponse>(
      `${BASE_URL}/upload-avatar`,
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      }
    );
  },
};