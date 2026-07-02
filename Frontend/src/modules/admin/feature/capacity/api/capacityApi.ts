import request from "@/common/services/request";
import type { CapacityPage } from "../types/Capacity";

const API = "/api/admin/capacities";

export const getCapacities = (
    params?: any
): Promise<CapacityPage> => {

    return request.get(API, {
        params
    });

};