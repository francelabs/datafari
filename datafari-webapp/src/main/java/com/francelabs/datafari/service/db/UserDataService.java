package com.francelabs.datafari.service.db;

import java.sql.*;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

public class UserDataService {

    final static Logger logger = LogManager.getLogger(UserDataService.class.getName());
    private static UserDataService instance;

    public final static String SEARCHADMINISTRATOR = "SearchAdministrator";
    public final static String USERCOLLECTION = "users";
    public final static String ROLECOLLECTION = "roles";

    public static final String USERNAMECOLUMN = "username";
    public final static String PASSWORDCOLUMN = "password";
    public final static String ISIMPORTEDCOLUMN = "is_imported";
    public final static String LASTREFRESHCOLUMN = "last_refresh";
    public final static String IMPORTCOLUMN = "imported";
    public final static String ROLECOLUMN = "role";

    private final String userDataTTL;

    private final PostgresService pgService = new PostgresService();

    public static synchronized UserDataService getInstance() {
        if (instance == null) {
            instance = new UserDataService();
        }
        return instance;
    }

    private UserDataService() {
        userDataTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL);
    }

    public boolean isInBase(final String username) throws DatafariServerException {
        try {
            String sql = "SELECT 1 FROM " + USERCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
            try (ResultSet results = pgService.executeSelect(sql, username)) {
                return results.next();
            }
        } catch (final Exception e) {
            logger.warn("Unable to check if user in base : " + e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    public String getPassword(final String username) throws DatafariServerException {
        try {
            String sql = "SELECT * FROM " + USERCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
            try (ResultSet results = pgService.executeSelect(sql, username)) {
                if (results.next()) {
                    return results.getString(PASSWORDCOLUMN);
                } else {
                    return null;
                }
            }
        } catch (final Exception e) {
            logger.warn("Unable to get password : " + e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    private JSONObject getUser(final String username) throws DatafariServerException {
        try {
            String sql = "SELECT * FROM " + USERCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
            try (ResultSet results = pgService.executeSelect(sql, username)) {
                if (!results.next()) return null;
                JSONObject user = new JSONObject();
                user.put(USERNAMECOLUMN, username);
                user.put(PASSWORDCOLUMN, results.getString(PASSWORDCOLUMN));
                user.put(ISIMPORTEDCOLUMN, results.getBoolean(ISIMPORTEDCOLUMN));
                return user;
            }
        } catch (final Exception e) {
            logger.warn("Unable to get user : " + e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    public List<String> getRoles(final String username) throws DatafariServerException {
        try {
            List<String> roles = new ArrayList<>();
            String sql = "SELECT " + ROLECOLUMN + " FROM " + ROLECOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
            try (ResultSet results = pgService.executeSelect(sql, username)) {
                while (results.next()) {
                    roles.add(results.getString(ROLECOLUMN));
                }
            }
            return roles;
        } catch (final Exception e) {
            logger.warn("Unable to get roles : " + e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    public JSONArray getAllADUsers() throws DatafariServerException {
        return getUsersByImported(true);
    }

    public JSONArray getAllDatafariUsers() throws DatafariServerException {
        return getUsersByImported(false);
    }

    public JSONArray getAllUsers() throws DatafariServerException {
        try {
            JSONArray users = new JSONArray();
            Map<String, JSONObject> listJsonUsers = new HashMap<>();

            String sqlUsers = "SELECT " + USERNAMECOLUMN + ", " + ISIMPORTEDCOLUMN + " FROM " + USERCOLLECTION;
            try (ResultSet userResults = pgService.executeSelect(sqlUsers)) {
                while (userResults.next()) {
                    String username = userResults.getString(USERNAMECOLUMN);
                    boolean isImported = userResults.getBoolean(ISIMPORTEDCOLUMN);
                    JSONObject userJ = new JSONObject();
                    userJ.put(USERNAMECOLUMN, username);
                    userJ.put(ISIMPORTEDCOLUMN, isImported);
                    userJ.put("roles", new JSONArray());
                    if (!listJsonUsers.containsKey(username)) {
                        listJsonUsers.put(username, userJ);
                    }
                }
            }

            String sqlRoles = "SELECT * FROM " + ROLECOLLECTION;
            try (ResultSet roleResults = pgService.executeSelect(sqlRoles)) {
                while (roleResults.next()) {
                    String username = roleResults.getString(USERNAMECOLUMN);
                    if (!listJsonUsers.containsKey(username)) {
                        JSONObject userJ = new JSONObject();
                        userJ.put(USERNAMECOLUMN, username);
                        userJ.put(ISIMPORTEDCOLUMN, false);
                        userJ.put("roles", new JSONArray());
                        listJsonUsers.put(username, userJ);
                    }
                    JSONArray userRoles = (JSONArray) listJsonUsers.get(username).get("roles");
                    userRoles.add(roleResults.getString(ROLECOLUMN));
                }
            }

            listJsonUsers.values().forEach(users::add);

            return users;
        } catch (final Exception e) {
            logger.warn("Unable to get all users : " + e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    private JSONArray getUsersByImported(boolean isImportedFlag) throws DatafariServerException {
        try {
            JSONArray users = new JSONArray();
            Map<String, JSONObject> listJsonUsers = new HashMap<>();

            String sqlUsers = "SELECT " + USERNAMECOLUMN + ", " + ISIMPORTEDCOLUMN + " FROM " + USERCOLLECTION + " WHERE " + ISIMPORTEDCOLUMN + "=?";
            try (ResultSet userResults = pgService.executeSelect(sqlUsers, isImportedFlag)) {
                while (userResults.next()) {
                    String username = userResults.getString(USERNAMECOLUMN);
                    boolean isImported = userResults.getBoolean(ISIMPORTEDCOLUMN);
                    JSONObject userJ = new JSONObject();
                    userJ.put(USERNAMECOLUMN, username);
                    userJ.put(ISIMPORTEDCOLUMN, isImported);
                    userJ.put("roles", new JSONArray());
                    if (!listJsonUsers.containsKey(username)) {
                        listJsonUsers.put(username, userJ);
                    }
                }
            }

            String sqlRoles = "SELECT * FROM " + ROLECOLLECTION;
            try (ResultSet roleResults = pgService.executeSelect(sqlRoles)) {
                while (roleResults.next()) {
                    String username = roleResults.getString(USERNAMECOLUMN);
                    if (listJsonUsers.containsKey(username)) {
                        JSONArray userRoles = (JSONArray) listJsonUsers.get(username).get("roles");
                        userRoles.add(roleResults.getString(ROLECOLUMN));
                    }
                }
            }

            listJsonUsers.values().forEach(users::add);
            return users;

        } catch (final Exception e) {
            logger.warn("Unable to get all AD/Datafari users : " + e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    public void changePassword(final String passwordHashed, final String username) throws DatafariServerException {
        try {
            String sql = "UPDATE " + USERCOLLECTION + " SET " + PASSWORDCOLUMN + " = ? WHERE " + USERNAMECOLUMN + " = ?";
            pgService.executeUpdate(sql, passwordHashed, username);
        } catch (final Exception e) {
            logger.warn("Unable to change password : " + e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    public void addRole(final String role, final String username) throws DatafariServerException {
        try {
            // TTL not supported, simulate with last_refresh
            String sql = "INSERT INTO " + ROLECOLLECTION + " (" + USERNAMECOLUMN + ", " + ROLECOLUMN + ", " + LASTREFRESHCOLUMN + ") " +
                    "VALUES (?, ?, ?) " +
                    "ON CONFLICT (" + USERNAMECOLUMN + ", " + ROLECOLUMN + ") DO UPDATE SET " + LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN;
            pgService.executeUpdate(sql, username, role, new Timestamp(System.currentTimeMillis()));
        } catch (final Exception e) {
            logger.warn("Unable to add role : " + e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    public boolean addUser(final String username, final String password, final List<String> roles, final boolean isImported) throws DatafariServerException {
        try {
            // TTL not supported
            String sql = "INSERT INTO " + USERCOLLECTION + " (" + USERNAMECOLUMN + ", " + PASSWORDCOLUMN + ", " + ISIMPORTEDCOLUMN + ", " + LASTREFRESHCOLUMN + ") " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT (" + USERNAMECOLUMN + ") DO UPDATE SET " + PASSWORDCOLUMN + " = EXCLUDED." + PASSWORDCOLUMN + ", " +
                    ISIMPORTEDCOLUMN + " = EXCLUDED." + ISIMPORTEDCOLUMN + ", " +
                    LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN;
            pgService.executeUpdate(sql, username, password, isImported, new Timestamp(System.currentTimeMillis()));
            for (String role : roles) {
                this.addRole(role, username);
            }
        } catch (final Exception e) {
            logger.warn("Unable to add user : " + e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
        return true;
    }

    private void refreshRoles(final String username, final List<String> roles) throws DatafariServerException {
        for (final String role : roles) {
            try {
                String sql = "UPDATE " + ROLECOLLECTION + " SET " + LASTREFRESHCOLUMN + " = ? " +
                        "WHERE " + USERNAMECOLUMN + " = ? AND " + ROLECOLUMN + " = ?";
                pgService.executeUpdate(sql, new Timestamp(System.currentTimeMillis()), username, role);
            } catch (final Exception e) {
                logger.warn("Unable to refresh roles : " + e.getMessage());
                throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
            }
        }
    }

    public void refreshUser(final String username) throws DatafariServerException {
        final List<String> roles = getRoles(username);
        try {
            String sql = "UPDATE " + USERCOLLECTION + " SET " + LASTREFRESHCOLUMN + " = ? WHERE " + USERNAMECOLUMN + " = ?";
            pgService.executeUpdate(sql, new Timestamp(System.currentTimeMillis()), username);
        } catch (final Exception e) {
            logger.warn("Unable to refresh user : " + e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
        refreshRoles(username, roles);
    }

    public void deleteUser(final String username) throws DatafariServerException {
        try {
            String sqlUser = "DELETE FROM " + USERCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
            pgService.executeUpdate(sqlUser, username);
            String sqlRole = "DELETE FROM " + ROLECOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
            pgService.executeUpdate(sqlRole, username);
        } catch (final Exception e) {
            logger.warn("Unable to remove user : " + e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    public void deleteRole(final String role, final String username) throws DatafariServerException {
        try {
            String sql = "DELETE FROM " + ROLECOLLECTION + " WHERE " + USERNAMECOLUMN + " = ? AND " + ROLECOLUMN + " = ?";
            pgService.executeUpdate(sql, username, role);
        } catch (final Exception e) {
            logger.warn("Unable to remove roles : " + e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }
}