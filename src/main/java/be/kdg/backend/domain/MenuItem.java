package be.kdg.backend.domain;

import lombok.*;

@Getter
@AllArgsConstructor
@ToString
public class MenuItem {
    private long id;
    private String name;
    private String description;
    private double price; //of bigdecimal
    private MenuCategory category;
    private DishStatus status;


    //methods
    //publish()
    //markOutOfStock()
}
