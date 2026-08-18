package com.example.servera.service;

import com.example.servera.dto.OrganizationRequest;
import com.example.servera.dto.OrganizationResponse;
import com.example.servera.entity.Organization;
import com.example.servera.exception.DuplicateResourceException;
import com.example.servera.exception.ResourceNotFoundException;
import com.example.servera.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationResponse create(OrganizationRequest request) {
        if (organizationRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Tổ chức đã tồn tại: " + request.getName());
        }
        Organization org = Organization.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return toResponse(organizationRepository.save(org));
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> findAll() {
        return organizationRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public OrganizationResponse update(Long id, OrganizationRequest request) {
        Organization org = getEntity(id);
        org.setName(request.getName());
        org.setDescription(request.getDescription());
        return toResponse(organizationRepository.save(org));
    }

    public void delete(Long id) {
        Organization org = getEntity(id);
        organizationRepository.delete(org);
    }

    private Organization getEntity(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Organization id=" + id));
    }

    private OrganizationResponse toResponse(Organization o) {
        return OrganizationResponse.builder()
                .id(o.getId())
                .name(o.getName())
                .description(o.getDescription())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
