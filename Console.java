import java.util.List;
import java.util.Scanner;

public class Console {

    private static final DBManager db = new DBManager();
    private static User currentUser = null;

    public static void main(String[] args) {

        if (!DBConfig.testConnection()) {
            System.err.println("Cannot connect to database.");
            System.exit(1);
        }

        Scanner scanner = new Scanner(System.in);
        printBanner();
        login(scanner);

        if (currentUser != null) {
            currentUser.RoleDashb();
            mainMenu(scanner);
        }
        DBConfig.closeConnection();
        System.out.println("\nEnd of Program.");
    }

    private static void printBanner() {
        System.out.println("\n--------------------------------------------------------------");
        System.out.println("                       WELCOME TO JURIS");
        System.out.println("        Judicial Unified Records and Information System");
        System.out.println("--------------------------------------------------------------");
    }

    private static void login(Scanner scanner) {
        int attempts = 3;
        while (attempts >0) {

            System.out.print("\nUsername: ");
            String username = scanner.nextLine().trim();

            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            currentUser = db.validateUser(username, password);
            if (currentUser != null) {
                db.writeAuditLog(username, "LOGIN", "Logged in as " + currentUser.getRole());
                System.out.println("\nLogin successful. Welcome, "+ currentUser.getFullName() + " [" + currentUser.getRole() + "]");
                return;
            }
            attempts--;
            System.out.println("Invalid credentials. " + attempts + " attempt(s) remaining.");
        }
        System.out.println("Too many failed attempts. Exiting.");
        System.exit(0);
    }

    private static void mainMenu(Scanner scanner) {
        boolean running = true;
        while (running) {
            System.out.println("\nMAIN MENU  [" + currentUser.getRole()+ ": " + currentUser.getFullName() + "]");

            if (currentUser.isAdmin() || currentUser.isStaff()) {
                System.out.println("  [1] Case Management");
                System.out.println("  [2] Search Cases");
                System.out.println("  [3] Schedule & Hearings");
            }

            if (currentUser.isProsecutor()) {
                System.out.println("  [1] My Assigned Cases");
                System.out.println("  [2] Search My Cases");
                System.out.println("  [3] My Hearing Schedule");
            }

            if (currentUser.isAdmin()) {
                System.out.println("  [4] User Management");
                System.out.println("  [5] Audit Log");
            }

            System.out.println("  [6] Change Password");
            System.out.println("  [0] Logout");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {

                case "1" -> {
                    if (currentUser.isProsecutor()){
                        myAssignedCases(scanner);
                    } else {
                        caseManagementMenu(scanner);
                    }
                }

                case "2" ->{
                    searchCasesMenu(scanner);
                }

                case "3" ->{
                    scheduleMenu(scanner);
                }

                case "4" -> {
                    if (currentUser.isAdmin()){
                        userManagementMenu(scanner);
                    } else {
                        invalid();
                    }
                }

                case "5" -> {
                    if (currentUser.isAdmin()){
                        db.viewAuditLog();
                    }else{
                        invalid();
                    }
                }

                case "6" -> {
                    changePassword(scanner);
                }

                case "0" -> {
                    running = false;
                }

                default  -> {
                    invalid();
                }
            }
        }
    }

