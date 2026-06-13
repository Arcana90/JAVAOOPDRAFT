package backend.employee;

import backend.db.ConnectionPoolManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Executes parameterized JDBC operations against the {@code employees} table.
 *
 * <p>All SQL is defined as private constants — no inline string concatenation,
 * no dynamic query building, no SQL injection vectors. Every parameter is bound
 * via {@link PreparedStatement} setters.
 */
public final class EmployeeRepository {

    private static final Logger LOG = Logger.getLogger(EmployeeRepository.class.getName());

    // ── SQL constants ────────────────────────────────────────────────────────────

    private static final String SQL_INSERT =
            "INSERT INTO employees (employee_id, first_name, last_name, department, status) "
          + "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT employee_id, first_name, last_name, department, status, archived, "
          + "       created_at, updated_at "
          + "FROM employees WHERE employee_id = ? AND archived = 0";

    private static final String SQL_FIND_ALL_ACTIVE =
            "SELECT employee_id, first_name, last_name, department, status, archived, "
          + "       created_at, updated_at "
          + "FROM employees WHERE archived = 0 ORDER BY last_name, first_name";

    private static final String SQL_FIND_BY_STATUS =
            "SELECT employee_id, first_name, last_name, department, status, archived, "
          + "       created_at, updated_at "
          + "FROM employees WHERE status = ? AND archived = 0 ORDER BY last_name, first_name";

    private static final String SQL_UPDATE_FULL =
            "UPDATE employees SET first_name = ?, last_name = ?, department = ?, status = ?, "
          + "                     updated_at = datetime('now') "
          + "WHERE employee_id = ? AND archived = 0";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE employees SET status = ?, updated_at = datetime('now') "
          + "WHERE employee_id = ? AND archived = 0";

    private static final String SQL_SOFT_DELETE =
            "UPDATE employees SET archived = 1, updated_at = datetime('now') "
          + "WHERE employee_id = ? AND archived = 0";

    private static final String SQL_EXISTS =
            "SELECT COUNT(1) FROM employees WHERE employee_id = ? AND archived = 0";

    // ── Singleton ────────────────────────────────────────────────────────────────

    private static volatile EmployeeRepository instance;

    private EmployeeRepository() {}

