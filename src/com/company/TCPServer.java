package com.company;

import com.company.handlers.DatabaseHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TCPServer extends Thread {

    public static final String separator = "]\\[";
    public static final int SERVER_PORT = 8080;
    public static final String STOP_WORDS = "This is a stop message";
    private boolean running = false;
    private DatabaseHandler databaseHandler;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private String message = "";
    private String answer = "";
    private static  String messageFromAnotherClass = "";
    private List<String> convertedDataFromClient;

    public static String getMessageFromAnotherClass() {
        return messageFromAnotherClass;
    }

    public static void setMessageFromAnotherClass(String messageFromAnotherClass) {
        TCPServer.messageFromAnotherClass = messageFromAnotherClass;
    }

    /**
     * Method to send the messages from server to client
     *
     * @param message the message sent by the server
     */
    public void sendMessage(String message) {
        System.out.println("My answer: " + message);
        try {
            if (outputStream != null) {
                outputStream.writeUTF(message);
                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendAnswer() {
        sendMessage(answer);
        answer = "";
    }

    private void sendMessageFromAnotherClass() {
        if (!messageFromAnotherClass.isEmpty()) {
            sendMessage(messageFromAnotherClass);
            messageFromAnotherClass = "";
        }
    }

    @Override
    public void run() {
        super.run();
        running = true;
        databaseHandler = new DatabaseHandler();
        try {
            System.out.println("S: Connecting...");
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            try {
                while (running) {
                    clientRequestHandling(serverSocket);
                }
            } catch (Exception e) {
                System.out.println("S: Error in request handling");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("S: Error in serverSocket");
            e.printStackTrace();
        }
    }

    private void clientRequestHandling(ServerSocket serverSocket) throws IOException {
        Socket client;
        client = getClient(serverSocket);
        workWithDatabaseHandler();
        sendAnswer();
        sendMessageFromAnotherClass();
        sendMessage(STOP_WORDS);
        client.close();
    }

    private void workWithDatabaseHandler() throws IOException {
        String messageFromClient;
        messageFromClient = getMessages();
        if (!messageFromClient.equals(STOP_WORDS)) {
            String functionToCall = convertMessagesForDatabaseHandler(messageFromClient);
            callingNeededFunctionInDatabaseHandler(functionToCall);
        }
    }

    private void callingNeededFunctionInDatabaseHandler(String functionToCall) {
        switch (functionToCall) {
            case "insert":
                answer = String.valueOf(databaseHandler.insert(convertedDataFromClient));
                break;
            case "delete":
                answer = String.valueOf(databaseHandler.delete(convertedDataFromClient));
                break;
            case "update":
                answer = String.valueOf(databaseHandler.update(convertedDataFromClient));
                break;
            case "select":
                answer = databaseHandler.select(convertedDataFromClient);
                sendToClientIsSelectSuccess(answer);
                break;
            default:
                System.out.println("Data isn't correct");
                break;
        }
    }

    private void sendToClientIsSelectSuccess(String answer) {
        String message = String.valueOf(!answer.isEmpty());
        sendMessage(message);
    }

    private String convertMessagesForDatabaseHandler(String messageFromClient) {
        List<String> splitMessageFromClient = Arrays.asList(messageFromClient.split(separator));
        splitMessageFromClient.removeIf(s -> s.equals(""));
        String methodName = splitMessageFromClient.get(0);
        setConvertedDataFromClient(splitMessageFromClient);
        return methodName;
    }

    private void setConvertedDataFromClient(List<String> splitMessageFromClient) {
        convertedDataFromClient = new ArrayList<>();
        convertedDataFromClient.addAll(splitMessageFromClient);
        convertedDataFromClient.remove(0);
    }

    private String getMessages() throws IOException {
        List<String> messages = new ArrayList<>();
        while (!message.equals(STOP_WORDS)) {
            message = inputStream.readUTF();
            messages.add(message);
            System.out.println("Message from client: " + message);
        }
        message = "";
        return messages.get(0);
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
