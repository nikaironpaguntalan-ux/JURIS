public abstract class User {
    protected String id;
    protected String username;
    protected String password;
    protected String role;
    protected String isActive;
    protected String fullName;

    public User(String id, String username, String password, String role, String isActive, String fullName) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.isActive = isActive;
        this.fullName = fullName;
    }

    public String getId() { 
        return id; 
    }

    public String getUsername(){
        return username; 
    }

    public String getPassword(){ 
        return password; 
    }

    public String getRole(){ 
        return role; 
    }

    public String getIsActive(){ 
        return isActive; 
    }

    public String getFullName(){ 
        return fullName; 
    }

    public void setId(String id){ 
        this.id = id; 
    }

    public void setUsername(String username){ 
        this.username = username; 
    }

    public void setPassword(String password){ 
        this.password = password; 
    }

    public void setRole(String role){ 
        this.role = role; 
    }

    public void setIsActive(String isActive){ 
        this.isActive = isActive; 
    }

    public void setFullName(String fullName){ 
        this.fullName = fullName; 
    }

    public boolean isAdmin(){ 
        return "Admin".equalsIgnoreCase(role); 
    }

    public boolean isStaff(){       
        return "Staff".equalsIgnoreCase(role); 
    }

    public boolean isProsecutor() { 
        return "Prosecutor".equalsIgnoreCase(role); 
    }

    public boolean canManageCases(){ 
        return isAdmin() || isStaff(); 
    }

    public boolean canManageUsers() {
        return isAdmin(); 
    }

    public abstract void RoleDashb();

    @Override
    public String toString() {
        return "ID: " + id + " | " + username + " | " + fullName + " | " + role + " | " + isActive;
    }
}
