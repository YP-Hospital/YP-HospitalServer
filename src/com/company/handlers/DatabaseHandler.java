package com.company.handlers;

import java.sql.*;
import java.util.List;
import java.util.Map;

public class DatabaseHandler {
    private Connection connect = null;
    private PreparedStatement preparedStatement = null; // PreparedStatements can use variables and are more efficient
    PropertiesHandler databaseConfig;

    public DatabaseHandler() {
        try {
            Class.forName("com.mysql.jdbc.Driver"); // This will load the MySQL driver, each DB has its own driver
            databaseConfig = new PropertiesHandler("databaseConfig.properties");
            connect = DriverManager.getConnection(databaseConfig.getPropertyByName("host"), databaseConfig.getProperty());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTable() throws SQLException {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS users"
                + "  (id           INTEGER,"
                + "               INTEGER,"
                + "   number          INTEGER,"
                + "   value           INTEGER,"
                + "   card_count           INTEGER,"
                + "   player_name     VARCHAR(50),"
                + "   player_position VARCHAR(20))";

        Statement stmt = connect.createStatement();
        stmt.execute(sqlCreate);
    }

    public void insert(List<String> dataFromClient) {
        String statement = getInsertQueryStatement(dataFromClient);
        workWithPreparedStatement(statement, dataFromClient);
    }

    public void delete(List<String> dataFromClient) {
        String statement = getDeleteQueryStatement(dataFromClient);
        workWithPreparedStatement(statement, dataFromClient);
    }

    public void update(List<String> dataFromClient) {
        String statement = getUpdateQueryStatement(dataFromClient);
        workWithPreparedStatement(statement, dataFromClient);
    }

    private void workWithPreparedStatement(String statement, List<String> dataFromClient) {
        try {
            preparedStatement = connect
                    .prepareStatement(statement);
            for (int i = 1; i < dataFromClient.size(); i++) {
                preparedStatement.setString(i, dataFromClient.get(i));
            }
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getUpdateQueryStatement(List<String> dataFromClient) {
        return  "update " + databaseConfig.getPropertyByName("schemaName") + "."
                        + dataFromClient.get(0) + " set " + dataFromClient.get(1) + " = ? where id = ?";
    }

    public String getInsertQueryStatement(List<String> dataFromClient) {
        String statement = "insert into " + databaseConfig.getPropertyByName("schemaName") + "." + dataFromClient.get(0) + " values (default";
        for (int i = 0; i < dataFromClient.size() - 1; i++) {
            statement += ", ?";
        }
        statement += ")";
        return statement;
    }

    private String getDeleteQueryStatement(List<String> dataFromClient) {
        return  "delete from " + databaseConfig.getPropertyByName("schemaName") + "." + dataFromClient.get(0) + " where id= ? ; ";
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
