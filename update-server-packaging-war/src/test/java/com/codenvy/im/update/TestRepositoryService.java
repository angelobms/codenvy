/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.im.update;

import com.codenvy.commons.user.UserImpl;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpTransport;
import com.google.inject.TypeLiteral;
import com.jayway.restassured.response.Response;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.everrest.assured.EverrestJetty;
import org.everrest.assured.JettyHttpServer;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.codenvy.im.artifacts.ArtifactProperties.*;
import static com.codenvy.im.utils.AccountUtils.SUBSCRIPTION_DATE_FORMAT;
import static com.jayway.restassured.RestAssured.given;
import static java.util.Calendar.getInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * @author Anatoliy Bazko
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class TestRepositoryService extends BaseTest {

    private final ArtifactStorage   artifactStorage;
    private final RepositoryService repositoryService;
    private final HttpTransport     transport;
    private final UserManager       userManager;
    private final MongoStorage      mongoStorage;

    private final Properties authenticationRequiredProperties = new Properties() {{
        put(AUTHENTICATION_REQUIRED_PROPERTY, "true");
    }};
    private final Properties subscriptionProperties           = new Properties() {{
        put(SUBSCRIPTION_PROPERTY, "OnPremises");
    }};

    {
        try {
            mongoStorage = new MongoStorage("mongodb://localhost:12000/update", true, "target");
            transport = mock(HttpTransport.class);
            userManager = mock(UserManager.class);
            artifactStorage = new ArtifactStorage(DOWNLOAD_DIRECTORY.toString());
            repositoryService = new RepositoryService("",
                                                      userManager,
                                                      artifactStorage,
                                                      mongoStorage,
                                                      transport);

        } catch (IOException e) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    @BeforeMethod
    public void setUp() throws Exception {
        when(userManager.getCurrentUser()).thenReturn(new UserImpl("name", "id", "token", Collections.<String>emptyList(), false));
        initStorage();
        super.setUp();
    }

    private void initStorage() {
        DBCollection collection = mongoStorage.getDb().getCollection(MongoStorage.ARTIFACTS_COLLECTION);
        collection.remove(new BasicDBObject());
        collection = mongoStorage.getDb().getCollection(MongoStorage.ARTIFACTS_DOWNLOADED_COLLECTION);
        collection.remove(new BasicDBObject());

        mongoStorage.saveDownloadInfo("uid1", "cdec", "1.0.1", true);
        mongoStorage.saveDownloadInfo("uid1", "cdec", "1.0.1", true);
        mongoStorage.saveDownloadInfo("uid1", "cdec", "1.0.1", false);
        mongoStorage.saveDownloadInfo("uid1", "cdec", "1.0.1", false);
        mongoStorage.saveDownloadInfo("uid2", "cdec", "1.0.1", true);
        mongoStorage.saveDownloadInfo("uid2", "cdec", "1.0.2", true);
        mongoStorage.saveDownloadInfo("uid2", "cdec", "1.0.3", true);
        mongoStorage.saveDownloadInfo("uid1", "artifact2", "1.0.1", false);
        mongoStorage.saveDownloadInfo("uid2", "artifact3", "1.0.1", false);
    }

    @Test
    public void testSaveInstalledInfoErrorIfInvalidUserAgent() throws Exception {
        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/installationinfo/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    public void testSaveInstalledInfoErrorIfVersionInvalid() throws Exception {
        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .header("user-agent", RepositoryService.VALID_USER_AGENT)
                .post(JettyHttpServer.SECURE_PATH + "/repository/installationinfo/cdec/.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testSaveInstalledInfo() throws Exception {
        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .header("user-agent", RepositoryService.VALID_USER_AGENT)
                .post(JettyHttpServer.SECURE_PATH + "/repository/installationinfo/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetInstalledInfo() throws Exception {
        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .header("user-agent", RepositoryService.VALID_USER_AGENT)
                .post(JettyHttpServer.SECURE_PATH + "/repository/installationinfo/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .header("user-agent", RepositoryService.VALID_USER_AGENT).get(JettyHttpServer.SECURE_PATH + "/repository/installationinfo/cdec");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Map m = response.as(Map.class);
        assertEquals(m.size(), 4);
        assertEquals(m.get("artifact"), "cdec");
        assertEquals(m.get("version"), "1.0.1");
        assertEquals(m.get("userId"), "id");
        assertNotNull(m.get("date"));
    }

    @Test
    public void testGetLatestVersion() throws Exception {
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), InstallManagerArtifact.NAME, "1.0.1", "tmp", new Properties());
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), InstallManagerArtifact.NAME, "1.0.2", "tmp", new Properties());

        Response response = given().when().get("repository/properties/installation-manager");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Map value = Commons.fromJson(response.body().asString(),
                                     Map.class,
                                     new TypeLiteral<Map<String, String>>() {}.getType());
        assertEquals(value.size(), 3);
        assertEquals(value.get(ARTIFACT_PROPERTY), InstallManagerArtifact.NAME);
        assertEquals(value.get(VERSION_PROPERTY), "1.0.2");
        assertNull(value.get(MD5_PROPERTY));
    }

    @Test
    public void testGetArtifactProperties() throws Exception {
        Map testProperties = new HashMap<String, String>() {{
            put(AUTHENTICATION_REQUIRED_PROPERTY, "true");
            put(SUBSCRIPTION_PROPERTY, "OnPremises");
        }};

        Properties testPropertiesContainer = new Properties();
        testPropertiesContainer.putAll(testProperties);

        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), InstallManagerArtifact.NAME, "1.0.1", "tmp", testPropertiesContainer);

        Response response = given().when().get("repository/properties/installation-manager/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Map value = Commons.fromJson(response.body().asString(),
                                     Map.class,
                                     new TypeLiteral<Map<String, String>>() {}.getType());
        assertEquals(value.size(), 4);
        assertEquals(value.get(ARTIFACT_PROPERTY), InstallManagerArtifact.NAME);
        assertEquals(value.get(VERSION_PROPERTY), "1.0.1");
        assertEquals(value.get(AUTHENTICATION_REQUIRED_PROPERTY), "true");
        assertNull(value.get(MD5_PROPERTY));
    }

    @Test
    public void testDownloadPublicArtifact() throws Exception {
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), InstallManagerArtifact.NAME, "1.0.1", "tmp", new Properties());

        Response response = given().when().get("repository/public/download/" + InstallManagerArtifact.NAME + "/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }

    @Test
    public void testDownloadPublicErrorWhenArtifactAbsent() throws Exception {
        Response response = given().when().get("repository/public/download/installation-manager/1.0.2");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testDownloadPublicArtifactLatestVersion() throws Exception {
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), InstallManagerArtifact.NAME, "1.0.1", "tmp", new Properties());

        Response response = given().when().get("repository/public/download/" + InstallManagerArtifact.NAME);
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }

    @Test
    public void testDownloadPublicWithSubscription() throws Exception {
        SimpleDateFormat  subscriptionDateFormat = new SimpleDateFormat(SUBSCRIPTION_DATE_FORMAT);
        Calendar cal = getInstance();
        cal.add(Calendar.DATE, -1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = getInstance();
        cal.add(Calendar.DATE, 1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(transport.doGetRequest("/account", userManager.getCurrentUser().getToken()))
        .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");

        when(transport.doGetRequest("/account/accountId/subscriptions", userManager.getCurrentUser().getToken()))
        .thenReturn("[{serviceId:OnPremises,id:subscriptionId}]");

        when(transport.doGetRequest("/account/subscriptions/subscriptionId/attributes", userManager.getCurrentUser().getToken()))
        .thenReturn("{startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}");

        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", subscriptionProperties);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/repository/download/cdec/1.0.1/accountId");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }

    @Test
    public void testDownloadPublicArtifactErrorWhenSubscriptionRequired() throws Exception {
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", subscriptionProperties);

        Response response = given().when().get("/repository/public/download/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void testDownloadPrivateArtifactWithoutSubscription() throws Exception {
        when(transport.doGetRequest("/account")).thenReturn("[{accountReference:{id:accountId}}]");
        when(transport.doGetRequest("/account/accountId/subscriptions")).thenReturn("[]");
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", authenticationRequiredProperties);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/repository/download/cdec/1.0.1/accountId");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }

    @Test
    public void testDownloadArtifactWhenAuthenticationError() throws Exception {
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", authenticationRequiredProperties);

        Response response = given().when().get("repository/public/download/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void testDownloadPrivateWhenUserWithoutSubscriptionError() throws Exception {
        when(transport.doGetRequest("/account", userManager.getCurrentUser().getToken()))
                 .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGetRequest("/account/accountId/subscriptions", userManager.getCurrentUser().getToken()))
                 .thenReturn("[]");
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", subscriptionProperties);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/repository/download/cdec/1.0.1/accountId");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    public void testDownloadPrivateErrorIfNoRolesAllowed() throws Exception {
        Response response = given().when().get("repository/download/cdec/1.0.1/accountId");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    public void testUploadDownloadSnapshotVersion() throws Exception {
        Path tmp = Paths.get("target/tmp-1.0.1.txt");
        Files.copy(new ByteArrayInputStream("content".getBytes()), tmp, StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .multiPart(tmp.toFile()).post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec/1.0.1-SNAPSHOT");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get("/repository/public/download/cdec");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }

    @Test
    public void testUploadSnapshotVersion() throws Exception {
        Path tmp = Paths.get("target/tmp-1.0.1.txt");
        Files.copy(new ByteArrayInputStream("content".getBytes()), tmp, StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .multiPart(tmp.toFile()).post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec/1.0.1-SNAPSHOT");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Path artifact = Paths.get("target", "download", "cdec", "1.0.1-SNAPSHOT", "tmp-1.0.1.txt");
        assertEquals(FileUtils.readFileToString(artifact.toFile()), "content");
        assertTrue(Files.exists(artifact));

        Path propertiesFile = Paths.get("target", "download", "cdec", "1.0.1-SNAPSHOT", ArtifactStorage.PROPERTIES_FILE);
        assertTrue(Files.exists(propertiesFile));

        Properties properties = new Properties();
        properties.load(Files.newInputStream(propertiesFile));
        assertEquals(properties.size(), 3);
        assertEquals(properties.get(VERSION_PROPERTY), "1.0.1-SNAPSHOT");
        assertEquals(properties.get(FILE_NAME_PROPERTY), "tmp-1.0.1.txt");
        assertEquals(properties.get(ARTIFACT_PROPERTY), "cdec");
    }


    @Test
    public void testUpload() throws Exception {
        Path tmp = Paths.get("target/tmp-1.0.1.txt");
        Files.copy(new ByteArrayInputStream("content".getBytes()), tmp, StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .multiPart(tmp.toFile()).post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec/1.0.1?revision=abcd&build-time=20140930");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Path artifact = Paths.get("target", "download", "cdec", "1.0.1", "tmp-1.0.1.txt");
        assertEquals(FileUtils.readFileToString(artifact.toFile()), "content");
        assertTrue(Files.exists(artifact));

        Path propertiesFile = Paths.get("target", "download", "cdec", "1.0.1", ArtifactStorage.PROPERTIES_FILE);
        assertTrue(Files.exists(propertiesFile));

        Properties properties = new Properties();
        properties.load(Files.newInputStream(propertiesFile));
        assertEquals(properties.size(), 4);
        assertEquals(properties.get(VERSION_PROPERTY), "1.0.1");
        assertEquals(properties.get(FILE_NAME_PROPERTY), "tmp-1.0.1.txt");
        assertEquals(properties.get(BUILD_TIME_PROPERTY), "20140930");
        assertEquals(properties.get(ARTIFACT_PROPERTY), "cdec");
    }

    @Test
    public void testUploadErrorIfVersionHasBadFormat() throws Exception {
        Path tmp = Paths.get("target/tmp");
        Files.copy(new ByteArrayInputStream("content".getBytes()), tmp, StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .multiPart(tmp.toFile()).post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec-1.01.1/1.01.1");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testUploadErrorIfNoStream() throws Exception {
        Files.copy(new ByteArrayInputStream("content".getBytes()), Paths.get("target/tmp"), StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec-1.01.1/1.01.1");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testGetDownloadStatisticByUser() throws Exception {
        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .header("user-agent", RepositoryService.VALID_USER_AGENT)
                .get(JettyHttpServer.SECURE_PATH + "/repository/download/statistic/user/uid1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .header("user-agent", RepositoryService.VALID_USER_AGENT).get(JettyHttpServer.SECURE_PATH + "/repository/download/statistic/user/uid1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Map m = response.as(Map.class);
        assertEquals(m.size(), 4);
        assertEquals(m.get("success"), "2");
        assertEquals(m.get("fail"), "3");
        assertEquals(m.get("total"), "5");
        assertNotNull(m.get("list"));

        List l = (List)m.get("list");
        assertEquals(l.size(), 2);
        m = (Map)l.get(0);
        assertEquals(m.get("userId"), "uid1");
        assertEquals(m.get("artifact"), "cdec");
        assertEquals(m.get("version"), "1.0.1");
        assertEquals(m.get("success"), "2");
        assertEquals(m.get("fail"), "2");

        m = (Map)l.get(1);
        assertEquals(m.get("userId"), "uid1");
        assertEquals(m.get("artifact"), "artifact2");
        assertEquals(m.get("version"), "1.0.1");
        assertEquals(m.get("success"), "0");
        assertEquals(m.get("fail"), "1");
    }

    @Test
    public void testGetDownloadStatisticByArtifact() throws Exception {
        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .header("user-agent", RepositoryService.VALID_USER_AGENT)
                .get(JettyHttpServer.SECURE_PATH + "/repository/download/statistic/artifact/cdec");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .header("user-agent", RepositoryService.VALID_USER_AGENT).get(
                        JettyHttpServer.SECURE_PATH + "/repository/download/statistic/artifact/cdec");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Map m = response.as(Map.class);
        assertEquals(m.size(), 4);
        assertEquals(m.get("success"), "5");
        assertEquals(m.get("fail"), "2");
        assertEquals(m.get("total"), "7");
        assertNotNull(m.get("list"));

        List l = (List)m.get("list");
        assertEquals(l.size(), 4);
        m = (Map)l.get(0);
        assertEquals(m.get("userId"), "uid2");
        assertEquals(m.get("artifact"), "cdec");
        assertEquals(m.get("version"), "1.0.3");
        assertEquals(m.get("success"), "1");
        assertEquals(m.get("fail"), "0");

        m = (Map)l.get(1);
        assertEquals(m.get("userId"), "uid2");
        assertEquals(m.get("artifact"), "cdec");
        assertEquals(m.get("version"), "1.0.2");
        assertEquals(m.get("success"), "1");
        assertEquals(m.get("fail"), "0");

        m = (Map)l.get(2);
        assertEquals(m.get("userId"), "uid2");
        assertEquals(m.get("artifact"), "cdec");
        assertEquals(m.get("version"), "1.0.1");
        assertEquals(m.get("success"), "1");
        assertEquals(m.get("fail"), "0");

        m = (Map)l.get(3);
        assertEquals(m.get("userId"), "uid1");
        assertEquals(m.get("artifact"), "cdec");
        assertEquals(m.get("version"), "1.0.1");
        assertEquals(m.get("success"), "2");
        assertEquals(m.get("fail"), "2");
    }
}
