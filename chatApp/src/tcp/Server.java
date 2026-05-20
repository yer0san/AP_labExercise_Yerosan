package tcp;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    // Thread-safe list of all connected client output streams
    private static final List<OutputStream> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket ssock = new ServerSocket(1234)) {
            System.out.println("Server listening on port " + ssock.getLocalPort());

            // Server can broadcast to all clients from the console
            new Thread(() -> {
                Scanner sc = new Scanner(System.in);
                while (sc.hasNextLine()) {
                    String msg = sc.nextLine();
                    if (msg.equalsIgnoreCase("quit")) break;
                    broadcast("[SERVER]: " + msg, null);  // null = send to everyone
                }
            }).start();

            while (true) {
                Socket csock = ssock.accept();
                System.out.println("New client: " + csock.getRemoteSocketAddress());
                new Thread(new ClientHandler(csock)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Sends a message to every connected client
    static void broadcast(String message, OutputStream sender) {
        byte[] data = (message + "\n").getBytes();
        for (OutputStream out : clients) {
            if (out == sender) continue;
            try {
                out.write(data);
                out.flush();
            } catch (IOException e) {

            }
        }
    }

    static class ClientHandler implements Runnable {

        private final Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                InputStream  in  = socket.getInputStream();
                OutputStream out = socket.getOutputStream()
            ) {
                clients.add(out);

                byte[] buff = new byte[1024];
                int bytesRead;

                while ((bytesRead = in.read(buff)) != -1) {
                    String msg = new String(buff, 0, bytesRead).trim();
                    System.out.println("[" + socket.getRemoteSocketAddress() + "]: " + msg);
                }

                System.out.println("Client disconnected: " + socket.getRemoteSocketAddress());

            } catch (IOException e) {
                System.out.println("Connection lost: " + socket.getRemoteSocketAddress());
            } finally {
                // always clean up
                try { socket.close(); } catch (IOException ignored) {}

            }
        }
    }
}