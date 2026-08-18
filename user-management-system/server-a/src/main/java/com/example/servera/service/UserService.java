package com.example.servera.service;

import com.example.servera.dto.UserRequest;
import com.example.servera.dto.UserResponse;
import com.example.servera.entity.Department;
import com.example.servera.entity.Role;
import com.example.servera.entity.User;
import com.example.servera.event.EventType;
import com.example.servera.event.UserEvent;
import com.example.servera.exception.DuplicateResourceException;
import com.example.servera.exception.ResourceNotFoundException;
import com.example.servera.kafka.UserEventProducer;
import com.example.servera.repository.DepartmentRepository;
import com.example.servera.repository.RoleRepository;
import com.example.servera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service xử lý nghiệp vụ chính (CRUD User).
 * Sau khi transaction CRUD thành công, service sẽ publish event bất đồng bộ
 * lên Kafka để Server B xử lý các nghiệp vụ phụ (notification, log, thống kê...)
 * mà KHÔNG làm chậm luồng response HTTP trả về cho client.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserEventProducer userEventProducer;

    public UserResponse create(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username đã tồn tại: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email đã tồn tại: " + request.getEmail());
        }

        Role role = getRole(request.getRoleId());
        Department department = getDepartment(request.getDepartmentId());

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(role)
                .department(department)
                .status(User.UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        log.info("Đã tạo user thành công: id={}, username={}", saved.getId(), saved.getUsername());

        // ==== Publish event bất đồng bộ sang Kafka (Server B sẽ tự tiêu thụ) ====
        publishEvent(EventType.USER_CREATED, saved);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = getEntity(id);

        Role role = getRole(request.getRoleId());
        Department department = getDepartment(request.getDepartmentId());

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(role);
        user.setDepartment(department);

        User saved = userRepository.save(user);
        log.info("Đã cập nhật user: id={}", saved.getId());

        publishEvent(EventType.USER_UPDATED, saved);

        return toResponse(saved);
    }

    public void delete(Long id) {
        User user = getEntity(id);

        // Soft-delete: giữ lại bản ghi, chỉ đổi trạng thái (tuỳ chọn thiết kế)
        user.setStatus(User.UserStatus.DELETED);
        userRepository.save(user);
        log.info("Đã xoá (soft-delete) user: id={}", id);

        publishEvent(EventType.USER_DELETED, user);
    }

    // ================= Helpers =================

    private void publishEvent(EventType type, User user) {
        UserEvent event = UserEvent.builder()
                .eventType(type)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roleId(user.getRole() != null ? user.getRole().getId() : null)
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .occurredAt(LocalDateTime.now())
                .source("server-a")
                .build();

        userEventProducer.publish(event);
    }

    private User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User id=" + id));
    }

    private Role getRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Role id=" + roleId));
    }

    private Department getDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Department id=" + departmentId));
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .status(u.getStatus())
                .roleId(u.getRole() != null ? u.getRole().getId() : null)
                .roleName(u.getRole() != null ? u.getRole().getName() : null)
                .departmentId(u.getDepartment() != null ? u.getDepartment().getId() : null)
                .departmentName(u.getDepartment() != null ? u.getDepartment().getName() : null)
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
