package com.company.handlers;

import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {
    private Connection connect = null;
    private PreparedStatement preparedStatement = null; // PreparedStatements can use variables and are more efficient
    private ResultSet resultSet = null;
    private PropertiesHandler databaseConfig;

    public DatabaseHandler() {
        try {
            Class.forName("com.mysql.jdbc.Driver"); // This will load the MySQL driver, each DB has its own driver
            databaseConfig = new PropertiesHandler("databaseConfig.properties");
            connect = DriverManager.getConnection(databaseConfig.getPropertyByName("host"), databaseConfig.getProperty());
            createUsersTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createUsersTable() throws SQLException {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS users  "
                + "  (id           INT UNSIGNED NOT NULL PRIMARY KEY UNIQUE AUTO_INCREMENT,"
                + "   login        VARCHAR(50) NOT NULL UNIQUE ,"
                + "   password     VARCHAR(225) NOT NULL ,"
                + "   name         VARCHAR(225) NOT NULL ,"
                + "   role         VARCHAR(20) NOT NULL,"
                + "   age          INTEGER,"
                + "   phone        VARCHAR(50) NOT NULL,"
                + "   doctor_id    INT UNSIGNED NOT NULL) CHARACTER SET = utf8 ";

        Statement stmt = connect.createStatement();
        stmt.execute(sqlCreate);
    }

    public String select(List<String> dataFromClient) {
        Integer n = dataFromClient.indexOf("where");
        List<String> fieldsNames, values = null, forStatement;
        if (n != -1) {
            fieldsNames = dataFromClient.subList(1, n);
            Integer valuesNumbers = (dataFromClient.size()-n)/2;
            values = dataFromClient.subList(dataFromClient.size()-valuesNumbers-1, dataFromClient.size());
            forStatement = dataFromClient.subList(0, dataFromClient.size() - valuesNumbers);
        } else {
            fieldsNames = dataFromClient.subList(1, dataFromClient.size());
            forStatement = new ArrayList<>(dataFromClient);
        }
        String statement = getSelectQueryStatement(forStatement);
        workWithPreparedStatement(statement, values);
        return resultSetToString(fieldsNames);
    }

    public Boolean insert(List<String> dataFromClient) {
        String statement = getInsertQueryStatement(dataFromClient);
        return workWithPreparedStatement(statement, dataFromClient);
    }

    public Boolean delete(List<String> dataFromClient) {
        String statement = getDeleteQueryStatement(dataFromClient);
        return  workWithPreparedStatement(statement, dataFromClient);
    }

    public Boolean update(List<String> dataFromClient) {
        int n = dataFromClient.size()/2;
        List<String> forStatement = dataFromClient.subList(0, n);
        String statement = getUpdateQueryStatement(forStatement);
        return workWithPreparedStatement(statement, dataFromClient.subList(n-1, dataFromClient.size()));
    }

    @NotNull
    private Boolean workWithPreparedStatement(String statement, List<String> dataFromClient) {
        try {
            preparedStatement = connect
                    .prepareStatement(statement);
            if (dataFromClient != null) {
                for (int i = 1; i < dataFromClient.size(); i++) {
                    preparedStatement.setString(i, dataFromClient.get(i));
                }
            }
            if (statement.startsWith("select")) {
                resultSet = preparedStatement.executeQuery();
            } else {
                preparedStatement.executeUpdate();
            }
            return  true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getUpdateQueryStatement(List<String> dataFromClient) {
        String statement =  "update " + databaseConfig.getPropertyByName("schemaName") + "."
                        + dataFromClient.get(0) + " set ";
        for (int i = 1; i < dataFromClient.size(); i++) {
            if (i != 1) {
                statement += ", ";
            }
            statement += dataFromClient.get(i) + " = ?";
        }
        statement += " where id = ?";
        return statement;
    }


    private String getSelectQueryStatement(List<String> dataFromClient) {
        Integer i;
        String statement = "select";
        for (i = 1; i < dataFromClient.size() && !dataFromClient.get(i).equals("where"); i++) {
            if (i > 1) {
                statement += ",";
            }
            statement += " " + dataFromClient.get(i);
        }
        statement += " from " + databaseConfig.getPropertyByName("schemaName") + "." + dataFromClient.get(0);
        if (dataFromClient.contains("where")) {
            statement += " where";
        }
        for (int j = i+1; j < dataFromClient.size(); j++) {
            if (!statement.endsWith("where")) {
                statement += " and";
            }
            statement += " " + dataFromClient.get(j) + "=?";
        }
        statement += ";";
        return statement;
    }

    private String getInsertQueryStatement(List<String> dataFromClient) {
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


    private String resultSetToString(List<String> fieldsNames) {
        Integer i = 0;
        String result = "";
        try {
            while (resultSet.next()) {
                if (i == 0) {
                    for (String field : fieldsNames) {
                        result += field + " ";
                    }
                }
                result += i++ +".";
                if (!fieldsNames.get(0).equals("*")) {
                    for (String field : fieldsNames) {
                        result += " " + resultSet.getString(field);
                    }
                } else {
                    result += " " + resultSet.getString("id")
                            + " " + resultSet.getString("login")
                            + " " + resultSet.getString("password")
                            + " " + resultSet.getString("name")
                            + " " + resultSet.getString("role")
                            + " " + resultSet.getString("age")
                            + " " + resultSet.getString("phone")
                            + " " + resultSet.getString("doctor_id");
                }
                result += " ";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
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
