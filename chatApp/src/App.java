import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class App extends Application {

    private static final int PORT = 1234;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    private TextArea    chatLog;
    private ListView<String> clientListView;
    private TextField   inputField;
    private Label       statusLabel;
    private Label       clientCountLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        // left: client list
        clientCountLabel = new Label("0 connected");
        clientCountLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        clientListView = new ListView<>();
        clientListView.setPlaceholder(new Label("No clients yet"));
        VBox.setVgrow(clientListView, Priority.ALWAYS);

        VBox leftPane = new VBox(6, bold("Connected Clients"), clientCountLabel, clientListView);
        leftPane.setPadding(new Insets(12));
        leftPane.setMinWidth(190);

        // right: chat log
        chatLog = new TextArea();
        chatLog.setEditable(false);
        chatLog.setWrapText(true);
        chatLog.setFont(Font.font("Monospaced", 13));
        chatLog.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #d4d4d4;");
        VBox.setVgrow(chatLog, Priority.ALWAYS);

        // input bar
        inputField = new TextField();
        inputField.setPromptText("Broadcast to all clients…");
        inputField.setOnAction(e -> sendBroadcast());
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button sendBtn = new Button("Broadcast");
        sendBtn.setDefaultButton(true);
        sendBtn.setOnAction(e -> sendBroadcast());
        sendBtn.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox inputBar = new HBox(8, inputField, sendBtn);
        inputBar.setPadding(new Insets(8, 0, 0, 0));
        inputBar.setAlignment(Pos.CENTER_LEFT);

        VBox rightPane = new VBox(6, bold("Chat Log"), chatLog, inputBar);
        rightPane.setPadding(new Insets(12));

        // status bar
        statusLabel = new Label("Starting…");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setStyle("-fx-background-color: #f3f3f3; -fx-padding: 4 8;");

        // root
        SplitPane split = new SplitPane(leftPane, rightPane);
        split.setDividerPositions(0.28);
        VBox.setVgrow(split, Priority.ALWAYS);

        VBox root = new VBox(split, statusLabel);

        stage.setTitle("TCP Server  –  port " + PORT);
        stage.setScene(new Scene(root, 800, 520));
        stage.setOnCloseRequest(e -> System.exit(0));
        stage.show();

        startServer();
    }

    //server logic 
    private void startServer() {
        new Thread(() -> {
            try (ServerSocket ssock = new ServerSocket(PORT)) {
                setStatus("✓  Listening on port " + PORT);
                while (true) {
                    Socket csock = ssock.accept();
                    ClientHandler handler = new ClientHandler(csock);
                    clients.add(handler);
                    refreshClientList();
                    log("system", csock.getRemoteSocketAddress() + " connected");
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                setStatus("✗  Server error: " + e.getMessage());
            }
        }, "accept-thread").start();
    }

    private void broadcast(String message, OutputStream sender) {
        byte[] data = (message + "\n").getBytes();
        for (ClientHandler h : clients) {
            if (h.out == sender) continue;
            try {
                h.out.write(data);
                h.out.flush();
            } catch (IOException ignored) {}
        }
    }

    private void sendBroadcast() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;
        inputField.clear();
        broadcast("[SERVER]: " + msg, null);
        log("server", msg);
    }

    private void log(String source, String msg) {
        String line = String.format("[%s] %-8s  %s%n",
                LocalTime.now().format(TIME_FMT), source.toUpperCase(), msg);
        Platform.runLater(() -> {
            chatLog.appendText(line);
            chatLog.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void refreshClientList() {
        Platform.runLater(() -> {
            clientListView.getItems().setAll(
                clients.stream().map(h -> h.address).toList()
            );
            clientCountLabel.setText(clients.size() + " connected");
        });
    }

    private void setStatus(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    private static Label bold(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 13));
        return l;
    }

    class ClientHandler implements Runnable {

        final Socket socket;
        final String address;
        final OutputStream out;

        ClientHandler(Socket socket) throws IOException {
            this.socket  = socket;
            this.address = socket.getRemoteSocketAddress().toString();
            this.out     = socket.getOutputStream();
        }

        @Override
        public void run() {
            try (InputStream in = socket.getInputStream()) {
                byte[] buff = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buff)) != -1) {
                    String msg = new String(buff, 0, bytesRead).trim();
                    log(address, msg);
                    broadcast(msg, out);
                }
            } catch (IOException e) {
                log("system", "Connection lost: " + address);
            } finally {
                clients.remove(this);
                refreshClientList();
                log("system", address + " disconnected");
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}