package com.francelabs.datafari.service.db;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.GDPRConfiguration;

public class DocumentDataService {

  // ==== Constantes table/colonnes =================================================
  public static final String USERNAMECOLUMN = "username";
  public static final String DOCUMENTIDCOLUMN = "document_id";
  public static final String DOCUMENTTITLECOLUMN = "document_title";
  public static final String FAVORITECOLLECTION = "favorite";
  // NB: pour éviter le mot réservé SQL 'LIKE', on utilise la table 'liked'
  public static final String LIKECOLLECTION = "liked";
  public static final String LASTREFRESHCOLUMN = "last_refresh";

  private static final Logger logger = LogManager.getLogger(DocumentDataService.class);

  // ==== Singleton ================================================================
  private static DocumentDataService instance;
  public static synchronized DocumentDataService getInstance() throws DatafariServerException {
    if (instance == null) {
      instance = new DocumentDataService();
    }
    return instance;
  }

  // ==== Dépendances SQL ==========================================================
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate named;

  // ==== Divers ==================================================================
  private final String userDataTTL; // conservé pour compatibilité (pas utilisé en SQL)

  private DocumentDataService() {
    // Récupération des helpers SQL (cf. SqlService fourni précédemment)
    this.jdbc = SqlService.get().jdbc();
    this.named = SqlService.get().named();
    this.userDataTTL = GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL);
  }

  // ==== Mappers =================================================================
  private static final RowMapper<String> STRING_MAPPER = (rs, i) -> rs.getString(1);

  // ==== Likes ===================================================================
  /** Ajoute un like (idempotent côté applicatif). */
  public void addLike(final String username, final String idDocument) throws DatafariServerException {
    try {
      // ON CONFLICT (username, document_id) DO NOTHING si tu as une PK/unique dessus
      final String sql = "INSERT INTO " + LIKECOLLECTION +
          " (" + USERNAMECOLUMN + ", " + DOCUMENTIDCOLUMN + ", " + LASTREFRESHCOLUMN + ") " +
          "VALUES (?, ?, now()) " +
          "ON CONFLICT DO NOTHING";
      jdbc.update(sql, username, idDocument);
    } catch (Exception e) {
      logger.warn("Unable to add like : {}", e.getMessage());
      throw new DatafariServerException(CodesReturned.ALREADYPERFORMED, e.getMessage());
    }
  }

  /** Retire un like. */
  public void unlike(final String username, final String idDocument) throws DatafariServerException {
    try {
      final String sql = "DELETE FROM " + LIKECOLLECTION +
          " WHERE " + USERNAMECOLUMN + " = ? AND " + DOCUMENTIDCOLUMN + " = ?";
      jdbc.update(sql, username, idDocument);
    } catch (Exception e) {
      logger.warn("Unable to unlike : {}", e.getMessage());
      throw new DatafariServerException(CodesReturned.ALREADYPERFORMED, e.getMessage());
    }
  }

  /** Renvoie la liste des IDs likés par l’utilisateur (ou test un sous-ensemble). */
  public List<String> getLikes(final String username, final String[] documentIDs) throws DatafariServerException {
    try {
      if (documentIDs == null) {
        final String q = "SELECT " + DOCUMENTIDCOLUMN +
                         " FROM " + LIKECOLLECTION +
                         " WHERE " + USERNAMECOLUMN + " = ?";
        return jdbc.query(q, ps -> ps.setString(1, username), STRING_MAPPER);
      } else if (documentIDs.length == 0) {
        return Collections.emptyList();
      } else {
        final String q = "SELECT " + DOCUMENTIDCOLUMN +
                         " FROM " + LIKECOLLECTION +
                         " WHERE " + USERNAMECOLUMN + " = :u AND " + DOCUMENTIDCOLUMN + " IN (:ids)";
        final MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("u", username)
            .addValue("ids", Arrays.asList(documentIDs));
        return named.query(q, params, (rs, i) -> rs.getString(DOCUMENTIDCOLUMN));
      }
    } catch (Exception e) {
      logger.warn("Unable to getLikes : {}", e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /** Supprime tous les likes d’un user (sans toucher aux favoris). */
  public void removeLikes(final String username) throws DatafariServerException {
    try {
      final String sql = "DELETE FROM " + LIKECOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
      jdbc.update(sql, username);
    } catch (Exception e) {
      logger.warn("Unable to remove likes for user {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /** Met à jour last_refresh sur tous les likes d’un user. */
  public void refreshLikes(final String username) throws DatafariServerException {
    try {
      final String sql = "UPDATE " + LIKECOLLECTION +
                         " SET " + LASTREFRESHCOLUMN + " = now()" +
                         " WHERE " + USERNAMECOLUMN + " = ?";
      jdbc.update(sql, username);
    } catch (Exception e) {
      logger.warn("Unable to refresh likes for user {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  // ==== Favoris =================================================================
  /** Ajoute un favori. */
  public void addFavorite(final String username, final String idDocument, final String titleDocument)
      throws DatafariServerException {
    try {
      final String sql = "INSERT INTO " + FAVORITECOLLECTION +
          " (" + USERNAMECOLUMN + ", " + DOCUMENTIDCOLUMN + ", " + DOCUMENTTITLECOLUMN + ", " + LASTREFRESHCOLUMN + ") " +
          "VALUES (?, ?, ?, now()) " +
          "ON CONFLICT (" + USERNAMECOLUMN + "," + DOCUMENTIDCOLUMN + ") DO UPDATE SET " +
          DOCUMENTTITLECOLUMN + " = EXCLUDED." + DOCUMENTTITLECOLUMN + ", " +
          LASTREFRESHCOLUMN + " = EXCLUDED." + LASTREFRESHCOLUMN;
      jdbc.update(sql, username, idDocument, titleDocument);
    } catch (Exception e) {
      logger.warn("Unable add favorite {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /** Supprime un favori. */
  public void deleteFavorite(final String username, final String idDocument) throws DatafariServerException {
    try {
      final String sql = "DELETE FROM " + FAVORITECOLLECTION +
          " WHERE " + DOCUMENTIDCOLUMN + " = ? AND " + USERNAMECOLUMN + " = ?";
      jdbc.update(sql, idDocument, username);
    } catch (Exception e) {
      logger.warn("Unable delete favorite {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /**
   * Renvoie les favoris sous forme de JSON (id/title) sérialisé en String
   * pour compatibilité avec l’existant.
   */
  public List<String> getFavorites(final String username, final String[] documentIDs) throws DatafariServerException {
    try {
      final RowMapper<String> FAVORITE_JSON_MAPPER = (rs, i) -> {
        final Map<String, String> fav = new HashMap<>();
        fav.put("id", rs.getString(DOCUMENTIDCOLUMN));
        fav.put("title", rs.getString(DOCUMENTTITLECOLUMN));
        return new JSONObject(fav).toJSONString();
      };

      if (documentIDs == null) {
        final String q = "SELECT " + DOCUMENTIDCOLUMN + ", " + DOCUMENTTITLECOLUMN +
                         " FROM " + FAVORITECOLLECTION +
                         " WHERE " + USERNAMECOLUMN + " = ?";
        return jdbc.query(q, ps -> ps.setString(1, username), FAVORITE_JSON_MAPPER);
      } else if (documentIDs.length == 0) {
        return Collections.emptyList();
      } else {
        final String q = "SELECT " + DOCUMENTIDCOLUMN + ", " + DOCUMENTTITLECOLUMN +
                         " FROM " + FAVORITECOLLECTION +
                         " WHERE " + USERNAMECOLUMN + " = :u AND " + DOCUMENTIDCOLUMN + " IN (:ids)";
        final MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("u", username)
            .addValue("ids", Arrays.asList(documentIDs));
        return named.query(q, params, FAVORITE_JSON_MAPPER);
      }
    } catch (Exception e) {
      logger.error("Unable getFavorites for {}", username, e);
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /** Supprime tous les favoris d’un user (sans toucher aux likes). */
  public void removeFavorites(final String username) throws DatafariServerException {
    try {
      final String sql = "DELETE FROM " + FAVORITECOLLECTION + " WHERE " + USERNAMECOLUMN + " = ?";
      jdbc.update(sql, username);
    } catch (Exception e) {
      logger.error("Unable removeFavorites for {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }

  /** Supprime tous les favoris et likes d’un user. */
  public void removeFavoritesAndLikeDB(final String username) throws DatafariServerException {
    removeFavorites(username);
    removeLikes(username);
  }

  /** Met à jour last_refresh sur tous les favoris d’un user. */
  public void refreshFavorites(final String username) throws DatafariServerException {
    try {
      final String sql = "UPDATE " + FAVORITECOLLECTION +
                         " SET " + LASTREFRESHCOLUMN + " = now()" +
                         " WHERE " + USERNAMECOLUMN + " = ?";
      jdbc.update(sql, username);
    } catch (Exception e) {
      logger.warn("Unable to refresh favorites for user {} : {}", username, e.getMessage());
      throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
    }
  }
}