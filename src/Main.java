import rs.etf.sab.operations.*;
import rs.etf.sab.solution.*;

import rs.etf.sab.tests.TestHandler;
import rs.etf.sab.tests.TestRunner;

public class Main {
    public static void main(String[] args) {
        ArticleOperations articleOperations = new SolutionArticleOperations();
        BuyerOperations buyerOperations = new SolutionBuyerOperations();
        CityOperations cityOperations = new SolutionCityOperations();
        GeneralOperations generalOperations = new SolutionGeneralOperations();
        OrderOperations orderOperations = new SolutionOrderOperations();
        ShopOperations shopOperations = new SolutionShopOperations();
        TransactionOperations transactionOperations = new SolutionTransactionOperations();

        TestHandler.createInstance(
                articleOperations,
                buyerOperations,
                cityOperations,
                generalOperations,
                orderOperations,
                shopOperations,
                transactionOperations
        );

        TestRunner.runTests();
    }
}