package com.pb.phonebook.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class ImportResultDto {

    private int createdCount = 0;
    private int updatedCount = 0;
    private final List<String> errors = new ArrayList<>();

    public void incrementCreated() {
        createdCount++;
    }

    public void incrementUpdated() {
        updatedCount++;
    }

    public void addError(int rowNumber, String message) {
        errors.add("Baris " + rowNumber + ": " + message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
