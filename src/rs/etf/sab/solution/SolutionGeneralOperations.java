package rs.etf.sab.solution;

import rs.etf.sab.operations.GeneralOperations;

import java.sql.*;
import java.util.Calendar;

/**
 * Implements the GeneralOperations interface and provides methods for general operations on the database.
 */
public class SolutionGeneralOperations implements GeneralOperations {
    private static final Connection c = DB.getInstance().getConnection();
    private static final Calendar currentDateTime = Calendar.getInstance();

    /**
     * Sets the initial time to the given calendar object.
     *
     * @param calendar the calendar object representing the initial time
     */
    @Override
    public void setInitialTime(Calendar calendar) {
        currentDateTime.setTimeInMillis(calendar.getTimeInMillis());
    }

    /**
     * Adds the specified number of days to the currentDateTime object.
     * Adjusts the status of orders accordingly.
     *
     * @param numberOfDays the number of days to add
     * @return the updated currentDateTime object
     */
    @Override
    public Calendar time(int numberOfDays) {
        currentDateTime.add(Calendar.DAY_OF_MONTH, numberOfDays);

        adjustOrdersStatus();
        return currentDateTime;
    }

    /**
     * Returns the current time as a Calendar object.
     *
     * @return the current time
     */
    @Override
    public Calendar getCurrentTime() {
        return (Calendar) currentDateTime.clone();
    }

    /**
     * Erases all data from the database.
     * Disables triggers and constraints, deletes data from all tables,
     * enables constraints and triggers, and resets identity values for the tables, respectively.
     *
     * @throws RuntimeException if a SQLException occurs during the erasure process
     */
    @Override
    public void eraseAll() {
        String query1 = "EXEC sp_MSforeachtable 'DISABLE TRIGGER ALL ON ?'";
        String query2 = "EXEC sp_MSForEachTable 'ALTER TABLE ? NOCHECK CONSTRAINT ALL'";
        String query3 = "EXEC sp_MSForEachTable 'DELETE FROM ?'";
        String query4 = "EXEC sp_MSForEachTable 'ALTER TABLE ? CHECK CONSTRAINT ALL'";
        String query5 = "EXEC sp_MSForEachTable 'ENABLE TRIGGER ALL ON ?'";
        String query6 = "DBCC CHECKIDENT (City, RESEED, 0); "
                + "DBCC CHECKIDENT (Shop, RESEED, 0); "
                + "DBCC CHECKIDENT (Buyer, RESEED, 0); "
                + "DBCC CHECKIDENT ([Transaction], RESEED, 0); "
                + "DBCC CHECKIDENT ([Order], RESEED, 0); "
                + "DBCC CHECKIDENT (Article, RESEED, 0); "
                + "DBCC CHECKIDENT (OrderItem, RESEED, 0);";

        try (Statement st = c.createStatement()) {
            st.execute(query1);
            st.execute(query2);
            st.execute(query3);
            st.execute(query4);
            st.execute(query5);
            st.execute(query6);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adjusts the status of orders based on the currentDateTime.
     * Orders with a 'sent' status and a DateArrived before or equal to the currentDateTime
     * will have their status updated to 'arrived'.
     *
     * @throws RuntimeException if a SQLException occurs during the adjustment process
     */
    private void adjustOrdersStatus() {
        String query = "UPDATE [Order] SET Status = 'arrived' WHERE Status = 'sent' AND DateArrived <= ?";

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setTimestamp(1, new Timestamp(currentDateTime.getTimeInMillis()));

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
