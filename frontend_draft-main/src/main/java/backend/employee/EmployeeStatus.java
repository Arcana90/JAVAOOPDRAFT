package backend.employee;

/**
 * Deterministic lifecycle enum enforcing the pipeline:
 * AVAILABLE -> OUT -> RETURNED -> (reset to AVAILABLE)
 */
public enum EmployeeStatus {

    /**
     * Employee is on-site and eligible to receive a new pass slip.
     */
    AVAILABLE,

    /**
     * Employee has an active, unresolved pass slip and has left the premises.
     * No new pass slip may be issued in this state.
     */
    OUT,

    /**
     * Employee has returned and the pass slip transaction has been closed.
     * Acts as a terminal marker before the employee record is reset to AVAILABLE.
     */
    RETURNED
}
