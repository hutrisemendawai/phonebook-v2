package com.pb.phonebook.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.pb.phonebook.entity.Location;
import com.pb.phonebook.repository.LocationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Migrasi SATU KALI PAKAI: mengganti seluruh isi tabel locations dengan data
 * dari src/main/resources/data/locations.csv (kolom: id,name,region,subregion,ext_code).
 *
 * PENTING: karena nama lokasi di CSV baru ini beda total dari data lama
 * (mis. "Gedung CPI" -> "CPI Ancol"), semua user yang sebelumnya sudah punya Location
 * akan KEHILANGAN assignment lokasinya (di-set kosong) setelah migrasi ini, dan perlu
 * di-assign ulang manual lewat halaman Edit User.
 *
 * CARA PAKAI:
 * 1. BACKUP DATABASE dulu (mysqldump phonebook > backup.sql).
 * 2. Jalankan aplikasi SEKALI dengan property: app.reload-locations=true
 * 3. Cek log konsol untuk ringkasan (berapa lokasi lama dihapus, berapa baru ditambahkan).
 * 4. Kembalikan app.reload-locations=false setelah selesai.
 * 5. Assign ulang Location tiap user yang butuh, lewat halaman Edit User.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationReloadRunner implements CommandLineRunner {

    private final DataSource dataSource;
    private final LocationRepository locationRepository;

    @Value("${app.reload-locations:false}")
    private boolean reloadLocations;

    @Override
    public void run(String... args) throws Exception {
        if (!reloadLocations) {
            return;
        }

        log.info("=== [RELOAD LOCATIONS] Mulai reload data lokasi dari CSV ===");

        long oldCount = locationRepository.count();

        // Lepas dulu semua relasi user -> location, supaya tabel locations bisa
        // dikosongkan tanpa kena foreign key constraint violation.
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            int affected = st.executeUpdate("UPDATE users SET location_id = NULL WHERE location_id IS NOT NULL");
            log.info("[RELOAD LOCATIONS] {} user dilepas dari lokasi lamanya (perlu di-assign ulang manual).", affected);
        }

        locationRepository.deleteAll();
        log.info("[RELOAD LOCATIONS] {} lokasi lama dihapus.", oldCount);

        if (columnExists("locations", "site")) {
            try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE locations DROP COLUMN site");
                log.info("[RELOAD LOCATIONS] Kolom lama locations.site dihapus.");
            }
        }

        List<Location> newLocations = readCsv();
        locationRepository.saveAll(newLocations);

        log.info("=== [RELOAD LOCATIONS] Selesai. {} lokasi baru ditambahkan dari CSV. " +
                  "Set app.reload-locations=false setelah ini, lalu assign ulang lokasi tiap user lewat Edit User. ===",
                  newLocations.size());
    }

    private boolean columnExists(String table, String column) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, column)) {
                return rs.next();
            }
        }
    }

    private List<Location> readCsv() throws Exception {
        List<Location> locations = new ArrayList<>();

        try (InputStream is = new ClassPathResource("data/locations.csv").getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String header = reader.readLine(); // lewati baris header: id,name,region,subregion,ext_code
            if (header == null) {
                return locations;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] cols = line.split(",", -1);
                if (cols.length < 5) {
                    log.warn("[RELOAD LOCATIONS] Baris CSV dilewati (kolom tidak lengkap): {}", line);
                    continue;
                }

                // cols[0] = id dari CSV, sengaja diabaikan - biar ID baru di-generate otomatis oleh DB.
                String name = cols[1].trim();
                String region = cols[2].trim();
                String subregion = cols[3].trim();
                String extCode = cols[4].trim();

                locations.add(Location.builder()
                        .name(name)
                        .region(region)
                        .subregion(subregion)
                        .extCode(extCode)
                        .build());
            }
        }

        return locations;
    }
}
