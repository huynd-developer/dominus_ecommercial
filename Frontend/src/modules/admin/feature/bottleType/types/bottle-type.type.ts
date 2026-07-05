// src/modules/admin/feature/bottleType/types/bottle-type.type.ts
export interface BottleTypeRequest {
  name: string;
  status: number;
}

export interface BottleType {
  id: number;
  name: string;
  status: number;
}