import request from "@/common/services/request";
import type { BottleTypePage } from "../types/BottleType";

const API = "/api/admin/bottle-types";

export const getBottleTypes = (
    params?: any
): Promise<BottleTypePage> => {

    return request.get(API, {
        params
    });

};