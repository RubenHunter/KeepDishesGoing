export interface Restaurant {
    id: number,
    name: string,
    address: string,
    email: string,
    //List<Openinghours> weeklyschedule,
    logoUrl: string,
    type: string,
    isManuallyClosed: boolean,
    manualOverrrideUntil: Date | null,
    isActive: boolean
}