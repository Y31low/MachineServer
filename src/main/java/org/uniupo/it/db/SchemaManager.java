package org.uniupo.it.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaManager {

    private static final String CHECK_SCHEMA_EXISTS = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'machine_%s_%s'";
    private static final String CREATE_SCHEMA = "CREATE SCHEMA IF NOT EXISTS machine_%s_%s";

    private static final String CREATE_CONSUMABLE_TYPE =
            "DO $$ BEGIN " +
                    "    CREATE TYPE machine_%s_%s.\"ConsumableType\" AS ENUM " +
                    "    ('MILK', 'CHOCOLATE', 'SUGAR', 'CUP', 'SPOON', 'TEA', 'COFFEE'); " +
                    "EXCEPTION " +
                    "    WHEN duplicate_object THEN null; " +
                    "END $$;";

    private static final String CREATE_FAULT_TYPE =
            "DO $$ BEGIN " +
                    "    CREATE TYPE machine_%s_%s.fault_type AS ENUM " +
                    "    ('CONSUMABILE_TERMINATO', 'CASSA_PIENA', 'GUASTO_GENERICO'); " +
                    "EXCEPTION " +
                    "    WHEN duplicate_object THEN null; " +
                    "END $$;";

    private static final String[] TABLE_CREATION = {

            "CREATE TABLE IF NOT EXISTS machine_%s_%s.\"Drink\" (" +
                    "    code text NOT NULL," +
                    "    price numeric NOT NULL," +
                    "    description text NOT NULL," +
                    "    name text NOT NULL," +
                    "    CONSTRAINT \"Drink_pkey\" PRIMARY KEY (code)" +
                    ")",

            "CREATE TABLE IF NOT EXISTS machine_%s_%s.\"Fault\" (" +
                    "    description text NOT NULL," +
                    "    id_fault uuid NOT NULL," +
                    "    \"timestamp\" timestamp without time zone NOT NULL," +
                    "    fault_type machine_%s_%s.fault_type NOT NULL," +
                    "    risolto boolean DEFAULT false," +
                    "    CONSTRAINT \"Fault_pkey\" PRIMARY KEY (id_fault)" +
                    ")",

            "CREATE TABLE IF NOT EXISTS machine_%s_%s.\"Machine\" (" +
                    "    \"totalBalance\" numeric NOT NULL DEFAULT 0," +
                    "    \"maxBalance\" numeric NOT NULL DEFAULT 200," +
                    "    \"faultStatus\" boolean NOT NULL DEFAULT false," +
                    "    \"totalCredit\" numeric DEFAULT 0" +
                    ")",

            "CREATE TABLE IF NOT EXISTS machine_%s_%s.consumable (" +
                    "    name machine_%s_%s.\"ConsumableType\" NOT NULL," +
                    "    quantity integer NOT NULL," +
                    "    \"maxQuantity\" integer NOT NULL," +
                    "    CONSTRAINT consumable_pkey PRIMARY KEY (name)," +
                    "    CONSTRAINT consumable_name_name1_key UNIQUE (name)" +
                    ")",

            "CREATE TABLE IF NOT EXISTS machine_%s_%s.\"Recipe\" (" +
                    "    \"drinkCode\" text NOT NULL," +
                    "    \"consumableName\" machine_%s_%s.\"ConsumableType\" NOT NULL," +
                    "    \"consumableQuantity\" integer NOT NULL," +
                    "    CONSTRAINT \"Recipe_pkey\" PRIMARY KEY (\"drinkCode\", \"consumableName\")," +
                    "    CONSTRAINT \"Recipe_drinkCode_fkey\" FOREIGN KEY (\"drinkCode\")" +
                    "        REFERENCES machine_%s_%s.\"Drink\" (code) ON DELETE CASCADE ON UPDATE CASCADE," +
                    "    CONSTRAINT \"Recipe_consumableName_fkey\" FOREIGN KEY (\"consumableName\")" +
                    "        REFERENCES machine_%s_%s.consumable (name) ON DELETE CASCADE ON UPDATE CASCADE" +
                    ")",

            "CREATE TABLE IF NOT EXISTS machine_%s_%s.\"Transaction\" (" +
                    "    \"transactionId\" serial NOT NULL," +
                    "    \"timeStamp\" timestamp without time zone NOT NULL," +
                    "    \"drinkCode\" text NOT NULL," +
                    "    \"sugarQuantity\" integer NOT NULL," +
                    "    CONSTRAINT \"Transaction_pkey\" PRIMARY KEY (\"transactionId\")" +
                    ")"
    };

    private static final String[][] INITIAL_CONSUMABLES = {
            {"COFFEE", "100", "100"},
            {"MILK", "100", "100"},
            {"CHOCOLATE", "100", "100"},
            {"SUGAR", "1000", "1000"},
            {"CUP", "100", "100"},
            {"SPOON", "100", "100"},
            {"TEA", "100", "100"}
    };

    private static final String[][] INITIAL_DRINKS = {
            {"ESPRESSO", "Classic espresso coffee", "1.0", "1"},
            {"CAPPUCCINO", "Cappuccino with milk", "1.5", "2"},
            {"MOCHA", "Mocha with chocolate", "2.0", "3"},
            {"TEA", "Hot tea", "1.0", "4"},
            {"DOUBLE_ESPRESSO", "Double shot espresso", "1.8", "5"},
            {"HOT_CHOCOLATE", "Rich hot chocolate", "2.0", "6"},
            {"COFFEE_WITH_MILK", "Coffee with extra milk", "1.5", "7"},
            {"MOCACCINO", "Coffee with milk and extra chocolate", "2.2", "8"}
    };

    private static final String[][] INITIAL_RECIPES = {
            {"1", "COFFEE", "1"},
            {"2", "COFFEE", "1"},
            {"2", "MILK", "1"},
            {"3", "COFFEE", "1"},
            {"3", "MILK", "1"},
            {"3", "CHOCOLATE", "1"},
            {"4", "TEA", "1"},
            {"5", "COFFEE", "2"},
            {"6", "CHOCOLATE", "2"},
            {"6", "MILK", "1"},
            {"7", "COFFEE", "1"},
            {"7", "MILK", "2"},
            {"8", "COFFEE", "1"},
            {"8", "MILK", "1"},
            {"8", "CHOCOLATE", "2"}
    };


    public static void createSchemaForMachine(String instituteId, String machineId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(String.format(CHECK_SCHEMA_EXISTS, instituteId, machineId));
                if (rs.next()) {
                    System.out.println("Schema already exists for machine " + machineId + " at institute " + instituteId);
                    return;
                }
            }

            initializeSchema(conn, instituteId, machineId);

            conn.commit();
            System.out.println("Successfully created and initialized schema for machine " + machineId);

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new RuntimeException("Error creating schema for machine: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void initializeSchema(Connection conn, String instituteId, String machineId) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Create schema
            stmt.execute(String.format(CREATE_SCHEMA, instituteId, machineId));

            // Create custom types
            stmt.execute(String.format(CREATE_CONSUMABLE_TYPE, instituteId, machineId));
            stmt.execute(String.format(CREATE_FAULT_TYPE, instituteId, machineId));

            // Create tables
            for (String tableQuery : TABLE_CREATION) {
                stmt.execute(String.format(tableQuery, instituteId, machineId, instituteId, machineId,
                        instituteId, machineId, instituteId, machineId));
            }
        }

        insertConsumables(conn, instituteId, machineId);


        insertDrinks(conn, instituteId, machineId);


        insertRecipes(conn, instituteId, machineId);


        insertMachineDefaults(conn, instituteId, machineId);
    }

    private static void insertConsumables(Connection conn, String instituteId, String machineId) throws SQLException {
        String insertConsumable = String.format(
                "INSERT INTO machine_%s_%s.consumable (name, quantity, \"maxQuantity\") VALUES (CAST(? AS machine_%s_%s.\"ConsumableType\"), ?, ?)",
                instituteId, machineId, instituteId, machineId
        );
        try (PreparedStatement ps = conn.prepareStatement(insertConsumable)) {
            for (String[] consumable : INITIAL_CONSUMABLES) {
                ps.setString(1, consumable[0]);
                ps.setInt(2, Integer.parseInt(consumable[1]));
                ps.setInt(3, Integer.parseInt(consumable[2]));
                ps.executeUpdate();
            }
        }
    }

    private static void insertDrinks(Connection conn, String instituteId, String machineId) throws SQLException {
        String insertDrink = String.format(
                "INSERT INTO machine_%s_%s.\"Drink\" (name, description, price, code) VALUES (?, ?, ?, ?)",
                instituteId, machineId
        );
        try (PreparedStatement ps = conn.prepareStatement(insertDrink)) {
            for (String[] drink : INITIAL_DRINKS) {
                ps.setString(1, drink[0]);
                ps.setString(2, drink[1]);
                ps.setBigDecimal(3, new java.math.BigDecimal(drink[2]));
                ps.setString(4, drink[3]);
                ps.executeUpdate();
            }
        }
    }

    private static void insertRecipes(Connection conn, String instituteId, String machineId) throws SQLException {
        String insertRecipe = String.format(
                "INSERT INTO machine_%s_%s.\"Recipe\" (\"drinkCode\", \"consumableName\", \"consumableQuantity\") " +
                        "VALUES (?, CAST(? AS machine_%s_%s.\"ConsumableType\"), ?)",
                instituteId, machineId, instituteId, machineId
        );
        try (PreparedStatement ps = conn.prepareStatement(insertRecipe)) {
            for (String[] recipe : INITIAL_RECIPES) {
                ps.setString(1, recipe[0]);
                ps.setString(2, recipe[1]);
                ps.setInt(3, Integer.parseInt(recipe[2]));
                ps.executeUpdate();
            }
        }
    }

    private static void insertMachineDefaults(Connection conn, String instituteId, String machineId) throws SQLException {
        String insertMachine = String.format(
                "INSERT INTO machine_%s_%s.\"Machine\" (\"totalBalance\", \"maxBalance\", \"faultStatus\", \"totalCredit\") " +
                        "VALUES (0, 200, false, 0) " +
                        "ON CONFLICT DO NOTHING",
                instituteId, machineId
        );
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(insertMachine);
        }
    }
}