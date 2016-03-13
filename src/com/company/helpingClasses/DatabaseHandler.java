package com.company.helpingClasses;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseHandler {
    private Connection connect = null;
    private PreparedStatement preparedStatement = null; // PreparedStatements can use variables and are more efficient
    public DatabaseHandler() {
        try {
            Class.forName("com.mysql.jdbc.Driver"); // This will load the MySQL driver, each DB has its own driver
            PropertiesHandler databaseConfig = new PropertiesHandler("res/databaseConfig.properties");
            connect = DriverManager.getConnection(databaseConfig.getPropertyByName("host"), databaseConfig.getProperty());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insert() {
        try {
            preparedStatement = connect
                    .prepareStatement("insert into  hospital.users values (default, ?)");
            preparedStatement.setString(1, "Test");
//            preparedStatement.setString(2, "TestEmail");
//            preparedStatement.setString(3, "TestWebpage");
//            preparedStatement.setDate(4, new java.sql.Date(2009, 12, 11));
//            preparedStatement.setString(5, "TestSummary");
//            preparedStatement.setString(6, "TestComment");
            preparedStatement.executeUpdate();

//            // Remove again the insert comment
//            preparedStatement = connect
//                    .prepareStatement("delete from feedback.comments where myuser= ? ; ");
//            preparedStatement.setString(1, "Test");
//            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            if (connect != null) {
                connect.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
