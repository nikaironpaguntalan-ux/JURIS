public class Complainant extends PersonInv {

    public Complainant(String name, int age, String gender, String address, String contactInfo) {
        super(name, age, gender, address, contactInfo);
    }

    @Override
    public String toString() {
        return "Name: "+name
             + "\nAge: "+age
             + "\nGender: "+gender
             + "\nAddress: "+address
             + "\nContact: "+contactInfo;
    }
}
