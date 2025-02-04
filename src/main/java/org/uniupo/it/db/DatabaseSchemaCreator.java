package org.uniupo.it.db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseSchemaCreator {
    private static final String URL = "jdbc:postgresql://localhost:5432/your_database_name";
    private static final String USER = "postgres";
    private static final String PASSWORD = "your_password";

    public static void createSchema(String institutionId, String machineId) {
        String schemaName = String.format("machine_%s_%s", institutionId, machineId);

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName + ";");
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(
                        "DO $$ BEGIN " +
                                "    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'consumabletype') THEN " +
                                "        CREATE TYPE " + schemaName + ".\"ConsumableType\" AS ENUM " +
                                "        ('MILK', 'CHOCOLATE', 'SUGAR', 'CUP', 'SPOON', 'TEA', 'COFFEE'); " +
                                "    END IF; " +
                                "END $$;"
                );
            }

            try (Statement stmt = conn.createStatement()) {
                // Replace all 'machine.' references with the dynamic schema name
                String[] tableCreationQueries = {
                        "CREATE TABLE IF NOT EXISTS " + schemaName + ".\"Drink\" (" +
                                "    code text NOT NULL," +
                                "    price numeric NOT NULL," +
                                "    description text NOT NULL," +
                                "    name text NOT NULL," +
                                "    CONSTRAINT \"Drink_pkey\" PRIMARY KEY (code)" +
                                ");",

                        "CREATE TABLE IF NOT EXISTS " + schemaName + ".\"Machine\" (" +
                                "    \"totalBalance\" numeric NOT NULL," +
                                "    \"maxBalance\" numeric NOT NULL," +
                                "    \"faultStatus\" boolean NOT NULL," +
                                "    \"totalCredit\" numeric DEFAULT 0" +
                                ");",

                        "CREATE TABLE IF NOT EXISTS " + schemaName + ".\"Recipe\" (" +
                                "    \"drinkCode\" text NOT NULL," +
                                "    \"consumableName\" " + schemaName + ".\"ConsumableType\" NOT NULL," +
                                "    \"consumableQuantity\" integer NOT NULL," +
                                "    CONSTRAINT \"Recipe_pkey\" PRIMARY KEY (\"drinkCode\", \"consumableName\")" +
                                ");",

                        "CREATE TABLE IF NOT EXISTS " + schemaName + ".\"Transaction\" (" +
                                "    \"transactionId\" serial NOT NULL," +
                                "    \"timeStamp\" timestamp without time zone NOT NULL," +
                                "    \"drinkCode\" text NOT NULL," +
                                "    \"sugarQuantity\" integer NOT NULL," +
                                "    CONSTRAINT \"Transaction_pkey\" PRIMARY KEY (\"transactionId\")" +
                                ");",

                        "CREATE TABLE IF NOT EXISTS " + schemaName + ".consumable (" +
                                "    name " + schemaName + ".\"ConsumableType\" NOT NULL," +
                                "    quantity integer NOT NULL," +
                                "    \"maxQuantity\" integer NOT NULL," +
                                "    CONSTRAINT consumable_pkey PRIMARY KEY (name)," +
                                "    CONSTRAINT consumable_name_name1_key UNIQUE (name)" +
                                ");"
                };

                for (String query : tableCreationQueries) {
                    stmt.execute(query);
                }

                stmt.execute(
                        "ALTER TABLE IF EXISTS " + schemaName + ".\"Recipe\" " +
                                "    ADD CONSTRAINT \"Recipe_consumableName_fkey\" " +
                                "    FOREIGN KEY (\"consumableName\") " +
                                "    REFERENCES " + schemaName + ".consumable (name) " +
                                "    ON UPDATE CASCADE " +
                                "    ON DELETE CASCADE;"
                );

                stmt.execute(
                        "ALTER TABLE IF EXISTS " + schemaName + ".\"Recipe\" " +
                                "    ADD CONSTRAINT \"Recipe_drinkCode_fkey\" " +
                                "    FOREIGN KEY (\"drinkCode\") " +
                                "    REFERENCES " + schemaName + ".\"Drink\" (code) " +
                                "    ON UPDATE CASCADE " +
                                "    ON DELETE CASCADE;"
                );
            }

            System.out.println("Schema " + schemaName + " created successfully!");

        } catch (Exception e) {
            System.err.println("Error creating schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        createSchema("001", "001"); // Example usage
    }
}