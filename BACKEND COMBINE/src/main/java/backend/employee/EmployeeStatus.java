package backend.employee;

/**
 * Represents the current pass-slip state of an employee.
 *
 * <ul>
 *   <li>{@code AVAILABLE} — employee is on-premises and has not checked out.</li>
 *   <li>{@code OUT}       — employee has an active pass slip; currently off-premises.</li>
 *   <li>{@code RETURNED}  — employee has returned and the pass has been closed.</li>
 * </ul>
 */
public enum EmployeeStatus {
    AVAILABLE,
    OUT,
    RETURNED;

    /** Maps a DB column string back to the enum constant (case-insensitive). */
    public static EmployeeStatus fromString(String value) {
        return switch (value.toUpperCase()) {
            case "AVAILABLE" -> AVAILABLE;
            case "OUT"       -> OUT;
            case "RETURNED"  -> RETURNED;
            default -> throw new IllegalArgumentException(
                    "Unknown EmployeeStatus value: '" + value + "'");
        };
    }
}
