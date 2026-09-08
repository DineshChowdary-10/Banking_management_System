import java.math.BigDecimal;
import java.sql.*;
import java.util.Scanner;

public class BankManagementSystem {

    static Scanner scanner = new Scanner(System.in);


    // =========================================================
    // CREATE CUSTOMER
    // =========================================================

    public static void createCustomer() {

        System.out.println("\n===== CREATE CUSTOMER =====");

        System.out.print("Enter customer name: ");
        String name = scanner.nextLine();

        System.out.print("Enter phone: ");
        String phone = scanner.nextLine();

        System.out.print("Enter email: ");
        String email = scanner.nextLine();

        System.out.print("Enter address: ");
        String address = scanner.nextLine();

        String sql = """
                INSERT INTO customer
                (customer_name, phone, email, address)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setString(3, email);
            pstmt.setString(4, address);

            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Customer created successfully!");
            }

        } catch (SQLException e) {
            System.out.println("Error creating customer.");
            System.out.println(e.getMessage());
        }
    }


    // =========================================================
    // CREATE ACCOUNT
    // =========================================================

    public static void createAccount() {

        System.out.println("\n===== CREATE ACCOUNT =====");

        System.out.print("Enter customer ID: ");
        int customerId = scanner.nextInt();

        System.out.print("Enter account number: ");
        long accountNo = scanner.nextLong();

        scanner.nextLine();

        System.out.print("Enter account type (SAVINGS/CURRENT): ");
        String accountType = scanner.nextLine();

        System.out.print("Enter initial deposit: ");
        BigDecimal balance = scanner.nextBigDecimal();

        scanner.nextLine();

        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            System.out.println("Initial deposit cannot be negative.");
            return;
        }

        String sql = """
                INSERT INTO account
                (account_no, customer_id, account_type, balance)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setLong(1, accountNo);
            pstmt.setInt(2, customerId);
            pstmt.setString(3, accountType);
            pstmt.setBigDecimal(4, balance);

            int rows = pstmt.executeUpdate();

            if (rows > 0) {

                System.out.println("Account created successfully!");

                if (balance.compareTo(BigDecimal.ZERO) > 0) {

                    recordTransaction(
                            accountNo,
                            null,
                            "DEPOSIT",
                            balance
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("Error creating account.");
            System.out.println(e.getMessage());
        }
    }


    // =========================================================
    // DEPOSIT
    // =========================================================

    public static void deposit() {

        System.out.println("\n===== DEPOSIT =====");

        System.out.print("Enter account number: ");
        long accountNo = scanner.nextLong();

        System.out.print("Enter amount: ");
        BigDecimal amount = scanner.nextBigDecimal();

        scanner.nextLine();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Amount must be greater than zero.");
            return;
        }

        String checkSql =
                "SELECT balance FROM account WHERE account_no = ?";

        String updateSql =
                "UPDATE account SET balance = balance + ? WHERE account_no = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement checkStmt =
                     con.prepareStatement(checkSql);
             PreparedStatement updateStmt =
                     con.prepareStatement(updateSql)) {

            // Check account
            checkStmt.setLong(1, accountNo);

            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("Account not found.");
                return;
            }

            // Update balance
            updateStmt.setBigDecimal(1, amount);
            updateStmt.setLong(2, accountNo);

            updateStmt.executeUpdate();

            // Record transaction
            recordTransaction(
                    accountNo,
                    null,
                    "DEPOSIT",
                    amount
            );

            System.out.println("Deposit successful!");

            showBalance(accountNo);

        } catch (SQLException e) {
            System.out.println("Deposit failed.");
            System.out.println(e.getMessage());
        }
    }


    // =========================================================
    // WITHDRAW
    // =========================================================

    public static void withdraw() {

        System.out.println("\n===== WITHDRAW =====");

        System.out.print("Enter account number: ");
        long accountNo = scanner.nextLong();

        System.out.print("Enter amount: ");
        BigDecimal amount = scanner.nextBigDecimal();

        scanner.nextLine();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Amount must be greater than zero.");
            return;
        }

        String selectSql =
                "SELECT balance FROM account WHERE account_no = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement selectStmt =
                     con.prepareStatement(selectSql)) {

            selectStmt.setLong(1, accountNo);

            ResultSet rs = selectStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("Account not found.");
                return;
            }

            BigDecimal balance =
                    rs.getBigDecimal("balance");

            // Check balance
            if (balance.compareTo(amount) < 0) {

                System.out.println("Insufficient balance.");
                System.out.println(
                        "Available balance: " + balance
                );

                return;
            }

            BigDecimal newBalance =
                    balance.subtract(amount);

            String updateSql =
                    "UPDATE account SET balance = ? WHERE account_no = ?";

            try (PreparedStatement updateStmt =
                         con.prepareStatement(updateSql)) {

                updateStmt.setBigDecimal(1, newBalance);
                updateStmt.setLong(2, accountNo);

                updateStmt.executeUpdate();
            }

            // Record transaction
            recordTransaction(
                    accountNo,
                    null,
                    "WITHDRAW",
                    amount
            );

            System.out.println("Withdrawal successful!");

            showBalance(accountNo);

        } catch (SQLException e) {

            System.out.println("Withdrawal failed.");
            System.out.println(e.getMessage());
        }
    }


    // =========================================================
    // TRANSFER MONEY
    // =========================================================

    public static void transfer() {

        System.out.println("\n===== FUND TRANSFER =====");

        System.out.print("Enter sender account number: ");
        long fromAccount = scanner.nextLong();

        System.out.print("Enter receiver account number: ");
        long toAccount = scanner.nextLong();

        System.out.print("Enter amount: ");
        BigDecimal amount = scanner.nextBigDecimal();

        scanner.nextLine();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Amount must be greater than zero.");
            return;
        }

        if (fromAccount == toAccount) {
            System.out.println(
                    "Sender and receiver cannot be the same."
            );
            return;
        }

        String balanceSql =
                "SELECT balance FROM account WHERE account_no = ?";

        String withdrawSql =
                "UPDATE account SET balance = balance - ? " +
                        "WHERE account_no = ?";

        String depositSql =
                "UPDATE account SET balance = balance + ? " +
                        "WHERE account_no = ?";

        String transactionSql = """
                INSERT INTO transaction_history
                (account_no, to_account_no, transaction_type, amount)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection con = DBConnection.getConnection()) {

            // Start transaction
            con.setAutoCommit(false);

            try {

                BigDecimal senderBalance;

                // -----------------------------------------
                // CHECK SENDER
                // -----------------------------------------

                try (PreparedStatement pstmt =
                             con.prepareStatement(balanceSql)) {

                    pstmt.setLong(1, fromAccount);

                    ResultSet rs = pstmt.executeQuery();

                    if (!rs.next()) {

                        System.out.println(
                                "Sender account not found."
                        );

                        con.rollback();
                        return;
                    }

                    senderBalance =
                            rs.getBigDecimal("balance");
                }


                // -----------------------------------------
                // CHECK RECEIVER
                // -----------------------------------------

                try (PreparedStatement pstmt =
                             con.prepareStatement(balanceSql)) {

                    pstmt.setLong(1, toAccount);

                    ResultSet rs = pstmt.executeQuery();

                    if (!rs.next()) {

                        System.out.println(
                                "Receiver account not found."
                        );

                        con.rollback();
                        return;
                    }
                }


                // -----------------------------------------
                // CHECK BALANCE
                // -----------------------------------------

                if (senderBalance.compareTo(amount) < 0) {

                    System.out.println(
                            "Insufficient balance."
                    );

                    System.out.println(
                            "Available balance: "
                                    + senderBalance
                    );

                    con.rollback();
                    return;
                }


                // -----------------------------------------
                // DEDUCT FROM SENDER
                // -----------------------------------------

                try (PreparedStatement pstmt =
                             con.prepareStatement(withdrawSql)) {

                    pstmt.setBigDecimal(1, amount);
                    pstmt.setLong(2, fromAccount);

                    pstmt.executeUpdate();
                }


                // -----------------------------------------
                // ADD TO RECEIVER
                // -----------------------------------------

                try (PreparedStatement pstmt =
                             con.prepareStatement(depositSql)) {

                    pstmt.setBigDecimal(1, amount);
                    pstmt.setLong(2, toAccount);

                    pstmt.executeUpdate();
                }


                // -----------------------------------------
                // RECORD TRANSFER
                // -----------------------------------------

                try (PreparedStatement pstmt =
                             con.prepareStatement(transactionSql)) {

                    pstmt.setLong(1, fromAccount);
                    pstmt.setLong(2, toAccount);
                    pstmt.setString(3, "TRANSFER");
                    pstmt.setBigDecimal(4, amount);

                    pstmt.executeUpdate();
                }


                // Everything successful
                con.commit();

                System.out.println(
                        "Transfer successful!"
                );

                System.out.println(
                        "Transferred amount: " + amount
                );

                showBalance(fromAccount);

            } catch (SQLException e) {

                // Undo everything if something fails
                con.rollback();

                System.out.println(
                        "Transfer failed."
                );

                System.out.println(
                        "Transaction rolled back."
                );

                System.out.println(e.getMessage());

            } finally {

                con.setAutoCommit(true);
            }

        } catch (SQLException e) {

            System.out.println(
                    "Database connection error."
            );

            System.out.println(e.getMessage());
        }
    }


    // =========================================================
    // CHECK BALANCE
    // =========================================================

    public static void checkBalance() {

        System.out.println("\n===== CHECK BALANCE =====");

        System.out.print("Enter account number: ");
        long accountNo = scanner.nextLong();

        scanner.nextLine();

        showBalance(accountNo);
    }


    // =========================================================
    // SHOW BALANCE
    // =========================================================

    public static void showBalance(long accountNo) {

        String sql = """
                SELECT account_no, account_type, balance
                FROM account
                WHERE account_no = ?
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt =
                     con.prepareStatement(sql)) {

            pstmt.setLong(1, accountNo);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {

                System.out.println(
                        "\n-----------------------------"
                );

                System.out.println(
                        "Account Number: "
                                + rs.getLong("account_no")
                );

                System.out.println(
                        "Account Type: "
                                + rs.getString("account_type")
                );

                System.out.println(
                        "Balance: "
                                + rs.getBigDecimal("balance")
                );

                System.out.println(
                        "-----------------------------"
                );

            } else {

                System.out.println(
                        "Account not found."
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Unable to retrieve balance."
            );

            System.out.println(e.getMessage());
        }
    }


    // =========================================================
    // RECORD TRANSACTION
    // =========================================================

    public static void recordTransaction(
            long accountNo,
            Long toAccountNo,
            String transactionType,
            BigDecimal amount) {

        String sql = """
                INSERT INTO transaction_history
                (account_no, to_account_no, transaction_type, amount)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt =
                     con.prepareStatement(sql)) {

            pstmt.setLong(1, accountNo);

            if (toAccountNo == null) {

                pstmt.setNull(
                        2,
                        Types.BIGINT
                );

            } else {

                pstmt.setLong(
                        2,
                        toAccountNo
                );
            }

            pstmt.setString(
                    3,
                    transactionType
            );

            pstmt.setBigDecimal(
                    4,
                    amount
            );

            pstmt.executeUpdate();

        } catch (SQLException e) {

            System.out.println(
                    "Unable to record transaction."
            );

            System.out.println(e.getMessage());
        }
    }


    // =========================================================
    // TRANSACTION HISTORY
    // =========================================================

    public static void transactionHistory() {

        System.out.println(
                "\n===== TRANSACTION HISTORY ====="
        );

        System.out.print("Enter account number: ");
        long accountNo = scanner.nextLong();

        scanner.nextLine();

        String sql = """
                SELECT transaction_id,
                       account_no,
                       to_account_no,
                       transaction_type,
                       amount,
                       transaction_date
                FROM transaction_history
                WHERE account_no = ?
                ORDER BY transaction_date DESC
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt =
                     con.prepareStatement(sql)) {

            pstmt.setLong(1, accountNo);

            ResultSet rs = pstmt.executeQuery();

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.println(
                        "\n-----------------------------"
                );

                System.out.println(
                        "Transaction ID: "
                                + rs.getInt("transaction_id")
                );

                System.out.println(
                        "Type: "
                                + rs.getString("transaction_type")
                );

                System.out.println(
                        "Amount: "
                                + rs.getBigDecimal("amount")
                );

                Long toAccount =
                        rs.getObject(
                                "to_account_no",
                                Long.class
                        );

                if (toAccount != null) {

                    System.out.println(
                            "To Account: "
                                    + toAccount
                    );
                }

                System.out.println(
                        "Date: "
                                + rs.getTimestamp(
                                "transaction_date"
                        )
                );
            }

            if (!found) {

                System.out.println(
                        "No transactions found."
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Unable to retrieve transaction history."
            );

            System.out.println(e.getMessage());
        }
    }


    // =========================================================
    // SHOW ALL CUSTOMERS
    // =========================================================

    public static void showCustomers() {

        System.out.println("\n===== ALL CUSTOMERS =====");

        String sql =
                "SELECT * FROM customer";

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.println(
                        "\n-----------------------------"
                );

                System.out.println(
                        "Customer ID: "
                                + rs.getInt("customer_id")
                );

                System.out.println(
                        "Name: "
                                + rs.getString("customer_name")
                );

                System.out.println(
                        "Phone: "
                                + rs.getString("phone")
                );

                System.out.println(
                        "Email: "
                                + rs.getString("email")
                );

                System.out.println(
                        "Address: "
                                + rs.getString("address")
                );
            }

            if (!found) {

                System.out.println(
                        "No customers found."
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Unable to retrieve customers."
            );

            System.out.println(e.getMessage());
        }
    }


    // =========================================================
    // SHOW ALL ACCOUNTS
    // =========================================================

    public static void showAccounts() {

        System.out.println("\n===== ALL ACCOUNTS =====");

        String sql = """
                SELECT account_no,
                       customer_id,
                       account_type,
                       balance
                FROM account
                """;

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.println(
                        "\n-----------------------------"
                );

                System.out.println(
                        "Account Number: "
                                + rs.getLong("account_no")
                );

                System.out.println(
                        "Customer ID: "
                                + rs.getInt("customer_id")
                );

                System.out.println(
                        "Account Type: "
                                + rs.getString("account_type")
                );

                System.out.println(
                        "Balance: "
                                + rs.getBigDecimal("balance")
                );
            }

            if (!found) {

                System.out.println(
                        "No accounts found."
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Unable to retrieve accounts."
            );

            System.out.println(e.getMessage());
        }
    }


    // =========================================================
    // MAIN MENU
    // =========================================================

    public static void main(String[] args) {

        while (true) {

            System.out.println(
                    "\n======================================"
            );

            System.out.println(
                    "        BANK MANAGEMENT SYSTEM"
            );

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "1. Create Customer"
            );

            System.out.println(
                    "2. Create Account"
            );

            System.out.println(
                    "3. Deposit"
            );

            System.out.println(
                    "4. Withdraw"
            );

            System.out.println(
                    "5. Transfer Money"
            );

            System.out.println(
                    "6. Check Balance"
            );

            System.out.println(
                    "7. Transaction History"
            );

            System.out.println(
                    "8. Show All Customers"
            );

            System.out.println(
                    "9. Show All Accounts"
            );

            System.out.println(
                    "10. Exit"
            );

            System.out.println(
                    "======================================"
            );

            System.out.print(
                    "Enter your choice: "
            );

            try {

                int choice = scanner.nextInt();

                scanner.nextLine();

                switch (choice) {

                    case 1:
                        createCustomer();
                        break;

                    case 2:
                        createAccount();
                        break;

                    case 3:
                        deposit();
                        break;

                    case 4:
                        withdraw();
                        break;

                    case 5:
                        transfer();
                        break;

                    case 6:
                        checkBalance();
                        break;

                    case 7:
                        transactionHistory();
                        break;

                    case 8:
                        showCustomers();
                        break;

                    case 9:
                        showAccounts();
                        break;

                    case 10:

                        System.out.println(
                                "Thank you for using Bank Management System!"
                        );

                        scanner.close();

                        return;

                    default:

                        System.out.println(
                                "Invalid choice. Please try again."
                        );
                }

            } catch (Exception e) {

                System.out.println(
                        "Invalid input. Please enter a valid value."
                );

                scanner.nextLine();
            }
        }
    }
}