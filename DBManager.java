
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DBManager {

    private Connection conn() {
        return DBConfig.getConnection();
    }

    public User validateUser(String username, String password) {
        String sql = "SELECT id, username, password, role, isActive, fullName FROM users WHERE username = ? AND password = ? AND isActive = 'Active'";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    String id = rs.getString("id");
                    String uname = rs.getString("username");
                    String pass = rs.getString("password");
                    String role = rs.getString("role");
                    String active = rs.getString("isActive");
                    String fullName = rs.getString("fullName");

                    if (role.toLowerCase().equals("admin")) {
                        return new Admin(id, uname, pass, role, active, fullName);
                    } else if (role.toLowerCase().equals("prosecutor")) {
                        return new Prosecutor(id, uname, pass, role, active, fullName);
                    } else {
                        return new Staff(id, uname, pass, role, active, fullName);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] validateUser: " + e.getMessage());
        }
        return null;
    }

    public int[] getCaseStatsByProsecutor(String prosecutor) {
        int[] stats = new int[4];
        String sql = "SELECT caseStatus, COUNT(*) AS cnt FROM cases WHERE prosecutor = ? GROUP BY caseStatus";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, prosecutor);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int cnt = rs.getInt("cnt");
                stats[0] += cnt;

                switch (rs.getString("caseStatus")) {
                    case "Active" ->
                        stats[1] += cnt;
                    case "Resolved" ->
                        stats[2] += cnt;
                    case "Dismissed" ->
                        stats[3] += cnt;
                }
            }

        } catch (SQLException e) {
            System.err.println("[DB] getCaseStatsByProsecutor: " + e.getMessage());
        }

        return stats;
    }

    public List<String[]> getCasesByTypeByProsecutor(String prosecutor) {

        List<String[]> list = new ArrayList<>();

        String sql = "SELECT caseType, COUNT(*) AS cnt FROM cases WHERE prosecutor = ? GROUP BY caseType ORDER BY cnt DESC";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, prosecutor);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("caseType"),
                    String.valueOf(rs.getInt("cnt"))
                });
            }

        } catch (SQLException e) {
            System.err.println("[DB] getCasesByTypeByProsecutor: " + e.getMessage());
        }

        return list;
    }

    public List<String[]> getMyWorkload(String prosecutor) {

        List<String[]> list = new ArrayList<>();

        String sql = "SELECT COUNT(*) AS cnt FROM cases WHERE prosecutor = ? AND caseStatus = 'Active'";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, prosecutor);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                list.add(new String[]{
                    "My Active Cases",
                    String.valueOf(rs.getInt("cnt"))
                });
            }

        } catch (SQLException e) {
            System.err.println("[DB] getMyWorkload: " + e.getMessage());
        }

        return list;
    }

    public boolean AddUser(String username, String password, String role, String fullName) {
        String sql = "INSERT INTO users (username, password, role, fullName, isActive) VALUES (?, ?, ?, ?, 'Active')";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, fullName != null ? fullName : username);

            if (ps.executeUpdate() > 0) {
                System.out.println("User '" + username + "' added.");
                return true;
            }

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.out.println("Username already exists.");
            } else {
                System.err.println("[DB] AddUser: " + e.getMessage());
            }
        }
        return false;
    }

    public void ViewUserList() {
        String sql = "SELECT id, username, fullName, role, isActive FROM users ORDER BY role, username";
        try (PreparedStatement ps = conn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            System.out.println("\n-------------------------------------------------------------------------");
            System.out.printf("  %-6s %-20s %-25s %-14s %-10s%n", "ID", "Username", "Full Name", "Role", "Status");
            System.out.println("-------------------------------------------------------------------------");

            int ctr = 0;
            while (rs.next()) {
                System.out.printf("  %-6s %-20s %-25s %-14s %-10s%n", rs.getString("id"), rs.getString("username"), rs.getString("fullName"), rs.getString("role"), rs.getString("isActive"));
                ctr++;
            }

            if (ctr == 0) {
                System.out.println("  No users found.");
            }

            System.out.println("-------------------------------------------------------------------------");
        } catch (SQLException e) {
            System.err.println("[DB] ViewUserList: " + e.getMessage());
        }
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, password, role, isActive, fullName FROM users ORDER BY role";
        try (PreparedStatement ps = conn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String un = rs.getString("username");
                String pw = rs.getString("password");
                String role = rs.getString("role");
                String ac = rs.getString("isActive");
                String fn = rs.getString("fullName");

                if (role.equalsIgnoreCase("Admin")) {
                    list.add(new Admin(id, un, pw, role, ac, fn));
                } else if (role.equalsIgnoreCase("Prosecutor")) {
                    list.add(new Prosecutor(id, un, pw, role, ac, fn));
                } else {
                    list.add(new Staff(id, un, pw, role, ac, fn));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] getAllUsers: " + e.getMessage());
        }
        return list;
    }

    public boolean DeactivateUser(String username) {
        return setActiveStatus(username, "Inactive");
    }

    public boolean ReactivateUser(String username) {
        return setActiveStatus(username, "Active");
    }

    private boolean setActiveStatus(String username, String status) {
        String sql = "UPDATE users SET isActive = ? WHERE username = ?";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, username);

            boolean ok = ps.executeUpdate() > 0;

            if (ok) {
                System.out.println("\nUser '" + username + "' is now " + status + ".");
            } else {
                System.out.println("\nUser '" + username + "' not found.");
            }

            return ok;

        } catch (SQLException e) {
            System.err.println("[DB] setActiveStatus: " + e.getMessage());
            return false;
        }
    }

    public boolean UpdatePassword(String username, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, username);

            boolean ok = ps.executeUpdate() > 0;

            if (ok) {
                System.out.println("\nPassword for user '" + username + "' updated.");
            } else {
                System.out.println("\nUser '" + username + "' not found.");
            }
            return ok;

        } catch (SQLException e) {
            System.err.println("[DB] UpdatePassword: " + e.getMessage());
            return false;
        }
    }

    public List<String> getProsecutorList() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT fullName FROM users WHERE role = 'Prosecutor' AND isActive = 'Active' ORDER BY fullName";
        try (PreparedStatement ps = conn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(rs.getString("fullName"));
            }

        } catch (SQLException e) {
            System.err.println("[DB] getProsecutorList: " + e.getMessage());
        }
        return list;
    }

    public int getProsecutorCount() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'Prosecutor' AND isActive = 'Active'";
        try (PreparedStatement ps = conn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("[DB] getProsecutorCount: " + e.getMessage());
        }
        return 0;
    }

    public void writeAuditLog(String username, String action, String details) {
        String sql = "INSERT INTO audit_logs (username, action, details, log_time) VALUES (?, ?, ?, NOW())";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] writeAuditLog: " + e.getMessage());
        }
    }

    public void viewAuditLog() {
        String sql = "SELECT log_time, username, action, details FROM audit_logs ORDER BY log_time DESC LIMIT 50";
        try (PreparedStatement ps = conn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            System.out.println("\n-----------------------------------------------------------------------------------");
            System.out.printf("  %-20s %-20s %-20s %s%n", "Time", "User", "Action", "Details");
            System.out.println("-----------------------------------------------------------------------------------");
            int ctr = 0;

            while (rs.next()) {
                System.out.printf("  %-20s %-20s %-20s %s%n",
                        rs.getString("log_time"),
                        rs.getString("username"),
                        rs.getString("action"),
                        rs.getString("details"));
                ctr++;
            }

            if (ctr == 0) {
                System.out.println("  No audit entries found.");
            }

        } catch (SQLException e) {
            System.err.println("[DB] viewAuditLog: " + e.getMessage());
        }
    }

    public List<String[]> getAuditLogs() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT log_time, username, action, details FROM audit_logs ORDER BY log_time DESC LIMIT 200";

        try (PreparedStatement ps = conn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("log_time"),
                    rs.getString("username"),
                    rs.getString("action"),
                    rs.getString("details")
                });
            }
        } catch (SQLException e) {
            System.err.println("[DB] getAuditLogs: " + e.getMessage());
        }
        return list;
    }

    public String getNextCaseID() {
        int year = LocalDate.now().getYear();
        String sql = "SELECT MAX(id) FROM cases";

        try (PreparedStatement ps = conn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int next = rs.getInt(1) + 1;
                return String.format("CR-%d-%05d", year, next);
            }
        } catch (SQLException e) {
            System.err.println("[DB] getNextCaseID: " + e.getMessage());
        }
        return String.format("CR-%d-%05d", year, 1);
    }

    public boolean addCase(String caseID, String caseType, String caseNature, String caseStatus, String accused, String complainant, String prosecutor, String judge, String filedDate, String hearingDate, String witness, String evidence, String branch, String verdict, String caseDesc) {
        String sql = "INSERT INTO cases (caseID, caseType, caseNature, caseStatus, accused, complainant, prosecutor, judge, filedDate, hearingDate, witness, evidence, branch, verdict, caseDesc) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, caseID);
            ps.setString(2, caseType);
            ps.setString(3, caseNature);
            ps.setString(4, caseStatus);
            ps.setString(5, accused);
            ps.setString(6, complainant);
            ps.setString(7, prosecutor);
            ps.setString(8, judge);
            ps.setString(9, filedDate);
            ps.setString(10, hearingDate);
            ps.setString(11, witness);
            ps.setString(12, evidence);
            ps.setString(13, branch);
            ps.setString(14, verdict);
            ps.setString(15, caseDesc);

            boolean ok = ps.executeUpdate() > 0;

            if (ok) {
                System.out.println("\nCase " + caseID + " added.");
            } else {
                System.out.println("\nFailed to add case.");
            }

            return ok;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.out.println("Case ID already exists.");
            } else {
                System.err.println("[DB] addCase: " + e.getMessage());
            }
            return false;
        }
    }

    public boolean updt_all(String oldCaseID, String caseType, String caseNature, String caseStatus, String accused, String complainant, String prosecutor, String judge, String filedDate, String hearingDate, String witness, String evidence, String branch, String verdict, String caseDesc, String newCaseID) {
        String sql = "UPDATE cases SET caseType=?, caseNature=?, caseStatus=?, accused=?, complainant=?, prosecutor=?, judge=?, filedDate=?, hearingDate=?, witness=?, evidence=?, branch=?, verdict=?, caseDesc=?, caseID=? WHERE caseID=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, caseType);
            ps.setString(2, caseNature);
            ps.setString(3, caseStatus);
            ps.setString(4, accused);
            ps.setString(5, complainant);
            ps.setString(6, prosecutor);
            ps.setString(7, judge);
            ps.setString(8, filedDate);
            ps.setString(9, hearingDate);
            ps.setString(10, witness);
            ps.setString(11, evidence);
            ps.setString(12, branch);
            ps.setString(13, verdict);
            ps.setString(14, caseDesc);
            ps.setString(15, (newCaseID != null && !newCaseID.trim().isEmpty()) ? newCaseID.trim() : oldCaseID);
            ps.setString(16, oldCaseID);

            boolean ok = ps.executeUpdate() > 0;

            if (ok) {
                System.out.println("\nCase updated.");
            } else {
                System.out.println("\nCase not found.");
            }
            return ok;

        } catch (SQLException e) {
            System.err.println("[DB] updt_all: " + e.getMessage());
            return false;
        }
    }

    public CaseRec getCaseByCaseID(String caseID) {
        List<CaseRec> list = queryCases("SELECT * FROM cases WHERE caseID = ?", caseID);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<CaseRec> getAllCases() {
        return queryCases("SELECT * FROM cases ORDER BY filedDate DESC", null);
    }

    public List<CaseRec> getActiveCases() {
        return queryCases("SELECT * FROM cases WHERE caseStatus = 'Active' ORDER BY filedDate DESC", null);
    }

    public List<CaseRec> getCasesByProsecutor(String prosecutor) {
        return queryCases("SELECT * FROM cases WHERE prosecutor = ? ORDER BY filedDate DESC", prosecutor);
    }

    public List<CaseRec> searchCases(String caseID, String accused, String caseType, String complainant, String judge, String prosecutor, String status, String dateFrom, String prosecutorFilter) {
        StringBuilder sql = new StringBuilder("SELECT * FROM cases WHERE 1=1");
        List<String> params = new ArrayList<>();

        if (notBlank(caseID)) {
            sql.append(" AND caseID LIKE ?");
            params.add("%" + caseID + "%");
        }

        if (notBlank(accused)) {
            sql.append(" AND accused LIKE ?");
            params.add("%" + accused + "%");
        }

        if (notBlank(caseType)) {
            sql.append(" AND caseType = ?");
            params.add(caseType);
        }

        if (notBlank(complainant)) {
            sql.append(" AND complainant LIKE ?");
            params.add("%" + complainant + "%");
        }

        if (notBlank(judge)) {
            sql.append(" AND judge LIKE ?");
            params.add("%" + judge + "%");
        }

        if (notBlank(prosecutor)) {
            sql.append(" AND prosecutor LIKE ?");
            params.add("%" + prosecutor + "%");
        }

        if (notBlank(status)) {
            sql.append(" AND caseStatus = ?");
            params.add(status);
        }

        if (notBlank(dateFrom)) {
            sql.append(" AND filedDate >= ?");
            params.add(dateFrom);
        }

        if (notBlank(prosecutorFilter)) {
            sql.append(" AND prosecutor = ?");
            params.add(prosecutorFilter);
        }

        sql.append(" ORDER BY filedDate DESC");

        List<CaseRec> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapCase(rs));
            }

        } catch (SQLException e) {
            System.err.println("[DB] searchCases: " + e.getMessage());
        }
        return list;
    }

    public List<CaseRec> searchCasesAdvanced(
            String caseId,
            String accused,
            String prosecutor,
            String type,
            String status,
            String dateFilter,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        List<CaseRec> results = new ArrayList<>();

        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM cases WHERE 1=1");
            List<Object> params = new ArrayList<>();

            // ✅ CASE ID
            if (caseId != null && !caseId.isEmpty()) {
                sql.append(" AND caseID LIKE ?");
                params.add("%" + caseId + "%");
            }

            // ✅ ACCUSED
            if (accused != null && !accused.isEmpty()) {
                sql.append(" AND accused LIKE ?");
                params.add("%" + accused + "%");
            }

            // ✅ PROSECUTOR
            if (prosecutor != null && !prosecutor.isEmpty()) {
                sql.append(" AND prosecutor LIKE ?");
                params.add("%" + prosecutor + "%");
            }

            // ✅ TYPE
            if (type != null) {
                sql.append(" AND caseType = ?");
                params.add(type);
            }

            // ✅ STATUS
            if (status != null) {
                sql.append(" AND caseStatus = ?");
                params.add(status);
            }

            // ✅ DATE FILTER
            if (dateFilter != null) {
                switch (dateFilter) {

                    case "Last 7 Days":
                        sql.append(" AND filedDate >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)");
                        break;

                    case "Last 30 Days":
                        sql.append(" AND filedDate >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)");
                        break;

                    case "This Year":
                        sql.append(" AND YEAR(filedDate) = YEAR(CURDATE())");
                        break;

                    case "Custom Range":
                        if (fromDate != null && toDate != null) {
                            sql.append(" AND filedDate BETWEEN ? AND ?");
                            params.add(fromDate.toString());
                            params.add(toDate.toString());
                        }
                        break;
                }
            }

            PreparedStatement ps = conn().prepareStatement(sql.toString());

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                CaseRec c = new CaseRec(
                        rs.getString("caseID"),
                        rs.getString("caseType"),
                        rs.getString("caseNature"),
                        rs.getString("caseStatus"),
                        rs.getString("accused"),
                        rs.getString("complainant"),
                        rs.getString("prosecutor"),
                        rs.getString("judge"),
                        rs.getString("filedDate"),
                        rs.getString("hearingDate"),
                        rs.getString("witness"),
                        rs.getString("evidence"),
                        rs.getString("branch"),
                        rs.getString("verdict"),
                        rs.getString("caseDesc")
                );

                results.add(c);
            }

        } catch (SQLException e) {
            System.err.println("[DB] searchCasesAdvanced: " + e.getMessage());
        }

        return results;
    }

    public boolean updtStatus(String id, String newStatus) {
        return updt(id, "caseStatus", newStatus);
    }

    public boolean updtProsec(String id, String newProsecutor) {
        return updt(id, "prosecutor", newProsecutor);
    }

    public boolean updtJudge(String id, String newJudge) {
        return updt(id, "judge", newJudge);
    }

    public boolean updtHearingDate(String id, String newHearingDate) {
        return updt(id, "hearingDate", newHearingDate);
    }

    public boolean updtWitness(String id, String newWitness) {
        return updt(id, "witness", newWitness);
    }

    public boolean updtEvidence(String id, String newEvidence) {
        return updt(id, "evidence", newEvidence);
    }

    public boolean updtBranch(String id, String newBranch) {
        return updt(id, "branch", newBranch);
    }

    public boolean updtVerdict(String id, String newVerdict) {
        return updt(id, "verdict", newVerdict);
    }

    public boolean updtCaseDesc(String id, String newCaseDesc) {
        return updt(id, "caseDesc", newCaseDesc);
    }

    private boolean updt(String caseID, String field, String value) {
        List<String> allowed = List.of("caseStatus", "prosecutor", "judge", "hearingDate", "witness", "evidence", "branch", "verdict", "caseDesc", "caseType", "caseNature", "accused", "complainant", "filedDate", "caseID");

        if (!allowed.contains(field)) {
            System.err.println("[DB] updt: rejected field: " + field);
            return false;
        }

        String sql = "UPDATE cases SET " + field + " = ? WHERE caseID = ?";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setString(2, caseID);

            boolean ok = ps.executeUpdate() > 0;

            if (ok) {
                System.out.println("\nCase updated");
            } else {
                System.out.println("\nCase not found.");
            }

            return ok;
        } catch (SQLException e) {
            System.err.println("[DB] updt: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteCase(String caseID) {
        String sql = "DELETE FROM cases WHERE caseID = ?";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, caseID);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DB] deleteCase: " + e.getMessage());
            return false;
        }
    }

    public int[] getCaseStats() {
        int[] stats = new int[4];
        String sql = "SELECT caseStatus, COUNT(*) AS cnt FROM cases GROUP BY caseStatus";
        try (PreparedStatement ps = conn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int cnt = rs.getInt("cnt");
                stats[0] += cnt;

                switch (rs.getString("caseStatus")) {
                    case "Active" -> {
                        stats[1] += cnt;
                    }
                    case "Resolved" -> {
                        stats[2] += cnt;
                    }
                    case "Dismissed" -> {
                        stats[3] += cnt;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] getCaseStats: " + e.getMessage());
        }
        return stats;
    }

    public List<String[]> getProsecWorkload() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT prosecutor, COUNT(*) AS cnt FROM cases WHERE caseStatus = 'Active' AND prosecutor IS NOT NULL AND prosecutor != '' GROUP BY prosecutor ORDER BY cnt DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new String[]{rs.getString("prosecutor"), String.valueOf(rs.getInt("cnt"))});
            }
        } catch (SQLException e) {
            System.err.println("[DB] getProsecWorkload: " + e.getMessage());
        }
        return list;
    }

    public List<String[]> getCasesByType() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT caseType, COUNT(*) AS cnt FROM cases GROUP BY caseType ORDER BY cnt DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new String[]{rs.getString("caseType"), String.valueOf(rs.getInt("cnt"))});
            }
        } catch (SQLException e) {
            System.err.println("[DB] getCasesByType: " + e.getMessage());
        }
        return list;
    }

    public boolean addSched(String caseID, String datetime, String createdBy) { //createdBy is  
        String sql = "INSERT INTO schedules (caseID, datetime, hsID) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, caseID);
            ps.setString(2, datetime);
            ps.setString(3, createdBy);

            boolean ok = ps.executeUpdate() > 0;

            if (ok) {
                System.out.println("\nSchedule added.");
            } else {
                System.out.println("\nFailed to add schedule.");
            }
            return ok;

        } catch (SQLException e) {
            System.err.println("[DB] addSched: " + e.getMessage());
            return false;
        }
    }

    public List<Schedules> searchSchedulesAdvanced(
            String caseId,
            String accused,
            String prosecutor,
            String dateFilter,
            LocalDate fromDate,
            LocalDate toDate) {

        List<Schedules> list = new ArrayList<>();

        try {
            Connection conn = DBConfig.getConnection();
            String sql = "SELECT * FROM schedules WHERE 1=1";
            List<Object> params = new ArrayList<>();
            if (caseId != null && !caseId.isEmpty()) {
                sql += " AND caseID LIKE ?";
                params.add("%" + caseId + "%");
            }
            if (accused != null && !accused.isEmpty()) {
                sql += " AND caseID IN (SELECT caseID FROM cases WHERE accused LIKE ?)";
                params.add("%" + accused + "%");
            }
            if (prosecutor != null && !prosecutor.isEmpty()) {
                sql += " AND caseID IN (SELECT caseID FROM cases WHERE LOWER(prosecutor) = LOWER(?))";
                params.add(prosecutor);
            }
            if (dateFilter != null) {
                if (dateFilter.equals("Last 7 Days")) {
                    sql += " AND datetime >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
                } else if (dateFilter.equals("Last 30 Days")) {
                    sql += " AND datetime >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
                } else if (dateFilter.equals("This Year")) {
                    sql += " AND YEAR(datetime) = YEAR(CURDATE())";
                } else if (dateFilter.equals("Custom Range") && fromDate != null && toDate != null) {
                    sql += " AND DATE(datetime) BETWEEN ? AND ?";
                    params.add(fromDate.toString());
                    params.add(toDate.toString());
                }
            }

            PreparedStatement ps = conn.prepareStatement(sql);

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapSchedule(rs)); 
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public boolean reschedule(int scheduleId, String newDatetime, String reason, String updatedBy) {
        String sql = "UPDATE schedules SET ReSched = ?, reason = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, newDatetime);
            ps.setString(2, reason);
            ps.setInt(3, scheduleId);

            boolean ok = ps.executeUpdate() > 0;

            if (ok) {
                System.out.println("\nRescheduled.");
            } else {
                System.out.println("\nSchedule not found.");
            }

            return ok;

        } catch (SQLException e) {
            System.err.println("[DB] reschedule: " + e.getMessage());
            return false;
        }
    }

    public List<Schedules> getUpcomingSchedules() {

        List<Schedules> list = new ArrayList<>();
        String sql = "SELECT * FROM schedules WHERE datetime >= NOW() ORDER BY datetime ASC LIMIT 50";

        try (PreparedStatement ps = conn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapSchedule(rs));
            }

        } catch (SQLException e) {
            System.err.println("[DB] getUpcomingSchedules: " + e.getMessage());
        }
        return list;
    }

    public List<Schedules> getSchedulesByCaseID(String caseID) {

        List<Schedules> list = new ArrayList<>();
        String sql = "SELECT * FROM schedules WHERE caseID = ? ORDER BY datetime";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, caseID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapSchedule(rs));
            }

        } catch (SQLException e) {
            System.err.println("[DB] getSchedulesByCaseID: " + e.getMessage());
        }
        return list;
    }

    private List<CaseRec> queryCases(String sql, String param) {

        List<CaseRec> list = new ArrayList<>();

        try (PreparedStatement ps = conn().prepareStatement(sql)) {

            if (param != null) {
                ps.setString(1, param);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapCase(rs));
            }

        } catch (SQLException e) {
            System.err.println("[DB] queryCases: " + e.getMessage());
        }
        return list;
    }

    private CaseRec mapCase(ResultSet rs) throws SQLException {
        CaseRec cr = new CaseRec(
                rs.getString("caseID"),
                rs.getString("caseType"),
                rs.getString("caseNature"),
                rs.getString("caseStatus"),
                rs.getString("accused"),
                rs.getString("complainant"),
                rs.getString("prosecutor"),
                rs.getString("judge"),
                rs.getString("filedDate"),
                rs.getString("hearingDate"),
                rs.getString("witness"),
                rs.getString("evidence"),
                rs.getString("branch"),
                rs.getString("verdict"),
                rs.getString("caseDesc")
        );
        try {
            cr.setDbId(rs.getInt("id"));
        } catch (Exception ignored) {
        }

        return cr;
    }

    private Schedules mapSchedule(ResultSet rs) throws SQLException {
        Schedules s = new Schedules(
                rs.getString("datetime"),
                rs.getString("caseID"),
                rs.getString("ReSched"),
                rs.getString("reason"),
                rs.getString("hsID")
        );

        s.setDbId(rs.getInt("id"));
        return s;
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
