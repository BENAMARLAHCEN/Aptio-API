package com.aptio.service;

import com.aptio.dto.ServiceCategoryDTO;
import com.aptio.dto.ServiceDTO;

import java.util.List;

public interface ServiceEntityService {

    // Service methods
    List<ServiceDTO> getAllServices();

    ServiceDTO getServiceById(String id);

    ServiceDTO createService(ServiceDTO serviceDTO);

    ServiceDTO updateService(String id, ServiceDTO serviceDTO);

    void deleteService(String id);

    ServiceDTO toggleServiceStatus(String id, boolean active);

    List<ServiceDTO> getServicesByCategory(String categoryName);

    List<ServiceDTO> searchServices(String query);

    // Category methods
    List<ServiceCategoryDTO> getAllCategories();

    ServiceCategoryDTO getCategoryById(String id);

    ServiceCategoryDTO createCategory(ServiceCategoryDTO categoryDTO);

    ServiceCategoryDTO updateCategory(String id, ServiceCategoryDTO categoryDTO);

    void deleteCategory(String id);

    ServiceCategoryDTO toggleCategoryStatus(String id, boolean active);
}