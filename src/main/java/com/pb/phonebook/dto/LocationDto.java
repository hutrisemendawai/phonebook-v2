package com.pb.phonebook.dto;

import com.pb.phonebook.entity.Location;

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
public class LocationDto {
    private Long id;
    private String name;
    private String region;
    private String subregion;
    private String extCode;

    public static LocationDto fromEntity(Location location) {
        LocationDto dto = new LocationDto();
        dto.setId(location.getId());
        dto.setName(location.getName());
        dto.setRegion(location.getRegion());
        dto.setSubregion(location.getSubregion());
        dto.setExtCode(location.getExtCode());
        
        return dto;
    }

    
}
