public class Staff extends User {

    public Staff(String id, String username, String password, String role, String isActive, String fullName) {
        super(id, username, password, role, isActive, fullName);
    }

    @Override
    public void RoleDashb() {
        System.out.println("[Staff] Case management access granted.");
    }
}
