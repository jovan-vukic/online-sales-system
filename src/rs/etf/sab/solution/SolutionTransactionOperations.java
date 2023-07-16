package rs.etf.sab.solution;

import rs.etf.sab.operations.TransactionOperations;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Implements the TransactionOperations interface and provides methods related to transactions.
 */
public class SolutionTransactionOperations implements TransactionOperations {
    private static final Connection c = DB.getInstance().getConnection();

    /**
     * Retrieves the total amount paid in the transactions made by a buyer.
     *
     * @param idBuyer the ID of the buyer
     * @return the total amount paid in the transactions, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public BigDecimal getBuyerTransactionsAmmount(int idBuyer) {
        String query = "SELECT COALESCE(SUM(Amount), 0) FROM [Transaction] WHERE IdBuyer = ?";
        BigDecimal balance = BigDecimal.valueOf(-1);

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idBuyer);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                balance = rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return balance.setScale(3);
    }

    /**
     * Retrieves the total amount of transactions made by a shop.
     *
     * @param idShop the ID of the shop
     * @return the total amount of transactions made by the shop, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public BigDecimal getShopTransactionsAmmount(int idShop) {
        String query = "SELECT COALESCE(SUM(Amount), 0) FROM [Transaction] WHERE IdShop = ?";
        BigDecimal balance = BigDecimal.valueOf(-1);

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idShop);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                balance = rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return balance.setScale(3);
    }

    /**
     * Retrieves the list of transaction IDs associated with a buyer.
     *
     * @param idBuyer the ID of the buyer
     * @return the list of transaction IDs for the buyer, or null otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public List<Integer> getTransationsForBuyer(int idBuyer) {
        String query = "SELECT Id FROM [Transaction] WHERE IdBuyer = ?";
        List<Integer> buyerTransactions = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idBuyer);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    buyerTransactions.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return buyerTransactions.isEmpty() ? null : buyerTransactions;
    }

    /**
     * Retrieves the transaction ID associated with a buyer's order.
     *
     * @param idOrder the ID of the buyer's order
     * @return the transaction ID for the buyer's order, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs
     */
    @Override
    public int getTransactionForBuyersOrder(int idOrder) {
        String query = "SELECT Id FROM [Transaction] WHERE IdShop IS NULL AND IdOrder = ?";
        int idTransaction = -1;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idOrder);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idTransaction = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return idTransaction;
    }

    /**
     * Retrieves the transaction ID associated with a shop and order.
     *
     * @param idOrder the ID of the order
     * @param idShop  the ID of the shop
     * @return the transaction ID for the shop and order, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int getTransactionForShopAndOrder(int idOrder, int idShop) {
        String query = "SELECT Id FROM [Transaction] WHERE IdOrder = ? AND IdShop = ?";
        int idTransaction = -1;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idOrder);
            ps.setInt(2, idShop);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idTransaction = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return idTransaction;
    }

    /**
     * Retrieves the list of transaction IDs associated with a shop.
     *
     * @param idShop the ID of the shop
     * @return the list of transaction IDs for the shop, or null otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public List<Integer> getTransationsForShop(int idShop) {
        String query = "SELECT Id FROM [Transaction] WHERE IdShop = ?";
        List<Integer> shopTransactions = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idShop);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    shopTransactions.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return shopTransactions.isEmpty() ? null : shopTransactions;
    }

    /**
     * Retrieves the date and time of execution for a transaction.
     *
     * @param idTransaction the ID of the transaction
     * @return the date and time of execution, or null otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public Calendar getTimeOfExecution(int idTransaction) {
        String query = "SELECT Date FROM [Transaction] T WHERE Id = ?";
        Calendar dateTimeArrived = null;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idTransaction);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp timestamp = rs.getTimestamp(1);

                    if (timestamp != null) {
                        dateTimeArrived = Calendar.getInstance();
                        dateTimeArrived.setTimeInMillis(timestamp.getTime());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return dateTimeArrived;
    }

    /**
     * Retrieves the amount that a buyer paid for an order.
     *
     * @param idOrder the ID of the order
     * @return the amount paid by the buyer for the order, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public BigDecimal getAmmountThatBuyerPayedForOrder(int idOrder) {
        String query = "SELECT Amount FROM [Transaction] WHERE IdShop IS NULL AND IdOrder = ?";
        BigDecimal payedAmount = BigDecimal.valueOf(-1);

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idOrder);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                payedAmount = rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return payedAmount.setScale(3);
    }

    /**
     * Retrieves the amount that a shop received for an order.
     *
     * @param idShop  the ID of the shop
     * @param idOrder the ID of the order
     * @return the amount received by the shop for the order, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public BigDecimal getAmmountThatShopRecievedForOrder(int idShop, int idOrder) {
        String query = "SELECT Amount FROM [Transaction] WHERE IdShop = ? AND IdOrder = ?";
        BigDecimal receivedAmount = BigDecimal.valueOf(-1);

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idShop);
            ps.setInt(2, idOrder);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                receivedAmount = rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return receivedAmount.setScale(3);
    }

    /**
     * Retrieves the amount of a transaction.
     *
     * @param idTransaction the ID of the transaction
     * @return the amount of the transaction, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public BigDecimal getTransactionAmount(int idTransaction) {
        String query = "SELECT Amount FROM [Transaction] WHERE Id = ?";
        BigDecimal receivedAmount = BigDecimal.valueOf(-1);

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idTransaction);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                receivedAmount = rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return receivedAmount.setScale(3);
    }

    /**
     * Retrieves the system profit calculated based on the transactions and order discounts.
     *
     * @return the system profit, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public BigDecimal getSystemProfit() {
        String query = """
                SELECT COALESCE(
                    SUM (T.Amount * (1 / (1 - O.BuyerDiscount / 100.0)) * (0.05 - O.BuyerDiscount / 100.0)),
                    0
                )
                FROM [Transaction] T JOIN [Order] O ON (T.IdOrder = O.Id)
                WHERE T.IdShop IS NULL AND O.Status = 'arrived'""";
        BigDecimal systemProfit = BigDecimal.valueOf(-1);

        try (
                PreparedStatement ps = c.prepareStatement(query);
                ResultSet rs = ps.executeQuery()
        ) {
            if (rs.next()) {
                systemProfit = rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return systemProfit.setScale(3);
    }
}
