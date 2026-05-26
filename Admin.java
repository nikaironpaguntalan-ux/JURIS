public class Admin extends User {

    public Admin(String id, String username, String password, String role, String isActive, String fullName) {
        super(id, username, password, role, isActive, fullName);
    }

    @Override
    public void RoleDashb() {
        System.out.println("[Admin] Full system access granted.");
    }
}
