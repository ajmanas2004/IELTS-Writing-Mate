package com.ieltsmate.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class BaseDAO<T> {
    protected final Connection connection;

    protected BaseDAO(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
    }

    public abstract Optional<T> findById(int id) throws SQLException;

    public abstract List<T> findAll() throws SQLException;

    public abstract void save(T entity) throws SQLException;

    public abstract void update(T entity) throws SQLException;

    public abstract void delete(int id) throws SQLException;

    protected final long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }

        try (PreparedStatement query = connection.prepareStatement("SELECT last_insert_rowid()");
             ResultSet result = query.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("Database did not return the generated row ID");
            }
            return result.getLong(1);
        }
    }

    public final void inTransaction(SqlOperation operation) throws SQLException {
        boolean ownsTransaction = connection.getAutoCommit();
        Savepoint savepoint = ownsTransaction ? null : connection.setSavepoint();
        if (ownsTransaction) {
            connection.setAutoCommit(false);
        }

        try {
            operation.run();
            if (ownsTransaction) {
                connection.commit();
            } else {
                connection.releaseSavepoint(savepoint);
            }
        } catch (SQLException | RuntimeException exception) {
            try {
                if (ownsTransaction) {
                    connection.rollback();
                } else {
                    connection.rollback(savepoint);
                }
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        } finally {
            if (ownsTransaction) {
                connection.setAutoCommit(true);
            }
        }
    }

    public final boolean sharesConnectionWith(BaseDAO<?> other) {
        return other != null && connection == other.connection;
    }

    @FunctionalInterface
    public interface SqlOperation {
        void run() throws SQLException;
    }
}
