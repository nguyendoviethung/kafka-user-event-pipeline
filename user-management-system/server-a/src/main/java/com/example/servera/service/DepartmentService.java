package com.example.servera.service;

import com.example.servera.dto.DepartmentRequest;
import com.example.servera.dto.DepartmentResponse;
import com.example.servera.entity.Department;
import com.example.servera.entity.Organization;
import com.example.servera.exception.ResourceNotFoundException;
import com.example.servera.repository.DepartmentRepository;
import com.example.servera.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;

    public DepartmentResponse create(DepartmentRequest request) {
        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Organization id=" + request.getOrganizationId()));

        Department dept = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .organization(org)
                .build();
        return toResponse(departmentRepository.save(dept));
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> findAll() {
        return departmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department dept = getEntity(id);
        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Organization id=" + request.getOrganizationId()));
        dept.setName(request.getName());
        dept.setDescription(request.getDescription());
        dept.setOrganization(org);
        return toResponse(departmentRepository.save(dept));
    }

    public void delete(Long id) {
        departmentRepository.delete(getEntity(id));
    }

    private Department getEntity(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Department id=" + id));
    }

    private DepartmentResponse toResponse(Department d) {
        return DepartmentResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .organizationId(d.getOrganization() != null ? d.getOrganization().getId() : null)
                .organizationName(d.getOrganization() != null ? d.getOrganization().getName() : null)
                .build();
    }
}
