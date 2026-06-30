package com.haushekmiva.cloudfilestorage.exception;

import com.haushekmiva.cloudfilestorage.dto.ErrorResponse;
import com.haushekmiva.cloudfilestorage.dto.ValidationError;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Locale;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUserAlreadyExistsException(UserAlreadyExistsException e, Locale locale, HttpServletRequest request) {
        log.warn("User already exists: username = {}, ip = {}.", e.getUsername(), request.getRemoteAddr());
        String message = messageSource.getMessage("error.user.already-exists", null, locale);
        return new ErrorResponse(message);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleResourceAlreadyExistsException(ResourceAlreadyExistsException e, Locale locale) {
        String message = messageSource.getMessage("error.resource.already-exists", null, locale);
        log.debug("Resource already exists: path = {}", e.getPath());
        return new ErrorResponse(message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException e, Locale locale) {
        List<ValidationError> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::processValidationErrors)
                .toList();
        String message = messageSource.getMessage("error.validation", null, locale);
        return new ErrorResponse(message, errors);
    }

    @ExceptionHandler(InvalidPathException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidPathException(InvalidPathException e, Locale locale) {
        String message = messageSource.getMessage("error.invalid-path", null, locale);
        log.debug("Invalid path: path = {}", e.getPath());
        return new ErrorResponse(message);
    }

    @ExceptionHandler(UserAuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(UserAuthenticationException e, Locale locale) {
        log.warn("Authentication error: {}", e.getMessage());
        String message = messageSource.getMessage("error.authentication", null, locale);
        return new ErrorResponse(message);
    }

    @ExceptionHandler({FileStorageException.class, Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneralException(Exception e, Locale locale) {
        log.error("Unknown error occurred.", e);
        String message = messageSource.getMessage("error.internal", null, locale);
        return new ErrorResponse(message);
    }

    private ValidationError processValidationErrors(FieldError fieldError) {
        log.debug("Validation error in field {}.", fieldError.getField());
        return new ValidationError(fieldError.getField(), fieldError.getDefaultMessage());
    }

}
