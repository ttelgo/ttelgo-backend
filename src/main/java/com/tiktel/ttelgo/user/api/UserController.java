package com.tiktel.ttelgo.user.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.PaginationMeta;
import com.tiktel.ttelgo.common.service.FileStorageService;
import com.tiktel.ttelgo.order.api.dto.OrderResponse;
import com.tiktel.ttelgo.order.application.OrderService;
import com.tiktel.ttelgo.user.api.dto.ChangePasswordRequest;
import com.tiktel.ttelgo.user.api.dto.UpdateUserRequest;
import com.tiktel.ttelgo.user.api.dto.UserResponse;
import com.tiktel.ttelgo.user.application.UserService;
import com.tiktel.ttelgo.security.RoleScopeResolver;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    private final UserService userService;
    private final OrderService orderService;
    private final RoleScopeResolver roleScopeResolver;
    private final FileStorageService fileStorageService;
    
    @Autowired
    public UserController(UserService userService, OrderService orderService,
                          RoleScopeResolver roleScopeResolver, FileStorageService fileStorageService) {
        this.userService = userService;
        this.orderService = orderService;
        this.roleScopeResolver = roleScopeResolver;
        this.fileStorageService = fileStorageService;
    }
    
    /**
     * Get current logged-in user's information.
     * 
     * Security:
     * - Requires JWT authentication (Bearer token in Authorization header)
     * - Extracts user_id from JWT token via Spring Security Authentication
     * - Fetches user data from database to ensure data is current
     * - Returns 401 if JWT is missing or invalid
     * - Users can only access their own profile
     * 
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        // Get authentication from SecurityContext (set by JwtAuthenticationFilter)
        org.springframework.security.core.Authentication authentication = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        // Check if user is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, 
                "Authentication required. Please provide a valid authentication token.");
        }
        
        // Extract user ID from Authentication context using RoleScopeResolver
        // This gets the user_id from the JWT token that was set by JwtAuthenticationFilter
        Long userId = roleScopeResolver.getCurrentUserId();
        
        // Validate that we have a user ID
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, 
                "Invalid authentication. User ID not found in authentication context.");
        }
        
        // Fetch user from database using the authenticated user's ID
        // This ensures we always return current data from the database
        UserResponse userResponse = userService.getUserById(userId);
        
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }
    
    /**
     * Get user by ID.
     * - Admins can access any user.
     * - Regular users can only access their own profile (use /me instead).
     * GET /api/v1/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        Long currentUserId = roleScopeResolver.getCurrentUserId();
        boolean isAdmin = roleScopeResolver.hasRole("ADMIN") || roleScopeResolver.hasRole("SUPER_ADMIN");
        // Allow access only if requester is admin or is accessing their own data
        if (!isAdmin && (currentUserId == null || !currentUserId.equals(id))) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Access denied. You can only view your own profile.");
        }
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * RESTful query-style lookup — admin only.
     * GET /api/v1/users?email=user@example.com
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> findUser(@RequestParam(required = false) String email) {
        boolean isAdmin = roleScopeResolver.hasRole("ADMIN") || roleScopeResolver.hasRole("SUPER_ADMIN");
        if (!isAdmin) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Access denied. Admin role required.");
        }
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Provide 'email' query parameter."));
        }
        UserResponse response = userService.getUserByEmail(email.trim());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get orders for a specific user.
     * - Admins can access any user's orders.
     * - Regular users can only access their own orders.
     * GET /api/v1/users/{id}/orders
     */
    @GetMapping("/{id}/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersForUser(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort
    ) {
        Long currentUserId = roleScopeResolver.getCurrentUserId();
        boolean isAdmin = roleScopeResolver.hasRole("ADMIN") || roleScopeResolver.hasRole("SUPER_ADMIN");
        if (!isAdmin && (currentUserId == null || !currentUserId.equals(id))) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Access denied. You can only view your own orders.");
        }
        List<OrderResponse> response = orderService.getOrdersByUserId(id);
        List<OrderResponse> sorted = applySort(response, sort);
        List<OrderResponse> paged = slice(sorted, page, size);
        return ResponseEntity.ok(ApiResponse.success(paged, "Success", PaginationMeta.simple(page, size, sorted.size())));
    }
    
    /**
     * Update current user's own profile.
     * PATCH /api/v1/users/me
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @RequestBody UpdateUserRequest request) {
        Long userId = roleScopeResolver.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authentication required.");
        }
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully."));
    }

    /**
     * Change current user's password.
     * POST /api/v1/users/me/change-password
     */
    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = roleScopeResolver.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authentication required.");
        }
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully."));
    }

    /**
     * Upload or replace profile picture for current user.
     * POST /api/v1/users/me/profile-picture  (multipart/form-data, field: file)
     */
    @PostMapping(value = "/me/profile-picture", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<UserResponse>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file) {
        Long userId = roleScopeResolver.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authentication required.");
        }
        try {
            String url = fileStorageService.storeProfilePicture(file, "profile-pictures");
            UserResponse response = userService.updateProfilePicture(userId, url);
            return ResponseEntity.ok(ApiResponse.success(response, "Profile picture updated."));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to upload profile picture.");
        }
    }

    /**
     * Full update of a user — admin only.
     * PUT /api/v1/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {
        boolean isAdmin = roleScopeResolver.hasRole("ADMIN") || roleScopeResolver.hasRole("SUPER_ADMIN");
        if (!isAdmin) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied. Admin role required.");
        }
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private List<OrderResponse> slice(List<OrderResponse> items, int page, int size) {
        if (items == null || items.isEmpty()) return items;
        if (size <= 0) return items;
        int from = Math.max(0, page) * size;
        if (from >= items.size()) return List.of();
        int to = Math.min(items.size(), from + size);
        return items.subList(from, to);
    }

    private List<OrderResponse> applySort(List<OrderResponse> items, String sort) {
        if (items == null) return List.of();
        if (sort == null || sort.isBlank()) return items;

        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
        boolean desc = "desc".equals(dir);

        return items.stream().sorted((a, b) -> {
            int cmp = 0;
            if ("createdAt".equals(field)) {
                LocalDateTime av = a.getCreatedAt();
                LocalDateTime bv = b.getCreatedAt();
                if (av == null && bv == null) cmp = 0;
                else if (av == null) cmp = 1;
                else if (bv == null) cmp = -1;
                else cmp = av.compareTo(bv);
            } else if ("status".equals(field)) {
                String av = a.getStatus() != null ? a.getStatus().name() : null;
                String bv = b.getStatus() != null ? b.getStatus().name() : null;
                if (av == null && bv == null) cmp = 0;
                else if (av == null) cmp = 1;
                else if (bv == null) cmp = -1;
                else cmp = av.compareToIgnoreCase(bv);
            } else {
                // default: createdAt
                LocalDateTime av = a.getCreatedAt();
                LocalDateTime bv = b.getCreatedAt();
                if (av == null && bv == null) cmp = 0;
                else if (av == null) cmp = 1;
                else if (bv == null) cmp = -1;
                else cmp = av.compareTo(bv);
            }

            return desc ? -cmp : cmp;
        }).collect(Collectors.toList());
    }
}

