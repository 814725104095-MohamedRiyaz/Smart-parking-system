package smartparking;

public class Vehicle {
    private final String number;
    private final String owner;
    private final String type;

    public Vehicle(String number, String owner, String type) {
        this.number = number;
        this.owner = owner;
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public String getOwner() {
        return owner;
    }

    public String getType() {
        return type;
    }
}
