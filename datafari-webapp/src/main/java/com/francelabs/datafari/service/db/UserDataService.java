package com.francelabs.datafari.service.db;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;

/**
 * UserDataService
 * - Keeps original public constants and method signatures for backward compatibility.
 * - Uses two password columns:
 *     password         -> SHA-256 hex (legacy, kept for the webapp)
 *     password_bcrypt  -> bcrypt hash (used by Apache mod_authn_dbd)
 */
@Service
public class UserDataService {

    private static final Logger logger = LogManager.getLogger(UserDataService.class.getName());

    // ===== Legacy singleton bridge for old code calling getInstance() =====
    private static volatile UserDataService instance;

    // ===== Table names =====
    public static final String USERCOLLECTION = "users";
    public static final String ROLECOLLECTION = "roles";

    // ===== Column names (keep legacy names) =====
    public static final String USERNAMECOLUMN    = "username";
    public static final String PASSWORDCOLUMN    = "password";          // legacy SHA-256 column name
    public static final String PASSWORD_BCRYPT_COL = "password_bcrypt"; // new bcrypt column
    public static final String ISIMPORTEDCOLUMN  = "is_imported";
    public static final String LASTREFRESHCOLUMN = "last_refresh";
    public static final String ROLECOLUMN        = "role";

    // ===== Legacy symbols still referenced elsewhere =====
    public static final String SEARCHADMINISTRATOR = "SearchAdministrator";
    public static final String IMPORTCOLUMN        = "imported"; // kept for compatibility (not used)

    // ===== Config / SQL handle =====
    private final SqlService sql;

    /** Legacy accessor for code paths still using a static singleton. */
    public static synchronized UserDataService getInstance() {
        return instance;
    }

    public UserDataService(SqlService sql) {
        this.sql = sql;
        instance = this; // expose Spring bean to legacy static accessor
    }

    // ---------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------

