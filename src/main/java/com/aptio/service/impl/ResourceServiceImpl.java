package com.aptio.service.impl;

import com.aptio.dto.ResourceDTO;
import com.aptio.exception.ResourceNotFoundException;
import com.aptio.model.Resource;
import com.aptio.repository.ResourceRepository;
import com.aptio.repository.ScheduleEntryRepository;
import com.aptio.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final ModelMapper modelMapper;

    public List<ResourceDTO> getAllResources() {
        return resourceRepository.findAll().stream()
                .map(resource -> modelMapper.map(resource, ResourceDTO.class))
                .collect(Collectors.toList());
    }

    public List<ResourceDTO> getAvailableResources() {
        return resourceRepository.findByIsAvailable(true).stream()
                .map(resource -> modelMapper.map(resource, ResourceDTO.class))
                .collect(Collectors.toList());
    }

    public List<ResourceDTO> getResourcesByType(String type) {
        return resourceRepository.findByType(type).stream()
                .map(resource -> modelMapper.map(resource, ResourceDTO.class))
                .collect(Collectors.toList());
    }

    public ResourceDTO getResourceById(String id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));
        return modelMapper.map(resource, ResourceDTO.class);
    }

    @Transactional
    public ResourceDTO createResource(ResourceDTO resourceDTO) {
        Resource resource = modelMapper.map(resourceDTO, Resource.class);
        Resource savedResource = resourceRepository.save(resource);
        return modelMapper.map(savedResource, ResourceDTO.class);
    }

    @Transactional
    public ResourceDTO updateResource(String id, ResourceDTO resourceDTO) {
        Resource existingResource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));

        existingResource.setName(resourceDTO.getName());
        existingResource.setType(resourceDTO.getType());
        existingResource.setCapacity(resourceDTO.getCapacity());
        existingResource.setAvailable(resourceDTO.getIsAvailable());
        existingResource.setColor(resourceDTO.getColor());

        Resource updatedResource = resourceRepository.save(existingResource);
        return modelMapper.map(updatedResource, ResourceDTO.class);
    }

    @Transactional
    public void deleteResource(String id) {
        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource", "id", id);
        }
        resourceRepository.deleteById(id);
    }

    @Transactional
    public ResourceDTO toggleResourceAvailability(String id, boolean isAvailable) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));

        resource.setAvailable(isAvailable);
        Resource updatedResource = resourceRepository.save(resource);
        return modelMapper.map(updatedResource, ResourceDTO.class);
    }

    public List<ResourceDTO> getAvailableResourcesForTimeSlot(LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<Resource> allResources = resourceRepository.findByIsAvailable(true);

        return allResources.stream()
                .filter(resource -> {
                    // Check if resource is available at this time
                    return scheduleEntryRepository.findOverlappingEntriesForResource(
                            date, startTime, endTime, resource.getId()).isEmpty();
                })
                .map(resource -> modelMapper.map(resource, ResourceDTO.class))
                .collect(Collectors.toList());
    }
}