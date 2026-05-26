public class Prosecutor extends User {

    public Prosecutor(String id, String username, String password, String role, String isActive, String fullName) {
        super(id, username, password, role, isActive, fullName);
    }

    @Override
    public void RoleDashb() {
        System.out.println("[Prosecutor] Access limited to assigned cases only.");
    }
}
