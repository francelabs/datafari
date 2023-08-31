package com.francelabs.datafari.service.db;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.internal.core.type.codec.TimestampCodec;

import java.util.concurrent.TimeUnit;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.service.db.AccessTokenDataService.AccessToken;
import com.francelabs.datafari.service.db.StatisticsDataService.UserActions;
import com.francelabs.datafari.utils.Environment;
import com.francelabs.datafari.utils.GDPRConfiguration;
import com.francelabs.datafari.utils.UsageStatisticsConfiguration;
import com.francelabs.licence.Licence;
import com.francelabs.licence.exception.LicenceException;

import java.net.InetSocketAddress;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

// Data services tests are gathered in a single class to reuse the cassandra container as much as possible.
// The database mocking code is also the same for all the data services.
@RunWith(PowerMockRunner.class)
@PrepareForTest({ CassandraManager.class, Environment.class, UsageStatisticsConfiguration.class })
@PowerMockIgnore({ "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*" })
public class TestDataServices {

    private static GenericContainer<?> cassandraContainer = new GenericContainer<>(
            DockerImageName.parse("cassandra:4.0.3"))
            .withClasspathResourceMapping("service/db/testdb.cql",
                    "/etc/testdb.cql",
                    BindMode.READ_ONLY)
            .withExposedPorts(9042)
            .withStartupTimeout(Duration.ofMinutes(5));

    final static String configHomeTemp = "conf";
    final static String resourcePathStr = "src/test/resources/testDatafariConf";
    private CqlSession session;

    @BeforeClass
    public static void cassandraStartup() {
        cassandraContainer.start();
    }

    @Before
    public void setUp() throws Exception {
        // Resetting db content before each test
        String stderr = "";
        do {
            Container.ExecResult result = cassandraContainer.execInContainer("cqlsh", "-f", "/etc/testdb.cql");
            stderr = result.getStderr();
            if (stderr.contains("Connection refused")) {
                TimeUnit.SECONDS.sleep(5);
            }
        } while (stderr.contains("Connection refused"));

        // setting up the cassandra session
        final CqlSessionBuilder sessionBuilder = new CqlSessionBuilder()
                .addContactPoint(
                        new InetSocketAddress(
                                cassandraContainer.getHost(),
                                cassandraContainer.getMappedPort(9042)))
                .withLocalDatacenter("datacenter1")
                .withKeyspace("datafari");
        session = sessionBuilder.build();

        // create temp dir for config files mocking
        Path tempDirectory = Files.createTempDirectory(configHomeTemp);
        FileUtils.copyDirectory(new File(resourcePathStr), tempDirectory.toFile());

        // set datafari_home to temp dir
        PowerMockito.mockStatic(Environment.class);
        PowerMockito.when(Environment.getEnvironmentVariable("CONFIG_HOME"))
                .thenReturn(tempDirectory.toFile().getAbsolutePath());

        // Mock Cassandra Manager to always use the cassandra test session
        CassandraManager mockedManager = PowerMockito.mock(CassandraManager.class);
        PowerMockito.when(mockedManager.getSession()).thenReturn(session);
        PowerMockito.doNothing().when(mockedManager).closeSession();
        PowerMockito.mockStatic(CassandraManager.class);
        PowerMockito.when(CassandraManager.getInstance()).thenReturn(mockedManager);

        // Mock usage statistics configuration to always return true
        UsageStatisticsConfiguration mockedUsageStatisticsConfig = PowerMockito
                .mock(UsageStatisticsConfiguration.class);
        PowerMockito.when(
                mockedUsageStatisticsConfig.getProperty(UsageStatisticsConfiguration.ENABLED, "false"))
                .thenReturn("true");
        PowerMockito.mockStatic(UsageStatisticsConfiguration.class);
        PowerMockito.when(UsageStatisticsConfiguration.getInstance()).thenReturn(mockedUsageStatisticsConfig);

    }

