package com.aptio.service;

import com.aptio.dto.ResourceDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ResourceService {

    List<ResourceDTO> getAllResources();

    List<ResourceDTO> getAvailableResources();

    List<ResourceDTO> getResourcesByType(String type);

    ResourceDTO getResourceById(String id);

    ResourceDTO createResource(ResourceDTO resourceDTO);

    ResourceDTO updateResource(String id, ResourceDTO resourceDTO);

    void deleteResource(String id);

    ResourceDTO toggleResourceAvailability(String id, boolean isAvailable);

    List<ResourceDTO> getAvailableResourcesForTimeSlot(LocalDate date, LocalTime startTime, LocalTime endTime);
}