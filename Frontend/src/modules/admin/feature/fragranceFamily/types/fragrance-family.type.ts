export interface FragranceFamily {
  id: number;
  name: string;
  status: number;
  isDeleted?: boolean;
}

export interface FragranceFamilyRequest {
  name: string;
  status?: number;
}