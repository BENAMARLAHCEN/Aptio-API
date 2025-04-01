package com.aptio.mapper;

import com.aptio.dto.ServiceDTO;
import com.aptio.model.Service;
import com.aptio.model.ServiceCategory;
import com.aptio.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class ServiceMapper {
    
    public ServiceDTO toDTO(Service service) {
        if (service == null) {
            return null;
        }

        ServiceDTO dto = new ServiceDTO();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setDescription(service.getDescription());
        dto.setDuration(service.getDuration());
        dto.setPrice(service.getPrice());
        dto.setCategory(service.getCategory().getName());
        dto.setActive(service.isActive());
        dto.setImageUrl(service.getImageUrl());

        return dto;
    }

   
    public Service toEntity(ServiceDTO dto) {
        if (dto == null) {
            return null;
        }

        Service service = new Service();
        service.setName(dto.getName());
        service.setDescription(dto.getDescription());
        service.setDuration(dto.getDuration());
        service.setPrice(dto.getPrice());
        service.setActive(dto.isActive());
        service.setImageUrl(dto.getImageUrl());

        return service;
    }
    
    public void updateEntityFromDTO(ServiceDTO dto, Service service) {
        if (dto == null || service == null) {
            return;
        }

        service.setName(dto.getName());
        service.setDescription(dto.getDescription());
        service.setDuration(dto.getDuration());
        service.setPrice(dto.getPrice());
        service.setActive(dto.isActive());
        service.setImageUrl(dto.getImageUrl());
    }
}