package com.pb.phonebook.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    
    private Long id;
    private String name;
    private String email;

    @Builder.Default
    private List<ExtensionDto> extensions = new ArrayList<>();

    private Long companyId;
    private String companyName;

    private Long locationId;
    private String locationName;
    private String locationSubRegion;
    private String locationExtCode;
}

