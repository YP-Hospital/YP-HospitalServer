package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer extends Thread {

    public static final int SERVER_PORT = 8080;
    private boolean running = false;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    /**
     * Method to send the messages from server to client
     * @param message the message sent by the server
     */
    public void sendMessage (String message) {
        try {
            if (outputStream != null) {
                outputStream.writeUTF(message);
                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();

        running = true;

        try {
            System.out.println("S: Connecting...");
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Waiting for a client...");
            Socket client = serverSocket.accept();
            System.out.println("S: Receiving...");
            InputStream sin = client.getInputStream();
            OutputStream sout = client.getOutputStream();

            inputStream = new DataInputStream(sin);
            outputStream = new DataOutputStream(sout);
            try {
                while (running) {
                    String message = inputStream.readUTF();
                    System.out.println("Message from client: " + message);
                }

            } catch (Exception e) {
                System.out.println("S: Error");
                e.printStackTrace();
            } finally {
                client.close();
                System.out.println("S: Done.");
            }

        } catch (Exception e) {
            System.out.println("S: Error");
            e.printStackTrace();
        }
    }

    public void stopServer() {
        running = false;
    }
}
