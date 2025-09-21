package be.kdg.backend.api.dto;

import be.kdg.backend.domain.Address;
import be.kdg.backend.domain.MenuItem;
import be.kdg.backend.domain.Restaurant;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record RestaurantDto(Long id, @NotBlank(message="A Restaurant needs a name") String name, @NotBlank(message="A Restaurant needs an Address") Address addres, List<MenuItem> menuItems, boolean isActive) {

    public static RestaurantDto from (Restaurant restaurant){
        return new RestaurantDto(restaurant.getId(), restaurant.getName(), restaurant.getAddress(), restaurant.getMenuItems(), restaurant.isActive());
    }

    public Restaurant to (){
        return new Restaurant(id, name, addres, menuItems, isActive);
    }

}
