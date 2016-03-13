package com.company;

import com.company.helpingClasses.DatabaseHandler;
import com.company.helpingClasses.TCPServer;

public class Main {

    public static void main(String[] args) {
//        TCPServer server = new TCPServer();
//        server.start();
        DatabaseHandler databaseHandler = new DatabaseHandler();
        databaseHandler.insert();
    }
}
