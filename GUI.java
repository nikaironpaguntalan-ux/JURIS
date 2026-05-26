import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.util.List;
import java.util.Optional;

public class GUI {

    static final String RED="#D91921";
    static final String BLUE="#03439E";
    static final String DARK_BLUE="#03285D";
    static final String LIGHT_BLUE="#D6E9FA";
    static final String WHITE="#FFFFFF";
    static final String LT_GRAY="#F5F5F5";
    static final String DK_GRAY="#212121";

    private final Stage stage;
    private final DBManager db = new DBManager();
    private User currentUser;
    private BorderPane root;

    public GUI(Stage stage) {
        this.stage = stage;
    }

    public void launch() {
        stage.setTitle("JURIS | Judicial Unified Records and Information System");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);

        if (!DBConfig.testConnection()) {
            new Alert(Alert.AlertType.ERROR, "Cannot connect to the database.\nCheck DBConfig.java settings.",ButtonType.OK).showAndWait();
            Platform.exit();
            return;
        }
        showLoginScreen();
    }

    private void showLoginScreen() {
        VBox loginRoot = new VBox();
        loginRoot.setStyle("-fx-background-color: " + DARK_BLUE + ";");
        loginRoot.setAlignment(Pos.CENTER);

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 50, 36, 50));
        card.setMaxWidth(440);
        card.setStyle("-fx-background-color: " + WHITE + ";-fx-background-radius: 10; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.4),20,0,0,6);");

        ImageView logoImg = new ImageView();

        try {
            Image img = new Image(GUI.class.getResourceAsStream("juris_logo.png"),320, 120, true, true);
            logoImg.setImage(img);
        } catch (Exception ignored) {}

        logoImg.setFitWidth(300);
        logoImg.setPreserveRatio(true);

        Label subtitle = new Label("Judicial Unified Records and Information System");
        subtitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;-fx-text-fill: " + BLUE + ";");
        subtitle.setAlignment(Pos.CENTER);

        Label userLbl = fieldLabel("Username");
        TextField userField = styledTextField("Enter username", "");

        Label passLbl = fieldLabel("Password");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter password");
        styleInput(passField);

        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill: " + RED + "; -fx-font-size: 12px;");

        Button loginBtn = new Button("LOGIN");
        styleRed(loginBtn);
        loginBtn.setPrefWidth(300);
        loginBtn.setPrefHeight(42);
        loginBtn.setStyle(loginBtn.getStyle()+ "-fx-font-size: 14px; -fx-font-weight: bold;");

        card.getChildren().addAll(logoImg, subtitle, new Separator(), userLbl, userField, passLbl, passField, errorLbl, loginBtn);
        loginRoot.getChildren().add(card);

        Runnable doLogin = () -> {
            String u = userField.getText().trim();
            String p = passField.getText().trim();

            if (u.isEmpty() || p.isEmpty()) {
                errorLbl.setText("Please enter both username and password.");
                return;
            }

            currentUser = db.validateUser(u, p);

            if (currentUser != null) {
                db.writeAuditLog(u, "LOGIN", "GUI login as " + currentUser.getRole());
                showDashboard();
            } else {
                errorLbl.setText("Invalid credentials or account inactive.");
                passField.clear();
            }
        };

        loginBtn.setOnAction(e -> doLogin.run());
        passField.setOnAction(e -> doLogin.run());

        stage.setScene(new Scene(loginRoot, 1100, 700));
        stage.show();
    }

    private void showDashboard() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: " + LT_GRAY + ";");
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildDashboardPanel()); 

        stage.setScene(new Scene(root, 1200, 750));
        stage.setTitle("JURIS | " + currentUser.getRole() + ": " + currentUser.getFullName());
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.setStyle("-fx-background-color: " + DARK_BLUE + ";");
        bar.setPadding(new Insets(0, 20, 0, 20));
        bar.setPrefHeight(52);
        bar.setAlignment(Pos.CENTER_LEFT);

        ImageView barLogo = new ImageView();
        try {
            Image img = new Image(GUI.class.getResourceAsStream("juris_whitelogo.png"), 160, 44, true, true);
            barLogo.setImage(img);
        } catch (Exception ignored) {}

        barLogo.setFitHeight(40);
        barLogo.setPreserveRatio(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLbl = new Label(currentUser.getFullName() + "  [" + currentUser.getRole() + "]");
        userLbl.setStyle("-fx-text-fill: " + LIGHT_BLUE + "; -fx-font-size: 13px;");

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: " + RED + "; -fx-text-fill: white;-fx-font-size: 12px; -fx-padding: 6 16; -fx-background-radius: 4;");
        logoutBtn.setOnAction(e -> {
            db.writeAuditLog(currentUser.getUsername(), "LOGOUT", "GUI logout");
            currentUser = null;
            showLoginScreen();
        });

        bar.getChildren().addAll(barLogo, spacer, userLbl, logoutBtn);
        return bar;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(2);
        sidebar.setPrefWidth(190);
        sidebar.setPadding(new Insets(12, 0, 12, 0));
        sidebar.setStyle("-fx-background-color: " + BLUE + ";");

        Object[][] items = {
            {"Dashboard","icon_dashboard.png",(Runnable)() -> root.setCenter(buildDashboardPanel())},
            {"Cases","icon_cases.png",(Runnable)() -> root.setCenter(buildCasesPanel())},
            {"Schedule","icon_schedule.png",(Runnable)() -> root.setCenter(buildSchedulePanel())},
            {"Users","icon_users.png",(Runnable)() -> root.setCenter(buildUsersPanel())},
            {"Audit Logs","icon_auditlog.png",(Runnable)() -> root.setCenter(buildAuditLogPanel())},
        };

        for (Object[] item : items) {
            String label = (String)item[0];
            String file= (String)item[1];
            Runnable action = (Runnable)item[2];

            if ((label.equals("Users") || label.equals("Audit Logs")) && !currentUser.isAdmin()){
                continue;
            }

            Button btn = new Button(label);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.BASELINE_LEFT);
            btn.setPadding(new Insets(10, 20, 10, 20));
            btn.setGraphicTextGap(10);
            btn.setContentDisplay(ContentDisplay.LEFT);

            try {
                Image img = new Image(GUI.class.getResourceAsStream(file), 20, 20, true, true);
                btn.setGraphic(new ImageView(img));
            } catch (Exception ignored) {}

            String normal = "-fx-background-color: transparent; -fx-text-fill: " + WHITE + "; -fx-font-size: 13px; -fx-cursor: hand;";
            String hover  = "-fx-background-color: " + DARK_BLUE + "; -fx-text-fill: " + WHITE + "; -fx-font-size: 13px; -fx-cursor: hand;";
            btn.setStyle(normal);
            btn.setOnMouseEntered(e -> btn.setStyle(hover));
            btn.setOnMouseExited(e  -> btn.setStyle(normal));
            btn.setOnAction(e -> action.run());
            sidebar.getChildren().add(btn);
        }
        return sidebar;
    }

    private ScrollPane buildDashboardPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(24));

        Label heading = sectionHeading("Dashboard");

        int[] stats = db.getCaseStats();
        HBox cards = new HBox(16,
            statCard("Total Active Cases", String.valueOf(stats[1]), BLUE),
            statCard("Resolved Cases",String.valueOf(stats[2]), "#1E8A44"),
            statCard("Dismissed Cases",String.valueOf(stats[3]), RED),
            statCard("Total Prosecutors",String.valueOf(db.getProsecutorCount()), DARK_BLUE)
        );

        VBox typeBox = chartBox("Case Categories", db.getCasesByType(), stats[0], BLUE);

        VBox wlBox = chartBox("Prosecutor Workload (Active Cases)", db.getProsecWorkload(), 50, RED);

        HBox bottom = new HBox(20, typeBox, wlBox);
        HBox.setHgrow(typeBox, Priority.ALWAYS);
        HBox.setHgrow(wlBox,   Priority.ALWAYS);

        panel.getChildren().addAll(heading, cards, bottom);
        ScrollPane sp = new ScrollPane(panel);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: " + LT_GRAY + ";");
        return sp;
    }

    private VBox statCard(String title, String value, String color) {

        VBox card = new VBox(6);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8;-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.15),10,0,0,3);");

        Label val = new Label(value);
        val.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.85);");
        card.getChildren().addAll(val, lbl);

        return card;
    }

    private VBox chartBox(String title, List<String[]> rows, int maxVal, String barColor) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8;-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),8,0,0,2);");

        Label hdr = new Label(title);
        hdr.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + DK_GRAY + ";");
        box.getChildren().add(hdr);

        if (rows.isEmpty()) {
            box.getChildren().add(new Label("No data available."));
            return box;
        }

        for (String[] row : rows) {
            HBox rowBox = new HBox(10);
            rowBox.setAlignment(Pos.CENTER_LEFT);

            Label name = new Label(row[0]);
            name.setMinWidth(150);
            name.setStyle("-fx-font-size: 13px;");

            Label count = new Label(row[1]);
            count.setStyle("-fx-font-weight: bold; -fx-text-fill: " + barColor + ";");

            ProgressBar pb = new ProgressBar();

            try {
                pb.setProgress(Integer.parseInt(row[1]) / (double) Math.max(maxVal, 1));
            } catch (Exception ignored) {
                pb.setProgress(0); 
            }

            pb.setPrefWidth(180);
            pb.setStyle("-fx-accent: " + barColor + ";");
            rowBox.getChildren().addAll(name, pb, count);
            box.getChildren().add(rowBox);
        }
        return box;
    }

    private VBox buildCasesPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(24));

        Button addBtn = new Button("+ New Case");
        styleRed(addBtn);
        HBox header = headerRow("Case List", addBtn);

        TextField searchFld = styledTextField("Search...", "");
        searchFld.setPrefWidth(220);

        ComboBox<String> typeCb = combo("All Types","Criminal","Civil","Violations","Administrative","Others");
        ComboBox<String> statusCb = combo("All Status","Active","Resolved","Dismissed");

        Button searchBtn = new Button("Search");
        styleBlue(searchBtn);

        HBox searchBar = new HBox(10, searchFld, typeCb, statusCb, searchBtn);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        TableView<CaseRec> table = buildCaseTable();

        refreshCaseTable(table, null, null, null);

        searchBtn.setOnAction(e -> refreshCaseTable(table, searchFld.getText().trim(), typeCb.getValue().startsWith("All")   ? null : typeCb.getValue(), statusCb.getValue().startsWith("All") ? null : statusCb.getValue()));
        searchFld.setOnAction(e -> searchBtn.fire());
        addBtn.setOnAction(e -> showCaseForm(null, table));

        table.setRowFactory(tv -> {
            TableRow<CaseRec> row = new TableRow<>();

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showCaseDetails(row.getItem(), table);
                }
            });

            return row;
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        panel.getChildren().addAll(header, searchBar, table);
        return panel;
    }

    private TableView<CaseRec> buildCaseTable() {
        TableView<CaseRec> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-font-size: 13px;");

        table.getColumns().addAll(
            plainCol("Case No.","caseID",130),
            plainCol("Accused","accused",150),
            plainCol("Case Type","caseType",110),
            plainCol("Prosecutor","prosecutor",140),
            statusCol(),
            plainCol("Filing Date","filedDate",110),
            actionCol(table)
        );

        return table;
    }

    private TableColumn<CaseRec, String> statusCol() {
        TableColumn<CaseRec, String> col = plainCol("Status", "caseStatus", 100);

        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String bg = switch (item) {
                    case "Active" -> BLUE;
                    case "Resolved" -> "#1E8A44";
                    case "Dismissed" -> RED;
                    default-> "#888";
                };
                setStyle("-fx-background-color: " + bg + "; -fx-text-fill: white;-fx-background-radius: 4; -fx-alignment: CENTER;");
            }
        });
        return col;
    }

    private TableColumn<CaseRec, Void> actionCol(TableView<CaseRec> table) {
        TableColumn<CaseRec, Void> col = new TableColumn<>("Actions");
        col.setPrefWidth(130);
        col.setCellFactory(c -> new TableCell<>() {

            final Button viewBtn=smallBtn("View", BLUE);
            final Button delBtn=smallBtn("Del",  RED);

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                viewBtn.setOnAction(e ->
                    showCaseDetails(getTableView().getItems().get(getIndex()), table));

                delBtn.setOnAction(e -> {
                    if (!currentUser.isAdmin()) {
                        alert("Access Denied", "Only Admin can delete cases."); return;
                    }

                    CaseRec c = getTableView().getItems().get(getIndex());
                    if (confirm("Delete case " + c.getCaseID() + "?")) {
                        db.deleteCase(c.getCaseID());
                        db.writeAuditLog(currentUser.getUsername(), "DELETE_CASE", c.getCaseID());
                        refreshCaseTable(table, null, null, null);
                    }
                });
                HBox box = new HBox(6, viewBtn, delBtn);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });
        return col;
    }

    private void refreshCaseTable(TableView<CaseRec> table, String keyword, String type, String status) {
        
        String prosecFilter = currentUser.isProsecutor() ? currentUser.getFullName() : null;

        List<CaseRec> cases = db.searchCases(keyword, null, type, null, null, prosecFilter, status, null, prosecFilter);
        table.setItems(FXCollections.observableArrayList(cases));
    }

    private void showCaseDetails(CaseRec c, TableView<CaseRec> table) {
        Stage dlg = dialog("Case Details | " + c.getCaseID());
        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: white;");

        String bg = statusColor(c.getCaseStatus());
        HBox hdr = new HBox(16);
        hdr.setPadding(new Insets(12, 16, 12, 16));
        hdr.setStyle("-fx-background-color: " + DARK_BLUE + "; -fx-background-radius: 6;");
        Label idLbl = new Label(c.getCaseID());
        idLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label statusLbl = new Label(c.getCaseStatus());
        statusLbl.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: white;-fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold;");
        hdr.getChildren().addAll(idLbl, sp, statusLbl);

        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(10);
        grid.setPadding(new Insets(12, 0, 0, 0));
        String[][] fields = {
            {"Case Type",c.getCaseType()},
            {"Nature",c.getCaseNature()},
            {"Filing Date",c.getFiledDate()},
            {"Accused", c.getAccused()},
            {"Complainant", c.getComplainant()},
            {"Prosecutor",c.getProsecutor()},
            {"Judge",c.getJudge()},
            {"Hearing Date",c.getHearingDate()},
            {"Branch", c.getBranch()},
            {"Witness",c.getWitness()},
            {"Evidence",c.getEvidence()},
            {"Verdict",c.getVerdict()},
            {"Description",c.getCaseDesc()},
        };

        for (int i = 0; i < fields.length; i++) {
            Label k = new Label(fields[i][0] + ":");
            k.setStyle("-fx-font-weight: bold; -fx-text-fill: " + DARK_BLUE + ";");
            Label v = new Label(notEmpty(fields[i][1]));
            v.setWrapText(true);
            grid.add(k, 0, i); grid.add(v, 1, i);
        }

        Button editBtn = new Button("Edit Case"); styleBlue(editBtn);
        editBtn.setOnAction(e -> { dlg.close(); showCaseForm(c, table); });
        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> dlg.close());

        box.getChildren().addAll(hdr, grid, new HBox(10, editBtn, closeBtn));
        dlg.setScene(new Scene(new ScrollPane(box), 540, 560));
        dlg.showAndWait();
    }

    private void showCaseForm(CaseRec existing, TableView<CaseRec> table) {
        boolean isEdit = existing != null;
        Stage dlg = dialog(isEdit ? "Edit Case - " + existing.getCaseID() : "Register New Case");

        VBox form = new VBox(10);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: white;");

        Label heading = new Label(isEdit ? "Edit Case" : "Case Registration");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;-fx-text-fill: " + DARK_BLUE + ";");

        String suggestedID = isEdit ? existing.getCaseID() : db.getNextCaseID();

        TextField caseIDFld = styledTextField("Case Number *", suggestedID);
        Label caseIDNote = smallNote("Auto-generated. You may change it.");

        ComboBox<String> typeCb = comboVal(existing != null ? existing.getCaseType() : "Criminal", "Criminal","Civil","Violations","Administrative","Others");

        ComboBox<String> statusCb = comboVal(existing != null ? existing.getCaseStatus() : "Active", "Active","Resolved","Dismissed");

        TextField natureFld = styledTextField("Nature of Case *",val(existing, existing == null ? "" : existing.getCaseNature()));
        TextField filedFld = styledTextField("Filing Date * (YYYY-MM-DD)", val(existing, existing == null ? "" : existing.getFiledDate()));
        TextField accusedFld = styledTextField("Accused Name *", val(existing, existing == null ? "" : existing.getAccused()));
        TextField complainantFld = styledTextField("Complainant Name *",val(existing, existing == null ? "" : existing.getComplainant()));
        TextField descFld = styledTextField("Description *", val(existing, existing == null ? "" : existing.getCaseDesc()));

        Label optLbl = smallNote("─── Optional Fields ───");

        List<String> prosecs = db.getProsecutorList();
        prosecs.add(0, "(Unassigned)");

        ComboBox<String> prosecCb = new ComboBox<>(FXCollections.observableArrayList(prosecs));
        String curProsec = (existing != null && existing.getProsecutor() != null) ? existing.getProsecutor() : "(Unassigned)";
        prosecCb.setValue(prosecs.contains(curProsec) ? curProsec : "(Unassigned)");

        TextField judgeFld= styledTextField("Judge (optional)", opt(existing == null ? null : existing.getJudge()));
        TextField hearingFld= styledTextField("Hearing Date (YYYY-MM-DD, optional)", opt(existing == null ? null : existing.getHearingDate()));
        TextField branchFld = styledTextField("Branch (optional)",opt(existing == null ? null : existing.getBranch()));
        TextField witnessFld = styledTextField("Witness (optional)", opt(existing == null ? null : existing.getWitness()));
        TextField evidenceFld = styledTextField("Evidence (optional)",opt(existing == null ? null : existing.getEvidence()));
        TextField verdictFld= styledTextField("Verdict (optional)", opt(existing == null ? null : existing.getVerdict()));

        Label errorLbl = new Label(""); errorLbl.setStyle("-fx-text-fill: " + RED + ";");

        Button saveBtn = new Button(isEdit ? "Save Changes" : "Register Case");
        styleRed(saveBtn); saveBtn.setPrefWidth(200);
        Button cancelBtn = new Button("Cancel"); cancelBtn.setOnAction(e -> dlg.close());

        form.getChildren().addAll( heading,
            label("Case Number *:"), caseIDFld, caseIDNote,
            label("Case Type *:"), typeCb,
            label("Case Status:"), statusCb,
            natureFld, filedFld, accusedFld, complainantFld, descFld, optLbl,
            label("Prosecutor:"), prosecCb,
            judgeFld, hearingFld, branchFld, witnessFld, evidenceFld, verdictFld,
            errorLbl, new HBox(12, saveBtn, cancelBtn)
        );

        saveBtn.setOnAction(e -> {

            if (caseIDFld.getText().trim().isEmpty()
                || natureFld.getText().trim().isEmpty()
                || filedFld.getText().trim().isEmpty()
                || accusedFld.getText().trim().isEmpty()
                || complainantFld.getText().trim().isEmpty()
                || descFld.getText().trim().isEmpty()) {
                errorLbl.setText("Please fill in all required (*) fields.");
                return;
            }

            String caseID = caseIDFld.getText().trim();
            String prosec = prosecCb.getValue().equals("(Unassigned)") ? null : prosecCb.getValue();

            boolean ok;
            if (isEdit) {
                db.updt_all(existing.getCaseID(),
                    typeCb.getValue(), natureFld.getText().trim(),
                    statusCb.getValue(), accusedFld.getText().trim(),
                    complainantFld.getText().trim(), prosec,
                    blank(judgeFld), filedFld.getText().trim(),
                    blank(hearingFld), blank(witnessFld), blank(evidenceFld),
                    blank(branchFld), blank(verdictFld), descFld.getText().trim(),
                    caseID);
                db.writeAuditLog(currentUser.getUsername(), "UPDATE_CASE", "Updated: " + existing.getCaseID());
                ok = true;
            } else {
                if (db.getCaseByCaseID(caseID) != null) {errorLbl.setText("Case ID already exists.");
                return;
                }

                ok = db.addCase(caseID, typeCb.getValue(),
                    natureFld.getText().trim(), statusCb.getValue(),
                    accusedFld.getText().trim(), complainantFld.getText().trim(),
                    prosec, blank(judgeFld), filedFld.getText().trim(),
                    blank(hearingFld), blank(witnessFld), blank(evidenceFld),
                    blank(branchFld), blank(verdictFld), descFld.getText().trim());

                if (ok){
                    db.writeAuditLog(currentUser.getUsername(),"REGISTER_CASE", caseID);
                }
            }

            if (ok) {
                dlg.close();
                if (table != null) refreshCaseTable(table, null, null, null);
            } else {
                errorLbl.setText("Failed to save. Check logs.");
            }
        });

        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        dlg.setScene(new Scene(sp, 520, 680));
        dlg.showAndWait();
    }

    private VBox buildSchedulePanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(24));

        Button addBtn = new Button("+ New Hearing"); styleRed(addBtn);
        HBox header = headerRow("Hearing Calendar", addBtn);

        TableView<Schedules> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-font-size: 13px;");

        TableColumn<Schedules, String> caseCol = new TableColumn<>("Case ID");
        caseCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCaseID()));
        caseCol.setPrefWidth(150);

        TableColumn<Schedules, String> dtCol = new TableColumn<>("Scheduled Date/Time");
        dtCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDatetime()));
        dtCol.setPrefWidth(180);

        TableColumn<Schedules, String> rsCol = new TableColumn<>("Rescheduled To");
        rsCol.setCellValueFactory(d -> new SimpleStringProperty(notEmpty(d.getValue().getReSched())));
        rsCol.setPrefWidth(180);

        TableColumn<Schedules, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(d -> new SimpleStringProperty(notEmpty(d.getValue().getReason())));
        reasonCol.setPrefWidth(200);

        TableColumn<Schedules, Void> actCol = new TableColumn<>("Action");
        actCol.setPrefWidth(130);
        actCol.setCellFactory(c -> new TableCell<>() {
            final Button rsBtn = smallBtn("Reschedule", BLUE);
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);

                if (empty) {
                    setGraphic(null);
                    return; 
                }

                rsBtn.setOnAction(e ->showRescheduleDialog(getTableView().getItems().get(getIndex()), table));
                setGraphic(rsBtn);
            }
        });

        table.getColumns().addAll(caseCol, dtCol, rsCol, reasonCol, actCol);
        table.setItems(FXCollections.observableArrayList(db.getUpcomingSchedules()));

        addBtn.setOnAction(e -> showAddScheduleDialog(table));

        VBox.setVgrow(table, Priority.ALWAYS);
        panel.getChildren().addAll(header, table);
        return panel;
    }

    private void showAddScheduleDialog(TableView<Schedules> table) {
        Stage dlg=dialog("Add Hearing Schedule");
        VBox form=whiteForm();

        form.getChildren().add(formHeading("New Hearing Schedule"));
        TextField caseIDFld = styledTextField("Case ID *", "");
        TextField datetimeFld = styledTextField("Date & Time * (YYYY-MM-DD HH:MM:SS)", "");

        Label err = errLabel();
        Button save = new Button("Save"); styleRed(save);
        Button cancel = new Button("Cancel"); cancel.setOnAction(e -> dlg.close());

        save.setOnAction(e -> {
            if (caseIDFld.getText().trim().isEmpty() || datetimeFld.getText().trim().isEmpty()) {
                err.setText("All fields required.");
                return;
            }

            if (db.getCaseByCaseID(caseIDFld.getText().trim()) == null) {
                err.setText("Case ID not found.");
                return;
            }

            db.addSched(caseIDFld.getText().trim(), datetimeFld.getText().trim(), currentUser.getUsername());
            db.writeAuditLog(currentUser.getUsername(), "ADD_SCHEDULE", "Case: " + caseIDFld.getText().trim());
            table.setItems(FXCollections.observableArrayList(db.getUpcomingSchedules()));
            dlg.close();
        });
        form.getChildren().addAll(caseIDFld, datetimeFld, err, new HBox(10, save, cancel));
        dlg.setScene(new Scene(form, 420, 280)); dlg.showAndWait();
    }

    private void showRescheduleDialog(Schedules s, TableView<Schedules> table) {
        Stage dlg = dialog("Reschedule - Case: " + s.getCaseID());
        VBox form = whiteForm();

        form.getChildren().add(formHeading("Reschedule Hearing"));
        TextField dtFld = styledTextField("New Date & Time (YYYY-MM-DD HH:MM:SS)", "");
        TextField reasonFld = styledTextField("Reason", "");

        Label err = errLabel();
        Button save = new Button("Save");
        styleRed(save);

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> dlg.close());

        save.setOnAction(e -> {
            if (dtFld.getText().trim().isEmpty()) { 
                err.setText("Date/time required.");
                return;
            }
            db.reschedule(s.getDbId(), dtFld.getText().trim(), reasonFld.getText().trim(), currentUser.getUsername());
            table.setItems(FXCollections.observableArrayList(db.getUpcomingSchedules()));
            dlg.close();
        });
        form.getChildren().addAll(dtFld, reasonFld, err, new HBox(10, save, cancel));
        dlg.setScene(new Scene(form, 420, 260)); dlg.showAndWait();
    }

    private VBox buildUsersPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(24));

        Button addBtn = new Button("+ Add User"); styleRed(addBtn);
        HBox header = headerRow("User Management", addBtn);

        TableView<User> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-font-size: 13px;");

        TableColumn<User, String> idCol = new TableColumn<>("User ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getId()));
        idCol.setPrefWidth(70);

        TableColumn<User, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        nameCol.setPrefWidth(180);

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole()));
        roleCol.setPrefWidth(110);

        TableColumn<User, String> statCol = new TableColumn<>("Status");
        statCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getIsActive()));
        statCol.setPrefWidth(80);
        statCol.setCellFactory(c -> new TableCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: " + ("Active".equals(item) ? "#1E8A44" : RED) + ";");
            }
        });

        TableColumn<User, Void> actCol = new TableColumn<>("Actions");
        actCol.setPrefWidth(210);

        actCol.setCellFactory(c -> new TableCell<>() {
            final Button toggleBtn = new Button();
            final Button pwBtn = smallBtn("Reset PW", BLUE);

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);

                if (empty) {
                    setGraphic(null);
                    return; 
                }

                User u = getTableView().getItems().get(getIndex());
                boolean active = "Active".equals(u.getIsActive());

                toggleBtn.setText(active ? "Deactivate" : "Activate");
                toggleBtn.setStyle("-fx-background-color: " + (active ? RED : "#1E8A44") + ";-fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 3 8;");
                
                toggleBtn.setOnAction(e -> {
                    if (active){
                        db.DeactivateUser(u.getUsername());
                    }else {
                        db.ReactivateUser(u.getUsername());
                    }
                    refreshUserTable(table);
                });
                pwBtn.setOnAction(e -> showResetPasswordDialog(u));
                HBox box = new HBox(6, toggleBtn, pwBtn);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        table.getColumns().addAll(idCol, nameCol, roleCol, statCol, actCol);
        refreshUserTable(table);
        addBtn.setOnAction(e -> showAddUserDialog(table));

        VBox.setVgrow(table, Priority.ALWAYS);
        panel.getChildren().addAll(header, table);
        return panel;
    }

    private void refreshUserTable(TableView<User> table) {
        table.setItems(FXCollections.observableArrayList(db.getAllUsers()));
    }

    private void showAddUserDialog(TableView<User> table) {
        Stage dlg = dialog("Add New User");
        VBox form = whiteForm();

        form.getChildren().add(formHeading("Register New User"));
        TextField usernameFld = styledTextField("Username *", "");
        TextField fullNameFld = styledTextField("Full Name *", "");

        ComboBox<String> roleCb = combo("Staff", "Staff", "Prosecutor", "Admin");

        PasswordField pwFld = new PasswordField();
        pwFld.setPromptText("Password *");
        styleInput(pwFld);

        Label err = errLabel();
        Button save = new Button("Create User");
        styleRed(save);
        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> dlg.close());

        save.setOnAction(e -> {
            if (usernameFld.getText().trim().isEmpty() || fullNameFld.getText().trim().isEmpty() || pwFld.getText().isEmpty()) {
                err.setText("All fields required."); 
                return; 
            }

            boolean ok = db.AddUser(usernameFld.getText().trim(), pwFld.getText(), roleCb.getValue(), fullNameFld.getText().trim());
            
            if (ok) {
                db.writeAuditLog(currentUser.getUsername(), "CREATE_USER", usernameFld.getText() + " [" + roleCb.getValue() + "]");
                refreshUserTable(table); dlg.close();
            } else {
                err.setText("Failed. Username may already exist."); 
            }

        });
        form.getChildren().addAll(usernameFld, fullNameFld, label("Role:"), roleCb, pwFld, err, new HBox(10, save, cancel));
        dlg.setScene(new Scene(form, 420, 340));
        dlg.showAndWait();
    }

    private void showResetPasswordDialog(User u) {

        Stage dlg = dialog("Reset Password - " + u.getUsername());

        VBox form = whiteForm();
        form.getChildren().add(formHeading("Reset Password for " + u.getFullName()));

        PasswordField pwFld = new PasswordField();
        pwFld.setPromptText("New Password *");
        styleInput(pwFld);

        PasswordField confirmFld = new PasswordField();
        confirmFld.setPromptText("Confirm Password *");
        styleInput(confirmFld);

        Label err = errLabel();
        Button save = new Button("Reset");
        styleRed(save);
        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> dlg.close());

        save.setOnAction(e -> {
            if (!pwFld.getText().equals(confirmFld.getText())) {
                err.setText("Passwords don't match.");
                return;
            }

            if (pwFld.getText().length() < 6) {
                err.setText("Minimum 6 characters.");
                return; 
            }

            db.UpdatePassword(u.getUsername(), pwFld.getText());
            db.writeAuditLog(currentUser.getUsername(), "RESET_PW", "Reset for: " + u.getUsername());
            dlg.close();
        });
        form.getChildren().addAll(pwFld, confirmFld, err, new HBox(10, save, cancel));
        dlg.setScene(new Scene(form, 400, 260));
        dlg.showAndWait();
    }

    private VBox buildAuditLogPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(24));
        panel.getChildren().add(sectionHeading("Audit Log"));

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-font-size: 13px;");

        String[][] cols = {{"Time","0","160"},{"User","1","140"},{"Action","2","160"},{"Details","3","300"}};
        for (String[] c : cols) {
            int idx = Integer.parseInt(c[1]);
            TableColumn<String[], String> col = new TableColumn<>(c[0]);
            col.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[idx]));
            col.setPrefWidth(Integer.parseInt(c[2]));
            table.getColumns().add(col);
        }
        table.setItems(FXCollections.observableArrayList(db.getAuditLogs()));

        VBox.setVgrow(table, Priority.ALWAYS);
        panel.getChildren().add(table);
        return panel;
    }

    private void styleRed(Button b) {
        String s = "-fx-background-color:%s;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:5;-fx-padding:8 20;-fx-cursor:hand;";
        b.setStyle(String.format(s, RED));
        b.setOnMouseEntered(e -> b.setStyle(String.format(s, "#b01419")));
        b.setOnMouseExited(e  -> b.setStyle(String.format(s, RED)));
    }

    private void styleBlue(Button b) {
        String s = "-fx-background-color:%s;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:5;-fx-padding:8 20;-fx-cursor:hand;";
        b.setStyle(String.format(s, BLUE));
        b.setOnMouseEntered(e -> b.setStyle(String.format(s, DARK_BLUE)));
        b.setOnMouseExited(e  -> b.setStyle(String.format(s, BLUE)));
    }

    private void styleInput(Control f) {
        f.setStyle("-fx-border-color:#CCCCCC;-fx-border-radius:4;-fx-background-radius:4;-fx-padding:8;-fx-font-size:13px;");
        f.setMaxWidth(Double.MAX_VALUE);
    }

    private TextField styledTextField(String prompt, String value) {
        TextField f = new TextField(value);
        f.setPromptText(prompt);
        styleInput(f);
        return f;
    }

    private Button smallBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-background-radius:4;-fx-padding:3 8;");
        return b;
    }

    private <T> TableColumn<CaseRec, T> plainCol(String title, String prop, int width) {
        TableColumn<CaseRec, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setPrefWidth(width);
        return col;
    }

    private ComboBox<String> combo(String defaultVal, String... options) {
        ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(options));
        cb.setValue(defaultVal);
        return cb;
    }

    private ComboBox<String> comboVal(String value, String... options) {
        ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(options));
        cb.setValue(value);
        return cb;
    }

    private HBox headerRow(String title, Button actionBtn) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = sectionHeading(title);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        row.getChildren().addAll(lbl, sp, actionBtn);
        return row;
    }

    private Label sectionHeading(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:" + DK_GRAY + ";");
        return l;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + DK_GRAY + ";");
        return l;
    }

    private Label label(String text) { return new Label(text); }

    private Label smallNote(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:11px;-fx-text-fill:gray;");
        return l;
    }

    private Label formHeading(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + DARK_BLUE + ";");
        return l;
    }

    private Label errLabel() {
        Label l = new Label("");
        l.setStyle("-fx-text-fill:" + RED + ";");
        return l;
    }

    private VBox whiteForm() {
        VBox form = new VBox(12);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color:white;");
        return form;
    }

    private Stage dialog(String title) {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle(title);
        return dlg;
    }

    private String statusColor(String status) {
        return switch (status != null ? status : "") {
            case "Active" -> BLUE;
            case "Resolved" -> "#1E8A44";
            case "Dismissed" -> RED;
            default -> "#888";
        };
    }

    private String notEmpty(String s) {
        return (s != null && !s.isEmpty()) ? s : "—";
    }

    private String opt(String s){ 
        return s != null ? s : ""; 
    }

    private String blank(TextField f) {
        String t = f.getText().trim();
        return t.isEmpty() ? null : t;
    }

    private String val(CaseRec c, String fallback) {
        return c != null ? fallback : "";
    }

    private void alert(String title, String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.YES;
    }
}
