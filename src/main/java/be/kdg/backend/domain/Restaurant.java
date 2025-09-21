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

    public void setId(Long id) {
        this.id=id;
    }

    public void setIsActive(boolean active) {
        isActive = active;
    }

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
