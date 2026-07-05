export interface BottleType {

    id: number;

    name: string;

}

export interface BottleTypePage {

    content: BottleType[];

    totalElements: number;

    totalPages: number;

    number: number;

    size: number;

}