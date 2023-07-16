package rs.etf.sab.solution;

import rs.etf.sab.operations.BuyerOperations;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the BuyerOperations interface and provides methods related to buyers.
 */
public class SolutionBuyerOperations implements BuyerOperations {
    private static final Connection c = DB.getInstance().getConnection();

    /**
     * Creates a new buyer with the given buyerName, idCity, and balance equal to zero.
     *
     * @param buyerName the name of the buyer
     * @param idCity    the ID of the city
     * @return the generated key of the created buyer, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int createBuyer(String buyerName, int idCity) {
        if (cityDoesNotExist(idCity)) return -1;

        String query = "INSERT INTO Buyer (Name, Balance, IdCity) VALUES (?, 0, ?)";
        int generatedKey = -1;

        try (PreparedStatement ps = c.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, buyerName);
            ps.setInt(2, idCity);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedKey = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return generatedKey;
    }

    /**
     * Sets the city of the buyer with the given idBuyer to the given idCity.
     *
     * @param idBuyer the ID of the buyer
     * @param idCity  the ID of the city
     * @return 1 if the city is set successfully, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int setCity(int idBuyer, int idCity) {
        if (cityDoesNotExist(idCity)) return -1;

        String query = "UPDATE Buyer SET IdCity = ? WHERE Id = ?";

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idCity);
            ps.setInt(2, idBuyer);

            return ps.executeUpdate() > 0 ? 1 : -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the ID of the city for the buyer with the given idBuyer.
     *
     * @param idBuyer the ID of the buyer
     * @return the ID of the city, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int getCity(int idBuyer) {
        String query = "SELECT IdCity FROM Buyer WHERE Id = ?";
        int idCity = -1;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idBuyer);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idCity = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return idCity;
    }

    /**
     * Increases the credit of the buyer with the given idBuyer by the specified credit amount.
     *
     * @param idBuyer the ID of the buyer
     * @param credit  the credit amount to increase
     * @return the updated balance of the buyer, or null otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public BigDecimal increaseCredit(int idBuyer, BigDecimal credit) {
        String query1 = "UPDATE Buyer SET Balance = Balance + ? WHERE Id = ?";
        String query2 = "SELECT Balance FROM Buyer WHERE Id = ?";
        BigDecimal updatedBalance = null;

        try (
                PreparedStatement ps1 = c.prepareStatement(query1);
                PreparedStatement ps2 = c.prepareStatement(query2)
        ) {
            ps1.setBigDecimal(1, credit);
            ps1.setInt(2, idBuyer);

            int rowsAffected = ps1.executeUpdate();
            if (rowsAffected > 0) {
                ps2.setInt(1, idBuyer);

                try (ResultSet rs = ps2.executeQuery()) {
                    if (rs.next()) {
                        updatedBalance = rs.getBigDecimal("Balance");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return updatedBalance;
    }

    /**
     * Creates a new empty order for the buyer with the given idBuyer.
     *
     * @param idBuyer the ID of the buyer
     * @return the generated key of the created order, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int createOrder(int idBuyer) {
        if (!buyerExists(idBuyer)) return -1;

        String query = "INSERT INTO [Order] (IdBuyer, Status) VALUES (?, 'created')";
        int generatedKey = -1;

        try (PreparedStatement ps = c.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idBuyer);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedKey = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return generatedKey;
    }

    /**
     * Retrieves all orders of the buyer with the given idBuyer.
     *
     * @param idBuyer the ID of the buyer
     * @return a list of order IDs, or null otherwise
     */
    @Override
    public List<Integer> getOrders(int idBuyer) {
        String query = "SELECT Id FROM [Order] WHERE IdBuyer = ?";
        List<Integer> allOrders = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idBuyer);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    allOrders.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return allOrders.isEmpty() ? null : allOrders;
    }

    /**
     * Retrieves the credit balance of the buyer with the given idBuyer.
     *
     * @param idBuyer the ID of the buyer
     * @return the credit balance of the buyer, or null otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public BigDecimal getCredit(int idBuyer) {
        String query = "SELECT Balance FROM Buyer WHERE Id = ?";
        BigDecimal balance = null;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idBuyer);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    balance = rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return balance;
    }

    /**
     * Checks if the city with the given idCity does not exist.
     *
     * @param idCity the ID of the city
     * @return true if the city does not exist, false otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    private boolean cityDoesNotExist(int idCity) {
        String query = "SELECT 1 FROM City WHERE Id = ?";

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idCity);

            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if the buyer with the given idBuyer exists.
     *
     * @param idBuyer the ID of the buyer
     * @return true if the buyer exists, false otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    private boolean buyerExists(int idBuyer) {
        String query = "SELECT 1 FROM Buyer WHERE Id = ?";

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idBuyer);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
