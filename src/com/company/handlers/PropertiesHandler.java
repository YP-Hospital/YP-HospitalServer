package com.company.handlers;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesHandler {

    private Properties property;

    /**
     * connect to property file with name in propertyFileName
     * @param propertyFileName name of property file
     */
    public PropertiesHandler(String propertyFileName) {
        String path = "src/res/";
        FileInputStream fis;
        property = new Properties();
        try {
            fis = new FileInputStream(path + propertyFileName);
            property.load(fis);
        } catch (IOException e) {
            System.err.println("ОШИБКА: Файл свойств отсуствует!");
        }
    }

    public Properties getProperty() {
        return property;
    }

    /**
     * Get value of property by key in current property file
     * @param propertyKey property's key
     * @return property's value
     */
    public String getPropertyByName(String propertyKey) {
        return property.getProperty(propertyKey);
    }
}
