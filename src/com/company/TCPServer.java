package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer extends Thread {

    public static final int SERVER_PORT = 8080;
    public static final String STOP_WORDS = "This is a stop message";
    private boolean running = false;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private String message = "";

    /**
     * Method to send the messages from server to client
     * @param message the message sent by the server
     */
    public void sendMessage(String message) {
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
        File dataBase = new File("hospital.db");
        try {
            System.out.println("S: Connecting...");
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            try {
                while (running) {
                    clientRequestHandling(dataBase, serverSocket);
                }
            } catch (Exception e) {
                System.out.println("S: Error");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("S: Error");
            e.printStackTrace();
        }
    }

    private void clientRequestHandling(File dataBase, ServerSocket serverSocket) throws IOException {
        Socket client;
        client = getClient(serverSocket);
        getMessages();
        updateDataBaseOnClient(dataBase, client);
        client.close();
    }

    private void updateDataBaseOnClient(File dataBase, Socket client) throws IOException {
        byte[] fileInBytes = new byte[(int) dataBase.length()];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(dataBase));
        bis.read(fileInBytes, 0, fileInBytes.length);
        OutputStream os = client.getOutputStream();
        os.write(fileInBytes, 0, fileInBytes.length);
        os.flush();
        os.close();
        bis.close();
    }

    private void getMessages() throws IOException {
        while (!message.equals(STOP_WORDS)) {
            message = inputStream.readUTF();
            System.out.println("Message from client: " + message);
        }
        message = "";
    }

    private Socket getClient(ServerSocket serverSocket) throws IOException {
        System.out.println("Waiting for a client...");
        Socket client = serverSocket.accept();
        System.out.println("S: Receiving...");
        InputStream sin = client.getInputStream();
        OutputStream sout = client.getOutputStream();
        inputStream = new DataInputStream(sin);
        outputStream = new DataOutputStream(sout);
        return client;
    }

    public void stopServer() {
        running = false;
    }
}
