
import java.sql.Connection;
// import java.sql.DriverManager;
// import java.sql.SQLException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.rmi.Naming;

import shared.HelperInterface;

/*
javac --module-path /home/dee/Downloads/openjfx-25.0.1_linux-x64_bin-sdk/javafx-sdk-25.0.1/lib --add-modules javafx.controls -cp "../lib/mariadb-java-client-3.5.8.jar" -d out university/Student.java university/Teacher.java App.java && java --module-path /home/dee/Downloads/openjfx-25.0.1_linux-x64_bin-sdk/javafx-sdk-25.0.1/lib --add-modules javafx.controls -cp "out:../lib/mariadb-java-client-3.5.8.jar" App
Not using this no-mo my friend, we got maven in this bihh
*/

public class App extends Application {

    private HelperInterface stub;

    private TextField s_nameField, s_depField, s_secField, s_yearField;
    private TextField t_nameField, t_depField, t_secField, t_yearField;
    private Label s_msgLabel, t_msgLabel;

    @Override
    public void start(Stage stage) {
        
        connectToServer();

        connectToDatabase();

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Student tab
        s_nameField = field("Full name");
        s_depField  = field("e.g. SWE");
        s_secField  = field("e.g. A");
        s_yearField = field("1 - 5");
        s_msgLabel  = new Label();

        Button sBtn = submitButton("Add Student", "#4ade80");
        sBtn.setOnAction(e -> handleAdd(
            s_nameField, s_depField, s_secField, s_yearField, s_msgLabel, "student"
        ));

        // Teacher tab
        t_nameField = field("Full name");
        t_depField  = field("e.g. SWE");
        t_secField  = field("e.g. A");
        t_yearField = field("Years of exp.");
        
        t_msgLabel  = new Label();

        Button tBtn = submitButton("Add Teacher", "#a78bfa");
        tBtn.setOnAction(e -> handleAdd(
            t_nameField, t_depField, t_secField, t_yearField, t_msgLabel, "teacher"
        ));

        tabPane.getTabs().addAll(
            new Tab(" Student", buildForm(
                new String[]{"Name", "Department", "Section", "Year"},
                new TextField[]{s_nameField, s_depField, s_secField, s_yearField},
                sBtn, s_msgLabel
            )),
            new Tab(" Teacher", buildForm(
                new String[]{"Name", "Department", "Section", "Exp. Years"},
                new TextField[]{t_nameField, t_depField, t_secField, t_yearField},
                tBtn, t_msgLabel
            ))
        );

        stage.setScene(new Scene(tabPane, 360, 320));
        stage.setTitle("University Manager");
        stage.setResizable(false);
        stage.show();
    }

    private Pane buildForm(String[] labels, TextField[] fields, Button btn, Label msg) {
        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(12);
        for (int i = 0; i < labels.length; i++) {
            Label lbl = new Label(labels[i]);
            lbl.setMinWidth(80);
            grid.add(lbl,      0, i);
            grid.add(fields[i], 1, i);
        }
        VBox root = new VBox(14, grid, btn, msg);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.TOP_LEFT);
        return root;
    }

    private void handleAdd(TextField nameF, TextField depF, TextField secF,
                           TextField yearF, Label msg, String type) {
        String name = nameF.getText().trim();
        String dep  = depF.getText().trim();
        String sec  = secF.getText().trim();
        int year;

        if (name.isEmpty() || dep.isEmpty() || sec.isEmpty()) {
            setMsg(msg, "All fields are required.", false);
            return;
        }
        try {
            year = Integer.parseInt(yearF.getText().trim());
        } catch (NumberFormatException e) {
            setMsg(msg, "Year must be a number.", false);
            return;
        }

        boolean ok;

        if (type.equals("student")) 
            ok = addStudent(name, dep, sec, year);

        else                        
            ok = addTeacher(name, dep, sec, year);

        if (ok) {
            setMsg(msg, "✔ " + name + " added!", true);
            nameF.clear(); depF.clear(); secF.clear(); yearF.clear();
        } else {
            setMsg(msg, "✘ Failed to save. Check DB.", false);
        }
    }

    private boolean addStudent(String name, String dep, String sec, int year){
        try {
            stub.addStudentToDb(name, dep, sec, year);
            return true;

        }catch (Exception e){
            return false;
        }
    }
    
    private boolean addTeacher(String name, String dep, String sec, int year){
        try {
            stub.addTeacherToDb(name, dep, sec, year);
            return true;

        }catch (Exception e){
            return false;
        }
    }


    private void connectToDatabase() {
        try {
            stub.connectToDatabase();
            System.out.println("DB connected.");
        } catch (Exception e) {
            System.out.println("DB failed: " + e.getMessage());
            Platform.runLater(() -> new Alert(
                Alert.AlertType.ERROR, "Could not connect to DB.\n" + e.getMessage()
            ).showAndWait());
        }
    }
    // A WORKING RMI baby, i'm so proud of myself for this design hehe
    private void connectToServer() {
        try {
            stub = (HelperInterface) Naming.lookup("rmi://localhost/helper");

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        Connection connection = stub.getConn();
        if (connection != null && !connection.isClosed()) 
            connection.close();
    }

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(180);
        return tf;
    }

    private Button submitButton(String text, String color) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(
            "-fx-background-color: " + color + "; -fx-text-fill: #1e1e2e;" +
            "-fx-font-weight: bold; -fx-background-radius: 6;" +
            "-fx-cursor: hand; -fx-padding: 8;"
        );
        return btn;
    }

    private void setMsg(Label lbl, String text, boolean ok) {
        lbl.setText(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (ok ? "green" : "red") + ";");
    }

    public static void main(String[] args) { 
        launch(args); 
    }
}