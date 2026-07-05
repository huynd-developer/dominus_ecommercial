import * as api from "../api/capacityApi";

class CapacityService {

    async getAll() {

        return api.getCapacities({

            page: 0,

            size: 999

        });

    }

}

export default new CapacityService();