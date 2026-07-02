export interface Capacity {

    id: number;

    value: number;

}

export interface CapacityPage {

    content: Capacity[];

    totalElements: number;

    totalPages: number;

    number: number;

    size: number;

}