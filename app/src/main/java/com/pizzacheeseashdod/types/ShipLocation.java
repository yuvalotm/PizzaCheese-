package com.pizzacheeseashdod.types;

public class ShipLocation {
    String name;
    double price;

    public ShipLocation(){
        this.name = "";
        this.price = 0.0;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public ShipLocation(String name, double price){
        this.name = name;
        this.price = price;
    }

    public double getPrice() {
        return price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }


}
