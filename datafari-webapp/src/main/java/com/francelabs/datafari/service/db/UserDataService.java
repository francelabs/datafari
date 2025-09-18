package com.francelabs.datafari.service.db;

import java.sql.Timestamp;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

@Service
public class UserDataService {

    final static Logger logger = LogManager.getLogger(UserDataService.class.getName());

    private static volatile UserDataService instance; // compat legacy

    public final static String SEARCHADMINISTRATOR = "SearchAdministrator";
    public final static String USERCOLLECTION = "users";
    public final static String ROLECOLLECTION = "roles";

    public static final String USERNAMECOLUMN = "username";
    public final static String PASSWORDCOLUMN = "password";
    public final static String ISIMPORTEDCOLUMN = "is_imported";
    public final static String LASTREFRESHCOLUMN = "last_refresh";
    public final static String IMPORTCOLUMN = "imported"; // non utilisé mais conservé
    public final static String ROLECOLUMN = "role";

    private final String userDataTTL; // conservé pour parité (TTL simulé via last_refresh)
    private final SqlService sql;

    /** Pont de compat pour l’ancien code qui fait getInstance() */
    public static synchronized UserDataService getInstance() {
        return instance;
    }

    public UserDataService(SqlService sql) {
        this.sql = sql;
        this.userDataTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL);
        instance = this; // publication du bean Spring pour l’ancien code
    }

    // =========================== Requêtes ===========================

    public boolean isInBase(final String username) throws DatafariServerException {
        try {
            Integer one = sql.getJdbcTemplate().query(
                "SELECT 1 FROM " + USERCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?",
                ps -> ps.setString(1, username),
                rs -> rs.next() ? 1 : null
            );
            return one != null;
        } catch (Exception e) {
            logger.warn("Unable to check if user in base : {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    public String getPassword(final String username) throws DatafariServerException {
        try {
            List<String> list = sql.getJdbcTemplate().query(
                "SELECT " + PASSWORDCOLUMN + " FROM " + USERCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?",
                ps -> ps.setString(1, username),
                (rs, rn) -> rs.getString(PASSWORDCOLUMN)
            );
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            logger.warn("Unable to get password : {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    private JSONObject getUser(final String username) throws DatafariServerException {
        try {
            List<JSONObject> list = sql.getJdbcTemplate().query(
                "SELECT " + USERNAMECOLUMN + "," + PASSWORDCOLUMN + "," + ISIMPORTEDCOLUMN +
                " FROM " + USERCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?",
                ps -> ps.setString(1, username),
                (rs, rn) -> {
                    JSONObject user = new JSONObject();
                    user.put(USERNAMECOLUMN, rs.getString(USERNAMECOLUMN));
                    user.put(PASSWORDCOLUMN, rs.getString(PASSWORDCOLUMN));
                    user.put(ISIMPORTEDCOLUMN, rs.getBoolean(ISIMPORTEDCOLUMN));
                    return user;
                }
            );
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            logger.warn("Unable to get user : {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    public List<String> getRoles(final String username) throws DatafariServerException {
        try {
            return sql.getJdbcTemplate().query(
                "SELECT " + ROLECOLUMN + " FROM " + ROLECOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?",
                ps -> ps.setString(1, username),
                (rs, rn) -> rs.getString(ROLECOLUMN)
            );
        } catch (Exception e) {
            logger.warn("Unable to get roles : {}", e.getMessage());
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
            Map<String, JSONObject> map = new HashMap<>();

            // Users
            sql.getJdbcTemplate().query(
                "SELECT " + USERNAMECOLUMN + ", " + ISIMPORTEDCOLUMN + " FROM " + USERCOLLECTION,
                rs -> {
                    String username = rs.getString(USERNAMECOLUMN);
                    boolean isImported = rs.getBoolean(ISIMPORTEDCOLUMN);
                    JSONObject userJ = new JSONObject();
                    userJ.put(USERNAMECOLUMN, username);
                    userJ.put(ISIMPORTEDCOLUMN, isImported);
                    userJ.put("roles", new JSONArray());
                    map.putIfAbsent(username, userJ);
                }
            );

            // Roles
            sql.getJdbcTemplate().query(
                "SELECT " + USERNAMECOLUMN + ", " + ROLECOLUMN + " FROM " + ROLECOLLECTION,
                rs -> {
                    String username = rs.getString(USERNAMECOLUMN);
                    String role = rs.getString(ROLECOLUMN);
                    map.computeIfAbsent(username, k -> {
                        JSONObject userJ = new JSONObject();
                        userJ.put(USERNAMECOLUMN, k);
                        userJ.put(ISIMPORTEDCOLUMN, false);
                        userJ.put("roles", new JSONArray());
                        return userJ;
                    });
                    ((JSONArray) map.get(username).get("roles")).add(role);
                }
            );

            JSONArray users = new JSONArray();
            map.values().forEach(users::add);
            return users;
        } catch (Exception e) {
            logger.warn("Unable to get all users : {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    private JSONArray getUsersByImported(boolean isImportedFlag) throws DatafariServerException {
        try {
            Map<String, JSONObject> map = new HashMap<>();

            // Filtered users
            sql.getJdbcTemplate().query(
                "SELECT " + USERNAMECOLUMN + ", " + ISIMPORTEDCOLUMN +
                " FROM " + USERCOLLECTION + " WHERE " + ISIMPORTEDCOLUMN + " = ?",
                ps -> ps.setBoolean(1, isImportedFlag),
                rs -> {
                    String username = rs.getString(USERNAMECOLUMN);
                    boolean isImported = rs.getBoolean(ISIMPORTEDCOLUMN);
                    JSONObject userJ = new JSONObject();
                    userJ.put(USERNAMECOLUMN, username);
                    userJ.put(ISIMPORTEDCOLUMN, isImported);
                    userJ.put("roles", new JSONArray());
                    map.putIfAbsent(username, userJ);
                }
            );

            // Roles (attach to selected users only)
            sql.getJdbcTemplate().query(
                "SELECT " + USERNAMECOLUMN + ", " + ROLECOLUMN + " FROM " + ROLECOLLECTION,
                rs -> {
                    String username = rs.getString(USERNAMECOLUMN);
                    if (map.containsKey(username)) {
                        ((JSONArray) map.get(username).get("roles")).add(rs.getString(ROLECOLUMN));
                    }
                }
            );

            JSONArray users = new JSONArray();
            map.values().forEach(users::add);
            return users;
        } catch (Exception e) {
            logger.warn("Unable to get all AD/Datafari users : {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    public void changePassword(final String passwordHashed, final String username) throws DatafariServerException {
        try {
            sql.getJdbcTemplate().update(
                "UPDATE " + USERCOLLECTION + " SET " + PASSWORDCOLUMN + " = ? WHERE " + USERNAMECOLUMN + " = ?",
                passwordHashed, username
            );
        } catch (Exception e) {
            logger.warn("Unable to change password : {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    public void addRole(final String role, final String username) throws DatafariServerException {
        try {
            sql.getJdbcTemplate().update(
                "INSERT INTO " + ROLECOLLECTION + " (" + USERNAMECOLUMN + ", " + ROLECOLUMN + ", " + LASTREFRESHCOLUMN + ") " +
                "VALUES (?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (" + USERNAMECOLUMN + ", " + ROLECOLUMN + ") DO UPDATE SET " +
                LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN,
                username, role
            );
        } catch (Exception e) {
            logger.warn("Unable to add role : {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    @Transactional
    public boolean addUser(final String username, final String password,
                           final List<String> roles, final boolean isImported) throws DatafariServerException {
        try {
            sql.getJdbcTemplate().update(
                "INSERT INTO " + USERCOLLECTION + " (" + USERNAMECOLUMN + ", " + PASSWORDCOLUMN + ", " + ISIMPORTEDCOLUMN + ", " + LASTREFRESHCOLUMN + ") " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (" + USERNAMECOLUMN + ") DO UPDATE SET " +
                PASSWORDCOLUMN + " = EXCLUDED." + PASSWORDCOLUMN + ", " +
                ISIMPORTEDCOLUMN + " = EXCLUDED." + ISIMPORTEDCOLUMN + ", " +
                LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN,
                username, password, isImported
            );
            for (String role : roles) {
                addRole(role, username);
            }
            return true;
        } catch (Exception e) {
            logger.warn("Unable to add user : {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    private void refreshRoles(final String username, final List<String> roles) throws DatafariServerException {
        for (final String role : roles) {
            try {
                sql.getJdbcTemplate().update(
                    "UPDATE " + ROLECOLLECTION + " SET " + LASTREFRESHCOLUMN + " = CURRENT_TIMESTAMP " +
                    "WHERE " + USERNAMECOLUMN + " = ? AND " + ROLECOLUMN + " = ?",
                    username, role
                );
            } catch (Exception e) {
                logger.warn("Unable to refresh roles : {}", e.getMessage());
                throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
            }
        }
    }

    public void refreshUser(final String username) throws DatafariServerException {
        final List<String> roles = getRoles(username);
        try {
            sql.getJdbcTemplate().update(
                "UPDATE " + USERCOLLECTION + " SET " + LASTREFRESHCOLUMN + " = CURRENT_TIMESTAMP " +
                "WHERE " + USERNAMECOLUMN + " = ?",
                username
            );
        } catch (Exception e) {
            logger.warn("Unable to refresh user : {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
        refreshRoles(username, roles);
    }

    @Transactional
    public void deleteUser(final String username) throws DatafariServerException {
        try {
            sql.getJdbcTemplate().update(
                "DELETE FROM " + ROLECOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?",
                username
            );
            sql.getJdbcTemplate().update(
                "DELETE FROM " + USERCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?",
                username
            );
        } catch (Exception e) {
            logger.warn("Unable to remove user : {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    public void deleteRole(final String role, final String username) throws DatafariServerException {
        try {
            sql.getJdbcTemplate().update(
                "DELETE FROM " + ROLECOLLECTION + " WHERE " + USERNAMECOLUMN + " = ? AND " + ROLECOLUMN + " = ?",
                username, role
            );
        } catch (Exception e) {
            logger.warn("Unable to remove roles : {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }
}