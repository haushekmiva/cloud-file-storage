    package com.haushekmiva.cloudfilestorage.exception;

    import com.haushekmiva.cloudfilestorage.dto.ErrorResponse;
    import com.haushekmiva.cloudfilestorage.dto.ValidationError;
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

        @ExceptionHandler(UserAlreadyExists.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ErrorResponse handleUserAlreadyExists(UserAlreadyExists e, Locale locale, HttpServletRequest request) {
            log.warn("User already exists: username = {}, ip = {}.", e.getUsername(), request.getRemoteAddr());
            String message = messageSource.getMessage("error.user.already-exists", null, locale);
            return new ErrorResponse(message);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ErrorResponse handleValidationError(MethodArgumentNotValidException e, Locale locale) {
            List<ValidationError> errors = e.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(this::processValidationErrors)
                    .toList();
            String message = messageSource.getMessage("error.validation", null, locale);
            return new ErrorResponse(message, errors);
        }

        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public ErrorResponse handleGeneral(Exception e, Locale locale) {
            log.error("Unknown error occurred.", e);
            String message = messageSource.getMessage("error.internal", null, locale);
            return new ErrorResponse(message);
        }

        private ValidationError processValidationErrors(FieldError fieldError) {
            log.debug("Validation error in field {}.", fieldError.getField());
            return new ValidationError(fieldError.getField(), fieldError.getDefaultMessage());
        }

    }
