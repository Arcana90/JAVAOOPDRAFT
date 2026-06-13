package backend.employee;

import backend.db.ConnectionPoolManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;

public class EmployeeRepository {

    public List<Employee> findAll() {
        List<Employee> employees = new ArrayList<>();
        Connection connection = null;

        String sql = """
            SELECT 
                e.employee_id,
                e.first_name,
                e.last_name,
                e.department,
                e.position,
                e.email,
                e.contact_number,
                e.status::text AS status,
                e.supervisor_id,
                COALESCE(s.first_name || ' ' || s.last_name, '') AS supervisor_name
            FROM employees e
            LEFT JOIN supervisors s ON e.supervisor_id = s.supervisor_id
            ORDER BY e.employee_id
            """;

        try {
            connection = ConnectionPoolManager.getInstance().acquire();

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    Employee employee = new Employee(
                            resultSet.getString("employee_id"),
                            resultSet.getString("first_name"),
                            resultSet.getString("last_name"),
                            resultSet.getString("department"),
                            resultSet.getString("position"),
                            resultSet.getString("email"),
                            resultSet.getString("contact_number"),
                            resultSet.getString("status"),
                            (Integer) resultSet.getObject("supervisor_id"),
                            resultSet.getString("supervisor_name")
                    );

                    employees.add(employee);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ConnectionPoolManager.getInstance().release(connection);
        }

        return employees;
    }

    public AddEmployeeResult addEmployee(Employee employee) {
        Connection connection = null;

        String sql = """
            INSERT INTO employees (
                employee_id, first_name, last_name, department, position,
                email, contact_number, status, supervisor_id
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::employee_status, ?)
            """;

        try {
            connection = ConnectionPoolManager.getInstance().acquire();

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, employee.getEmployeeId());
                statement.setString(2, employee.getFirstName());
                statement.setString(3, employee.getLastName());
                statement.setString(4, employee.getDepartment());
                statement.setString(5, employee.getPosition());
                statement.setString(6, employee.getEmail());
                statement.setString(7, employee.getContactNumber());
                statement.setString(8, employee.getStatus());

                if (employee.getSupervisorId() == null) {
                    statement.setNull(9, java.sql.Types.INTEGER);
                } else {
                    statement.setInt(9, employee.getSupervisorId());
                }

                int rowsAffected = statement.executeUpdate();

                if (rowsAffected > 0) {
                    return AddEmployeeResult.SUCCESS;
                }

                return AddEmployeeResult.FAILED;
            }

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                return AddEmployeeResult.DUPLICATE;
            }

            e.printStackTrace();
            return AddEmployeeResult.FAILED;

        } catch (Exception e) {
            e.printStackTrace();
            return AddEmployeeResult.FAILED;

        } finally {
            ConnectionPoolManager.getInstance().release(connection);
        }
    }

    public enum AddEmployeeResult {
        SUCCESS,
        DUPLICATE,
        FAILED
    }

    public boolean deactivateEmployee(String employeeId) {
        Connection connection = null;

        String sql = """
            UPDATE employees
            SET status = 'Inactive'::employee_status
            WHERE employee_id = ?
            """;

        try {
            connection = ConnectionPoolManager.getInstance().acquire();

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, employeeId);
                return statement.executeUpdate() > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            ConnectionPoolManager.getInstance().release(connection);
        }
    }
}