package com.titanium.product.liquibase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
 * 产品读模型 created_at 回填变更集测试。
 * <p>
 * 该 DML 在服务启动时对存量库执行，写错表名/列名不会报错、只会静默不改任何行 ——
 * 故在 H2 上验证「空值被回填为本行 create_time、已有值不被改写、回滚可撤销」。
 * </p>
 */
class ProductViewCreatedAtBackfillLiquibaseTest {

    private static final String CHANGELOG =
            "liquibase/dml/t_product_view_202609211100_weisun_dml.sql";

    @Test
    void shouldBackfillOnlyNullCreatedAtAndRollback() throws Exception {
        String databaseName = "product_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                // 仅保留本变更集涉及的两列：最小可用的 t_product_view 替身
                statement.execute("CREATE TABLE t_product_view ("
                        + "id VARCHAR(64) PRIMARY KEY, create_time TIMESTAMP, created_at TIMESTAMP)");
                statement.execute("INSERT INTO t_product_view VALUES "
                        // 修订产出的版本行：created_at 为空，应被回填
                        + "('REVISED', TIMESTAMP '2026-09-09 16:39:08', NULL),"
                        // 创建路径写入的行：created_at 已有值，须保持不变
                        + "('CREATED', TIMESTAMP '2026-09-15 15:24:33', TIMESTAMP '2026-09-15 15:24:33')");
            }
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    CHANGELOG, new ClassLoaderResourceAccessor(), database);

            liquibase.update(new Contexts(), new LabelExpression());
            assertEquals(LocalDateTime.of(2026, 9, 9, 16, 39, 8), createdAt(connection, "REVISED"),
                    "空值应回填为本行 create_time");
            assertEquals(LocalDateTime.of(2026, 9, 15, 15, 24, 33), createdAt(connection, "CREATED"),
                    "已有值不得被改写");

            liquibase.rollback(1, new Contexts(), new LabelExpression());
            assertNull(createdAt(connection, "REVISED"), "回滚后应恢复为空");
        }
    }

    private LocalDateTime createdAt(Connection connection, String id) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT created_at FROM t_product_view WHERE id = '" + id + "'")) {
            resultSet.next();
            Timestamp value = resultSet.getTimestamp(1);
            return value == null ? null : value.toLocalDateTime();
        }
    }
}
