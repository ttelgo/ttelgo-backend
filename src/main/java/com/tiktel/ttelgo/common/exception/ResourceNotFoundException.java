package com.tiktel.ttelgo.common.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends BusinessException {
    private final String resourceType;
    private final Object resourceId;
    
    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(ErrorCode.RESOURCE_NOT_FOUND, 
              String.format("%s not found with id: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        this.resourceType = null;
        this.resourceId = null;
    }
}

