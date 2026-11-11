package com.pb.phonebook.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.pb.phonebook.dto.ExtensionDto;
import com.pb.phonebook.dto.ImportResultDto;
import com.pb.phonebook.dto.UserDto;
import com.pb.phonebook.entity.Company;
import com.pb.phonebook.entity.Location;
import com.pb.phonebook.entity.User;
import com.pb.phonebook.repository.CompanyRepository;
import com.pb.phonebook.repository.ExtensionRepository;
import com.pb.phonebook.repository.LocationRepository;
import com.pb.phonebook.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserImportService {

    private static final String[] HEADERS = {
            "Name", "Email", "Company", "Location", "Subregion", "Extension Type", "Extension Number"
    };

    private static final List<String> DEFAULT_EXTENSION_TYPES = List.of("IPPBX", "Cisco", "PABX", "Zoom");

    private final UserService userService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final LocationRepository locationRepository;
    private final ExtensionRepository extensionRepository;

    private record RawRow(int rowNumber, String name, String email, String company,
            String location, String subregion,
            String extType, String extNumber) {
    }

    // ============ TEMPLATE ============

    public byte[] generateTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Users");

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            var headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }

            String[][] example = {
                    { "Contoh Nama", "contoh.nama@cp.co.id", "CHAROEN POKPHAND INDONESIA", "CPI Ancol", "Ancol",
                            "IPPBX", "1099" },
                    { "Contoh Nama", "contoh.nama@cp.co.id", "CHAROEN POKPHAND INDONESIA", "CPI Ancol", "Ancol",
                            "Cisco", "299" }
            };
            for (int r = 0; r < example.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < example[r].length; c++) {
                    row.createCell(c).setCellValue(example[r][c]);
                }
            }

            List<String> knownTypes = extensionRepository.findDistinctTypes();
            if (knownTypes.isEmpty()) {
                knownTypes = DEFAULT_EXTENSION_TYPES;
            }
            if (!knownTypes.isEmpty()) {
                DataValidationHelper dvHelper = sheet.getDataValidationHelper();
                DataValidationConstraint constraint = dvHelper.createExplicitListConstraint(
                        knownTypes.toArray(new String[0]));
                CellRangeAddressList addressList = new CellRangeAddressList(1, 500, 5, 5);
                DataValidation validation = dvHelper.createValidation(constraint, addressList);
                validation.setSuppressDropDownArrow(true);
                validation.setShowErrorBox(false);
                sheet.addValidationData(validation);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ============ IMPORT ============

    public ImportResultDto importFromExcel(InputStream excelStream) throws IOException {
        ImportResultDto result = new ImportResultDto();
        List<RawRow> rawRows = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(excelStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null)
                    continue;

                String name = cellText(row, 0);
                String email = cellText(row, 1);
                String company = cellText(row, 2);
                String location = cellText(row, 3);
                String subregion = cellText(row, 4);
                String extType = cellText(row, 5);
                String extNumber = cellText(row, 6);

                boolean rowIsBlank = isBlank(name) && isBlank(email) && isBlank(company)
                        && isBlank(location) && isBlank(extType) && isBlank(extNumber);
                if (rowIsBlank)
                    continue;

                rawRows.add(new RawRow(r + 1, name, email, company, location, subregion, extType, extNumber));
            }
        }

        // Kelompokkan baris berdasarkan kombinasi 4 kriteria:
        // Nama + Company + Location + Subregion
        Map<String, List<RawRow>> groups = new LinkedHashMap<>();
        for (RawRow row : rawRows) {
            if (isBlank(row.name())) {
                result.addError(row.rowNumber(), "Name wajib diisi, baris dilewati.");
                continue;
            }

            String groupKey = String.format("%s|%s|%s|%s",
                    row.name().trim().toLowerCase(),
                    row.company() != null ? row.company().trim().toLowerCase() : "",
                    row.location() != null ? row.location().trim().toLowerCase() : "",
                    row.subregion() != null ? row.subregion().trim().toLowerCase() : "");

            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(row);
        }

        for (List<RawRow> group : groups.values()) {
            processGroup(group, result);
        }

        return result;
    }

    private void processGroup(List<RawRow> group, ImportResultDto result) {
        RawRow first = group.get(0);
        String name = first.name().trim();
        String email = isBlank(first.email()) ? null : first.email().trim();

        Long companyId = null;
        Long locationId = null;
        Company companyEntity = null;
        Location locationEntity = null;

        for (RawRow row : group) {
            if (companyId == null && !isBlank(row.company())) {
                companyId = resolveCompany(row, result);
                if (companyId != null) {
                    companyEntity = companyRepository.findById(companyId).orElse(null);
                }
            }
            if (locationId == null && !isBlank(row.location())) {
                locationId = resolveLocation(row, result);
                if (locationId != null) {
                    locationEntity = locationRepository.findById(locationId).orElse(null);
                }
            }
        }

        // Kumpulkan extension unik (type + number) dari baris-baris dalam grup ini
        Set<String> seen = new LinkedHashSet<>();
        List<ExtensionDto> newExtensions = new ArrayList<>();
        for (RawRow row : group) {
            if (isBlank(row.extType()) || isBlank(row.extNumber())) {
                continue;
            }
            String key = row.extType().trim().toLowerCase() + "|" + row.extNumber().trim();
            if (seen.add(key)) {
                newExtensions.add(ExtensionDto.builder()
                        .type(row.extType().trim())
                        .number(row.extNumber().trim())
                        .build());
            }
        }

        // Cari user eksis di database berdasarkan 4 kombinasi kolom
        User existing = null;
        if (companyEntity != null && locationEntity != null && locationEntity.getSubregion() != null) {
            existing = userRepository.findExactDuplicate(
                    name,
                    companyEntity.getName(),
                    locationEntity.getName(),
                    locationEntity.getSubregion()).orElse(null);
        }

        if (existing != null) {
            // Jika 4 kriteria sama persis: perbarui dan merge extension
            UserDto dto = UserDto.builder()
                    .name(name)
                    .email(email != null ? email : existing.getEmail())
                    .companyId(companyId != null ? companyId
                            : (existing.getCompany() != null ? existing.getCompany().getId() : null))
                    .locationId(locationId != null ? locationId
                            : (existing.getLocation() != null ? existing.getLocation().getId() : null))
                    .build();

            List<ExtensionDto> merged = new ArrayList<>();
            Set<String> existingKeys = new LinkedHashSet<>();
            for (var ext : existing.getExtensions()) {
                merged.add(ExtensionDto.builder().type(ext.getType()).number(ext.getNumber()).build());
                existingKeys.add(ext.getType().trim().toLowerCase() + "|" + ext.getNumber().trim());
            }
            for (ExtensionDto ext : newExtensions) {
                String key = ext.getType().trim().toLowerCase() + "|" + ext.getNumber().trim();
                if (existingKeys.add(key)) {
                    merged.add(ext);
                }
            }
            dto.setExtensions(merged);

            userService.updateUser(existing.getId(), dto);
            result.incrementUpdated();
        } else {
            // Jika ada salah satu yang berbeda (atau data baru): buat sebagai entri user
            // terpisah
            UserDto dto = UserDto.builder()
                    .name(name)
                    .email(email)
                    .companyId(companyId)
                    .locationId(locationId)
                    .extensions(newExtensions)
                    .build();

            userService.createUser(dto);
            result.incrementCreated();
        }
    }

    private Long resolveCompany(RawRow row, ImportResultDto result) {
        Company c = companyRepository.findByNameIgnoreCase(row.company().trim()).orElse(null);
        if (c == null) {
            result.addError(row.rowNumber(), "Company '" + row.company() + "' tidak ditemukan, dilewati.");
            return null;
        }
        return c.getId();
    }

    private Long resolveLocation(RawRow row, ImportResultDto result) {
        String locName = row.location().trim();

        if (!isBlank(row.subregion())) {
            Location l = locationRepository.findByNameIgnoreCaseAndSubregionIgnoreCase(locName, row.subregion().trim())
                    .orElse(null);
            if (l == null) {
                result.addError(row.rowNumber(),
                        "Location '" + locName + "' di subregion '" + row.subregion() + "' tidak ditemukan, dilewati.");
                return null;
            }
            return l.getId();
        }

        List<Location> matches = locationRepository.findByNameIgnoreCase(locName);
        if (matches.isEmpty()) {
            result.addError(row.rowNumber(), "Location '" + locName + "' tidak ditemukan, dilewati.");
            return null;
        }
        if (matches.size() > 1) {
            result.addError(row.rowNumber(), "Location '" + locName
                    + "' ada lebih dari satu (isi kolom Subregion buat pastiin yang mana), dilewati.");
            return null;
        }
        return matches.get(0).getId();
    }

    private String cellText(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null)
            return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            double val = cell.getNumericCellValue();
            if (val == Math.floor(val)) {
                return String.valueOf((long) val);
            }
            return String.valueOf(val);
        }
        return cell.toString().trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}