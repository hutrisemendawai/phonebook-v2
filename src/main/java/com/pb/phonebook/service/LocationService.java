package com.pb.phonebook.service;

import java.util.List;

import com.pb.phonebook.dto.LocationDto;

public interface LocationService {
    LocationDto createLocation(LocationDto locationDto);
    LocationDto getLocationById(Long id);
    List<LocationDto> getAllLocations();
    List<LocationDto> getLocationsBySubregion(String subregion);
    LocationDto updateLocation(Long id, LocationDto locationDto);
    void deleteLocation(Long id);
}
