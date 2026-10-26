package com.bcp.workshop.auth;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

@ApplicationScoped
public class LegacyDatabase {
    private static final String DB_URL = System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");
    private static final String DEMO_STUDENT_USERNAME = "student";
    private static final String DEMO_STUDENT_PASSWORD = "Workshop123!";
    private static final String DEMO_STUDENT_DISPLAY_NAME = "Workshop Student";
    private static final String DEMO_STUDENT_ROLE = "STUDENT";
    private static final String DEMO_TRAINER_USERNAME = "trainer";
    private static final String DEMO_TRAINER_PASSWORD = "Trainer123!";
    private static final String DEMO_TRAINER_DISPLAY_NAME = "Workshop Trainer";
    private static final String DEMO_TRAINER_ROLE = "TRAINER";
    private static final String FIND_BY_CREDENTIALS_SQL =
            "SELECT id, username, password, display_name, role FROM legacy_users WHERE username=?";
    private static final String UPDATE_PASSWORD_SQL =
            "UPDATE legacy_users SET password=? WHERE id=? AND password=?";

    private final ConnectionProvider connectionProvider;
    private final PasswordHasher passwordHasher;

    public LegacyDatabase() {
        this(() -> DriverManager.getConnection(DB_URL), new PasswordHasher());
    }

    LegacyDatabase(ConnectionProvider connectionProvider) {
        this(connectionProvider, new PasswordHasher());
    }

    LegacyDatabase(ConnectionProvider connectionProvider, PasswordHasher passwordHasher) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
    }

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(Path.of("data"));
            try (Connection connection = connectionProvider.open()) {
                initializeSchemaAndDemoUsers(connection);
            }
        } catch (Exception error) {
            throw new IllegalStateException("Failed to initialize authentication database", error);
        }
    }

    void initializeSchemaAndDemoUsers(Connection connection)
            throws SQLException, java.security.GeneralSecurityException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS legacy_users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT NOT NULL, display_name TEXT NOT NULL, role TEXT NOT NULL)");
        }
        insertDemoUser(connection, DEMO_STUDENT_USERNAME, DEMO_STUDENT_PASSWORD,
                DEMO_STUDENT_DISPLAY_NAME, DEMO_STUDENT_ROLE);
        insertDemoUser(connection, DEMO_TRAINER_USERNAME, DEMO_TRAINER_PASSWORD,
                DEMO_TRAINER_DISPLAY_NAME, DEMO_TRAINER_ROLE);
    }

    private void insertDemoUser(
            Connection connection,
            String username,
            String password,
            String displayName,
            String role
    ) throws SQLException, java.security.GeneralSecurityException {
        String encodedPassword = passwordHasher.hash(password);
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT OR IGNORE INTO legacy_users(username,password,display_name,role) VALUES (?, ?, ?, ?)")) {
            insert.setString(1, username);
            insert.setString(2, encodedPassword);
            insert.setString(3, displayName);
            insert.setString(4, role);
            insert.executeUpdate();
        }
    }

    public UserRecord findByCredentials(String username, String password) {
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_CREDENTIALS_SQL)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    UserRecord user = readUser(result);
                    String storedPassword = result.getString("password");
                    if (!passwordMatches(password, storedPassword)) {
                        return null;
                    }
                    if (!passwordHasher.isEncodedHash(storedPassword)) {
                        migrateLegacyPassword(connection, user.id(), storedPassword, password);
                    }
                    return user;
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
        return null;
    }

    private UserRecord readUser(ResultSet result) throws SQLException {
        return new UserRecord(
                result.getLong("id"),
                result.getString("username"),
                result.getString("display_name"),
                result.getString("role")
        );
    }

    private boolean passwordMatches(String suppliedPassword, String storedPassword) throws SQLException {
        if (passwordHasher.isEncodedHash(storedPassword)) {
            try {
                return passwordHasher.matches(suppliedPassword, storedPassword);
            } catch (java.security.GeneralSecurityException error) {
                throw new SQLException("Password verification failed", error);
            }
        }
        return passwordHasher.matchesLegacyPassword(suppliedPassword, storedPassword);
    }

    private void migrateLegacyPassword(
            Connection connection,
            long userId,
            String storedPassword,
            String suppliedPassword
    ) throws SQLException, java.security.GeneralSecurityException {
        String encodedPassword = passwordHasher.hash(suppliedPassword);
        try (PreparedStatement update = connection.prepareStatement(UPDATE_PASSWORD_SQL)) {
            update.setString(1, encodedPassword);
            update.setLong(2, userId);
            update.setString(3, storedPassword);
            if (update.executeUpdate() != 1) {
                throw new SQLException("Legacy password migration did not update exactly one user");
            }
        }
    }

    @FunctionalInterface
    interface ConnectionProvider {
        Connection open() throws SQLException;
    }

}
