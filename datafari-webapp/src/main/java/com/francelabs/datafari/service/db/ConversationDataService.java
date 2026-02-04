/*******************************************************************************
 *  Copyright 2026 France Labs
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.service.db;

import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import org.json.simple.parser.JSONParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Service
public class ConversationDataService {

  // DB table names
  public static final String CONVERSATION_COLLECTION  = "conversation";
  public static final String DOCS_BASKET_COLLECTION   = "docsbasket";
  public static final String MESSAGES_COLLECTION      = "messages";

  // DB column names
  public static final String ID_COLUMN                = "id";
  public static final String TITLE_COLUMN             = "title";
  public static final String CONVERSATION_ID_COLUMN   = "conversation_id";
  public static final String CONTENT_COLUMN           = "content";
  public static final String ROLE_COLUMN              = "role";
  public static final String USER_COLUMN              = "username";
  public static final String CREATED_AT               = "created_at";
  public static final String DOC_ID_COLUMN            = "document_id";
  public static final String DOC_TITLE_COLUMN         = "document_title";
  public static final String SEARCH_RESULTS_COLUMN    = "search_results";

  public static boolean enabled;

  private static final Logger logger = LogManager.getLogger(ConversationDataService.class);

  private static volatile ConversationDataService instance; // legacy bridge for existing static calls

  private final SqlService sql;

  // RowMapper that converts a row into Properties (kept for backward compatibility with existing API)
  private static final RowMapper<Properties> CONVERSATION_MAPPER = (rs, rowNum) -> rowToProps(rs);


  // --- Legacy bridge: keep getInstance() so older code keeps working --------------------
  public static synchronized ConversationDataService getInstance() {
    return instance;
  }

  public ConversationDataService(final SqlService sql) {
    this.sql = sql;
    instance = this; // publish legacy singleton reference when Spring creates the bean

    // Is conversation storage enabled ?
    RagConfiguration config = RagConfiguration.getInstance();
    enabled = config.getBooleanProperty(RagConfiguration.ENABLE_CONVERSATION_STORAGE);
  }

  private void checkIdEnabled() throws DatafariServerException {
      if (enabled) {
          throw new DatafariServerException(CodesReturned.FALSE, "Conversation storage is disabled.");
      }
  }

  // -------------------------------------------------------------------------------------
  // Create
  // -------------------------------------------------------------------------------------
  public String createConversation(final Properties conversationProp) throws DatafariServerException {
    checkIdEnabled();
    try {
      final UUID uuid = UUID.randomUUID();
      SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
      final String defaultTitle  = dateFormat.format(new Date());

      sql.getJdbcTemplate().update(
          "INSERT INTO public." + CONVERSATION_COLLECTION + " (" +
              ID_COLUMN + ", " + TITLE_COLUMN + ", " + USER_COLUMN + ", " + CREATED_AT +") " +
              "VALUES (?, ?, ?, ?)",
          uuid,
          conversationProp.getProperty("title", defaultTitle),
          conversationProp.getProperty("username"),
          Timestamp.from(Instant.now())
      );

      return uuid.toString();
    } catch (Exception e) {
      logger.error("Unable to create conversation", e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public String addMessage(final Properties messageProp) throws DatafariServerException {
    checkIdEnabled();
    try {
        final UUID uuid = UUID.randomUUID();
        final UUID conversationId = UUID.fromString(messageProp.getProperty("conversationId"));

        final String role = messageProp.getProperty("role", "user");
        final String content = messageProp.getProperty("content", "");
        final String searchResults = messageProp.getProperty("searchResults"); // Can be null

        // If the message contains search results
        if (searchResults != null && !searchResults.isBlank()) {
            try {
                // JSON validation
                new JSONParser().parse(searchResults);
            } catch (Exception ex) {
                throw new DatafariServerException(CodesReturned.PARAMETERNOTWELLSET, "Invalid search_results JSON");
            }

            sql.getJdbcTemplate().update(
                "INSERT INTO public.messages (id, conversation_id, role, content, created_at, search_results) " +
                    "VALUES (?, ?, ?, ?, ?, ?::jsonb)",
                uuid, conversationId, role, content, Timestamp.from(Instant.now()), searchResults
            );
        } else {
            sql.getJdbcTemplate().update(
                "INSERT INTO public.messages (id, conversation_id, role, content, created_at, search_results) " +
                    "VALUES (?, ?, ?, ?, ?, NULL)",
                uuid, conversationId, role, content, Timestamp.from(Instant.now())
            );
        }

        return uuid.toString();
    } catch (Exception e) {
        logger.error("Unable to save message", e);
        throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public String addDocToBasket(final Properties messageProp) throws DatafariServerException {
    try {
      checkIdEnabled();
      final UUID uuid = UUID.randomUUID();
      final UUID conversationId = UUID.fromString(messageProp.getProperty("conversationId"));


      sql.getJdbcTemplate().update(
          "INSERT INTO public." + DOCS_BASKET_COLLECTION + " (" +
              ID_COLUMN + ", " + CONVERSATION_ID_COLUMN + ", " + DOC_ID_COLUMN + ", " + DOC_TITLE_COLUMN + ") " +
              "VALUES (?, ?, ?, ?)",
          uuid,
          conversationId,
          messageProp.getProperty("docId"),
          messageProp.getProperty("docTitle")
      );

      return uuid.toString();
    } catch (Exception e) {
      logger.error("Unable to add document to basket", e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  // -------------------------------------------------------------------------------------
  // Read
  // -------------------------------------------------------------------------------------
  public List<Properties> getUserConversations(final String username) throws DatafariServerException {
    checkIdEnabled();
    if (username == null || username.isBlank()) {
      throw new DatafariServerException(CodesReturned.PARAMETERNOTWELLSET, "Missing username");
    }

    try {

      return sql.getJdbcTemplate().query(
          "SELECT " +
              "c.*, " +
              "COALESCE(MAX(m." + CREATED_AT + "), c." + CREATED_AT + ") AS last_message_at, " +
              "COUNT(m." + ID_COLUMN + ") AS message_count " +
              "FROM public." + CONVERSATION_COLLECTION + " c " +
              "LEFT JOIN public." + MESSAGES_COLLECTION + " m ON m." + CONVERSATION_ID_COLUMN + " = c." + ID_COLUMN + " " +
              "WHERE c." + USER_COLUMN + " = ? " +
              "GROUP BY c." + ID_COLUMN + ", c." + TITLE_COLUMN + ", c." + USER_COLUMN + ", c." + CREATED_AT + " " +
              "ORDER BY last_message_at DESC",
          ps -> ps.setString(1, username),
          CONVERSATION_MAPPER
      );
    } catch (Exception e) {
      logger.error("Unable to list conversations for the user: {}", username, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public String getConversationTitle(final String conversationId) throws DatafariServerException {
      checkIdEnabled();
      if (conversationId == null || conversationId.isBlank()) {
        return null;
      }

      try {
          final List<String> res = sql.getJdbcTemplate().query(
              "SELECT " + TITLE_COLUMN + " " +
                  "FROM public." + CONVERSATION_COLLECTION + " " +
                  "WHERE " + ID_COLUMN + " = ? " +
                  "LIMIT 1",
              new Object[] { UUID.fromString(conversationId) },
              (rs, rowNum) -> rs.getString(1)
          );

          return res.isEmpty() ? null : res.getFirst();

      } catch (Exception e) {
          logger.error("Unable to retrieve conversation title for id {}", conversationId, e);
          throw new DatafariServerException(
              CodesReturned.PROBLEMCONNECTIONDATABASE,
              e.getMessage()
          );
      }
  }

  public Properties getLatestConversation(final String username) throws DatafariServerException {
    checkIdEnabled();
    if (username == null || username.isBlank()) {
      throw new DatafariServerException(CodesReturned.PARAMETERNOTWELLSET, "Missing username");
    }

    try {
      final List<Properties> res = sql.getJdbcTemplate().query(
          "SELECT " +
                "c.*, " +
                "COALESCE(MAX(m." + CREATED_AT + "), c." + CREATED_AT + ") AS last_message_at " +
              "FROM public." + CONVERSATION_COLLECTION + " c " +
              "LEFT JOIN public." + MESSAGES_COLLECTION + " m ON m." + CONVERSATION_ID_COLUMN + " = c." + ID_COLUMN + " " +
              "WHERE c." + USER_COLUMN + " = ? " +
              "GROUP BY c." + ID_COLUMN + ", c." + TITLE_COLUMN + ", c." + USER_COLUMN + ", c." + CREATED_AT + " " +
              "ORDER BY last_message_at DESC " +
              "LIMIT 1",
          new Object[] { username },
          CONVERSATION_MAPPER
      );
      return res.isEmpty() ? null : res.getFirst();
    } catch (Exception e) {
      logger.error("Unable to get latest conversation for user {}", username, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public List<Properties> getMessagesByConversation(final String conversationId, final String username) throws DatafariServerException {
    checkIdEnabled();
    if (username == null || username.isBlank()) {
      throw new DatafariServerException(CodesReturned.PARAMETERNOTWELLSET, "Missing username");
    }

    try {
      return sql.getJdbcTemplate().query(
          "SELECT m.* " +
              "FROM public." + MESSAGES_COLLECTION + " m " +
              "JOIN public." + CONVERSATION_COLLECTION + " c ON c." + ID_COLUMN + " = m." + CONVERSATION_ID_COLUMN + " " +
              "WHERE c." + ID_COLUMN + " = ? " +
              "AND c." + USER_COLUMN + " = ? " +
              "ORDER BY m." + CREATED_AT + " ASC",
          ps -> {
            ps.setObject(1, UUID.fromString(conversationId));
            ps.setString(2, username);
          },
          CONVERSATION_MAPPER
      );
    } catch (Exception e) {
      logger.error("Unable to list messages for conversation {}", conversationId, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public List<Properties> getDocsBasketByConversation(final String conversationId, final String username) throws DatafariServerException {
    checkIdEnabled();
    if (username == null || username.isBlank()) {
      throw new DatafariServerException(CodesReturned.PARAMETERNOTWELLSET, "Missing username");
    }

    if (conversationId == null) {
      throw new DatafariServerException(CodesReturned.PARAMETERNOTWELLSET, "Missing conversationId");
    }

    try {
      return sql.getJdbcTemplate().query(
          "SELECT db.* " +
              "FROM public." + DOCS_BASKET_COLLECTION + " db " +
              "JOIN public." + CONVERSATION_COLLECTION + " c ON c." + ID_COLUMN + " = db." + CONVERSATION_ID_COLUMN + " " +
              "WHERE c." + ID_COLUMN + " = ? " +
              "AND c." + USER_COLUMN + " = ? " +
              "ORDER BY db.created_at DESC",
          ps -> {
            ps.setObject(1, UUID.fromString(conversationId));
            ps.setString(2, username);
          },
          CONVERSATION_MAPPER
      );
    } catch (Exception e) {
      logger.error("Unable to list messages for conversation {}", conversationId, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * @return true if the conversationId belongs to the user
   */
  public boolean userOwnsConversation(String conversationId, String username) {
    if (conversationId == null || conversationId.isBlank() || username == null) {
      return false;
    }

    try {
      final List<Integer> res = sql.getJdbcTemplate().query(
          "SELECT 1 " +
              "FROM public." + CONVERSATION_COLLECTION + " " +
              "WHERE " + ID_COLUMN + " = ? " +
              "AND " + USER_COLUMN + " = ? " +
              "LIMIT 1",
          new Object[] { UUID.fromString(conversationId), username },
          (rs, rowNum) -> rs.getInt(1)
      );
      return !res.isEmpty();
    } catch (Exception e) {
      logger.warn("userOwnsConversation check failed for conversationId={} user={}", conversationId, username, e);
      return false;
    }
  }

  // -------------------------------------------------------------------------------------
  // Update
  // -------------------------------------------------------------------------------------


  // -------------------------------------------------------------------------------------
  // Delete
  // -------------------------------------------------------------------------------------
  public void deleteConversation(final String id, String username) throws DatafariServerException {
    try {
      sql.getJdbcTemplate().update(
          "DELETE FROM public." + CONVERSATION_COLLECTION + " WHERE " + ID_COLUMN + " = ? AND " + USER_COLUMN + " = ?",
          UUID.fromString(id),
          username
      );
    } catch (Exception e) {
      logger.error("Unable to delete alert {}", id, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  public void removeDocFromBasketById(final String basketId, final String username) throws DatafariServerException {
    checkIdEnabled();
    if (username == null || username.isBlank()) {
      throw new DatafariServerException(CodesReturned.PARAMETERNOTWELLSET, "Missing username");
    }

    try {
      sql.getJdbcTemplate().update(
          "DELETE FROM public." + DOCS_BASKET_COLLECTION + " b " +
              "USING public." + CONVERSATION_COLLECTION + " c " +
              "WHERE b." + CONVERSATION_ID_COLUMN + " = c." + ID_COLUMN + " " +
              "AND b." + ID_COLUMN + " = ? " +
              "AND c." + USER_COLUMN + " = ?",
          UUID.fromString(basketId),
          username
      );
    } catch (Exception e) {
      logger.error("Unable to remove doc basket entry {} for user {}", basketId, username, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  // -------------------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------------------
  private static Properties rowToProps(final ResultSet rs) throws SQLException {
    final Properties p = new Properties();
    final ResultSetMetaData md = rs.getMetaData();
    final int n = md.getColumnCount();

    for (int i = 1; i <= n; i++) {
      final String key = md.getColumnLabel(i); // use the alias if any
      final Object val = rs.getObject(i);
      if (val == null) {
        continue;
      }

      if (val instanceof Timestamp) {
        // ISO-8601 UTC (plus stable côté front)
        final String iso = ((Timestamp) val).toInstant().atOffset(ZoneOffset.UTC).toString();
        p.setProperty(key, iso);
      } else {
        p.setProperty(key, String.valueOf(val));
      }
    }
    return p;
  }

  /**
   * Rename a conversation that belongs to user
   */
  public void updateConversationTitle(final String conversationId, final String username, final String newTitle)
          throws DatafariServerException {
      checkIdEnabled();
      if (username == null || username.isBlank()) {
          throw new DatafariServerException(CodesReturned.PARAMETERNOTWELLSET, "Missing username");
      } else if (conversationId == null || conversationId.isBlank() || newTitle == null || newTitle.isBlank()) {
          throw new DatafariServerException(CodesReturned.PARAMETERNOTWELLSET, "Invalid request. Missing conversationId or title.");
      }

      try {
        final UUID convId = UUID.fromString(conversationId);

        final int updated = sql.getJdbcTemplate().update(
            "UPDATE public." + CONVERSATION_COLLECTION + " " +
                "SET " + TITLE_COLUMN + " = ? " +
                "WHERE " + ID_COLUMN + " = ? " +
                "AND " + USER_COLUMN + " = ?",
            newTitle,
            convId,
            username
        );

        if (updated == 0) {
          // Missing conversation or wrong username
          throw new DatafariServerException(CodesReturned.PROBLEMQUERY, "Conversation not found or forbidden.");
        }

      } catch (IllegalArgumentException badUuid) {
          throw new DatafariServerException(CodesReturned.PARAMETERNOTWELLSET, "Invalid conversationId UUID.");
      } catch (Exception e) {
        logger.error("Unable to update conversation title for id {} user {}", conversationId, username, e);
        throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
      }
  }
}