package rs.etf.sab.solution;

import rs.etf.sab.operations.GeneralOperations;
import rs.etf.sab.operations.OrderOperations;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implements the OrderOperations interface and provides methods related to orders.
 */
public class SolutionOrderOperations implements OrderOperations {
    private static final Connection c = DB.getInstance().getConnection();
    private static final GeneralOperations go = new SolutionGeneralOperations();

    private static final HashMap<Integer, List<Integer>> pathFromNearestToBuyerCity = new HashMap<>();

    /**
     * Adds an article to an order.
     * <p>
     * This method adds an article to the order only if there are sufficient quantities available in the shop.
     * If the article is already included in the order, it increases the quantity of the ordered article.
     * <p>
     * It is assumed that adding and removing order items is allowed only when the order is in the 'created' state.
     * Therefore, the 'Quantity' in the 'Article' table does not need to be modified until the order is 'sent'.
     *
     * @param idOrder   the ID of the order
     * @param idArticle the ID of the article
     * @param quantity  the quantity of the article
     * @return the ID of the newly added item if successful, or -1 otherwise
     */
    @Override
    public int addArticle(int idOrder, int idArticle, int quantity) {
        if (quantity <= 0 || !getState(idOrder).equals("created")) return -1;

        String query1 = "SELECT Quantity FROM Article WHERE Id = ?";
        String query2 = "SELECT Id, Quantity FROM OrderItem WHERE IdOrder = ? AND IdArticle = ?";
        String query3 = "INSERT INTO OrderItem (Quantity, IdOrder, IdArticle) VALUES (?, ?, ?)";
        String query4 = "UPDATE OrderItem SET Quantity = Quantity + ? WHERE IdOrder = ? AND IdArticle = ?";
        int availableQuantity = 0, existingItemQuantity = 0, existingItemId = -1;
        int idItem = -1;

        try (
                PreparedStatement ps1 = c.prepareStatement(query1);
                PreparedStatement ps2 = c.prepareStatement(query2);
                PreparedStatement ps3 = c.prepareStatement(query3, PreparedStatement.RETURN_GENERATED_KEYS);
                PreparedStatement ps4 = c.prepareStatement(query4)
        ) {
            // Query 1
            ps1.setInt(1, idArticle);

            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                availableQuantity = rs1.getInt(1);
            }

            // Query 2
            ps2.setInt(1, idOrder);
            ps2.setInt(2, idArticle);

            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
                existingItemId = rs2.getInt(1);
                existingItemQuantity = rs2.getInt(2);
            }