    /**********************************************************
     *
     * Testing AccessTokenDataService
     *
     **********************************************************/
    @Test
    public void testGetToken() throws IOException {
        try {
            AccessToken retrieved = AccessTokenDataService.getInstance().getToken("john", "An api 1");
            Assert.assertEquals("An api 1", retrieved.getApi());
            Assert.assertEquals("An identifier 1", retrieved.getIdentifier());
            Assert.assertEquals("A token 1", retrieved.getToken());
        } catch (Exception e) {
            Assert.fail("An exception was raised while retrieving token: " + e.getMessage());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetTokens() throws IOException {
        try {
            JSONArray retrievedTokens = AccessTokenDataService.getInstance().getTokens("john");
            JSONArray expectedTokens = new JSONArray();
            List<JSONObject> expectedTokensList = new ArrayList<>();
            HashMap<String, String> token = new HashMap<String, String>();
            token.put("api", "An api 1");
            token.put("identifier", "An identifier 1");
            token.put("a_token", "A token 1");
            expectedTokensList.add(new JSONObject(token));
            token = new HashMap<String, String>();
            token.put("api", "An api 2");
            token.put("identifier", "An identifier 2");
            token.put("a_token", "A token 2");
            expectedTokensList.add(new JSONObject(token));
            // No way to parametrize the addAll call to org.json.simple.JSONArray
            // And it does not support a constructor taking a List or Collection as
            // parameter.
            // Thus the suppress warning used on this method
            expectedTokens.addAll(expectedTokensList);
            Assert.assertEquals(expectedTokens, retrievedTokens);
        } catch (Exception e) {
            Assert.fail("An exception was raised while retrieving tokens: " + e.getMessage());
        }
    }

    @Test
    public void testSetToken() {
        try {
            int code = AccessTokenDataService.getInstance().setToken("alice", "api3", "identifier3", "token3");
            AccessToken retrieved = AccessTokenDataService.getInstance().getToken("alice", "api3");
            Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
            Assert.assertEquals("api3", retrieved.getApi());
            Assert.assertEquals("identifier3", retrieved.getIdentifier());
            Assert.assertEquals("token3", retrieved.getToken());
        } catch (Exception e) {
            Assert.fail("An exception was raised while setting token: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateToken() {
        try {
            int code = AccessTokenDataService.getInstance().updateToken("john", "An api 1", "An identifier 1",
                    "new token 1");
            AccessToken retrieved = AccessTokenDataService.getInstance().getToken("john", "An api 1");
            Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
            Assert.assertEquals("An api 1", retrieved.getApi());
            Assert.assertEquals("An identifier 1", retrieved.getIdentifier());
            Assert.assertEquals("new token 1", retrieved.getToken());
        } catch (Exception e) {
            Assert.fail("An exception was raised while updating token: " + e.getMessage());
        }
    }

    @Test
    public void testDeleteToken() {
        try {
            int code = AccessTokenDataService.getInstance().deleteToken("john", "An api 1", "An identifier 1");
            AccessToken retrieved = AccessTokenDataService.getInstance().getToken("john", "An api 1");
            Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
            Assert.assertNull(retrieved);
        } catch (Exception e) {
            Assert.fail("An exception was raised while deleting token: " + e.getMessage());
        }
    }

    @Test
    public void testRemoveTokens() {
        try {
            int code = AccessTokenDataService.getInstance().removeTokens("john");
            JSONArray retrieved = AccessTokenDataService.getInstance().getTokens("john");
            Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
            Assert.assertEquals(new JSONArray(), retrieved);
        } catch (Exception e) {
            Assert.fail("An exception was raised while removing tokens: " + e.getMessage());
        }
    }

    @Test
    public void testRefreshTokens() {
        try {
            int userDataTTL = Integer
                    .parseInt(GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL));
            TimeUnit.SECONDS.sleep(10);
            ResultSet originalResults = session
                    .execute("SELECT last_refresh, TTL(last_refresh) FROM access_tokens WHERE username='john'");
            AccessTokenDataService.getInstance().refreshAccessTokens("john");
            ResultSet postRefreshResults = session
                    .execute("SELECT last_refresh, TTL(last_refresh) FROM access_tokens WHERE username='john'");
            Iterator<Row> originalIt = originalResults.iterator();
            Iterator<Row> postRefreshIt = postRefreshResults.iterator();
            while (originalIt.hasNext() && postRefreshIt.hasNext()) {
                Row originalRow = originalIt.next();
                Row postRefreshRow = postRefreshIt.next();
                Instant originalTimestamp = originalRow.get("last_refresh", new TimestampCodec());
                Integer originalTTL = originalRow.getInt("ttl(last_refresh)");
                Instant refreshedTimestamp = postRefreshRow.get("last_refresh", new TimestampCodec());
                Integer refreshedTTL = postRefreshRow.getInt("ttl(last_refresh)");
                Assert.assertTrue("Has the timestamp been refreshed", refreshedTimestamp.isAfter(originalTimestamp));
                Assert.assertTrue("Is refreshed TTL greater that original TTL", refreshedTTL > originalTTL);
                Assert.assertTrue("Original TTL should be at least 10 less than userDataTTL",
                        originalTTL <= userDataTTL - 10);
            }
            Assert.assertFalse(originalIt.hasNext());
            Assert.assertFalse(postRefreshIt.hasNext());
        } catch (Exception e) {
            Assert.fail("An exception was raised while setting history: " + e.getMessage());
        }
    }

    /**********************************************************
     *
     * Testing AlertDataService
     *
     **********************************************************/
    @Test
    public void testGetAlerts() throws IOException {
        try {
            List<Properties> alerts = AlertDataService.getInstance().getAlerts();
            Assert.assertEquals(2, alerts.size());
            Properties alertProp1 = new Properties();
            alertProp1.put("keyword", "keyword 2");
            alertProp1.put("filters", "filters 2");
            alertProp1.put("core", "core 2");
            alertProp1.put("frequency", "frequency 2");
            alertProp1.put("mail", "mail 2");
            alertProp1.put("subject", "subject 2");
            alertProp1.put("user", "alice");
            Properties alertProp2 = new Properties();
            alertProp2.put("keyword", "keyword 1");
            alertProp2.put("filters", "filters 1");
            alertProp2.put("core", "core 1");
            alertProp2.put("frequency", "frequency 1");
            alertProp2.put("mail", "mail 1");
            alertProp2.put("subject", "subject 1");
            alertProp2.put("user", "john");
            for (Properties alert : alerts) {
                if (alert.getProperty("keyword").contentEquals("keyword 2")) {
                    alertProp1.put("_id", alert.get("_id"));
                    Assert.assertEquals(alertProp1, alert);
                } else {
                    alertProp2.put("_id", alert.get("_id"));
                    Assert.assertEquals(alertProp2, alert);
                }
            }
        } catch (Exception e) {
            Assert.fail("An exception was raised while getting alerts: " + e.getMessage());
        }
    }

    @Test
    public void testGetUserAlerts() throws IOException {
        try {
            List<Properties> alerts = AlertDataService.getInstance().getUserAlerts("john");
            Assert.assertEquals(1, alerts.size());
            List<Properties> targetAlerts = new ArrayList<>();
            Properties alertProp = new Properties();
            alertProp.put("_id", alerts.get(0).getProperty("_id"));
            alertProp.put("keyword", "keyword 1");
            alertProp.put("filters", "filters 1");
            alertProp.put("core", "core 1");
            alertProp.put("frequency", "frequency 1");
            alertProp.put("mail", "mail 1");
            alertProp.put("subject", "subject 1");
            alertProp.put("user", "john");
            targetAlerts.add(alertProp);
            Assert.assertEquals(targetAlerts, alerts);
        } catch (Exception e) {
            Assert.fail("An exception was raised while getting user alerts: " + e.getMessage());
        }
    }

    @Test
    public void testAddAlert() {
        try {
            Properties alertProp = new Properties();
            alertProp.put("keyword", "keyword 3");
            alertProp.put("filters", "filters 3");
            alertProp.put("core", "core 3");
            alertProp.put("frequency", "frequency 3");
            alertProp.put("mail", "mail 3");
            alertProp.put("subject", "subject 3");
            alertProp.put("user", "alice");
            String uuidString = AlertDataService.getInstance().addAlert(alertProp);
            try {
                UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {
                Assert.fail("Couldn't create UUID from string when adding alert");
            }
            List<Properties> alerts = AlertDataService.getInstance().getUserAlerts("alice");
            Assert.assertEquals(2, alerts.size());
            Properties alertProp1 = new Properties();
            alertProp1.put("_id", alerts.get(0).getProperty("_id"));
            alertProp1.put("keyword", "keyword 2");
            alertProp1.put("filters", "filters 2");
            alertProp1.put("core", "core 2");
            alertProp1.put("frequency", "frequency 2");
            alertProp1.put("mail", "mail 2");
            alertProp1.put("subject", "subject 2");
            alertProp1.put("user", "alice");
            Properties alertProp2 = new Properties();
            alertProp2.put("_id", alerts.get(1).getProperty("_id"));
            alertProp2.put("keyword", "keyword 3");
            alertProp2.put("filters", "filters 3");
            alertProp2.put("core", "core 3");
            alertProp2.put("frequency", "frequency 3");
            alertProp2.put("mail", "mail 3");
            alertProp2.put("subject", "subject 3");
            alertProp2.put("user", "alice");
            for (Properties alert : alerts) {
                if (alert.getProperty("keyword").contentEquals("keyword 2")) {
                    alertProp1.put("_id", alert.get("_id"));
                    Assert.assertEquals(alertProp1, alert);
                } else {
                    alertProp2.put("_id", alert.get("_id"));
                    Assert.assertEquals(alertProp2, alert);
                }
            }
        } catch (Exception e) {
            Assert.fail("An exception was raised while adding alert: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateAlert() {
        try {
            List<Properties> alerts = AlertDataService.getInstance().getUserAlerts("alice");
            Properties alertProp = alerts.get(0);
            alertProp.put("keyword", "keyword 2 modified");
            alertProp.put("filters", "filters 2 modified");
            alertProp.put("core", "core 2 modified");
            alertProp.put("frequency", "frequency 2 modified");
            alertProp.put("mail", "mail 2 modified");
            alertProp.put("subject", "subject 2 modified");
            AlertDataService.getInstance().updateAlert(alertProp);

            alerts = AlertDataService.getInstance().getUserAlerts("alice");
            Assert.assertEquals(1, alerts.size());
            List<Properties> targetAlerts = new ArrayList<>();
            alertProp = new Properties();
            alertProp.put("_id", alerts.get(0).getProperty("_id"));
            alertProp.put("keyword", "keyword 2 modified");
            alertProp.put("filters", "filters 2 modified");
            alertProp.put("core", "core 2 modified");
            alertProp.put("frequency", "frequency 2 modified");
            alertProp.put("mail", "mail 2 modified");
            alertProp.put("subject", "subject 2 modified");
            alertProp.put("user", "alice");
            targetAlerts.add(alertProp);
            Assert.assertEquals(targetAlerts, alerts);
        } catch (Exception e) {
            Assert.fail("An exception was raised while updating alert: " + e.getMessage());
        }
    }

    @Test
    public void testDeleteUserAlerts() {
        try {
            AlertDataService.getInstance().deleteUserAlerts("john");
            List<Properties> alerts = AlertDataService.getInstance().getUserAlerts("john");
            Assert.assertEquals(0, alerts.size());
        } catch (Exception e) {
            Assert.fail("An exception was raised while deleting user alerts: " + e.getMessage());
        }
    }

    @Test
    public void testDeleteAlert() {
        try {
            List<Properties> alerts = AlertDataService.getInstance().getUserAlerts("john");
            int initialSize = alerts.size();
            Assert.assertTrue("There should be at least one alert for user john", initialSize >= 1);
            String removedId = alerts.get(0).getProperty("_id");
            AlertDataService.getInstance().deleteAlert(removedId);
            alerts = AlertDataService.getInstance().getUserAlerts("john");
            Assert.assertEquals(initialSize - 1, alerts.size());
            for (Properties alert : alerts) {
                Assert.assertNotEquals(removedId, alert.getProperty("_id"));
            }
        } catch (Exception e) {
            Assert.fail("An exception was raised while deleting user alerts: " + e.getMessage());
        }
    }

    @Test
    public void testRefreshAlerts() {
        try {
            int userDataTTL = Integer
                    .parseInt(GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL));
            TimeUnit.SECONDS.sleep(10);
            ResultSet originalResults = session
                    .execute("SELECT last_refresh, TTL(last_refresh) FROM alerts WHERE user='john'");
            AlertDataService.getInstance().refreshUserAlerts("john");
            ResultSet postRefreshResults = session
                    .execute("SELECT last_refresh, TTL(last_refresh) FROM alerts WHERE user='john'");
            Iterator<Row> originalIt = originalResults.iterator();
            Iterator<Row> postRefreshIt = postRefreshResults.iterator();
            while (originalIt.hasNext() && postRefreshIt.hasNext()) {
                Row originalRow = originalIt.next();
                Row postRefreshRow = postRefreshIt.next();
                Instant originalTimestamp = originalRow.get("last_refresh", new TimestampCodec());
                Integer originalTTL = originalRow.getInt("ttl(last_refresh)");
                Instant refreshedTimestamp = postRefreshRow.get("last_refresh", new TimestampCodec());
                Integer refreshedTTL = postRefreshRow.getInt("ttl(last_refresh)");
                Assert.assertTrue("The timestamp in last_refresh has been updated",
                        refreshedTimestamp.isAfter(originalTimestamp));
                Assert.assertTrue("Refreshed TTL is greater that original TTL", refreshedTTL > originalTTL);
                Assert.assertTrue("Original TTL should be at least 10 less than userDataTTL",
                        originalTTL <= userDataTTL - 10);
            }
            Assert.assertFalse(originalIt.hasNext());
            Assert.assertFalse(postRefreshIt.hasNext());
        } catch (Exception e) {
            Assert.fail("An exception was raised while setting history: " + e.getMessage());
        }
    }

    /**********************************************************
     *
     * Testing DepartmentDataService
     *
     **********************************************************/
    @Test
    public void testGetDepartment() throws IOException {
        try {
            String department = DepartmentDataService.getInstance().getDepartment("john");
            Assert.assertEquals("department 1", department);
        } catch (Exception e) {
            Assert.fail("An exception was raised while getting department: " + e.getMessage());
        }
    }

    @Test
    public void testSetDepartment() {
        try {
            int code = DepartmentDataService.getInstance().setDepartment("alice", "department 3");
            Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
            String retrieved = DepartmentDataService.getInstance().getDepartment("alice");
            Assert.assertEquals("department 3", retrieved);
        } catch (Exception e) {
            Assert.fail("An exception was raised while setting department: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateDepartment() {
        try {
            int code = DepartmentDataService.getInstance().updateDepartment("jack", "department 2 bis");
            Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
            String retrieved = DepartmentDataService.getInstance().getDepartment("jack");
            Assert.assertEquals("department 2 bis", retrieved);
        } catch (Exception e) {
            Assert.fail("An exception was raised while updating department: " + e.getMessage());
        }
    }

    @Test
    public void testDeleteDepartment() {
        try {
            int code = DepartmentDataService.getInstance().deleteDepartment("jack");
            Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
            String retrieved = DepartmentDataService.getInstance().getDepartment("jack");
            Assert.assertNull(retrieved);
        } catch (Exception e) {
            Assert.fail("An exception was raised while deleting user department: " + e.getMessage());
        }
    }

    /**********************************************************
     *
     * Testing DocumentDataService
     *
     **********************************************************/
    @Test
    public void testGetLikes() throws IOException {
        try {
            List<String> likes = DocumentDataService.getInstance().getLikes("john", null);
            List<String> expectedList = Arrays.asList("document_id 3", "document_id 2");
            Assert.assertEquals("Should have the same size", expectedList.size(), likes.size());
            Assert.assertTrue("Should contain all expected document id", likes.containsAll(expectedList));
        } catch (Exception e) {
            Assert.fail("An exception was raised while getting likes: " + e.getMessage());
        }
    }

    @Test
    public void testGetLikesWithList() throws IOException {
        try {
            String[] param = { "test", "truc", "bidule", "document_id 2" };
            List<String> likes = DocumentDataService.getInstance().getLikes("john", param);
            List<String> expectedList = Arrays.asList("document_id 2");
            Assert.assertEquals("Should have the same size", expectedList.size(), likes.size());
            Assert.assertTrue("Should contain all expected document id", likes.containsAll(expectedList));
        } catch (Exception e) {
            Assert.fail("An exception was raised while getting like with list: " + e.getMessage());
        }
    }

    @Test
    public void testAddLike() {
        try {
            DocumentDataService.getInstance().addLike("alice", "document id 4/with/specials'`weird/things");
            List<String> retrieved = DocumentDataService.getInstance().getLikes("alice", null);
            Assert.assertEquals(1, retrieved.size());
            Assert.assertEquals("document id 4/with/specials'`weird/things", retrieved.get(0));
        } catch (Exception e) {
            Assert.fail("An exception was raised while adding like: " + e.getMessage());
        }
    }

    @Test
    public void testUnlike() {
        try {
            DocumentDataService.getInstance().unlike("john", "document_id 2");
            String[] param = { "document_id 2" };
            List<String> retrieved = DocumentDataService.getInstance().getLikes("john", param);
            Assert.assertEquals(0, retrieved.size());
        } catch (Exception e) {
            Assert.fail("An exception was raised while using unlike: " + e.getMessage());
        }
    }

    @Test
    public void testRemoveLikes() {
        try {
            DocumentDataService.getInstance().removeLikes("john");
            List<String> retrieved = DocumentDataService.getInstance().getLikes("john", null);
            Assert.assertEquals(0, retrieved.size());
        } catch (Exception e) {
            Assert.fail("An exception was raised while removing likes: " + e.getMessage());
        }
    }

    @Test
    public void testRefreshLikes() {
        try {
            int userDataTTL = Integer
                    .parseInt(GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL));
            TimeUnit.SECONDS.sleep(10);
            ResultSet originalResults = session
                    .execute("SELECT last_refresh, TTL(last_refresh) FROM like WHERE username='john'");
            DocumentDataService.getInstance().refreshLikes("john");
            ResultSet postRefreshResults = session
                    .execute("SELECT last_refresh, TTL(last_refresh) FROM like WHERE username='john'");
            Iterator<Row> originalIt = originalResults.iterator();
            Iterator<Row> postRefreshIt = postRefreshResults.iterator();
            while (originalIt.hasNext() && postRefreshIt.hasNext()) {
                Row originalRow = originalIt.next();
                Row postRefreshRow = postRefreshIt.next();
                Instant originalTimestamp = originalRow.get("last_refresh", new TimestampCodec());
                Integer originalTTL = originalRow.getInt("ttl(last_refresh)");
                Instant refreshedTimestamp = postRefreshRow.get("last_refresh", new TimestampCodec());
                Integer refreshedTTL = postRefreshRow.getInt("ttl(last_refresh)");
                Assert.assertTrue("Has the timestamp been refreshed", refreshedTimestamp.isAfter(originalTimestamp));
                Assert.assertTrue("Is refreshed TTL greater that original TTL", refreshedTTL > originalTTL);
                Assert.assertTrue("Original TTL should be at least 10 less than userDataTTL",
                        originalTTL <= userDataTTL - 10);
            }
            Assert.assertFalse(originalIt.hasNext());
            Assert.assertFalse(postRefreshIt.hasNext());
        } catch (Exception e) {
            Assert.fail("An exception was raised while refreshing likes: " + e.getMessage());
        }
    }

    @Test
    public void testGetFavorites() throws IOException {
        try {
            List<String> favorites = DocumentDataService.getInstance().getFavorites("john", null);
            List<String> expectedList = new ArrayList<>();
            HashMap<String, Object> objContent = new HashMap<>();
            objContent.put("id", "document_id 1");
            objContent.put("title", "document_title 1");
            expectedList.add((new JSONObject(objContent)).toJSONString());
            objContent = new HashMap<>();
            objContent.put("id", "document_id 2");
            objContent.put("title", "document_title 2");
            expectedList.add((new JSONObject(objContent)).toJSONString());
            Assert.assertEquals("Should have the same size", expectedList.size(), favorites.size());
            Assert.assertTrue("Should contain all expected document id", favorites.containsAll(expectedList));
        } catch (Exception e) {
            Assert.fail("An exception was raised while getting favorites: " + e.getMessage());
        }
    }

    @Test
    public void testGetFavoritesWithList() throws IOException {
        try {
            String[] param = { "test", "truc", "bidule", "document_id 2" };
            List<String> favorites = DocumentDataService.getInstance().getFavorites("john", param);
            List<String> expectedList = new ArrayList<>();
            HashMap<String, Object> objContent = new HashMap<String, Object>();
            objContent.put("id", "document_id 2");
            objContent.put("title", "document_title 2");
            expectedList.add((new JSONObject(objContent)).toJSONString());
            Assert.assertEquals("Should have the same size", expectedList.size(), favorites.size());
            Assert.assertTrue("Should contain all expected document id", favorites.containsAll(expectedList));
        } catch (Exception e) {
            Assert.fail("An exception was raised while getting favorites with list: " + e.getMessage());
        }
    }

    @Test
    public void testAddFavorite() {
        try {
            DocumentDataService.getInstance().addFavorite("alice", "idDocument4", "titleDocument4");
            ;
            List<String> favorites = DocumentDataService.getInstance().getFavorites("alice", null);
            List<String> expectedList = new ArrayList<>();
            HashMap<String, Object> objContent = new HashMap<String, Object>();
            objContent.put("id", "idDocument4");
            objContent.put("title", "titleDocument4");
            expectedList.add((new JSONObject(objContent)).toJSONString());
            Assert.assertEquals("Should have the same size", expectedList.size(), favorites.size());
            Assert.assertTrue("Should contain all expected document id", favorites.containsAll(expectedList));
        } catch (Exception e) {
            Assert.fail("An exception was raised while adding favorite: " + e.getMessage());
        }
    }

    @Test
    public void testDeleteFavorite() {
        try {
            DocumentDataService.getInstance().deleteFavorite("john", "document_id 1");
            ;
            String[] param = { "document_id 1" };
            List<String> favorites = DocumentDataService.getInstance().getFavorites("john", param);
            Assert.assertEquals("Should be empty", 0, favorites.size());
        } catch (Exception e) {
            Assert.fail("An exception was raised while deleting favorite: " + e.getMessage());
        }
    }

    @Test
    public void testRemoveFavorites() {
        try {
            DocumentDataService.getInstance().removeFavorites("john");
            List<String> favorites = DocumentDataService.getInstance().getFavorites("john", null);
            Assert.assertEquals("Should be empty", 0, favorites.size());
        } catch (Exception e) {
            Assert.fail("An exception was raised while removing favorites: " + e.getMessage());
        }
    }

    @Test
    public void testRemoveFavoritesAndLikeDB() {
        try {
            DocumentDataService.getInstance().removeFavoritesAndLikeDB("john");
            List<String> favorites = DocumentDataService.getInstance().getFavorites("john", null);
            Assert.assertEquals("Should be empty", 0, favorites.size());
            List<String> likes = DocumentDataService.getInstance().getLikes("john", null);
            Assert.assertEquals("Should be empty", 0, likes.size());
        } catch (Exception e) {
            Assert.fail("An exception was raised while performing removeFavoriteAndLikeDB: " + e.getMessage());
        }
    }

    @Test
    public void testRefreshFavorites() {
        try {
            int userDataTTL = Integer
                    .parseInt(GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL));
            TimeUnit.SECONDS.sleep(10);
            ResultSet originalResults = session
                    .execute("SELECT last_refresh, TTL(last_refresh) FROM favorite WHERE username='john'");
            DocumentDataService.getInstance().refreshFavorites("john");
            ResultSet postRefreshResults = session
                    .execute("SELECT last_refresh, TTL(last_refresh) FROM favorite WHERE username='john'");
            Iterator<Row> originalIt = originalResults.iterator();
            Iterator<Row> postRefreshIt = postRefreshResults.iterator();
            while (originalIt.hasNext() && postRefreshIt.hasNext()) {
                Row originalRow = originalIt.next();
                Row postRefreshRow = postRefreshIt.next();
                Instant originalTimestamp = originalRow.get("last_refresh", new TimestampCodec());
                Integer originalTTL = originalRow.getInt("ttl(last_refresh)");
                Instant refreshedTimestamp = postRefreshRow.get("last_refresh", new TimestampCodec());
                Integer refreshedTTL = postRefreshRow.getInt("ttl(last_refresh)");
                Assert.assertTrue("Has the timestamp been refreshed", refreshedTimestamp.isAfter(originalTimestamp));
                Assert.assertTrue("Is refreshed TTL greater that original TTL", refreshedTTL > originalTTL);
                Assert.assertTrue("Original TTL should be at least 10 less than userDataTTL",
                        originalTTL <= userDataTTL - 10);
            }
            Assert.assertFalse(originalIt.hasNext());
            Assert.assertFalse(postRefreshIt.hasNext());
        } catch (Exception e) {
            Assert.fail("An exception was raised while refreshing favorites: " + e.getMessage());
        }
    }

    /**********************************************************
     *
     * Testing LangDataService
     *
     **********************************************************/

    @Test
    public void testGetLang() throws DatafariServerException {
        String lang = LangDataService.getInstance().getLang("john");
        Assert.assertEquals("lang 1", lang);
    }

    @Test
    public void testSetLang() throws DatafariServerException {
        int code = LangDataService.getInstance().setLang("alice", "fr");
        Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
        String lang = LangDataService.getInstance().getLang("alice");
        Assert.assertEquals("fr", lang);
    }

    @Test
    public void testDeleteLang() throws DatafariServerException {
        int code = LangDataService.getInstance().deleteLang("john");
        Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
        String lang = LangDataService.getInstance().getLang("john");
        Assert.assertNull(lang);
    }

    @Test
    public void testUpdateLang() throws DatafariServerException, InterruptedException {
        TimeUnit.SECONDS.sleep(2);
        ResultSet originalResults = session
                .execute("SELECT last_refresh, TTL(last_refresh) FROM lang WHERE username='john'");
        Row originalRow = originalResults.one();
        Instant originalTimestamp = originalRow.get("last_refresh", new TimestampCodec());
        Integer originalTTL = originalRow.getInt("ttl(last_refresh)");
        int code = LangDataService.getInstance().updateLang("john", "en");
        Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
        ResultSet postUpdateResults = session
                .execute("SELECT last_refresh, TTL(last_refresh) FROM lang WHERE username='john'");
        Row postUpdateRow = postUpdateResults.one();
        Instant postUpdateTimestamp = postUpdateRow.get("last_refresh", new TimestampCodec());
        Integer postUpdateTTL = postUpdateRow.getInt("ttl(last_refresh)");
        Assert.assertTrue("Has the timestamp been refreshed", postUpdateTimestamp.isAfter(originalTimestamp));
        Assert.assertTrue("Is refreshed TTL greater that original TTL", postUpdateTTL > originalTTL);
        String lang = LangDataService.getInstance().getLang("john");
        Assert.assertEquals("en", lang);
    }

    @Test
    public void testRefreshLang() throws DatafariServerException, InterruptedException {
        int userDataTTL = Integer
                .parseInt(GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL));
        TimeUnit.SECONDS.sleep(10);
        ResultSet results = session.execute("SELECT last_refresh, TTL(last_refresh) FROM lang WHERE username='john'");
        Row result = results.one();
        Instant originalTimestamp = result.get("last_refresh", new TimestampCodec());
        Integer originalTTL = result.getInt("ttl(last_refresh)");
        LangDataService.getInstance().refreshLang("john");
        results = session.execute("SELECT last_refresh, TTL(last_refresh) FROM lang WHERE username='john'");
        result = results.one();
        Instant refreshedTimestamp = result.get("last_refresh", new TimestampCodec());
        Integer refreshedTTL = result.getInt("ttl(last_refresh)");
        Assert.assertTrue("Has the timestamp been refreshed", refreshedTimestamp.isAfter(originalTimestamp));
        Assert.assertTrue("Is refreshed TTL greater that original TTL", refreshedTTL > originalTTL);
        Assert.assertTrue("Original TTL should be at least 10 less than userDataTTL", originalTTL <= userDataTTL - 10);
    }

    /**********************************************************
     *
     * Testing LicenceDataService
     *
     **********************************************************/

    @Test
    public void testSetLicence() throws DatafariServerException, LicenceException {
        Licence licence = Licence.getDemoLicence();
        int code = LicenceDataService.getInstance().saveLicence("testLicenceId", licence);
        Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
        Licence retrievedLicence = LicenceDataService.getInstance().getLicence("testLicenceId");
        Assert.assertTrue(licence.isIdentical(retrievedLicence));
    }

    @Test
    public void testSavingNewLicenceLicence() throws DatafariServerException, LicenceException {
        Licence licence = Licence.getDemoLicence();
        int code = LicenceDataService.getInstance().saveLicence("testLicenceId", licence);
        Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
        code = LicenceDataService.getInstance().saveLicence("testLicenceId2", licence);
        Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
        Licence retrievedLicence = LicenceDataService.getInstance().getLicence("testLicenceId");
        Assert.assertNull(retrievedLicence);
        retrievedLicence = LicenceDataService.getInstance().getLicence("testLicenceId2");
        Assert.assertTrue(licence.isIdentical(retrievedLicence));
    }

    /**********************************************************
     *
     * Testing SavedSearchDataService
     *
     **********************************************************/

    @Test
    public void testGetSearches() throws Exception {
        Map<String, String> searches = SavedSearchDataService.getInstance().getSearches("john");
        Map<String, String> expected = new HashMap<>();
        expected.put("name 1", "request 1");
        expected.put("name 2", "request 2");
        Assert.assertEquals(expected.size(), searches.size());
        searches.forEach((key, value) -> {
            Assert.assertTrue(expected.containsKey(key));
            Assert.assertEquals(expected.get(key), value);
        });
    }

    @Test
    public void testSaveSearch() throws Exception {
        int code = SavedSearchDataService.getInstance().saveSearch("alice", "name test", "request test");
        Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
        Map<String, String> searches = SavedSearchDataService.getInstance().getSearches("alice");
        Map<String, String> expected = new HashMap<>();
        expected.put("name test", "request test");
        expected.put("name 3", "request 3");
        Assert.assertEquals(expected.size(), searches.size());
        searches.forEach((key, value) -> {
            Assert.assertTrue(expected.containsKey(key));
            Assert.assertEquals(expected.get(key), value);
        });
    }

    @Test
    public void testDeleteSearch() throws Exception {
        int code = SavedSearchDataService.getInstance().deleteSearch("john", "name 1", "request 1");
        Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
        Map<String, String> searches = SavedSearchDataService.getInstance().getSearches("john");
        Map<String, String> expected = new HashMap<>();
        expected.put("name 2", "request 2");
        Assert.assertEquals(expected.size(), searches.size());
        searches.forEach((key, value) -> {
            Assert.assertTrue(expected.containsKey(key));
            Assert.assertEquals(expected.get(key), value);
        });
    }

    @Test
    public void testRemoveSearches() throws Exception {
        int code = SavedSearchDataService.getInstance().removeSearches("john");
        Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
        Map<String, String> searches = SavedSearchDataService.getInstance().getSearches("john");
        Assert.assertEquals(0, searches.size());
    }

    @Test
    public void testRefreshSavedSearches() throws DatafariServerException, InterruptedException {
        int userDataTTL = Integer
                .parseInt(GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL));
        TimeUnit.SECONDS.sleep(10);
        ResultSet originalResults = session
                .execute("SELECT last_refresh, TTL(last_refresh) FROM search WHERE username='john'");
        SavedSearchDataService.getInstance().refreshSavedSearches("john");
        ResultSet postRefreshResults = session
                .execute("SELECT last_refresh, TTL(last_refresh) FROM search WHERE username='john'");
        Iterator<Row> originalIt = originalResults.iterator();
        Iterator<Row> postRefreshIt = postRefreshResults.iterator();
        while (originalIt.hasNext() && postRefreshIt.hasNext()) {
            Row originalRow = originalIt.next();
            Row postRefreshRow = postRefreshIt.next();
            Instant originalTimestamp = originalRow.get("last_refresh", new TimestampCodec());
            Integer originalTTL = originalRow.getInt("ttl(last_refresh)");
            Instant refreshedTimestamp = postRefreshRow.get("last_refresh", new TimestampCodec());
            Integer refreshedTTL = postRefreshRow.getInt("ttl(last_refresh)");
            Assert.assertTrue("Has the timestamp been refreshed", refreshedTimestamp.isAfter(originalTimestamp));
            Assert.assertTrue("Is refreshed TTL greater that original TTL", refreshedTTL > originalTTL);
            Assert.assertTrue("Original TTL should be at least 10 less than userDataTTL",
                    originalTTL <= userDataTTL - 10);
        }
        Assert.assertFalse(originalIt.hasNext());
        Assert.assertFalse(postRefreshIt.hasNext());
    }

    /**********************************************************
     *
     * Testing StatisticsDataService
     *
     **********************************************************/

    // Suppress warnings caused by JSONArray manipulations. Can't do anything
    // about it with json.simple.
    @SuppressWarnings("unchecked")
    @Test
    public void testGetUserStatistics() throws Exception {
        final JSONArray statistics = StatisticsDataService.getInstance().getUserStatistics("alice");
        HashMap<String, Object> stat = new HashMap<>();
        stat.put("query_id", "query 1");
        stat.put("user_id", "alice");
        stat.put("action", "action 1");
        stat.put("time_stamp", Instant.parse("2022-04-01T12:34:56.000Z"));
        HashMap<String, Object> params = new HashMap<>();
        params.put("param1", "value1");
        stat.put("parameters", new JSONObject(params));
        final JSONObject stat1 = new JSONObject(stat);
        stat = new HashMap<>();
        stat.put("query_id", "query 2");
        stat.put("user_id", "alice");
        stat.put("action", "action 2");
        stat.put("time_stamp", Instant.parse("2022-04-01T12:34:56.001Z"));
        params = new HashMap<>();
        params.put("param2", "value2");
        stat.put("parameters", new JSONObject(params));
        final JSONObject stat2 = new JSONObject(stat);
        Assert.assertEquals(2, statistics.size());
        statistics.forEach((value) -> {
            JSONObject jsonValue = (JSONObject) value;
            if (((String) jsonValue.get("query_id")).contentEquals("query 1")) {
                Assert.assertEquals(stat1, jsonValue);
            } else if (((String) jsonValue.get("query_id")).contentEquals("query 2")) {
                Assert.assertEquals(stat2, jsonValue);
            } else {
                Assert.fail();
            }
        });
    }

    @Test
    public void testSaveQueryStatistics() throws Exception {
        UUID testId = UUID.randomUUID();
        Instant testInstant = Instant.parse("2022-04-01T12:34:56.123Z");
        boolean success = StatisticsDataService.getInstance().saveQueryStatistics(
                testId.toString(),
                "query test",
                "john",
                23,
                testInstant);
        Assert.assertTrue(success);
        final JSONArray statistics = StatisticsDataService.getInstance().getUserStatistics("john");
        Assert.assertEquals(1, statistics.size());
        HashMap<String, Object> props = new HashMap<>();
        props.put("query_id", testId.toString());
        props.put("user_id", "john");
        props.put("action", UserActions.SEARCH.toString());
        props.put("time_stamp", testInstant);
        HashMap<String, Object> params = new HashMap<>();
        params.put("query", "query test");
        params.put("num_hit", 23);
        props.put("parameters", new JSONObject(params));
        JSONObject expected = new JSONObject(props);
        Assert.assertEquals(
                expected.toJSONString(),
                ((JSONObject) statistics.get(0)).toJSONString());
    }

    @Test
    public void testSaveClickStatistics() throws Exception {
        UUID testId = UUID.randomUUID();
        Instant testInstant = Instant.parse("2022-04-01T12:34:56.123Z");
        boolean success = StatisticsDataService.getInstance().saveClickStatistics(
                testId.toString(),
                "query test",
                "john",
                "document test",
                5,
                testInstant);
        Assert.assertTrue(success);
        final JSONArray statistics = StatisticsDataService.getInstance().getUserStatistics("john");
        Assert.assertEquals(1, statistics.size());
        HashMap<String, Object> props = new HashMap<>();
        props.put("doc_id", "document test");
        props.put("rank", 5);
        final JSONObject parameters = new JSONObject(props);
        props = new HashMap<>();
        props.put("query_id", testId.toString());
        props.put("user_id", "john");
        props.put("action", UserActions.OPEN.toString());
        props.put("time_stamp", testInstant);
        props.put("parameters", parameters);
        JSONObject expected = new JSONObject(props);
        Assert.assertEquals(
                expected.toJSONString(),
                ((JSONObject) statistics.get(0)).toJSONString());
    }

    @Test
    public void testDeleteUserStatistics() throws Exception {
        int code = StatisticsDataService.getInstance().deleteUserStatistics("alice");
        Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
        final JSONArray statistics = StatisticsDataService.getInstance().getUserStatistics("alice");
        Assert.assertEquals(0, statistics.size());
    }

    /**********************************************************
     *
     * Testing UiConfigDataService
     *
     **********************************************************/

    @Test
    public void testGetUiConfig() throws DatafariServerException {
        String uiConfig = UiConfigDataService.getInstance().getUiConfig("john");
        Assert.assertEquals("{\"left\": [],\"center\": [],\"right\": []}", uiConfig);
    }

    @Test
    public void testSetUiConfig() throws DatafariServerException {
        int code = UiConfigDataService.getInstance().setUiConfig("alice", "{\"left\": [],\"center\": [],\"right\": []}");
        Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
        String uiConfig = UiConfigDataService.getInstance().getUiConfig("alice");
        Assert.assertEquals("{\"left\": [],\"center\": [],\"right\": []}", uiConfig);
    }

    @Test
    public void testDeleteUiConfig() throws DatafariServerException {
        int code = UiConfigDataService.getInstance().deleteUiConfig("john");
        Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
        String uiConfig = UiConfigDataService.getInstance().getUiConfig("john");
        Assert.assertNull(uiConfig);
    }

    @Test
    public void testUpdateUiConfig() throws DatafariServerException, InterruptedException {
        TimeUnit.SECONDS.sleep(2);
        ResultSet originalResults = session
                .execute("SELECT last_refresh, TTL(last_refresh) FROM ui_config WHERE username='john'");
        Row originalRow = originalResults.one();
        Instant originalTimestamp = originalRow.get("last_refresh", new TimestampCodec());
        Integer originalTTL = originalRow.getInt("ttl(last_refresh)");
        int code = UiConfigDataService.getInstance().updateUiConfig("john", "{\"left\": [{\"test\":\"truc\"}],\"center\": [],\"right\": []}");
        Assert.assertEquals(CodesReturned.ALLOK.getValue(), code);
        ResultSet postUpdateResults = session
                .execute("SELECT last_refresh, TTL(last_refresh) FROM ui_config WHERE username='john'");
        Row postUpdateRow = postUpdateResults.one();
        Instant postUpdateTimestamp = postUpdateRow.get("last_refresh", new TimestampCodec());
        Integer postUpdateTTL = postUpdateRow.getInt("ttl(last_refresh)");
        Assert.assertTrue("Has the timestamp been refreshed", postUpdateTimestamp.isAfter(originalTimestamp));
        Assert.assertTrue("Is refreshed TTL greater that original TTL", postUpdateTTL > originalTTL);
        String uiConfig = UiConfigDataService.getInstance().getUiConfig("john");
        Assert.assertEquals("{\"left\": [{\"test\":\"truc\"}],\"center\": [],\"right\": []}", uiConfig);
    }

    @Test
    public void testRefreshUiConfig() throws DatafariServerException, InterruptedException {
        int userDataTTL = Integer
                .parseInt(GDPRConfiguration.getInstance().getProperty(GDPRConfiguration.USER_DATA_TTL));
        TimeUnit.SECONDS.sleep(10);
        ResultSet results = session.execute("SELECT last_refresh, TTL(last_refresh) FROM ui_config WHERE username='john'");
        Row result = results.one();
        Instant originalTimestamp = result.get("last_refresh", new TimestampCodec());
        Integer originalTTL = result.getInt("ttl(last_refresh)");
        UiConfigDataService.getInstance().refreshUiConfig("john");
        results = session.execute("SELECT last_refresh, TTL(last_refresh) FROM ui_config WHERE username='john'");
        result = results.one();
        Instant refreshedTimestamp = result.get("last_refresh", new TimestampCodec());
        Integer refreshedTTL = result.getInt("ttl(last_refresh)");
        Assert.assertTrue("Has the timestamp been refreshed", refreshedTimestamp.isAfter(originalTimestamp));
        Assert.assertTrue("Is refreshed TTL greater that original TTL", refreshedTTL > originalTTL);
        Assert.assertTrue("Original TTL should be at least 10 less than userDataTTL", originalTTL <= userDataTTL - 10);
    }

    @After
    public void tearDown() throws Exception {
        session.close();
    }

    @AfterClass
    public static void cassandraTearDown() {
        cassandraContainer.stop();
    }
}
