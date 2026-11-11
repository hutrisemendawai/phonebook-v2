package com.pb.phonebook.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.pb.phonebook.dto.LocationDto;
import com.pb.phonebook.entity.Location;
import com.pb.phonebook.repository.LocationRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService{
    
    private final LocationRepository locationRepository;

    private LocationDto mapToDto(Location location) {
        return LocationDto.builder()
                .id(location.getId())
                .name(location.getName())
                .region(location.getRegion())
                .subregion(location.getSubregion())
                .extCode(location.getExtCode())
                .build();
    }

    private Location mapToEntity(LocationDto dto) {
        return Location.builder()
                .id(dto.getId())
                .name(dto.getName())
                .region(dto.getRegion())
                .subregion(dto.getSubregion())
                .extCode(dto.getExtCode())
                .build();
    }

    @Override
    public List<LocationDto> getAllLocations() {
        return locationRepository.findAll()
                                 .stream()
                                 .map(LocationDto::fromEntity)
                                 .sorted(Comparator.comparing(LocationDto::getName, String.CASE_INSENSITIVE_ORDER))
                                 .collect(Collectors.toList());
    }

    @Override
    public LocationDto getLocationById(Long id) {
        return locationRepository.findById(id)
                .map(this::mapToDto)
                .orElse(null);
    }

    @Override
    public List<LocationDto> getLocationsBySubregion(String subregion) {
        return locationRepository.findBySubregion(subregion)
                                 .stream()
                                 .map(LocationDto::fromEntity)
                                 .sorted(Comparator.comparing(LocationDto::getName, String.CASE_INSENSITIVE_ORDER))
                                 .collect(Collectors.toList());
    }

    @Override
    public LocationDto createLocation(LocationDto locationDto) {
        Location location = mapToEntity(locationDto);
        Location saved = locationRepository.save(location);
        return mapToDto(saved);
    }

    @Override
    public LocationDto updateLocation(Long id, LocationDto locationDto) {
        return locationRepository.findById(id)
                .map(existing -> {
                    existing.setName(locationDto.getName());
                    existing.setRegion(locationDto.getRegion());
                    existing.setSubregion(locationDto.getSubregion());
                    existing.setExtCode(locationDto.getExtCode());
                    return mapToDto(locationRepository.save(existing));
                })
                .orElse(null);
    }

    @Override
    public void deleteLocation(Long id) {
        locationRepository.deleteById(id);
    }

}