    /** Returns true if the user exists. */
    public boolean isInBase(final String username) throws DatafariServerException {
        try {
            Integer one = sql.getJdbcTemplate().query(
                "SELECT 1 FROM " + USERCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?",
                ps -> ps.setString(1, username),
                rs -> rs.next() ? 1 : null
            );
            return one != null;
        } catch (Exception e) {
            logger.warn("Unable to check if user exists: {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    /**
     * Original signature kept: returns SHA-256 hex from column 'password'.
     * Used by the legacy login path that compares SHA-256.
     */
    public String getPassword(final String username) throws DatafariServerException {
        try {
            List<String> list = sql.getJdbcTemplate().query(
                "SELECT " + PASSWORDCOLUMN + " FROM " + USERCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?",
                ps -> ps.setString(1, username),
                (rs, rn) -> rs.getString(1)
            );
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            logger.warn("Unable to get password (sha256): {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    /** Optional helper: returns bcrypt hash from column 'password_bcrypt'. */
    public String getPasswordBcrypt(final String username) throws DatafariServerException {
        try {
            List<String> list = sql.getJdbcTemplate().query(
                "SELECT " + PASSWORD_BCRYPT_COL + " FROM " + USERCOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?",
                ps -> ps.setString(1, username),
                (rs, rn) -> rs.getString(1)
            );
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            logger.warn("Unable to get password (bcrypt): {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    /** Internal helper: returns minimal user JSON (username, password sha256, imported). */
    private JSONObject getUser(final String username) throws DatafariServerException {
        try {
            List<JSONObject> list = sql.getJdbcTemplate().query(
                "SELECT " + USERNAMECOLUMN + ", " + PASSWORDCOLUMN + ", " + ISIMPORTEDCOLUMN +
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
            logger.warn("Unable to get user: {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    /** Returns roles for the given user. */
    public List<String> getRoles(final String username) throws DatafariServerException {
        try {
            return sql.getJdbcTemplate().query(
                "SELECT " + ROLECOLUMN + " FROM " + ROLECOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?",
                ps -> ps.setString(1, username),
                (rs, rn) -> rs.getString(ROLECOLUMN)
            );
        } catch (Exception e) {
            logger.warn("Unable to get roles: {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    /** Returns all AD users with their roles. */
    public JSONArray getAllADUsers() throws DatafariServerException {
        return getUsersByImported(true);
    }

    /** Returns all Datafari (local) users with their roles. */
    public JSONArray getAllDatafariUsers() throws DatafariServerException {
        return getUsersByImported(false);
    }

    /** Returns all users and their roles. */
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
            logger.warn("Unable to get all users: {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    /** Returns users filtered by is_imported and attaches their roles. */
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

            // Attach roles only for selected users
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
            logger.warn("Unable to get filtered users: {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Mutations
    // ---------------------------------------------------------------------

    /**
     * Original method (signature unchanged): updates ONLY the SHA-256 column.
     * Kept for backward compatibility where only SHA-256 is expected.
     */
    public void changePassword(final String passwordHashedSha256, final String username) throws DatafariServerException {
        try {
            sql.getJdbcTemplate().update(
                "UPDATE " + USERCOLLECTION + " SET " +
                PASSWORDCOLUMN + " = ?, " + LASTREFRESHCOLUMN + " = CURRENT_TIMESTAMP " +
                "WHERE " + USERNAMECOLUMN + " = ?",
                passwordHashedSha256, username
            );
        } catch (Exception e) {
            logger.warn("Unable to change password (sha256 only): {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    /**
     * New method: updates BOTH SHA-256 and bcrypt columns.
     * Used by the new User.changePassword(newPassword) path.
     */
    public void changePasswordDualHash(final String username,
                                       final String passwordHashedSha256,
                                       final String passwordBcrypt) throws DatafariServerException {
        try {
            sql.getJdbcTemplate().update(
                "UPDATE " + USERCOLLECTION + " SET " +
                PASSWORDCOLUMN + " = ?, " +
                PASSWORD_BCRYPT_COL + " = ?, " +
                LASTREFRESHCOLUMN + " = CURRENT_TIMESTAMP " +
                "WHERE " + USERNAMECOLUMN + " = ?",
                passwordHashedSha256, passwordBcrypt, username
            );
        } catch (Exception e) {
            logger.warn("Unable to change password (dual): {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    /** Adds a role to a user (idempotent via ON CONFLICT). */
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
            logger.warn("Unable to add role: {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    /**
     * Original method (signature unchanged): inserts/updates user with SHA-256 only.
     * Kept for backward compatibility with legacy code paths.
     */
    @Transactional
    public boolean addUser(final String username, final String passwordSha256,
                           final List<String> roles, final boolean isImported) throws DatafariServerException {
        try {
            sql.getJdbcTemplate().update(
                "INSERT INTO " + USERCOLLECTION + " (" +
                    USERNAMECOLUMN + ", " + PASSWORDCOLUMN + ", " + ISIMPORTEDCOLUMN + ", " + LASTREFRESHCOLUMN + ") " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (" + USERNAMECOLUMN + ") DO UPDATE SET " +
                    PASSWORDCOLUMN + " = EXCLUDED." + PASSWORDCOLUMN + ", " +
                    ISIMPORTEDCOLUMN + " = EXCLUDED." + ISIMPORTEDCOLUMN + ", " +
                    LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN,
                username, passwordSha256, isImported
            );
            for (String role : roles) {
                addRole(role, username);
            }
            return true;
        } catch (Exception e) {
            logger.warn("Unable to add user (sha256 only): {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    /**
     * New method: inserts/updates user with BOTH SHA-256 and bcrypt.
     * Used by the new User.signup(...) path.
     */
    @Transactional
    public boolean addUserDualHash(final String username, final String passwordSha256, final String passwordBcrypt,
                                   final List<String> roles, final boolean isImported) throws DatafariServerException {
        try {
            sql.getJdbcTemplate().update(
                "INSERT INTO " + USERCOLLECTION + " (" +
                    USERNAMECOLUMN + ", " + PASSWORDCOLUMN + ", " + PASSWORD_BCRYPT_COL + ", " +
                    ISIMPORTEDCOLUMN + ", " + LASTREFRESHCOLUMN + ") " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (" + USERNAMECOLUMN + ") DO UPDATE SET " +
                    PASSWORDCOLUMN + " = EXCLUDED." + PASSWORDCOLUMN + ", " +
                    PASSWORD_BCRYPT_COL + " = EXCLUDED." + PASSWORD_BCRYPT_COL + ", " +
                    ISIMPORTEDCOLUMN + " = EXCLUDED." + ISIMPORTEDCOLUMN + ", " +
                    LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN,
                username, passwordSha256, passwordBcrypt, isImported
            );
            for (String role : roles) {
                addRole(role, username);
            }
            return true;
        } catch (Exception e) {
            logger.warn("Unable to add user (dual): {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    /** Touch user last_refresh and roles timestamps. */
    public void refreshUser(final String username) throws DatafariServerException {
        final List<String> roles = getRoles(username);
        try {
            sql.getJdbcTemplate().update(
                "UPDATE " + USERCOLLECTION + " SET " + LASTREFRESHCOLUMN + " = CURRENT_TIMESTAMP " +
                "WHERE " + USERNAMECOLUMN + " = ?",
                username
            );
        } catch (Exception e) {
            logger.warn("Unable to refresh user: {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
        refreshRoles(username, roles);
    }

    /** Touch roles last_refresh for the given user/roles set. */
    private void refreshRoles(final String username, final List<String> roles) throws DatafariServerException {
        for (final String role : roles) {
            try {
                sql.getJdbcTemplate().update(
                    "UPDATE " + ROLECOLLECTION + " SET " + LASTREFRESHCOLUMN + " = CURRENT_TIMESTAMP " +
                    "WHERE " + USERNAMECOLUMN + " = ? AND " + ROLECOLUMN + " = ?",
                    username, role
                );
            } catch (Exception e) {
                logger.warn("Unable to refresh roles: {}", e.getMessage());
                throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
            }
        }
    }

    /** Deletes a user and its roles. */
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
            logger.warn("Unable to remove user: {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }

    /** Deletes one role for a given user. */
    public void deleteRole(final String role, final String username) throws DatafariServerException {
        try {
            sql.getJdbcTemplate().update(
                "DELETE FROM " + ROLECOLLECTION + " WHERE " + USERNAMECOLUMN + " = ? AND " + ROLECOLUMN + " = ?",
                username, role
            );
        } catch (Exception e) {
            logger.warn("Unable to remove role: {}", e.getMessage());
            throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
        }
    }
}