    private static void caseManagementMenu(Scanner scanner) {
        boolean running = true;
        while (running) {
            System.out.println("\nCASE MANAGEMENT");
            System.out.println("  [1] View All Cases");
            System.out.println("  [2] Register New Case");
            System.out.println("  [3] Update Case");
            System.out.println("  [4] View Case Details");

            if (currentUser.isAdmin()){
                System.out.println("  [5] Delete Case");
            }

            System.out.println("  [0] Back");
            System.out.print("Select an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> {
                    viewAllCases();
                }

                case "2" -> {
                    registerCase(scanner);
                }

                case "3" ->{
                    updateCaseMenu(scanner);
                }

                case "4" ->{
                    viewCaseDetails(scanner);
                }

                case "5" -> {
                    if (currentUser.isAdmin()){
                        deleteCase(scanner);
                    } else {
                        invalid();
                    }
                }

                case "0" -> {
                    running = false;
                }

                default  ->{
                    invalid();
                }
            }
        }
    }

    private static void viewAllCases() {
        printCaseTable(db.getAllCases(), "ALL CASES");
    }

    private static void myAssignedCases(Scanner scanner) {

        List<CaseRec> cases = db.getCasesByProsecutor(currentUser.getFullName());
        printCaseTable(cases, "MY ASSIGNED CASES");

        System.out.println("[1] Update a case");
        System.out.println("[0] Back");
        System.out.print("Select an option: ");
        String choice = scanner.nextLine().trim();

        if ("1".equalsIgnoreCase(choice)){
            updateCaseMenu(scanner);
        }
    }

    private static void printCaseTable(List<CaseRec> cases, String title) {

        System.out.println("\n----------------------------------------------------------------------------------------------------------------------");
        System.out.println("  " + title + " (" + cases.size() + " records)");
        System.out.println("----------------------------------------------------------------------------------------------------------------------");
        System.out.printf("  %-18s %-22s %-16s %-14s %-18s %-12s%n","Case ID", "Accused", "Case Type", "Status", "Prosecutor", "Filing Date");
        System.out.println("----------------------------------------------------------------------------------------------------------------------");
        
        if (cases.isEmpty()) {
            System.out.println("  No records found.");
        } else {
            for (CaseRec c : cases) {
                System.out.printf("  %-18s %-22s %-16s %-14s %-18s %-12s%n", c.getCaseID(), truncate(c.getAccused(), 20),truncate(c.getCaseType(), 14),c.getCaseStatus(),truncate(c.getProsecutor() != null? c.getProsecutor() : "Unassigned", 16),c.getFiledDate());
            }
        }

        System.out.println("----------------------------------------------------------------------------------------------------------------------");
    }

    private static void registerCase(Scanner scanner) {

        if (!currentUser.canManageCases()) {
            invalid(); return;
        }

        System.out.println("\nREGISTER NEW CASE");

        String suggestedID = db.getNextCaseID();

        System.out.println("Suggested Case ID: " + suggestedID);
        System.out.print("Case Number (press Enter to use suggested): ");
        String caseID = scanner.nextLine().trim();

        if (caseID.isEmpty()){
            caseID = suggestedID;
        }

        if (db.getCaseByCaseID(caseID) != null) {
            System.out.println("Case ID already exists.");
            return;
        }

        String caseType= promptChoice(scanner,"Case Type", new String[]{"Criminal", "Civil", "Violations", "Administrative", "Others"});
        String caseNature= prompt(scanner,"Nature/Offense (e.g. Theft)");
        String caseStatus= promptChoice(scanner,"Case Status", new String[]{"Active", "Resolved", "Dismissed"});
        String filedDate= prompt(scanner,"Filing Date (YYYY-MM-DD)");
        String accused= prompt(scanner,"Name of Accused");
        String complainant= prompt(scanner,"Name of Complainant");
        String caseDesc= prompt(scanner,"Case Description");

        System.out.println("\nOptional Fields (press Enter to skip)");
        String prosecutor = promptProsecutor(scanner);
        String judge = prompt(scanner, "Name of Judge");
        String hearingDate = prompt(scanner, "Hearing Date (YYYY-MM-DD, optional)");
        String branch = prompt(scanner, "Branch");
        String witness = prompt(scanner, "Witness");
        String evidence = prompt(scanner, "Evidence");
        String verdict = prompt(scanner, "Verdict");

        System.out.println("\nCase ID    : " + caseID);
        System.out.println("Type       : " + caseType + " | " + caseNature);
        System.out.println("Accused    : " + accused);
        System.out.println("Complainant:" + complainant);
        System.out.print("Confirm Registration? (y/n): ");

        if (!"y".equalsIgnoreCase(scanner.nextLine().trim())) {
            System.out.println("Registration Cancelled.");
            return;
        }

        boolean ok = db.addCase(caseID, caseType, caseNature, caseStatus, accused, complainant, prosecutor.isEmpty() ? null : prosecutor,judge.isEmpty() ? null : judge, filedDate, hearingDate.isEmpty() ? null : hearingDate,witness.isEmpty() ? null : witness, evidence.isEmpty() ? null : evidence, branch.isEmpty() ? null : branch, verdict.isEmpty() ? null : verdict, caseDesc);

        if (ok){
            db.writeAuditLog(currentUser.getUsername(), "REGISTER_CASE","Registered case: " + caseID);
        }
    }

    private static void updateCaseMenu(Scanner scanner) {

        if (!currentUser.canManageCases()) {
            invalid();
            return;
        }

        System.out.print("\nEnter Case ID to update: ");
        String caseID = scanner.nextLine().trim();
        CaseRec c = db.getCaseByCaseID(caseID);

        if (c == null) {
            System.out.println("Case not found.");
            return;
        }

        System.out.println("\n  Updating: " + c.getCaseID() + " | " + c.getCaseNature());
        System.out.println("  [1] Status");
        System.out.println("  [2] Prosecutor");
        System.out.println("  [3] Judge");
        System.out.println("  [4] Hearing");
        System.out.println("  [5] Branch");
        System.out.println("  [6] Witness");
        System.out.println("  [7] Evidence");
        System.out.println("  [8] Verdict");
        System.out.println("  [9] Description");
        System.out.print("Select an option: ");

        String newVal;
        switch (scanner.nextLine().trim()) {

            case "1" -> {
                newVal = promptChoice(scanner, "New Status",new String[]{"Active", "Resolved", "Dismissed"});
                db.updtStatus(caseID, newVal);
            }

            case "2" -> {
                newVal = promptProsecutor(scanner);
                db.updtProsec(caseID, newVal);
            }

            case "3" -> {
                newVal = prompt(scanner, "New Judge");
                db.updtJudge(caseID, newVal);
            }

            case "4" -> {
                newVal = prompt(scanner, "New Hearing Date (YYYY-MM-DD)");
                db.updtHearingDate(caseID, newVal);
            }

            case "5" -> {
                newVal = prompt(scanner, "New Branch");
                db.updtBranch(caseID, newVal);
            }

            case "6" -> {
                newVal = prompt(scanner, "New Witness");
                db.updtWitness(caseID, newVal);
            }

            case "7" -> {
                newVal = prompt(scanner, "New Evidence");
                db.updtEvidence(caseID, newVal);
            }

            case "8" -> {
                newVal = prompt(scanner, "New Verdict");
                db.updtVerdict(caseID, newVal);
            }

            case "9" -> {
                newVal = prompt(scanner, "New Description");
                db.updtCaseDesc(caseID, newVal);
            }

            default  -> {
                invalid();
                return; }
        }
        db.writeAuditLog(currentUser.getUsername(), "UPDATE_CASE", "Updated case: " + caseID);
    }

    private static void viewCaseDetails(Scanner scanner) {
        System.out.print("\nEnter Case ID: ");
        String caseID = scanner.nextLine().trim();
        CaseRec c = db.getCaseByCaseID(caseID);

        if (c == null) {
            System.out.println("  Case not found.");
            return;
        }
        System.out.println("\n" + c);
        System.out.println("  Judge: "+ (c.getJudge()!= null ? c.getJudge(): "—"));
        System.out.println("  Hearing Date: "+ (c.getHearingDate()!= null ? c.getHearingDate() : "—"));
        System.out.println("  Branch: "+ (c.getBranch()!= null ? c.getBranch(): "—"));
        System.out.println("  Witness: "+ (c.getWitness()!= null ? c.getWitness(): "—"));
        System.out.println("  Evidence: " + (c.getEvidence()!= null ? c.getEvidence(): "—"));
        System.out.println("  Verdict: "+ (c.getVerdict()!= null ? c.getVerdict(): "—"));
        System.out.println("  Description: " + (c.getCaseDesc()!= null ? c.getCaseDesc(): "—"));
    }

    private static void deleteCase(Scanner scanner) {

        System.out.print("\nCase ID to delete: ");
        String caseID = scanner.nextLine().trim();

        if (db.getCaseByCaseID(caseID) == null) {
            System.out.println("Case not found.");
            return;
        }

        System.out.print("Confirm delete '" + caseID + "'? (y/n): ");

        if (!"y".equalsIgnoreCase(scanner.nextLine().trim())) {
            System.out.println("Deletion Cancelled.");
            return;
        }

        if (db.deleteCase(caseID)){
            db.writeAuditLog(currentUser.getUsername(), "DELETE_CASE", "Deleted case: " + caseID);
        }
    }

    private static void searchCasesMenu(Scanner scanner) {
        System.out.println("\nSEARCH CASES");
        String caseID = prompt(scanner, "Case ID");
        String accused = prompt(scanner, "Name of Accused");
        String caseType = prompt(scanner, "Case Type");
        String status = prompt(scanner, "Status");
        String prosecutor = currentUser.isProsecutor() ? currentUser.getFullName(): prompt(scanner, "Prosecutor");

        String forProsc = currentUser.isProsecutor() ? currentUser.getFullName() : null;
        List<CaseRec> results = db.searchCases(caseID, accused, caseType,"", "", prosecutor, status, "", forProsc);
        printCaseTable(results, "SEARCH RESULTS");
    }

    private static void scheduleMenu(Scanner scanner) {
        boolean running = true;
        while (running) {
            System.out.println("\nSCHEDULE & HEARINGS");
            System.out.println("  [1] View Upcoming Schedules");
            System.out.println("  [2] Add Schedule to Case");
            System.out.println("  [3] Reschedule Hearing");
            System.out.println("  [4] View Schedules for a Case");
            System.out.println("  [0] Back");
            System.out.print("Select an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> {
                    List<Schedules> upcoming = db.getUpcomingSchedules();
                    System.out.println("\n  UPCOMING (" + upcoming.size() + "):");
                    upcoming.forEach(s -> System.out.println("  → " + s));

                    if (upcoming.isEmpty()){
                        System.out.println("  No upcoming schedules.");
                    }
                }
                case "2" -> {
                    String caseID = prompt(scanner, "Case ID");

                    if (db.getCaseByCaseID(caseID) == null) {
                        System.out.println("  Case not found.");
                        break;
                    }

                    String dt = prompt(scanner, "Date & Time (YYYY-MM-DD HH:MM:SS)");
                    if (db.addSched(caseID, dt, currentUser.getUsername())){
                        db.writeAuditLog(currentUser.getUsername(), "ADD_SCHEDULE", "Case: " + caseID);
                    }
                }

                case "3" -> {
                    System.out.print("Schedule ID: ");
                    int id;
                    try {
                        id = Integer.parseInt(scanner .nextLine().trim());
                    }
                    catch (NumberFormatException e) {
                        System.out.println("Invalid ID.");
                        break;
                    }

                    String newDt= prompt(scanner, "New Date & Time (YYYY-MM-DD HH:MM:SS)");
                    String reason = prompt(scanner, "Reason");

                    db.reschedule(id, newDt, reason, currentUser.getUsername());
                }

                case "4" -> {
                    String caseID = prompt(scanner , "Case ID");
                    List<Schedules> list = db.getSchedulesByCaseID(caseID);

                    System.out.println("\nSchedules for " + caseID + ":");
                    list.forEach(s -> System.out.println("  [" + s.getDbId() + "] " + s));

                    if (list.isEmpty()){
                        System.out.println("None found.");
                    }
                }
                case "0" ->{
                    running = false;
                }

                default  -> {
                    invalid();
                }
            }
        }
    }

    private static void userManagementMenu(Scanner scanner) {
        boolean running = true;
        while (running) {
            System.out.println("\nUSER MANAGEMENT (Admin)");
            System.out.println("  [1] View All Users");
            System.out.println("  [2] Register New User");
            System.out.println("  [3] Deactivate User");
            System.out.println("  [4] Reactivate User");
            System.out.println("  [5] Reset User Password");
            System.out.println("  [0] Back");
            System.out.print("Select an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> {
                    db.ViewUserList();
                }
                case "2" -> {
                    registerUser(scanner);
                }
                case "3" -> {
                    System.out.print("Username to deactivate: ");
                    db.DeactivateUser(scanner.nextLine().trim());
                }
                case "4" -> {
                    System.out.print("Username to reactivate: ");
                    db.ReactivateUser(scanner.nextLine().trim());
                }
                case "5" -> {
                    System.out.print("  Username: ");
                    String u = scanner.nextLine().trim();
                    String pw = prompt(scanner, "New Password");
                    db.UpdatePassword(u, pw);
                }
                case "0" ->{
                    running = false;
                }
                default  -> {
                    invalid();
                }
            }
        }
    }

    private static void registerUser(Scanner scanner) {
        System.out.println("\nREGISTER NEW USER");
        String username = prompt(scanner, "Username");

        if (username.isEmpty()) {
            System.out.println("Registration Cancelled.");
            return;
        }

        String fullName = prompt(scanner, "Full Name");
        String role = promptChoice(scanner, "Role", new String[]{"Staff", "Prosecutor", "Admin"});
        String password = prompt(scanner, "Password");

        System.out.println("\nUser: " + username + " [" + role + "] | " + fullName);
        System.out.print("Confirm registration? (y/n): ");

        if (!"y".equalsIgnoreCase(scanner.nextLine().trim())) {
            System.out.println("Registration Cancelled.");
            return;
        }

        if (db.AddUser(username, password, role, fullName)){
            db.writeAuditLog(currentUser.getUsername(), "CREATE_USER", "Created: " + username + " [" + role + "]");
        }
    }

    private static void changePassword(Scanner scanner) {
        System.out.println("\nCHANGE PASSWORD");
        String current = prompt(scanner, "Current Password");

        if (db.validateUser(currentUser.getUsername(), current) == null) {
            System.out.println("Incorrect current password.");
            return;
        }

        String newPw = prompt(scanner, "New Password");
        String confirm = prompt(scanner, "Confirm New Password");

        if (!newPw.equals(confirm)){
            System.out.println("Passwords do not match.");
            return;
        }

        if (newPw.length() < 6){
            System.out.println("Min 6 characters required.");
            return;
        }

        if (db.UpdatePassword(currentUser.getUsername(), newPw)){
            db.writeAuditLog(currentUser.getUsername(), "CHANGE_PASSWORD", "Password changed");
        }
    }

    private static String prompt(Scanner scanner, String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    private static String promptChoice(Scanner scanner, String label, String[] options) {
        System.out.println(label + ":");

        for (int i = 0; i < options.length; i++)
            System.out.println(" [" + (i + 1) + "] " + options[i]);

        System.out.print("Select an option: ");

        try {
            int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;

            if (idx >= 0 && idx < options.length){
                return options[idx];
            }
        } catch (NumberFormatException ignored) {}

        return options[0];
    }

    private static String promptProsecutor(Scanner scanner) {

        List<String> prosecutors = db.getProsecutorList();

        if (prosecutors.isEmpty()) {
            System.out.print("Name of Prosecutor: ");
            return scanner.nextLine().trim();
        }
        prosecutors.add(0, "(Skip / Unassigned)");

        String chosen = promptChoice(scanner, "Assign Prosecutor", prosecutors.toArray(new String[0]));

        return chosen.startsWith("(Skip") ? "" : chosen;
    }

    private static void invalid() {
        System.out.println("Invalid option or insufficient permissions.");
    }

    private static String truncate(String s, int max) {

        if (s == null){
            return "";
        }

        return s.length() > max ? s.substring(0, max - 1) + "..." : s;
    }
}