            if (availableQuantity >= existingItemQuantity + quantity) { //there's enough articles in the shop
                if (existingItemQuantity == 0) {
                    // Query 3
                    ps3.setInt(1, quantity);
                    ps3.setInt(2, idOrder);
                    ps3.setInt(3, idArticle);

                    int rowsAffected = ps3.executeUpdate();
                    if (rowsAffected > 0) {
                        ResultSet rs3 = ps3.getGeneratedKeys();
                        if (rs3.next()) {
                            idItem = rs3.getInt(1);
                        }
                    }
                } else {
                    // Query 4
                    ps4.setInt(1, quantity);
                    ps4.setInt(2, idOrder);
                    ps4.setInt(3, idArticle);

                    int rowsAffected = ps4.executeUpdate();
                    if (rowsAffected > 0) {
                        idItem = existingItemId;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return idItem;
    }

    /**
     * Removes an article from an order.
     * It is assumed that adding and removing order items is allowed only when the order is in the 'created' state.
     * Therefore, the 'Quantity' in the 'Article' table does not need to be modified until the order is 'sent'.
     *
     * @param idOrder   the ID of the order
     * @param idArticle the ID of the article
     * @return 1 if successful, or -1 otherwise
     */
    @Override
    public int removeArticle(int idOrder, int idArticle) {
        if (!getState(idOrder).equals("created")) return -1; // Order has been sent, can't delete item now

        String query = "DELETE FROM OrderItem WHERE IdOrder = ? AND IdArticle = ?";

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idOrder);
            ps.setInt(2, idArticle);

            return ps.executeUpdate() > 0 ? 1 : -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves all items in an order.
     *
     * @param idOrder the ID of the order
     * @return a list of item IDs if items exist, or null otherwise
     */
    @Override
    public List<Integer> getItems(int idOrder) {
        String query = "SELECT Id FROM OrderItem WHERE IdOrder = ?";
        List<Integer> allItems = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idOrder);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    allItems.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return allItems.isEmpty() ? null : allItems;
    }

    /**
     * Completes an order.
     * <p>
     * It reduces the quantity of each article by the quantity request in the order,
     * sets all the required fields when completing the order,
     * withdraws money from the buyer's account and creates transaction for money withdrawal.
     * <p>
     * It is assumed there is no 'OrderItem' instance with Quantity greater than Quantity of related Article.
     * That is prevented in 'addArticle()' and 'removeArticle()' methods.
     *
     * @param idOrder the ID of the order
     * @return 1 if successful, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int completeOrder(int idOrder) {
        if (!getState(idOrder).equals("created")) return -1;
        if (getItems(idOrder) == null) return -1; // isEmpty() has been checked inside 'getItems()'
        if (canAffordPurchase(idOrder) < 0) return -1;

        String query1 = """
                UPDATE Article
                SET Quantity = Quantity - (
                    SELECT Quantity
                    FROM OrderItem
                    WHERE IdOrder = ? AND IdArticle = Article.Id
                )
                WHERE Id IN (
                    SELECT IdArticle
                    FROM OrderItem
                    WHERE IdOrder = ?
                )""";
        String query2 = """
                UPDATE [Order]
                SET Status = 'sent', DateSent = ?,
                    DateNearest = ?, DateArrived = ?,
                    DaysToAssemble = ?, IdNearestCity = ?
                WHERE Id = ?""";
        String query3 = """
                UPDATE Buyer
                SET Balance = Balance - ?
                WHERE Id = ?""";
        String query4 = "INSERT INTO [Transaction] (Date, Amount, IdOrder, IdShop, IdBuyer) VALUES (?, ?, ?, NULL, ?)";
        int idBuyer = getBuyer(idOrder);

        try (
                PreparedStatement ps1 = c.prepareStatement(query1);
                PreparedStatement ps2 = c.prepareStatement(query2);
                PreparedStatement ps3 = c.prepareStatement(query3);
                PreparedStatement ps4 = c.prepareStatement(query4)
        ) {
            // Query 1: Reduce the quantity of each article by the quantity requested in the order
            ps1.setInt(1, idOrder);
            ps1.setInt(2, idOrder);
            if (ps1.executeUpdate() == 0) return -1;

            // Query 2: Set all the required fields when completing the order
            DijkstraAlgorithm da = new DijkstraAlgorithm();
            pathFromNearestToBuyerCity.put(idOrder, da.dijkstraAlgorithm(getBuyerCity(idBuyer), idOrder));

            // Calculate the 'DateSent', 'DateNearest' and the 'DateArrived'
            Calendar dateTime = go.getCurrentTime();
            Timestamp dateSent = new Timestamp(dateTime.getTimeInMillis());

            int daysToAssemble = da.getMaxDistanceToNearestCity();
            int daysFromNearestToBuyer = da.getMinDistanceFromBuyerToNearestCity();

            dateTime.add(Calendar.DAY_OF_MONTH, daysToAssemble);
            Timestamp dateNearest = new Timestamp(dateTime.getTimeInMillis());

            dateTime.add(Calendar.DAY_OF_MONTH, daysFromNearestToBuyer);
            Timestamp dateArrived = new Timestamp(dateTime.getTimeInMillis());

            ps2.setTimestamp(1, dateSent);
            ps2.setTimestamp(2, dateNearest);
            ps2.setTimestamp(3, dateArrived);
            ps2.setInt(4, daysToAssemble);
            ps2.setInt(5, da.getIdNearestCity());
            ps2.setInt(6, idOrder);

            if (ps2.executeUpdate() == 0) return -1;

            // Query 3: Withdraw money from the buyer's account
            BigDecimal amount = getFinalPrice(idOrder);

            ps3.setBigDecimal(1, amount);
            ps3.setInt(2, idBuyer);

            if (ps3.executeUpdate() == 0) return -1;

            // Query 4: Create transaction for money withdrawal
            ps4.setTimestamp(1, dateSent);
            ps4.setBigDecimal(2, amount);
            ps4.setInt(3, idOrder);
            ps4.setInt(4, idBuyer);

            if (ps4.executeUpdate() == 0) return -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return 1;
    }

    /**
     * Retrieves the final price of an order after all the discounts.
     * It calculates the discounted price and sets the buyer discount value.
     *
     * @param idOrder the ID of the order
     * @return the final price if successful, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public BigDecimal getFinalPrice(int idOrder) {
        String query = "SELECT DiscountedPrice FROM [Order] WHERE Id = ?";
        String callableQuery = "{ call SP_FINAL_PRICE (?, ?) }";
        BigDecimal discountedPrice = BigDecimal.valueOf(-1);

        try (
                PreparedStatement ps = c.prepareStatement(query);
                CallableStatement cs = c.prepareCall(callableQuery)
        ) {
            ps.setInt(1, idOrder);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    discountedPrice = rs.getBigDecimal(1);

                    // Check if discounted price wasn't calculated before (NULL attribute)
                    if (discountedPrice == null) { // Covers the situation: 'Status' == 'created'
                        // Calculate 'TotalPrice', 'DiscountedPrice', 'BuyerDiscount'
                        cs.setInt(1, idOrder);
                        cs.registerOutParameter(2, Types.DECIMAL);
                        cs.execute();

                        discountedPrice = cs.getBigDecimal(2);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return discountedPrice.setScale(3);
    }

    /**
     * Retrieves the discount sum of an order.
     *
     * @param idOrder the ID of the order
     * @return the discount sum if successful, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public BigDecimal getDiscountSum(int idOrder) {
        if (getState(idOrder).equals("created")) {
            return BigDecimal.valueOf(-1).setScale(3);
        }

        String query = "SELECT TotalPrice, DiscountedPrice FROM [Order] WHERE Id = ?";
        BigDecimal discountSum = BigDecimal.valueOf(-1);

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idOrder);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal totalPrice = rs.getBigDecimal(1); //price without discounts
                    BigDecimal discountedPrice = rs.getBigDecimal(2);

                    discountSum = totalPrice.subtract(discountedPrice);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return discountSum.setScale(3);
    }

    /**
     * Retrieves the status of an order.
     *
     * @param idOrder the ID of the order
     * @return the state of the order if successful, or null otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public String getState(int idOrder) {
        String query = "SELECT Status FROM [Order] Where Id = ?";
        String status = null;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idOrder);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    status = rs.getString("Status");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return status;
    }

    /**
     * Retrieves the sent time of an order.
     * <p>
     * The order is considered complete and 'sent' to the buyer when its 'Status' transitions to 'sent'.
     * At that point, we record the current time in the 'DateSent' attribute of the order.
     *
     * @param idOrder the ID of the order
     * @return the sent time of the order if successful, or null otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public Calendar getSentTime(int idOrder) {
        String query = "SELECT DateSent FROM [Order] Where Id = ?";
        Calendar dateTimeCreated = null;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idOrder);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp timestamp = rs.getTimestamp(1);

                    if (timestamp != null) { //same as: if (!getState(idOrder).equals("created"))
                        dateTimeCreated = Calendar.getInstance();
                        dateTimeCreated.setTimeInMillis(timestamp.getTime());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return dateTimeCreated;
    }

    /**
     * Retrieves the received time of an order.
     *
     * @param idOrder the ID of the order
     * @return the received time of the order if successful, or null otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public Calendar getRecievedTime(int idOrder) {
        if (!getState(idOrder).equals("arrived")) return null;

        String query = "SELECT DateArrived FROM [Order] Where Id = ?";
        Calendar dateTimeArrived = null;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idOrder);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp timestamp = rs.getTimestamp(1);

                    if (timestamp != null) { // Covers: if (!getState(idOrder).equals("created"))
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
     * Retrieves the buyer of an order.
     *
     * @param idOrder the ID of the order
     * @return the ID of the buyer if successful, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int getBuyer(int idOrder) {
        String query = "SELECT IdBuyer FROM [Order] Where Id = ?";
        int idBuyer = -1;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idOrder);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idBuyer = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return idBuyer;
    }

    /**
     * Retrieves the location of an order.
     * <p>
     * If order is assembled and order is moving from city C1 to city C2 then location of an order is city C1.
     * If order is not yet assembled then location of the order is location of the shop closest to buyer's city.
     * If order is in state "created" then location is -1.
     *
     * @param idOrder the ID of the order
     * @return the ID of the location if successful, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int getLocation(int idOrder) {
        if (getState(idOrder).equals("created")) return -1;

        String query = "SELECT DateNearest, IdNearestCity FROM [Order] WHERE Id = ?";
        Timestamp dateTimeCurrent = new Timestamp(go.getCurrentTime().getTimeInMillis());
        int idCity = -1;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idOrder);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp dateTimeNearest = rs.getTimestamp("DateNearest");

                if (getState(idOrder).equals("arrived")) { //'DateArrived' <= 'DateCurrent'
                    // Order has arrived
                    idCity = getBuyerCity(getBuyer(idOrder));
                } else {
                    // Order has been 'sent', has not 'arrived' yet
                    // Situation 1: If order is not yet assembled then location of the order is the nearest city
                    idCity = rs.getInt("IdNearestCity"); // id of the 'nearest city'

                    if (dateTimeNearest.compareTo(dateTimeCurrent) < 0) {
                        // Situation 2: Order is somewhere between the nearest city and the buyer city
                        long diffInMillis = Math.abs(dateTimeNearest.getTime() - dateTimeCurrent.getTime());
                        long diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);

                        for (int i = 0; i < pathFromNearestToBuyerCity.get(idOrder).size() - 1; i++) {
                            int idCity1 = pathFromNearestToBuyerCity.get(idOrder).get(i);
                            int idCity2 = pathFromNearestToBuyerCity.get(idOrder).get(i + 1);
                            int distance = getDistanceBetweenCities(idCity1, idCity2);

                            if (distance > diffInDays) break;
                            else {
                                diffInDays -= distance;
                                idCity = idCity1 == idCity ? idCity2 : idCity1;
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return idCity;
    }

    /**
     * Retrieves the ID of the buyer's city.
     *
     * @param idBuyer the ID of the buyer
     * @return the ID of the buyer's city if successful, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    private int getBuyerCity(int idBuyer) {
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
     * Retrieves the distance between two cities.
     *
     * @param idCity1 the ID of the first city
     * @param idCity2 the ID of the second city
     * @return the distance between the cities if successful, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    private int getDistanceBetweenCities(int idCity1, int idCity2) {
        String query = "SELECT Distance FROM Line WHERE (Id1 = ? AND Id2 = ?) OR (Id1 = ? AND Id2 = ?)";
        int distance = -1;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idCity1);
            ps.setInt(2, idCity2);
            ps.setInt(3, idCity2);
            ps.setInt(4, idCity1);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    distance = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return distance;
    }

    /**
     * Checks if the buyer can afford the purchase.
     *
     * @param idOrder the ID of the order
     * @return 1 if successful, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    private int canAffordPurchase(int idOrder) {
        String query = """
                SELECT
                    IIF(C.Balance >= (
                        SELECT SUM(OI.Quantity * P.Price * (100 - S.Discount) / 100.0) AS 'DiscountedPrice'
                        FROM OrderItem OI
                            JOIN Article P ON (OI.IdArticle = P.Id)
                            JOIN Shop S ON (P.IdShop = S.Id)
                        WHERE OI.IdOrder = ?
                    ), 1, -1)
                FROM Buyer C
                WHERE C.Id = ?""";
        int canAfford = -1;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idOrder);
            ps.setInt(2, getBuyer(idOrder));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                canAfford = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return canAfford;
    }
}
