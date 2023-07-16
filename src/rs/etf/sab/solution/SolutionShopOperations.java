package rs.etf.sab.solution;

import rs.etf.sab.operations.ShopOperations;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the ShopOperations interface and provides methods related to shops.
 */
public class SolutionShopOperations implements ShopOperations {
    private static final Connection c = DB.getInstance().getConnection();

    /**
     * Creates a new shop with the specified unique name and city, and with 0% discount.
     *
     * @param shopName the name of the shop
     * @param cityName the name of the city
     * @return the ID of the newly created shop, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int createShop(String shopName, String cityName) {
        String query = "INSERT INTO Shop (Name, Balance, Discount, IdCity) VALUES (?, 0, 0, ?)";
        int generatedKey = -1;
        int idCity = getCity(cityName);

        if (shopExists(shopName) || idCity == -1) return -1;

        try (PreparedStatement ps = c.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, shopName);
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
     * Sets the city of the specified shop.
     *
     * @param idShop   the ID of the shop
     * @param cityName the name of the city
     * @return the number of rows affected by the update, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int setCity(int idShop, String cityName) {
        String query = "UPDATE Shop SET IdCity = ? WHERE Id = ?";
        int idCity = getCity(cityName);

        if (idCity == -1) return -1;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idCity);
            ps.setInt(2, idShop);

            return ps.executeUpdate() > 0 ? 1 : -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the city ID associated with the specified shop.
     *
     * @param idShop the ID of the shop
     * @return the ID of the city associated with the shop, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int getCity(int idShop) {
        String query = "SELECT IdCity FROM Shop WHERE Id = ?";
        int idCity = -1;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idShop);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idCity = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return idCity;
    }

    /**
     * Sets the discount percentage for the specified shop.
     *
     * @param idShop             the ID of the shop
     * @param discountPercentage the discount percentage to set
     * @return the number of rows affected by the update, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int setDiscount(int idShop, int discountPercentage) {
        if (discountPercentage < 0 || discountPercentage > 100) return -1;

        String query = "UPDATE Shop SET Discount = ? WHERE Id = ?";

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setBigDecimal(1, BigDecimal.valueOf(discountPercentage).setScale(3));
            ps.setInt(2, idShop);

            return ps.executeUpdate() > 0 ? 1 : -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Increases the article count of the specified article.
     *
     * @param idArticle the ID of the article
     * @param increment the amount to increase the article count by
     * @return the new article count, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int increaseArticleCount(int idArticle, int increment) {
        String query = "UPDATE Article SET Quantity = Quantity + ? OUTPUT inserted.Quantity WHERE Id = ?";
        int returnValue = -1;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, increment);
            ps.setInt(2, idArticle);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    returnValue = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return returnValue;
    }

    /**
     * Retrieves the article count for the specified article.
     *
     * @param idArticle the ID of the article
     * @return the article count, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int getArticleCount(int idArticle) {
        String query = "SELECT Quantity FROM Article WHERE Id = ?";
        int returnValue = -1;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idArticle);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    returnValue = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return returnValue;
    }

    /**
     * Retrieves the list of articles associated with the specified shop.
     *
     * @param idShop the ID of the shop
     * @return the list of article IDs, or null otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public List<Integer> getArticles(int idShop) {
        String query = "SELECT Id FROM Article WHERE IdShop = ?";
        List<Integer> articlesInShop = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idShop);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    articlesInShop.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return articlesInShop.isEmpty() ? null : articlesInShop;
    }

    /**
     * Retrieves the discount percentage for the specified shop.
     *
     * @param idShop the ID of the shop
     * @return the discount percentage, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int getDiscount(int idShop) {
        String query = "SELECT Discount FROM Shop WHERE Id = ?";
        int discount = -1;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idShop);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                discount = rs.getBigDecimal(1).intValue();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return discount;
    }

    /**
     * Checks if a shop with the given name already exists.
     *
     * @param shopName the name of the shop
     * @return true if the shop exists, false otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    private boolean shopExists(String shopName) {
        String query = "SELECT 1 FROM Shop WHERE Name = ?";

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setString(1, shopName);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the ID of the city with the given name.
     *
     * @param cityName the name of the city
     * @return the ID of the city, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    private int getCity(String cityName) {
        String query = "SELECT Id FROM City WHERE Name = ?";
        int idCity = -1;

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setString(1, cityName);

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
}
