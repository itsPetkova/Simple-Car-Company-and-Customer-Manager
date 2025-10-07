package carsharing;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    // JDBC driver name and database URL
    static final String JDBC_DRIVER = "org.h2.Driver";
    static final String DB_URL = "jdbc:h2:./src/carsharing/db/carsharing";

    //  Database credentials
    static final String USER = "";
    static final String PASS = "";

    // Queries
    static final String createCompanyTable = "CREATE TABLE IF NOT EXISTS COMPANY (" +
            "id INT PRIMARY KEY AUTO_INCREMENT, " +
            "name VARCHAR(255) UNIQUE NOT NULL)";
    static final String getCompanies = "SELECT * FROM COMPANY ORDER BY id";
    static final String getCompany = "SELECT * FROM COMPANY WHERE id=(?)";
    static final String getCompanyCars = "SELECT * FROM CAR WHERE company_id=(?) ORDER BY id";
    static final String getFreeCompanyCars = "SELECT * FROM CAR " +
            "WHERE company_id = ? " +
            "AND id NOT IN (SELECT rented_car_id FROM CUSTOMER WHERE rented_car_id IS NOT NULL)" +
            "ORDER BY id";
    static final String createCompany = "INSERT INTO COMPANY (name) VALUES (?)";
    static final String deleteCompanies = "DELETE FROM COMPANY";
    static final String restartCompanies = "ALTER TABLE company ALTER COLUMN id RESTART WITH 1";

    static final String createCarTable = "CREATE TABLE IF NOT EXISTS CAR (" +
            "id INT PRIMARY KEY AUTO_INCREMENT, " +
            "name VARCHAR(255) UNIQUE NOT NULL, " +
            "company_id INT NOT NULL, " +
            "FOREIGN KEY (company_id) REFERENCES COMPANY(id) ON DELETE CASCADE)";
    static final String getCars = "SELECT * FROM CAR WHERE company_id=(?) ORDER BY id";
    static final String getCar = "SELECT * FROM CAR WHERE id=(?)";
    static final String createCar = "INSERT INTO CAR (name, company_id) VALUES (?, ?)";
    static final String deleteCars = "DELETE FROM CAR";

    static final String createCustomerTable = "CREATE TABLE IF NOT EXISTS CUSTOMER (" +
            "id INT PRIMARY KEY AUTO_INCREMENT, " +
            "name VARCHAR(255) UNIQUE NOT NULL, " +
            "rented_car_id INT, " +
            "FOREIGN KEY (rented_car_id) REFERENCES CAR(id) ON DELETE CASCADE)";
    static final String getCustomers = "SELECT * FROM CUSTOMER ORDER BY id";
    static final String getCustomer = "SELECT * FROM Customer WHERE id=(?)";
    static final String alterCustomer = "UPDATE CUSTOMER SET rented_car_id=(?) WHERE id=(?)";
    static final String createCustomer = "INSERT INTO CUSTOMER (name, rented_car_id) VALUES (?, ?)";
    static final String deleteCustomers = "DELETE FROM CUSTOMER";

    public static void main(String[] args) {
        try (
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                Statement stmt = conn.createStatement();
                Scanner sc = new Scanner(System.in)
        ) {
            Class.forName(JDBC_DRIVER);
            stmt.executeUpdate(createCompanyTable);
            //stmt.executeUpdate(deleteCompanies);
            //stmt.executeUpdate(restartCompanies);
            stmt.executeUpdate(createCarTable);
            //stmt.executeUpdate(deleteCars);
            stmt.executeUpdate(createCustomerTable);
            //stmt.executeUpdate(deleteCustomers);

            boolean exitProgram = false;

            while (!exitProgram) {
                printMainMenu();
                String input = sc.nextLine().trim();

                switch (input) {
                    case "1":
                        handleManagerMenu(sc, conn, stmt);
                        break;
                    case "2":
                        handleCustomersMenu(sc, conn, stmt);
                        break;
                    case "3":
                        createCustomer(sc, conn);
                        break;
                    case "0":
                        exitProgram = true;
                        break;
                    default:
                        System.out.println("Invalid option. Try again.");
                }
            }

            System.out.println("Goodbye!");

        } catch (SQLException | ClassNotFoundException se) {
            se.printStackTrace();
        }
    }

    private static void handleManagerMenu(Scanner sc, Connection conn, Statement stmt) throws SQLException {
        boolean backToMain = false;

        while (!backToMain) {
            System.out.println();
            System.out.println("1. Company list");
            System.out.println("2. Create a company");
            System.out.println("0. Back");
            System.out.print("> ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    ResultSet rs = stmt.executeQuery(getCompanies);
                    if (!rs.next()) {
                        System.out.println("The company list is empty!\n");
                        break;
                    } else {
                        System.out.println("\nChoose a company:");
                        do {
                            System.out.println(rs.getInt("id") + ". " + rs.getString("name"));
                        } while (rs.next());
                    }
                    rs.close();
                    System.out.println("0. Back");
                    System.out.print("> ");
                    String companyId = sc.nextLine().trim();
                    if (!companyId.equals("0")) {
                        try (PreparedStatement pstmt = conn.prepareStatement(getCompany)) {
                            pstmt.setInt(1, Integer.parseInt(companyId));
                            try (ResultSet company = pstmt.executeQuery()) {
                                if (company.next()) {
                                    int id = company.getInt("id");
                                    String name = company.getString("name");
                                    handleCompanyMenu(sc, conn, id, name);
                                } else {
                                    System.out.println("Company not found.\n");
                                }
                            }
                        } catch (SQLException e) {
                            System.out.println(e.getMessage());
                            System.out.println("Error finding company (maybe wrong ID).\n");
                        }
                    }
                    break;
                case "2":
                    System.out.print("\nEnter the company name: ");
                    String companyName = sc.nextLine().trim();

                    if (companyName.isEmpty()) {
                        System.out.println("Company name cannot be empty.\n");
                    } else {
                        try (PreparedStatement pstmt = conn.prepareStatement(createCompany)) {
                            pstmt.setString(1, companyName);
                            pstmt.executeUpdate();
                            System.out.println("The company was created!\n");
                        } catch (SQLException e) {
                            System.out.println("Error creating company (maybe duplicate name).\n");
                        }
                    }
                    break;
                case "0":
                    backToMain = true;
                    break;
                default:
                    System.out.println("Invalid option. Try again.\n");
            }
        }
    }

    private static void handleCustomersMenu(Scanner sc, Connection conn, Statement stmt) throws SQLException {
        boolean backToMain = false;

        ResultSet rs = stmt.executeQuery(getCustomers);

        while (!backToMain) {
            System.out.println();
            System.out.println("Choose a customer:");
            if (!rs.next()) {
                System.out.println();
                System.out.println("The customer list is empty!\n");
                backToMain = true;
                continue;
            } else {
                do {
                    System.out.println(rs.getInt("id") + ". " + rs.getString("name"));
                } while (rs.next());
            }
            System.out.println("0. Back");

            String choice = sc.nextLine().trim();

            if (!choice.equals("0")) {
                try (PreparedStatement pstmt = conn.prepareStatement(getCustomer)) {
                    pstmt.setInt(1, Integer.parseInt(choice));
                    try (ResultSet customer = pstmt.executeQuery()) {
                        if (customer.next()) {
                            String name = customer.getString("name");
                            handleCustomerMenu(sc, conn, stmt, Integer.parseInt(choice), name);
                        } else {
                            System.out.println("Company not found.\n");
                        }
                    }
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                    System.out.println("Error finding company (maybe wrong ID).\n");
                }
            }
            break;
        }
    }

    private static void handleCustomerMenu(Scanner sc, Connection conn, Statement stmt, int customerId, String name) throws SQLException {
        boolean backToCustomerList = false;

        while (!backToCustomerList) {
            System.out.println();
            System.out.println(name + " customer:");
            System.out.println("1. Rent a car");
            System.out.println("2. Return a rented car");
            System.out.println("3. My rented car");
            System.out.println("0. Back");
            System.out.print("> ");

            String input = sc.nextLine().trim();

            switch (input) {
                case "1":
                    try (PreparedStatement getCustomerStmt = conn.prepareStatement(getCustomer)) {
                        getCustomerStmt.setInt(1, customerId);
                        try (ResultSet customer = getCustomerStmt.executeQuery()) {
                            if (customer.next()) {
                                Integer rentedCarId = customer.getObject("rented_car_id", Integer.class);
                                if (rentedCarId != null) {
                                    System.out.println("You've already rented a car!\n");
                                    break;
                                }
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }

                    ResultSet rs = stmt.executeQuery(getCompanies);
                    if (!rs.next()) {
                        System.out.println("The company list is empty!\n");
                        backToCustomerList = true;
                        break;
                    } else {
                        System.out.println("\nChoose a company:");
                        do {
                            System.out.println(rs.getInt("id") + ". " + rs.getString("name"));
                        } while (rs.next());
                    }
                    rs.close();
                    System.out.println("0. Back\n");
                    System.out.print("> ");
                    String companyId = sc.nextLine().trim();
                    if (!companyId.equals("0")) {
                        try (PreparedStatement pstmt = conn.prepareStatement(getCompany)) {
                            pstmt.setInt(1, Integer.parseInt(companyId));

                            try (ResultSet company = pstmt.executeQuery()) {
                                if (company.next()) {
                                    int id = company.getInt("id");
                                    String companyName = company.getString("name");

                                    try (PreparedStatement getCompanyCarsStmt = conn.prepareStatement(getFreeCompanyCars)) {
                                        getCompanyCarsStmt.setInt(1, id);
                                        ResultSet companyCars = getCompanyCarsStmt.executeQuery();
                                        if (!companyCars.next()) {
                                            System.out.println();
                                            System.out.println("No available cars in the " + companyName + " company");
                                        } else {
                                            List<Integer> carIds = new ArrayList<>();
                                            List<String> carNames = new ArrayList<>();
                                            System.out.println("Choose a car:");
                                            int index = 1;
                                            do {
                                                System.out.println(index++ + ". " + companyCars.getString("name"));
                                                carIds.add(companyCars.getInt("id"));
                                                carNames.add(companyCars.getString("name"));
                                            } while (companyCars.next());
                                            System.out.println("0. Back\n");
                                            System.out.print("> ");
                                            String chosenCarIndex = sc.nextLine().trim();

                                            if (chosenCarIndex.equals("0")) {
                                                break;
                                            }

                                            try (PreparedStatement alterCustomerStmt = conn.prepareStatement(alterCustomer)) {
                                                alterCustomerStmt.setInt(1, carIds.get(Integer.parseInt(chosenCarIndex) - 1));
                                                alterCustomerStmt.setInt(2, customerId);
                                                alterCustomerStmt.executeUpdate();
                                                System.out.println("You rented '" + carNames.get(Integer.parseInt(chosenCarIndex) - 1) + "'\n");
                                            }
                                        }
                                    }
                                } else {
                                    System.out.println("Company not found.\n");
                                }
                            }
                        } catch (SQLException e) {
                            System.out.println(e.getMessage());
                            System.out.println("Error finding company (maybe wrong ID).\n");
                        }
                    }
                    break;

                case "2":
                    try (PreparedStatement getCustomerStmt = conn.prepareStatement(getCustomer)) {
                        getCustomerStmt.setInt(1, customerId);
                        try (ResultSet customer = getCustomerStmt.executeQuery()) {
                            if (customer.next()) {
                                customer.getInt("rented_car_id");
                                if (customer.wasNull()) {
                                    System.out.println("You didn't rent a car!\n");
                                    break;
                                }
                                try (PreparedStatement alterCustomerStmt = conn.prepareStatement(alterCustomer)) {
                                    alterCustomerStmt.setNull(1, Types.INTEGER);
                                    alterCustomerStmt.setInt(2, customerId);
                                    alterCustomerStmt.executeUpdate();
                                    System.out.println("You've returned a rented car!\n");
                                }
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "3":
                    try (PreparedStatement getCustomerStmt = conn.prepareStatement(getCustomer)) {
                        getCustomerStmt.setInt(1, customerId);
                        try (ResultSet customer = getCustomerStmt.executeQuery()) {
                            if (customer.next()) {
                                int car_id = customer.getInt("rented_car_id");
                                if (customer.wasNull()) {
                                    System.out.println("You didn't rent a car!\n");
                                    break;
                                }

                                try (PreparedStatement getCarStmt = conn.prepareStatement(getCar)) {
                                    getCarStmt.setInt(1, car_id);
                                    ResultSet car = getCarStmt.executeQuery();
                                    if (car.next()) {
                                        System.out.println("Your rented car:");
                                        System.out.println(car.getString("name"));
                                        System.out.println("Company: ");
                                        int company_id = car.getInt("company_id");
                                        try (PreparedStatement getCompanyStmt = conn.prepareStatement(getCompany)) {
                                            getCompanyStmt.setInt(1, company_id);
                                            ResultSet company = getCompanyStmt.executeQuery();
                                            if (company.next()) {
                                                System.out.println(company.getString("name"));
                                            }
                                        }
                                    } else {
                                        System.out.println("Car not found.\n");
                                    }
                                }
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "0":
                    backToCustomerList = true;
                    break;
                default:
                    System.out.println("Invalid option. Try again.\n");
            }
        }
    }

    private static void handleCompanyMenu(Scanner sc, Connection conn, int id, String name) throws SQLException {
        boolean backToCompanyList = false;

        while (!backToCompanyList) {
            System.out.println();
            System.out.println(name + " company:");
            System.out.println("1. Car list");
            System.out.println("2. Create a car");
            System.out.println("0. Back");
            System.out.print("> ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    try (PreparedStatement getCarsStmt = conn.prepareStatement(getCars)) {
                        getCarsStmt.setInt(1, id);
                        ResultSet rs = getCarsStmt.executeQuery();
                        if (!rs.next()) {
                            System.out.println();
                            System.out.println("The car list is empty!\n");
                        } else {
                            System.out.println("\n" + name + " cars:");
                            int index = 1;
                            do {
                                System.out.println(index++ + ". " + rs.getString("name"));
                            } while (rs.next());
                        }
                    }
                    break;
                case "2":
                    System.out.println("\nEnter the car name:\n");
                    System.out.print("> ");
                    String carName = sc.nextLine().trim();

                    if (carName.isEmpty()) {
                        System.out.println("Car name cannot be empty.\n");
                    } else {
                        try (PreparedStatement pstmt = conn.prepareStatement(createCar)) {
                            pstmt.setString(1, carName);
                            pstmt.setInt(2, id);
                            pstmt.executeUpdate();
                            System.out.println("The car was added!\n");
                        }
                    }
                    break;
                case "0":
                    backToCompanyList = true;
                    break;
                default:
                    System.out.println("Invalid option. Try again.\n");
            }
        }
    }

    private static void createCustomer(Scanner sc, Connection conn) {
        System.out.println("\nEnter the customer name:\n");
        System.out.print("> ");
        String customerName = sc.nextLine().trim();
        try (PreparedStatement pstmt = conn.prepareStatement(createCustomer)) {
            pstmt.setString(1, customerName);
            pstmt.setNull(2, Types.INTEGER);
            pstmt.executeUpdate();
            System.out.println("The customer was added!\n");
        } catch (SQLException e) {
            System.out.println("Error creating customer (maybe duplicate name).\n");
        }
    }

    private static void printMainMenu() {
        System.out.println("1. Log in as a manager");
        System.out.println("2. Log in as a customer");
        System.out.println("3. Create a customer");
        System.out.println("0. Exit");
        System.out.print("> ");
    }
}
