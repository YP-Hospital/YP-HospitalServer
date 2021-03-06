package com.company.handlers;

import com.company.Crypto.ExtraCrypto;
import com.company.Crypto.PKI;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseHandler {
    public final static String EMPTY = "empty";
    private int tableNameIndex = 0;
    private int startFieldsNames = 1;
    private String separator = "][";
    public static String separatorForSplit = "]\\[";
    private Connection connect = null;
    private PreparedStatement preparedStatement = null; // PreparedStatements can use variables and are more efficient
    private ResultSet resultSet = null;
    private PropertiesHandler databaseConfig;

    public DatabaseHandler() {
        try {
            Class.forName("com.mysql.jdbc.Driver"); // This will load the MySQL driver, each DB has its own driver
            databaseConfig = new PropertiesHandler("databaseConfig.properties");
            connect = DriverManager.getConnection(databaseConfig.getPropertyByName("host"), databaseConfig.getProperty());
            createTables();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        createUsersTable();
        createDiseaseHistoryTable();
        createCertificateTable();
        createSignatureTable();
    }

    private void createUsersTable() throws SQLException {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS users  "
                + "  (id           INT UNSIGNED NOT NULL PRIMARY KEY UNIQUE AUTO_INCREMENT,"
                + "   login        VARCHAR(50)  NOT NULL UNIQUE ,"
                + "   password     VARCHAR(225) NOT NULL ,"
                + "   name         VARCHAR(225) NOT NULL ,"
                + "   role         VARCHAR(20)  NOT NULL,"
                + "   age          INTEGER,"
                + "   phone        VARCHAR(50)  NOT NULL,"
                + "   doctor_id    INT UNSIGNED NOT NULL) "
                + "   CHARACTER SET = utf8 ";

        Statement stmt = connect.createStatement();
        stmt.execute(sqlCreate);
    }

    private void createDiseaseHistoryTable() throws SQLException {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS disease_histories  "
                + "  (id                          INT UNSIGNED  NOT NULL PRIMARY KEY UNIQUE AUTO_INCREMENT,"
                + "   title                       VARCHAR(50)   NOT NULL UNIQUE,"
                + "   open_date                   DATE          NOT NULL,"
                + "   close_date                  DATE          NOT NULL,"
                + "   text                        VARCHAR(1000) NOT NULL,"
                + "   patient_id                  INT UNSIGNED  NOT NULL,"
                + "   last_modified_by            VARCHAR(1000) NOT NULL,"
                + "   signature_of_last_modified  VARCHAR(1000) NOT NULL,"
                + "   FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE ) CHARACTER SET = utf8 ";

        Statement stmt = connect.createStatement();
        stmt.execute(sqlCreate);
    }

    private void createCertificateTable() throws SQLException {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS certificates  "
                + "  (id                INT UNSIGNED  NOT NULL PRIMARY KEY UNIQUE AUTO_INCREMENT,"
                + "   open_key          VARCHAR(225)  NOT NULL UNIQUE,"
                + "   doctor_id         INT UNSIGNED  NOT NULL,"
                + "   first_part_key    LONGTEXT      NOT NULL,"
                + "   servers_key       VARCHAR(10)   NOT NULL UNIQUE,"
                + "   prime             VARCHAR(45)   NOT NULL UNIQUE,"
                + "   FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE) CHARACTER SET = utf8 ";

        Statement stmt = connect.createStatement();
        stmt.execute(sqlCreate);
    }

    private void createSignatureTable() throws SQLException {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS signatures  "
                + "  (id                INT UNSIGNED  NOT NULL PRIMARY KEY UNIQUE AUTO_INCREMENT,"
                + "   signature         LONGTEXT      NOT NULL,"
                + "   symmetricKey      LONGTEXT      NOT NULL) CHARACTER SET = utf8 ";
        Statement stmt = connect.createStatement();
        stmt.execute(sqlCreate);
        String tmp = select(new ArrayList<>(Arrays.asList(new String[]{"signatures", "id", "where", "id",
                "1"})));
        if (tmp.isEmpty()) {
            insert(new ArrayList<>(Arrays.asList(new String[]{"signatures", EMPTY, EMPTY})));
        }
    }

    public void updateFirstPartsKeys() {
        String newKey = ExtraCrypto.generateNewSymmetricKey();
        String oldKey = getKeyFromDB();
        String text = select(new ArrayList<>(Arrays.asList(new String[]{"certificates", "id", "first_part_key"})));
        if (!oldKey.equals(EMPTY) & !text.isEmpty()) {
            String[] words =  text.split(separatorForSplit);
            for (int i = 2; i < words.length; i+=3) {
                String id = words[i+1];
                String encryptedFirstPartKey = words[i+2];
                String decryptFirstPartKey = ExtraCrypto.textSymmetricKeyDecrypt(encryptedFirstPartKey, oldKey);
                encryptedFirstPartKey = ExtraCrypto.textSymmetricKeyEncrypt(decryptFirstPartKey, newKey);
                update(new ArrayList<>(Arrays.asList(new String[]{"certificates", "first_part_key", encryptedFirstPartKey, id})));
            }
        }
        updateSymmetricKeyInBD(newKey);
    }


    private String getKeyFromDB() {
        String tmp = select(new ArrayList<>(Arrays.asList(new String[]{"signatures", "symmetricKey", "where", "id",
                "1"})));
        return tmp.split(separatorForSplit)[2];
    }

    private void updateSymmetricKeyInBD(String newKey) {
        String tmp = select(new ArrayList<>(Arrays.asList(new String[]{"signatures", "id", "where", "id",
                "1"})));
        this.update(new ArrayList<>(Arrays.asList(new String[]{"signatures", "symmetricKey", newKey, "1"})));
    }

    private void userTriggerAfterInsert(String login) {
        String newUser = select(new ArrayList<>(Arrays.asList(new String[]{"users", "id", "role", "where", "login", login})));
        String symmetricKey = getKeyFromDB();
        if (!newUser.split(separatorForSplit)[4].equals("Patient")) {
            List<String> certificateToInsert = PKI.createKeysToUser(newUser.split(separatorForSplit)[3], symmetricKey);
            insert(certificateToInsert);
        }
    }

    private Boolean diseaseTriggerBeforeInsertOrUpdate(List<String> values) {
        String signature = getSignature(values);
        if (signature.equals("false")) {
            return false;
        }
        //If it is 9 strings in List, it mean list contains id of history and it will update
        Boolean isUpdate = (values.size() == 9);
        if (isUpdate) {
            values.add(values.size() - 2, signature);
        } else {
            values.add(values.size() - 1, signature);
        }
        return true;
    }

    private Boolean certificateTriggerBeforeUpdateOrDelete(List<String> values) {
        String key = values.get(values.size() - 1);
        String certificates = select(getCertificatesQuery());
        String prime = PKI.getKeysPrime(key, certificates, getKeyFromDB());
        if (prime.equals("false")) {
            return false;
        }
        String certificate = select(new ArrayList<>(Arrays.asList(new String[]{"certificates", "doctor_id",
                "where", "prime", prime})));
        String userRole = select(new ArrayList<>(Arrays.asList(new String[]{"users", "role",
                "where", "id", certificate.split(DatabaseHandler.separatorForSplit)[2]})));
        return userRole.split(DatabaseHandler.separatorForSplit)[2].equals("Admin");
    }

    private void certificateTriggerAfterDeleteOrUpdateOrInsert(String key) {
        String text = select(new ArrayList<>(Arrays.asList(new String[]{"certificates", "id", "open_key", "doctor_id",
                                                          "first_part_key", "servers_key", "prime"})));
        String certificates = select(getCertificatesQuery());
        String signature = PKI.getNewSignature(key, text, certificates, getKeyFromDB());
        update(new ArrayList<>(Arrays.asList(new String[]{"signatures", "signature", signature, "1"})));
    }

    private String getSignature(List<String> value) {
        Integer n = 5;
        String mainData = "";
        for (int i = 1; i < n; i++) {
            mainData += value.get(i) + " ";
        }
        String certificates = select(getCertificatesQuery());
        return PKI.getNewSignature(value.get(value.size() - 1), mainData, certificates, getKeyFromDB());
    }

    private List<String> getCertificatesQuery() {
        return new ArrayList<>(Arrays.asList(new String[]{"certificates", "first_part_key",
                "servers_key", "prime"}));
    }

    public String select(List<String> dataFromClient) {
        Integer n = dataFromClient.indexOf("where");
        List<String> fieldsNames, values = null, forStatement;
        if (n != -1) {
            fieldsNames = dataFromClient.subList(startFieldsNames, n);
            Integer valuesNumbers = (dataFromClient.size()-n)/2;
            values = dataFromClient.subList(dataFromClient.size()-valuesNumbers-1, dataFromClient.size());
            forStatement = dataFromClient.subList(0, dataFromClient.size() - valuesNumbers);
        } else {
            fieldsNames = dataFromClient.subList(startFieldsNames, dataFromClient.size());
            forStatement = new ArrayList<>(dataFromClient);
        }
        String statement = getSelectQueryStatement(forStatement);
        workWithPreparedStatement(statement, values);
        return userResultSetToString(fieldsNames, dataFromClient.get(tableNameIndex));
    }

    public Boolean insert(List<String> dataFromClient) {
        int loginIndex = 1;
        if (dataFromClient.get(tableNameIndex).equals("disease_histories")) {
            if (diseaseTriggerBeforeInsertOrUpdate(dataFromClient)) {
                dataFromClient.remove(dataFromClient.size() - 1);
            } else return false;
        }
        String statement = getInsertQueryStatement(dataFromClient);
        Boolean isSuccess =  workWithPreparedStatement(statement, dataFromClient);
        if (dataFromClient.get(tableNameIndex).equals("users")) {
            userTriggerAfterInsert(dataFromClient.get(loginIndex));
        }
        return isSuccess;
    }

    public Boolean delete(List<String> dataFromClient) {
        String key = "";
        if (dataFromClient.get(tableNameIndex).equals("certificates")) {
            if (!certificateTriggerBeforeUpdateOrDelete(dataFromClient)) {
                return false;
            }
            key = dataFromClient.get(dataFromClient.size() - 1);
            dataFromClient.remove(dataFromClient.size() - 1);
        }
        String statement = getDeleteQueryStatement(dataFromClient);
        Boolean isSuccess = workWithPreparedStatement(statement, dataFromClient);
        if (dataFromClient.get(tableNameIndex).equals("certificates")) {
            certificateTriggerAfterDeleteOrUpdateOrInsert(key);
        }
        return isSuccess;
    }

    public Boolean update(List<String> dataFromClient) {
        String key = "";
        int k = 0;
        int n = dataFromClient.size()/2;
        List<String> forStatement = new ArrayList<>(dataFromClient.subList(0, n));
        List<String> values = new ArrayList<>(dataFromClient.subList(n-1, dataFromClient.size()));
        String statement = getUpdateQueryStatement(forStatement);
        if (dataFromClient.get(tableNameIndex).equals("disease_histories")) {
            if (diseaseTriggerBeforeInsertOrUpdate(values)) {
                values.remove(values.size() - 1);
            } else return false;
        } else if (dataFromClient.get(tableNameIndex).equals("users") && dataFromClient.get(tableNameIndex+1).equals("doctor_id")) {
            if (!userUpdateDoctorTriggerBeforeUpdate(values)) {
                return false;
            }
        }
        Boolean isSuccess = workWithPreparedStatement(statement, values);
        return isSuccess;
    }

    private Boolean userUpdateDoctorTriggerBeforeUpdate(List<String> values) {
        String patient = select(new ArrayList<>(Arrays.asList(new String[]{"users", "id", "doctor_id", "where", "id", values.get(values.size()-1)})));
        return patient.split(separatorForSplit)[4].equals("0") || values.get(values.size()-2).equals("0");
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
                        + dataFromClient.get(tableNameIndex) + " set ";
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
        statement += " from " + databaseConfig.getPropertyByName("schemaName") + "." + dataFromClient.get(tableNameIndex);
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
        String statement = "insert into " + databaseConfig.getPropertyByName("schemaName") + "." + dataFromClient.get(tableNameIndex) + " values (default";
        for (int i = 0; i < dataFromClient.size() - 1; i++) {
            statement += ", ?";
        }
        statement += ")";
        return statement;
    }

    private String getDeleteQueryStatement(List<String> dataFromClient) {
        return  "delete from " + databaseConfig.getPropertyByName("schemaName") + "." + dataFromClient.get(tableNameIndex) + " where id= ? ; ";
    }

    private String userResultSetToString(List<String> fieldsNames, String table) {
        Integer i = 0;
        String result = "";
        try {
            while (resultSet.next()) {
                if (i == 0) {
                    for (String field : fieldsNames) {
                        result += field + separator;
                    }
                }
                result += i++ +".";
                if (!fieldsNames.get(0).equals("*")) {
                    for (String field : fieldsNames) {
                        result += separator + resultSet.getString(field);
                    }
                } else {
                    if (table.equals("users")) {
                        result += separator + resultSet.getString("id")
                                + separator + resultSet.getString("login")
                                + separator + resultSet.getString("password")
                                + separator + resultSet.getString("name")
                                + separator + resultSet.getString("role")
                                + separator + resultSet.getString("age")
                                + separator + resultSet.getString("phone")
                                + separator + resultSet.getString("doctor_id");
                    } else if (table.equals("disease_histories")) {
                        result += separator + resultSet.getString("id")
                                + separator + resultSet.getString("title")
                                + separator + resultSet.getString("open_date")
                                + separator + resultSet.getString("close_date")
                                + separator + resultSet.getString("text")
                                + separator + resultSet.getString("patient_id")
                                + separator + resultSet.getString("last_modified_by")
                                + separator + resultSet.getString("signature_of_last_modified");
                    } else if (table.equals("certificates")) {
                        result += separator + resultSet.getString("id")
                                + separator + resultSet.getString("open_key")
                                + separator + resultSet.getString("doctor_id");
                    }
                }
                result += separator;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        resultSet = null;
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
