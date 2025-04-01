package com.aptio.service.impl;

import com.aptio.dto.ServiceCategoryDTO;
import com.aptio.dto.ServiceDTO;
import com.aptio.exception.ResourceNotFoundException;
import com.aptio.exception.ValidationException;
import com.aptio.mapper.ServiceMapper;
import com.aptio.model.Service;
import com.aptio.model.ServiceCategory;
import com.aptio.repository.ServiceCategoryRepository;
import com.aptio.repository.ServiceRepository;
import com.aptio.service.ServiceEntityService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceEntityServiceImpl implements ServiceEntityService {

    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final ServiceMapper serviceMapper;
    private final ModelMapper modelMapper;
    public List<ServiceDTO> getAllServices() {
        return serviceRepository.findAll().stream()
                .map(serviceMapper::toDTO)
                .collect(Collectors.toList());
    }

    public ServiceDTO getServiceById(String id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));
        return serviceMapper.toDTO(service);
    }

    @Transactional
    public ServiceDTO createService(ServiceDTO serviceDTO) {
        ServiceCategory category = categoryRepository.findByName(serviceDTO.getCategory())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "name", serviceDTO.getCategory()));

        Service service = serviceMapper.toEntity(serviceDTO);
        service.setCategory(category);
        service.setCreatedAt(LocalDateTime.now());
        service.setUpdatedAt(LocalDateTime.now());
        Service savedService = serviceRepository.save(service);

        return serviceMapper.toDTO(savedService);
    }

    @Transactional
    public ServiceDTO updateService(String id, ServiceDTO serviceDTO) {
        Service existingService = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));
        ServiceCategory category = categoryRepository.findByName(serviceDTO.getCategory())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "name", serviceDTO.getCategory()));
        serviceMapper.updateEntityFromDTO(serviceDTO, existingService);
        existingService.setCategory(category);

        Service updatedService = serviceRepository.save(existingService);
        return serviceMapper.toDTO(updatedService);
    }

    @Transactional
    public void deleteService(String id) {
        if (!serviceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Service", "id", id);
        }
        serviceRepository.deleteById(id);
    }

    @Transactional
    public ServiceDTO toggleServiceStatus(String id, boolean active) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", id));

        service.setActive(active);
        Service updatedService = serviceRepository.save(service);

        return serviceMapper.toDTO(updatedService);
    }

    public List<ServiceDTO> getServicesByCategory(String categoryName) {
        return serviceRepository.findByCategoryName(categoryName).stream()
                .map(serviceMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ServiceDTO> searchServices(String query) {
        return serviceRepository.searchServices(query).stream()
                .map(serviceMapper::toDTO)
                .collect(Collectors.toList());
    }
    public List<ServiceCategoryDTO> getAllCategories() {
        List<ServiceCategory> categories = categoryRepository.findAll();
        return categories.stream()
                .map(category -> {
                    ServiceCategoryDTO dto = modelMapper.map(category, ServiceCategoryDTO.class);
                    dto.setServicesCount(category.getServicesCount());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public ServiceCategoryDTO getCategoryById(String id) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        ServiceCategoryDTO dto = modelMapper.map(category, ServiceCategoryDTO.class);
        dto.setServicesCount(category.getServicesCount());
        return dto;
    }

    @Transactional
    public ServiceCategoryDTO createCategory(ServiceCategoryDTO categoryDTO) {
        if (categoryRepository.existsByName(categoryDTO.getName())) {
            throw new ValidationException("Category name already exists");
        }

        ServiceCategory category = modelMapper.map(categoryDTO, ServiceCategory.class);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        ServiceCategory savedCategory = categoryRepository.save(category);

        ServiceCategoryDTO dto = modelMapper.map(savedCategory, ServiceCategoryDTO.class);
        dto.setServicesCount(0);
        return dto;
    }

    @Transactional
    public ServiceCategoryDTO updateCategory(String id, ServiceCategoryDTO categoryDTO) {
        ServiceCategory existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        if (!existingCategory.getName().equals(categoryDTO.getName()) &&
                categoryRepository.existsByName(categoryDTO.getName())) {
            throw new ValidationException("Category name already exists");
        }
        existingCategory.setName(categoryDTO.getName());
        existingCategory.setDescription(categoryDTO.getDescription());
        existingCategory.setActive(categoryDTO.isActive());

        existingCategory.setUpdatedAt(LocalDateTime.now());

        ServiceCategory updatedCategory = categoryRepository.save(existingCategory);

        ServiceCategoryDTO dto = modelMapper.map(updatedCategory, ServiceCategoryDTO.class);
        dto.setServicesCount(updatedCategory.getServicesCount());
        return dto;
    }

    @Transactional
    public void deleteCategory(String id) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        if (category.getServicesCount() > 0) {
            throw new ValidationException("Cannot delete category with services. Reassign or delete those services first.");
        }

        categoryRepository.deleteById(id);
    }

    @Transactional
    public ServiceCategoryDTO toggleCategoryStatus(String id, boolean active) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        category.setActive(active);
        ServiceCategory updatedCategory = categoryRepository.save(category);

        ServiceCategoryDTO dto = modelMapper.map(updatedCategory, ServiceCategoryDTO.class);
        dto.setServicesCount(updatedCategory.getServicesCount());
        return dto;
    }
}