    public static EmployeeRepository getInstance() {
        if (instance == null) {
            synchronized (EmployeeRepository.class) {
                if (instance == null) instance = new EmployeeRepository();
            }
        }
        return instance;
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    /**
     * Inserts a new employee record.
     *
     * @param dto a validated {@link EmployeeDTO} (archived flag and timestamps are ignored)
     * @throws RepositoryException if the employee ID already exists or a DB error occurs
     */
    public void insert(EmployeeDTO dto) {
        if (existsById(dto.getEmployeeId())) {
            throw new RepositoryException(
                    "Employee ID '" + dto.getEmployeeId() + "' already exists.");
        }
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
                ps.setString(1, dto.getEmployeeId());
                ps.setString(2, dto.getFirstName());
                ps.setString(3, dto.getLastName());
                ps.setString(4, dto.getDepartment());
                ps.setString(5, dto.getStatus().name());
                int rows = ps.executeUpdate();
                if (rows != 1) throw new RepositoryException("INSERT did not affect exactly 1 row.");
            }
        });
    }

    /**
     * Finds an active employee by their unique ID.
     *
     * @param employeeId the primary key
     * @return an {@link Optional} containing the record, or empty if not found
     */
    public Optional<EmployeeDTO> findById(String employeeId) {
        return withConnectionResult(c -> {
            try (PreparedStatement ps = c.prepareStatement(SQL_FIND_BY_ID)) {
                ps.setString(1, employeeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Optional.of(mapRow(rs));
                    return Optional.empty();
                }
            }
        });
    }

    /**
     * Returns all non-archived employees ordered by last name then first name.
     */
    public List<EmployeeDTO> findAllActive() {
        return withConnectionResult(c -> {
            List<EmployeeDTO> result = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(SQL_FIND_ALL_ACTIVE);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
            return result;
        });
    }

    /**
     * Returns all non-archived employees with a given status.
     */
    public List<EmployeeDTO> findByStatus(EmployeeStatus status) {
        return withConnectionResult(c -> {
            List<EmployeeDTO> result = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(SQL_FIND_BY_STATUS)) {
                ps.setString(1, status.name());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(mapRow(rs));
                }
            }
            return result;
        });
    }

    /**
     * Updates all mutable fields for an existing employee.
     *
     * @param dto validated record containing updated values; {@code employeeId} is the key
     * @throws RepositoryException if no matching active record is found
     */
    public void update(EmployeeDTO dto) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(SQL_UPDATE_FULL)) {
                ps.setString(1, dto.getFirstName());
                ps.setString(2, dto.getLastName());
                ps.setString(3, dto.getDepartment());
                ps.setString(4, dto.getStatus().name());
                ps.setString(5, dto.getEmployeeId());
                int rows = ps.executeUpdate();
                if (rows == 0) throw new RepositoryException(
                        "No active employee found with ID '" + dto.getEmployeeId() + "'.");
            }
        });
    }

    /**
     * Updates only the status field of an employee (e.g. OUT → RETURNED).
     *
     * @param employeeId the target employee
     * @param newStatus  the desired status
     * @throws RepositoryException if no matching active record is found
     */
    public void updateStatus(String employeeId, EmployeeStatus newStatus) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(SQL_UPDATE_STATUS)) {
                ps.setString(1, newStatus.name());
                ps.setString(2, employeeId);
                int rows = ps.executeUpdate();
                if (rows == 0) throw new RepositoryException(
                        "No active employee found with ID '" + employeeId + "'.");
            }
        });
    }

    /**
     * Soft-deletes (archives) an employee. The record is retained in the DB for audit purposes
     * but will not appear in any active queries.
     *
     * @param employeeId the target employee
     * @throws RepositoryException if no matching active record is found
     */
    public void archive(String employeeId) {
        withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(SQL_SOFT_DELETE)) {
                ps.setString(1, employeeId);
                int rows = ps.executeUpdate();
                if (rows == 0) throw new RepositoryException(
                        "No active employee found with ID '" + employeeId + "'.");
            }
        });
    }

    /**
     * Checks whether an active (non-archived) record with the given ID exists.
     */
    public boolean existsById(String employeeId) {
        return withConnectionResult(c -> {
            try (PreparedStatement ps = c.prepareStatement(SQL_EXISTS)) {
                ps.setString(1, employeeId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        });
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private EmployeeDTO mapRow(ResultSet rs) throws SQLException {
        String createdStr = rs.getString("created_at");
        String updatedStr = rs.getString("updated_at");
        return new EmployeeDTO(
                rs.getString("employee_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("department"),
                EmployeeStatus.fromString(rs.getString("status")),
                rs.getInt("archived") == 1,
                createdStr != null ? LocalDateTime.parse(createdStr.replace(" ", "T")) : null,
                updatedStr != null ? LocalDateTime.parse(updatedStr.replace(" ", "T")) : null
        );
    }

    @FunctionalInterface
    private interface ConnectionAction {
        void execute(Connection c) throws SQLException;
    }

    @FunctionalInterface
    private interface ConnectionFunction<T> {
        T execute(Connection c) throws SQLException;
    }

    private void withConnection(ConnectionAction action) {
        ConnectionPoolManager pool = ConnectionPoolManager.getInstance();
        Connection c = null;
        try {
            c = pool.acquire();
            action.execute(c);
        } catch (RepositoryException re) {
            throw re;
        } catch (SQLException e) {
            throw new RepositoryException("Database error: " + e.getMessage(), e);
        } finally {
            pool.release(c);
        }
    }

    private <T> T withConnectionResult(ConnectionFunction<T> fn) {
        ConnectionPoolManager pool = ConnectionPoolManager.getInstance();
        Connection c = null;
        try {
            c = pool.acquire();
            return fn.execute(c);
        } catch (RepositoryException re) {
            throw re;
        } catch (SQLException e) {
            throw new RepositoryException("Database error: " + e.getMessage(), e);
        } finally {
            pool.release(c);
        }
    }
}
