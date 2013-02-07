/*
 * Copyright 2007-2012 Scott C. Gray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sqsh;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.sqsh.analyzers.ANSIAnalyzer;
import org.sqsh.analyzers.SQLAnalyzer;
import org.sqsh.analyzers.NullAnalyzer;

public class SQLDriver 
    implements Comparable<SQLDriver> {
    
    private static final Logger LOG = 
        Logger.getLogger(SQLDriver.class.getName());
    
    public static String USER_PROPERTY = "user";
    public static String PASSWORD_PROPERTY = "password";
    public static String SERVER_PROPERTY = "server";
    public static String PORT_PROPERTY = "port";
    public static String DATABASE_PROPERTY = "db";
    public static String SID_PROPERTY = "SID";
    public static String DOMAIN_PROPERTY = "domain";
    
    /**
     * Simple class to describe the set of variables used to configure the
     * driver.
     */
    public static class DriverVariable implements Comparable<DriverVariable>{
        
        private String name;
        private String displayName;
        private String defaultValue;
        
        public DriverVariable (String name, String displayName, String defaultValue) {
            
            this.name = name;
            this.displayName = displayName;
            this.defaultValue = defaultValue;
        }

        public String getName() {
        
            return name;
        }

        public String getDisplayName() {
        
            return displayName;
        }

        public String getDefaultValue() {
        
            return defaultValue;
        }

        @Override
        public int compareTo(DriverVariable o) {

            return this.displayName.compareTo(o.displayName);
        }
    }
    
    private SQLDriverManager driverMan = null;
    private String name = null;
    private String target = null;
    private String url = null;
    private String clazz = null;
    private boolean isInternal = false;
    private boolean isAvailable = false;
    private Map<String, String> variables = new HashMap<String, String>();
    private Map<String, String> properties = new HashMap<String, String>();
    private Map<String, String> sessionVariables = new HashMap<String, String>();
    private SQLAnalyzer analyzer = new NullAnalyzer();
    
    public SQLDriver() {
        
    }
    
    public SQLDriver(String name, String clazz, String url) {
        
        this.name = name;
        this.url = url;
        
        setDriverClass(clazz);
    }
    
    /**
     * Retrieves the variable map used by the driver.
     * 
     * @return The variable map used by the driver.
     */
    public Map<String, String> getVariables() {
        
        return variables;
    }

    /**
     * Retrieves the set of variables that are to set in the users
     * session upon connection.
     * 
     * @return The session variable map to be placed in the user's
     *    session.
     */
    public Map<String, String> getSessionVariables() {
        
        return sessionVariables;
    }
    
    /**
     * Sets the name of the class that will be utilized for analyzing the
     * SQL statements to be executed.
     * 
     * @param clazz The name of the class.
     */
    public void setAnalyzer(String sqlAnalyzer) {
        
        try {
            
            Class clazz = Class.forName(sqlAnalyzer);
            Constructor<SQLAnalyzer> constructor =  clazz.getConstructor();
            
            analyzer = constructor.newInstance();
        }
        catch (Exception e) {
            
            throw new CannotSetValueError("Unable to instantiate "
                + sqlAnalyzer + ": " + e.getMessage());
        }
    }
    
    /**
     * Returns the SQL analyzer for this driver.
     * @return The SQL analyzer for this driver or null if none is defined.
     */
    public SQLAnalyzer getAnalyzer() {
        
        if (analyzer == null) {
            
            analyzer = new ANSIAnalyzer();
        }
        
        return analyzer;
    }
    
    /**
     * Sets the manager for this driver.
     * @param driverMan The manager for this driver.
     */
    protected void setDriverManager(SQLDriverManager driverMan) {
        
        this.driverMan = driverMan;
    }
    
    /**
     * Returns true if the driver is available for use.
     * 
     * @return true if the driver is available for use.
     */
    public boolean isAvailable() {
        
        return isAvailable;
    }
    
    /**
     * Sets whether or not the driver is available.
     * 
     * @param isAvailable
     */
    public void setAvailable (boolean isAvailable) {
        
        this.isAvailable = isAvailable;
    }
    
    /**
     * The target is the target database platform.
     * 
     * @param target Target database platorm.
     */
    public void setTarget(String target) {
        
        this.target = target;
    }
    
    /**
     * Returns the target database platform.
     * @return The target database paltform.
     */
    public String getTarget() {
        
        return target;
    }
    
    /**
     * Sets the class name that the driver requires.
     * 
     * @param clazz The name of the class that implements the driver.
     */
    public void setDriverClass(String clazz) {
        
        this.clazz = clazz;
        
        if (clazz != null) {
            
            try {
                
                Class.forName(clazz);
                isAvailable = true;
            }
            catch (Exception e) {
                
                isAvailable = false;
            }
        }
    }
    
    /**
     * Returns the name of the class implementing this driver.
     * 
     * @return the name of the class implementing this driver.
     */
    public String getDriverClass() {
        
        return clazz;
    }
    
    /**
     * Sets the value of a variable.
     * @param name The name of the variable
     * @param value The value of the variable.
     */
    public void setVariable(String name, String value) {
        
        variables.put(name, value);
    }
    
    /**
     * Returns the value of a variable.
     * @param name The name to look up
     * @return The value or null if the variable is not defined.
     */
    public String getVariable(String name) {
        
        return variables.get(name);
    }

    /**
     * The the value of a variable that is to be set in the user's session
     * upon successfully establishing a connection to a server.
     *
     * @param name The name of the variable
     * @param value The value of the variable.
     */
    public void setSessionVariable(String name, String value) {
        
        sessionVariables.put(name, value);
    }
    
    /**
     * Returns the value of a variable.
     * @param name The name to look up
     * @return The value or null if the variable is not defined.
     */
    public String getSessionVariable(String name) {
        
        return sessionVariables.get(name);
    }
    
    /**
     * Adds a property to the driver. These properties are passed
     * in during the connection process.
     * 
     * @param name The name of the property
     * @param value The value of the property.
     */
    public void setProperty(String name, String value) {
        
        properties.put(name, value);
    }
    
    /**
     * Retrieves a property from the driver definition.
     * 
     * @param name The name of the property
     * @return The value of the property or NULL if the property 
     *    is not define.
     */
    public String getProperty(String name) {
        
        return properties.get(name);
    }
    
    /**
     * Retrieves the set of property names that have been associated
     * this this driver definition.
     * 
     * @return A set of property names.
     */
    public Collection<String> getPropertyNames() {
        
        return properties.keySet();
    }
    
    /**
     * Returns the JDBC URL for this driver.
     * @return The JDBC URL for this driver.
     */
    public String getUrl () {
    
        return url;
    }

    /**
     * Sets the JDBC URL for this driver.
     * 
     * @param url The url
     */
    public void setUrl (String url) {
    
        this.url = url;
    }

    /**
     * Returns an indicator as to whether or not the driver is an internal
     * driver or a user-defined driver.
     * 
     * @return true if it is an internal driver.
     */
    public boolean isInternal () {
    
        return isInternal;
    }

    /**
     * Sets whether or not this is an internal driver.
     * @param isInternal 
     */
    public void setInternal (boolean isInternal) {
    
        this.isInternal = isInternal;
    }

    /**
     * Set the name of the command.
     * 
     * @param name The name of the command.
     */
    public void setName(String name) {
        
        this.name = name;
    }
    
    /**
     * Returns the name of the variable.
     * 
     * @return The name of the variable.
     */
    public String getName() {
        
        return name;
    }
    
    /**
     * @return A list of variables the driver actually uses and the default
     *     value (if any) for each variable
     */
    public List<DriverVariable> getVariableDescriptions() {
        
        List<DriverVariable> vars = new ArrayList<SQLDriver.DriverVariable>();
        
        /*
         * This is gross, I need to make real metadata somewhere.
         */
        String url = getUrl();
        final int sz = url.length();
        
        int idx = 0;
        while (idx < sz) {
            
            char ch = url.charAt(idx++);
            if (ch == '$') {
                
                if (idx < sz && url.charAt(idx) == '{') {
                    
                    int start = ++idx;
                    while (idx < sz && url.charAt(idx) != '}') {
                        
                        ++idx;
                    }
                    
                    String name = url.substring(start, idx);
                    ++idx;
                    
                    String displayName = null;
                    if (name.equals(SERVER_PROPERTY)) {
                        
                        displayName = "Server";
                    }
                    else if (name.equals(PORT_PROPERTY)) {
                        
                        displayName = "Port";
                    }
                    else if (name.equals(DATABASE_PROPERTY)) {
                        
                        displayName = "Database/Schema";
                    }
                    else if (name.equals(SID_PROPERTY)) {
                        
                        displayName = "SID";
                    }
                    else if (name.equals(DOMAIN_PROPERTY)) {
                        
                        displayName = "Domain";
                    }
                    else {
                        
                        displayName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    }
                    
                    DriverVariable var = new DriverVariable(name, displayName, getVariable(name));
                    
                    if (! vars.contains(var)) {
                        
                        vars.add(new DriverVariable(name, displayName, getVariable(name)));
                    }
                }
            }
        }
        
        Collections.sort(vars);
        
        vars.add(new DriverVariable(USER_PROPERTY, "Username", System.getProperty("user.name")));
        vars.add(new DriverVariable(PASSWORD_PROPERTY, "Password", null));
        
        return vars;
    }
    
    /**
     * Compares the names of two drivers. This method is provided primarily
     * to allow for easy sorting of drivers on display.
     * 
     * @param o The object to compare to.
     * @return The results of the comparison.
     */
    public int compareTo(SQLDriver o) {
        
        return name.compareTo(((SQLDriver) o).getName());
    }
    
    /**
     * Compares two drivers for equality. Drivers are considered
     * equal if their names match.
     * 
     * @param o The object to test equality.
     * @return true if o is a Drivers that has the same name as this.
     */
    public boolean equals(Object o) {
        
        if (o instanceof SQLDriver) {
            
            return ((SQLDriver) o).getName().equals(name);
        }
        
        return false;
    }
    
    /**
     * Returns a hash value for the driver. The hash code is nothing
     * more than the hash code for the driver name itself.
     * 
     * @return The hash code of the driver's name.
     */
    public int hashCode() {
        
        return name.hashCode();
    }
}
