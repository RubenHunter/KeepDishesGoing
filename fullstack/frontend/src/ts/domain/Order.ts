export interface Order {
    id: number,
    restaurantId: number,
    //Orderstatus: status,
    placedAt: string,//of Date (maar dit heeft timezones)
    acceptedAt: string,
    //customer: CustomerInfo, //CustomerInfo is Valueobject
    //totalPrice: Money,  //money is Valueobject

}