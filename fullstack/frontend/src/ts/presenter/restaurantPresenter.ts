import {getRestaurants} from "../infrastructure/restaurantRest";
import type {Restaurant} from "../domain/Restaurant";

const dishesTable=document.querySelector<HTMLElement>('#restaurants')!;


function showDishes(restaurants:Restaurant[]) {
    dishesTable.innerHTML = restaurants.map(restaurant =>  `
           <tr>
          <td>${restaurant.id}</td>
          <td>${restaurant.name}</td>
          <td>${restaurant.address}</td>
          <td>${restaurant.email}</td>
          <td>${restaurant.logoUrl}</td>
          <td>${restaurant.type}</td>
          <td>${restaurant.isManuallyClosed}</td>
          <!--<td>${restaurant.manualOverrrideUntil ? restaurant.manualOverrrideUntil : ""}</td>-->
          <td>${restaurant.isActive}</td>
      </tr>
    `   ).join("");

}

export async function getAndShowRestaurants() {
    showDishes(await getRestaurants())
}
