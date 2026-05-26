public abstract class PersonInv {

    protected String name;
    protected int age;
    protected String gender;
    protected String address;
    protected String contactInfo;

    public PersonInv(String name, int age, String gender, String address, String contactInfo) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.address = address;
        this.contactInfo = contactInfo;
    }

    public String getName(){ 
        return name; 
    }

    public int getAge(){ 
        return age; 
    }

    public String getGender(){ 
        return gender; 
    }

    public String getAddress(){ 
        return address; 
    }

    public String getContactInfo() {
        return contactInfo; 
    }

    public void setName(String name){ 
        this.name = name; 
    }

    public void setAge(int age){ 
        this.age = age;
    }

    public void setGender(String gender){ 
        this.gender = gender;
    }

    public void setAddress(String address){
        this.address = address; 
    }

    public void setContactInfo(String info) { 
        this.contactInfo = info; 
    }

    @Override
    public abstract String toString();
}
