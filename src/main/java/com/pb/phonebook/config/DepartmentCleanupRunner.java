package com.pb.phonebook.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Migrasi SATU KALI PAKAI: menghapus kolom users.department_id dan tabel departments,
 * karena fitur Department sudah dihapus dari aplikasi. Hibernate ddl-auto=update tidak
 * pernah men-drop kolom/tabel yang sudah tidak dipakai lagi, jadi harus dibersihkan manual.
 *
 * CARA PAKAI:
 * 1. BACKUP DATABASE dulu (mysqldump phonebook > backup.sql).
 * 2. Jalankan aplikasi SEKALI dengan property: app.remove-department=true
 * 3. Cek log konsol.
 * 4. Kembalikan app.remove-department=false setelah selesai.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepartmentCleanupRunner implements CommandLineRunner {

    private final DataSource dataSource;

    @Value("${app.remove-department:false}")
    private boolean removeDepartment;

    @Override
    public void run(String... args) throws Exception {
        if (!removeDepartment) {
            return;
        }

        log.info("=== [CLEANUP DEPARTMENT] Mulai bersihkan sisa tabel/kolom department ===");

        if (columnExists("users", "department_id")) {
            String fkName = findForeignKeyName("users", "department_id");
            if (fkName != null) {
                try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                    st.execute("ALTER TABLE users DROP FOREIGN KEY " + fkName);
                    log.info("[CLEANUP DEPARTMENT] Foreign key '{}' dihapus.", fkName);
                }
            }

            try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE users DROP COLUMN department_id");
                log.info("[CLEANUP DEPARTMENT] Kolom users.department_id dihapus.");
            }
        } else {
            log.info("[CLEANUP DEPARTMENT] Kolom users.department_id sudah tidak ada, dilewati.");
        }

        if (tableExists("departments")) {
            try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                st.execute("DROP TABLE departments");
                log.info("[CLEANUP DEPARTMENT] Tabel departments dihapus.");
            }
        } else {
            log.info("[CLEANUP DEPARTMENT] Tabel departments sudah tidak ada, dilewati.");
        }

        log.info("=== [CLEANUP DEPARTMENT] Selesai. Set app.remove-department=false setelah ini. ===");
    }

    private boolean columnExists(String table, String column) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, column)) {
                return rs.next();
            }
        }
    }

    private boolean tableExists(String table) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, table, null)) {
                return rs.next();
            }
        }
    }

    private String findForeignKeyName(String table, String column) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getImportedKeys(conn.getCatalog(), null, table)) {
                while (rs.next()) {
                    if (column.equalsIgnoreCase(rs.getString("FKCOLUMN_NAME"))) {
                        return rs.getString("FK_NAME");
                    }
                }
            }
        }
        return null;
    }
}
