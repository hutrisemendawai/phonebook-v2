package com.pb.phonebook.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.pb.phonebook.entity.Extension;
import com.pb.phonebook.entity.User;
import com.pb.phonebook.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Migrasi SATU KALI PAKAI: menggabungkan baris User lama yang di-split manual
 * per jenis extension (misal "Hasan", "Hasan Cisco", "Hasan PABX") menjadi
 * satu baris User dengan beberapa Extension.
 *
 * CARA PAKAI:
 * 1. BACKUP DATABASE dulu (mysqldump phonebook > backup.sql).
 * 2. Jalankan aplikasi SEKALI dengan property: app.migrate-extensions=true
 *    (bisa lewat application.properties, atau:
 *     mvn spring-boot:run -Dspring-boot.run.arguments=--app.migrate-extensions=true)
 * 3. Cek log konsol - setiap penggabungan akan di-log jelas (id mana gabung ke id mana).
 * 4. Kalau sudah beres, matikan lagi (app.migrate-extensions=false / hapus property),
 *    biar runner ini ga jalan ulang tiap start aplikasi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExtensionMigrationRunner implements CommandLineRunner {

    private final DataSource dataSource;
    private final UserRepository userRepository;

    @Value("${app.migrate-extensions:false}")
    private boolean migrateExtensions;

    // Nama yang diakhiri salah satu suffix ini dianggap extension tambahan,
    // contoh: "Hasan Cisco" -> baseName="Hasan", type="Cisco"
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("(?i)^(.*?)\\s+(Cisco|PABX|Zoom)$");

    private record LegacyRow(Long id, String name, String extension, String email) {}
    private record GroupKey(String baseNameLower, String emailLower) {}

    @Override
    public void run(String... args) throws Exception {
        if (!migrateExtensions) {
            return;
        }

        log.info("=== [MIGRASI] Mulai migrasi data extension lama ===");

        if (!columnExists("users", "extension")) {
            log.info("[MIGRASI] Kolom 'extension' lama sudah tidak ada di tabel users. " +
                      "Migrasi dilewati (kemungkinan sudah pernah dijalankan sebelumnya).");
            return;
        }

        List<LegacyRow> legacyRows = readLegacyRows();
        log.info("[MIGRASI] Ditemukan {} baris user lama.", legacyRows.size());

        Map<Long, String> typeById = new LinkedHashMap<>();
        Map<Long, String> baseNameById = new LinkedHashMap<>();
        Map<GroupKey, List<Long>> groups = new LinkedHashMap<>();

        for (LegacyRow row : legacyRows) {
            String type = "IPPBX";
            String baseName = row.name() == null ? "" : row.name().trim();

            Matcher m = SUFFIX_PATTERN.matcher(baseName);
            if (m.matches()) {
                baseName = m.group(1).trim();
                type = normalizeType(m.group(2));
            }

            typeById.put(row.id(), type);
            baseNameById.put(row.id(), baseName);

            String emailKey = row.email() == null ? "" : row.email().trim().toLowerCase();
            GroupKey key = new GroupKey(baseName.toLowerCase(), emailKey);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row.id());
        }

        log.info("[MIGRASI] Dikelompokkan jadi {} orang unik dari {} baris lama.", groups.size(), legacyRows.size());

        for (Map.Entry<GroupKey, List<Long>> entry : groups.entrySet()) {
            List<Long> ids = new ArrayList<>(entry.getValue());
            ids.sort(Long::compareTo);
            Long canonicalId = ids.get(0);

            User canonicalUser = userRepository.findById(canonicalId).orElse(null);
            if (canonicalUser == null) {
                log.warn("[MIGRASI] User id={} tidak ditemukan (mungkin sudah kehapus), dilewati.", canonicalId);
                continue;
            }

            canonicalUser.setName(baseNameById.get(canonicalId));

            for (Long id : ids) {
                String legacyExtValue = legacyRows.stream()
                        .filter(r -> r.id().equals(id))
                        .map(LegacyRow::extension)
                        .findFirst().orElse(null);

                if (legacyExtValue != null && !legacyExtValue.isBlank()) {
                    Extension ext = Extension.builder()
                            .type(typeById.get(id))
                            .number(legacyExtValue.trim())
                            .user(canonicalUser)
                            .build();
                    canonicalUser.getExtensions().add(ext);
                }
            }

            userRepository.save(canonicalUser);

            for (Long id : ids) {
                if (!id.equals(canonicalId)) {
                    userRepository.deleteById(id);
                    log.info("[MIGRASI] User id={} ({}) digabung ke id={} sebagai extension tipe '{}'.",
                            id, baseNameById.get(canonicalId), canonicalId, typeById.get(id));
                }
            }
        }

        dropLegacyExtensionColumn();

        log.info("=== [MIGRASI] Selesai. Total {} orang unik dari {} baris lama. " +
                  "Set app.migrate-extensions=false setelah ini biar ga jalan ulang. ===",
                  groups.size(), legacyRows.size());
    }

    private List<LegacyRow> readLegacyRows() throws SQLException {
        List<LegacyRow> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, extension, email FROM users")) {
            while (rs.next()) {
                rows.add(new LegacyRow(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("extension"),
                        rs.getString("email")
                ));
            }
        }
        return rows;
    }

    private void dropLegacyExtensionColumn() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE users DROP COLUMN extension");
            log.info("[MIGRASI] Kolom 'extension' lama di tabel users sudah dihapus.");
        }
    }

    private String normalizeType(String raw) {
        return switch (raw.toLowerCase()) {
            case "cisco" -> "Cisco";
            case "pabx" -> "PABX";
            case "zoom" -> "Zoom";
            default -> raw;
        };
    }

    private boolean columnExists(String table, String column) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, column)) {
                return rs.next();
            }
        }
    }
}
