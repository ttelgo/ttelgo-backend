package com.tiktel.ttelgo.user.application;

import com.tiktel.ttelgo.common.exception.ResourceNotFoundException;
import com.tiktel.ttelgo.user.api.dto.UpdateUserRequest;
import com.tiktel.ttelgo.user.api.dto.UserResponse;
import com.tiktel.ttelgo.user.api.mapper.UserApiMapper;
import com.tiktel.ttelgo.user.application.port.UserRepositoryPort;
import com.tiktel.ttelgo.user.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    
    private final UserRepositoryPort userRepositoryPort;
    private final UserApiMapper userApiMapper;
    
    @Autowired
    public UserService(UserRepositoryPort userRepositoryPort, UserApiMapper userApiMapper) {
        this.userRepositoryPort = userRepositoryPort;
        this.userApiMapper = userApiMapper;
    }
    
    public UserResponse getUserById(Long id) {
        User user = userRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userApiMapper.toResponse(user);
    }
    
    public UserResponse getUserByEmail(String email) {
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userApiMapper.toResponse(user);
    }
    
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userApiMapper.updateUserFromRequest(user, request);
        user = userRepositoryPort.save(user);
        return userApiMapper.toResponse(user);
    }
}

