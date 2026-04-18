package com.netbridge.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.netbridge.framework.security.service.JwtTokenService;
import com.netbridge.framework.security.util.UserContextHolder;
import com.netbridge.framework.web.api.ApiResponse;
import com.netbridge.framework.web.api.PageResult;
import com.netbridge.framework.web.exception.BusinessException;
import com.netbridge.module.log.api.annotation.OperationAudit;
import com.netbridge.module.user.api.dto.LoginResponse;
import com.netbridge.module.user.api.dto.UserDto;
import com.netbridge.module.user.api.request.UserCreateRequest;
import com.netbridge.module.user.api.request.UserLoginRequest;
import com.netbridge.module.user.api.request.UserQueryRequest;
import com.netbridge.module.user.api.request.UserUpdateRequest;
import com.netbridge.module.user.entity.UserEntity;
import com.netbridge.module.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public UserController(UserService userService, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<UserDto>> list(UserQueryRequest request) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
                .like(request.getUsername() != null && !request.getUsername().isBlank(), UserEntity::getUsername, request.getUsername())
                .eq(request.getStatus() != null, UserEntity::getStatus, request.getStatus())
                .orderByDesc(UserEntity::getId);
        Page<UserEntity> page = userService.page(new Page<>(request.getPageNo(), request.getPageSize()), wrapper);
        List<UserDto> records = page.getRecords().stream().map(this::toDto).toList();
        return ApiResponse.success(new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDto> getById(@PathVariable Long id) {
        return ApiResponse.success(toDto(userService.getById(id)));
    }

    @GetMapping("/info")
    public ApiResponse<UserDto> info() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("unauthorized");
        }
        return ApiResponse.success(toDto(userService.getById(userId)));
    }

    @PostMapping("/create")
    @OperationAudit(
            action = "USER_CREATE",
            resourceType = "user",
            resourceId = "#result.data.id",
            successDetail = "'created user ' + #request.username",
            failureDetail = "'failed to create user ' + #request.username + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<UserDto> create(@Valid @RequestBody UserCreateRequest request) {
        validateUserUniqueness(request.getUsername(), request.getEmail(), null);
        UserEntity entity = new UserEntity();
        entity.setUsername(request.getUsername());
        entity.setPassword(passwordEncoder.encode(request.getPassword()));
        entity.setEmail(request.getEmail());
        entity.setRole(request.getRole() == null || request.getRole().isBlank() ? "user" : request.getRole());
        entity.setStatus(1);
        userService.save(entity);
        return ApiResponse.success(toDto(entity));
    }

    @PostMapping("/register")
    public ApiResponse<UserDto> register(@Valid @RequestBody UserCreateRequest request) {
        return create(request);
    }

    @PostMapping("/login")
    @OperationAudit(
            action = "USER_LOGIN",
            resourceType = "user",
            successDetail = "'user login ' + #request.username",
            failureDetail = "'failed login for ' + #request.username + ': ' + #errorMessage"
            ,resourceId = "#result.data.user.id"
            ,recordOnSuccess = true
    )
    public ApiResponse<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        UserEntity entity = userService.lambdaQuery()
                .eq(UserEntity::getUsername, request.getUsername())
                .one();
        if (entity == null || !passwordEncoder.matches(request.getPassword(), entity.getPassword())) {
            throw BusinessException.unauthorized("username or password is incorrect");
        }
        String token = jwtTokenService.generateToken(entity.getId(), entity.getUsername());
        return ApiResponse.success(new LoginResponse(token, toDto(entity)));
    }

    @PutMapping("/update")
    @OperationAudit(
            action = "USER_UPDATE",
            resourceType = "user",
            resourceId = "#request.id",
            successDetail = "'updated user ' + #request.username",
            failureDetail = "'failed to update user #' + #request.id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<UserDto> update(@Valid @RequestBody UserUpdateRequest request) {
        UserEntity entity = userService.getById(request.getId());
        if (entity == null) {
            throw BusinessException.notFound("user not found");
        }
        validateUserUniqueness(request.getUsername(), request.getEmail(), entity.getId());
        entity.setUsername(request.getUsername());
        entity.setEmail(request.getEmail());
        entity.setRole(request.getRole());
        entity.setStatus(request.getStatus());
        userService.updateById(entity);
        return ApiResponse.success(toDto(entity));
    }

    @DeleteMapping("/{id}")
    @OperationAudit(
            action = "USER_DELETE",
            resourceType = "user",
            resourceId = "#id",
            successDetail = "'deleted user #' + #id",
            failureDetail = "'failed to delete user #' + #id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success(userService.removeById(id));
    }

    private void validateUserUniqueness(String username, String email, Long excludeId) {
        boolean usernameExists = userService.lambdaQuery()
                .eq(UserEntity::getUsername, username)
                .ne(excludeId != null, UserEntity::getId, excludeId)
                .exists();
        if (usernameExists) {
            throw BusinessException.conflict("username already exists");
        }
        boolean emailExists = userService.lambdaQuery()
                .eq(UserEntity::getEmail, email)
                .ne(excludeId != null, UserEntity::getId, excludeId)
                .exists();
        if (emailExists) {
            throw BusinessException.conflict("email already exists");
        }
    }

    private UserDto toDto(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        UserDto dto = new UserDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
