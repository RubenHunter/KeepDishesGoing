package be.kdg.backend.domain;

import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@ToString
public class Restaurant {
    private Long id;
    private String name;
    private Address address;
    private List<MenuItem> menuItems;
    private boolean isActive;

    /*
    private String contactEmail;
    private String type;
    private boolean isManuallyClosed;
    */


    //setters



    //methods (ik denk dat dit in de service layer moet gebeuren)

    //addMenuItem()
    //removeMenuItem()
    //updateMenuItem()
    //deactivateRestaurant()
}
