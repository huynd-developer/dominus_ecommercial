import * as api from "../api/concentrationApi";

import type {
    ConcentrationPage
} from "../types/Concentration";

class ConcentrationService {

    async getAll(
        params?: any
    ): Promise<ConcentrationPage> {

        return await api.getConcentrations(
            params
        );

    }

    async getById(
        id: number
    ) {

        return await api.getConcentrationById(
            id
        );

    }

    async create(
        data: any
    ) {

        return await api.createConcentration(
            data
        );

    }

    async update(
        id: number,
        data: any
    ) {

        return await api.updateConcentration(
            id,
            data
        );

    }

    async delete(
        id: number
    ) {

        return await api.deleteConcentration(
            id
        );

    }

}

export default new ConcentrationService();