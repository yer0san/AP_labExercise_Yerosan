package tcp;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try {
            Socket csock = new Socket("localhost", 1234);
            System.out.println("Connected to server: " + csock.getRemoteSocketAddress());

            InputStream in = csock.getInputStream();
            OutputStream out = csock.getOutputStream();

            Scanner sc = new Scanner(System.in);

            Thread reader = new Thread(() -> {
                try {
                    byte[] buff = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = in.read(buff)) != -1) {
                        String msg = new String(buff, 0, bytesRead);
                        System.out.println("server: " + msg);
                    }

                    System.out.println("Server disconnected.");
                } catch (IOException e) {
                    System.out.println("Connection closed.");
                }
            });

            reader.start();

            while (true) {
                String me = sc.nextLine();

                if (me.equalsIgnoreCase("exit")) {
                    break;
                }

                out.write(me.getBytes());
                out.flush();
            }

            csock.close();
            System.out.println("Client shut down.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}