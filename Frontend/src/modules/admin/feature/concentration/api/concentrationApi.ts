import request from "@/common/services/request";
import type { ConcentrationPage } from "../types/Concentration";

const API = "/api/admin/concentrations";

export const getConcentrations = (
    params?: any
): Promise<ConcentrationPage> => {

    return request.get(API, {
        params
    });

};

export const getConcentrationById = (
    id: number
) => {

    return request.get(
        `${API}/${id}`
    );

};

export const createConcentration = (
    data: any
) => {

    return request.post(
        API,
        data
    );

};

export const updateConcentration = (
    id: number,
    data: any
) => {

    return request.put(
        `${API}/${id}`,
        data
    );

};

export const deleteConcentration = (
    id: number
) => {

    return request.delete(
        `${API}/${id}`
    );

};