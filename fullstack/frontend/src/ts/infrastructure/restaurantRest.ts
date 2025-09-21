import type {Restaurant} from "../domain/Restaurant";

const SERVER_URL = "http://localhost:8080/api/";
const DISHES_URL = SERVER_URL + "restaurants/";

export async function getRestaurants():Promise<Restaurant[]>{
    const response = await fetch(DISHES_URL);
    if (response.ok) {
        return(  response.json()) ;
    }
    // or throw an execption and handle the error in the presenter
    return Promise.resolve([]);
}