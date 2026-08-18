package com.example.servera.service;

import com.example.servera.dto.RoleRequest;
import com.example.servera.dto.RoleResponse;
import com.example.servera.entity.Role;
import com.example.servera.exception.DuplicateResourceException;
import com.example.servera.exception.ResourceNotFoundException;
import com.example.servera.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Role đã tồn tại: " + request.getName());
        }
        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return toResponse(roleRepository.save(role));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public RoleResponse update(Long id, RoleRequest request) {
        Role role = getEntity(id);
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        return toResponse(roleRepository.save(role));
    }

    public void delete(Long id) {
        roleRepository.delete(getEntity(id));
    }

    private Role getEntity(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Role id=" + id));
    }

    private RoleResponse toResponse(Role r) {
        return RoleResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .description(r.getDescription())
                .build();
    }
}
