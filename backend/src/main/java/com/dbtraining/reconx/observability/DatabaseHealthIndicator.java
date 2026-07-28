package com.dbtraining.reconx.observability;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

@Component("reconxDatabase")
public class DatabaseHealthIndicator extends AbstractHealthIndicator {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        super("ReconX database health check failed");
        this.dataSource = dataSource;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        final String query = "SELECT 1";
        long start = System.nanoTime();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = executeQueryWithTimeout(stmt, query)) {

            if (rs != null) rs.next();

            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            builder.up()
                   .withDetail("query", query)
                   .withDetail("elapsedMs", elapsedMs);
        } catch (SQLException e) {
            builder.down(e).withDetail("query", query);
        }
    }

    private ResultSet executeQueryWithTimeout(Statement stmt, String query) throws SQLException {
        stmt.setQueryTimeout((int) TIMEOUT.toSeconds());
        return stmt.executeQuery(query);
    }
}
