import * as api from "../api/bottleTypeApi";

class BottleTypeService {

    async getAll() {

        return api.getBottleTypes({

            page: 0,

            size: 999

        });

    }

}

export default new BottleTypeService();