package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDatabaseTest {
    private static final String EXPECTED_SQL =
            "SELECT id, username, password, display_name, role FROM legacy_users WHERE username=?";

    @Test
    void bindsCredentialsAsPreparedStatementParameters() {
        Map<Integer, String> boundParameters = new HashMap<>();
        String[] preparedSql = new String[1];
        ResultSet resultSet = createProxy(ResultSet.class, (proxy, method, arguments) -> {
            if (method.getName().equals("next")) {
                return false;
            }
            if (method.getName().equals("close")) {
                return null;
            }
            return unexpectedCall(method);
        });
        PreparedStatement preparedStatement = createProxy(PreparedStatement.class, (proxy, method, arguments) -> {
            if (method.getName().equals("setString")) {
                boundParameters.put((Integer) arguments[0], (String) arguments[1]);
                return null;
            }
            if (method.getName().equals("executeQuery")) {
                return resultSet;
            }
            if (method.getName().equals("close")) {
                return null;
            }
            return unexpectedCall(method);
        });
        Connection connection = createProxy(Connection.class, (proxy, method, arguments) -> {
            if (method.getName().equals("prepareStatement")) {
                preparedSql[0] = (String) arguments[0];
                return preparedStatement;
            }
            if (method.getName().equals("close")) {
                return null;
            }
            return unexpectedCall(method);
        });

        LegacyDatabase database = new LegacyDatabase(() -> connection);

        assertNull(database.findByCredentials("student'", "Workshop123!"));
        assertEquals(EXPECTED_SQL, preparedSql[0]);
        assertEquals(Map.of(1, "student'"), boundParameters);
    }

    @Test
    void initializesDemoUsersWithHashedPasswordsAndAllowsLogin() throws Exception {
        Class.forName("org.sqlite.JDBC");
        String databaseName = "new-auth-" + UUID.randomUUID();
        String databaseUrl = "jdbc:sqlite:file:" + databaseName + "?mode=memory&cache=shared";

        try (Connection keeper = DriverManager.getConnection(databaseUrl)) {
            LegacyDatabase database = new LegacyDatabase(() -> DriverManager.getConnection(databaseUrl));
            database.initializeSchemaAndDemoUsers(keeper);

            try (PreparedStatement query = keeper.prepareStatement(
                    "SELECT password FROM legacy_users WHERE username=?")) {
                query.setString(1, "student");
                try (ResultSet result = query.executeQuery()) {
                    assertTrue(result.next());
                    String storedPassword = result.getString("password");
                    assertTrue(storedPassword.startsWith("pbkdf2-sha256$"));
                    assertFalse(storedPassword.equals("Workshop123!"));
                }
            }
            assertNotNull(database.findByCredentials("student", "Workshop123!"));
            assertNull(database.findByCredentials("student", "incorrect-password"));
        }
    }

    @Test
    void authenticatesCredentialsAlreadyStoredInPlaintext() throws Exception {
        Class.forName("org.sqlite.JDBC");
        String databaseName = "legacy-auth-" + UUID.randomUUID();
        String databaseUrl = "jdbc:sqlite:file:" + databaseName + "?mode=memory&cache=shared";

        try (Connection keeper = DriverManager.getConnection(databaseUrl);
             Statement statement = keeper.createStatement()) {
            statement.executeUpdate("CREATE TABLE legacy_users (id INTEGER PRIMARY KEY, username TEXT, password TEXT, display_name TEXT, role TEXT)");
            statement.executeUpdate("INSERT INTO legacy_users VALUES (1, 'student', 'Workshop123!', 'Workshop Student', 'STUDENT')");

            LegacyDatabase database = new LegacyDatabase(() -> DriverManager.getConnection(databaseUrl));

            UserRecord user = database.findByCredentials("student", "Workshop123!");

            assertNotNull(user);
            assertEquals("student", user.username());
            try (ResultSet result = statement.executeQuery("SELECT password FROM legacy_users WHERE id=1")) {
                result.next();
                assertTrue(result.getString("password").startsWith("pbkdf2-sha256$"));
            }
            assertNotNull(database.findByCredentials("student", "Workshop123!"));
            assertNull(database.findByCredentials("student", "wrong-password"));
        }
    }

    private static <T> T createProxy(Class<T> proxyType, InvocationHandler handler) {
        Object proxy = Proxy.newProxyInstance(
                proxyType.getClassLoader(),
                new Class<?>[]{proxyType},
                handler
        );
        return proxyType.cast(proxy);
    }

    private static Object unexpectedCall(Method method) {
        throw new AssertionError("Unexpected JDBC call: " + method.getName());
    }
}
