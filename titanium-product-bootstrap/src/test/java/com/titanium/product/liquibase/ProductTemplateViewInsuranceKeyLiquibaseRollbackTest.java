package com.titanium.product.liquibase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * 产品模板读模型险种键切分变更集测试（G10/g10-02）。
 * <p>
 * 该变更集把遗留扁平列 {@code insurance_type} 更名 {@code legacy_insurance_type}、
 * 新增权威三级键列 {@code insurance_product_type}。更名若写错（丢值、rollback
 * 第二句未执行）在 SQL 文本层面不可见，故在 H2 上验证「更名后旧值保真（含 NULL 行）、
 * 新列就位、回滚可完整还原列形态与数据」。
 * </p>
 */
class ProductTemplateViewInsuranceKeyLiquibaseRollbackTest {

    private static final String CHANGELOG =
            "liquibase/ddl/product_template_view_insurance_key_202610091849_weisun_ddl.sql";

    @Test
    void shouldRenameLegacyColumnAndRollbackPreservingData() throws Exception {
        String databaseName = "product_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                // 仅保留本变更集涉及的列：最小可用的 t_product_template_view 替身（变更前形态）
                statement.execute("CREATE TABLE t_product_template_view ("
                        + "template_id VARCHAR(64) PRIMARY KEY,"
                        + "insurance_category VARCHAR(32),"
                        + "insurance_type VARCHAR(32) NULL)");
                statement.execute("INSERT INTO t_product_template_view VALUES "
                        + "('TPL-LEGACY-001', 'MAIN', 'LIFE'),"
                        // 遗留列可为空：更名与回滚对 NULL 行不得报错
                        + "('TPL-LEGACY-002', 'MAIN', NULL)");
            }
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    CHANGELOG, new ClassLoaderResourceAccessor(), database);

            liquibase.update(new Contexts(), new LabelExpression());
            assertTrue(columnExists(connection, "insurance_product_type"), "应新增权威三级键列");
            assertTrue(columnExists(connection, "legacy_insurance_type"), "旧列应更名保留");
            assertFalse(columnExists(connection, "insurance_type"), "旧列名不得残留");
            assertEquals("LIFE", valueOf(connection, "legacy_insurance_type", "TPL-LEGACY-001"),
                    "更名须保真旧值");

            liquibase.rollback(1, new Contexts(), new LabelExpression());
            assertFalse(columnExists(connection, "insurance_product_type"), "回滚须删除新增列");
            assertFalse(columnExists(connection, "legacy_insurance_type"), "回滚须撤销更名");
            assertTrue(columnExists(connection, "insurance_type"), "回滚后应恢复原列名");
            assertEquals("LIFE", valueOf(connection, "insurance_type", "TPL-LEGACY-001"),
                    "回滚须保真原值");
        }
    }

    private boolean columnExists(Connection connection, String columnName) throws Exception {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                + " WHERE LOWER(TABLE_NAME) = 't_product_template_view' AND LOWER(COLUMN_NAME) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private String valueOf(Connection connection, String columnName, String templateId) throws Exception {
        String sql = "SELECT " + columnName + " FROM t_product_template_view WHERE template_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, templateId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getString(1);
            }
        }
    }
}
