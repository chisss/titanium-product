package com.titanium.product.api.response;

import lombok.Data;

/**
 * API响应统一格式
 * 用于所有API请求的统一响应格式
 */
@Data
public class ApiResponse<T> {
    /**
     * 响应码（200-成功，其他-失败）
     */
    private int code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 成功响应
     * @param data 响应数据
     * @param <T> 数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }
    
    /**
     * 失败响应
     * @param code 响应码
     * @param message 响应消息
     * @param <T> 数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> fail(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}