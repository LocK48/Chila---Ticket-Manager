package com.chila.backend.common.api;

import java.util.Map;
import java.util.TreeMap;

import com.chila.backend.user.DuplicateEmailException;
import com.chila.backend.user.UserNotFoundException;
import com.chila.backend.auth.InvalidCredentialsException;
import com.chila.backend.project.DuplicateProjectKeyException;
import com.chila.backend.project.ProjectNotFoundException;
import com.chila.backend.workspace.MembershipConflictException;
import com.chila.backend.workspace.WorkspaceAccessDeniedException;
import com.chila.backend.workspace.WorkspaceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = new TreeMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed.",
                request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The request body is malformed or contains an invalid value.",
                request, Map.of());
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidInput(
            InvalidInputException exception,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage(),
                request, Map.of(exception.getField(), exception.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(
            DuplicateEmailException exception,
            HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", exception.getMessage(),
                request, Map.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(WorkspaceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleWorkspaceNotFound(
            WorkspaceNotFoundException exception,
            HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "WORKSPACE_NOT_FOUND", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MembershipConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleMembershipConflict(
            MembershipConflictException exception,
            HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "MEMBERSHIP_ALREADY_EXISTS", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(WorkspaceAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleWorkspaceAccessDenied(
            WorkspaceAccessDeniedException exception,
            HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "ACCESS_DENIED", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProjectNotFound(
            ProjectNotFoundException exception,
            HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DuplicateProjectKeyException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateProjectKey(
            DuplicateProjectKeyException exception,
            HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "PROJECT_KEY_ALREADY_EXISTS", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataConflict(HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data.",
                request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        logger.error("Unexpected error handling {}", request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected server error occurred.", request, Map.of());
    }

    public static ApiErrorResponse body(HttpStatus status, String code, String message, String path) {
        return new ApiErrorResponse(status.value(), code, message, path, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.value(), code, message, request.getRequestURI(), fieldErrors));
    }
}
