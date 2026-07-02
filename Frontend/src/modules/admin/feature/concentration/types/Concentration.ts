export interface Concentration {

    id: number;

    name: string;

    status: number;

}

export interface ConcentrationPage {

    content: Concentration[];

    totalElements: number;

    totalPages: number;

    number: number;

    size: number;

}