    package com.haushekmiva.cloudfilestorage.exception;

    import com.haushekmiva.cloudfilestorage.dto.ErrorResponse;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.context.MessageSource;
    import org.springframework.http.HttpStatus;
    import org.springframework.web.bind.annotation.ExceptionHandler;
    import org.springframework.web.bind.annotation.ResponseStatus;
    import org.springframework.web.bind.annotation.RestControllerAdvice;

    import java.util.Locale;

    @RestControllerAdvice
    @Slf4j
    @RequiredArgsConstructor
    public class GlobalExceptionHandler {

        private final MessageSource messageSource;

        @ExceptionHandler(UserAlreadyExists.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ErrorResponse handleUserAlreadyExists(UserAlreadyExists e, Locale locale) {
            log.error("User already exists: username = {}.", e.getUsername());
            String message = messageSource.getMessage("error.user.already-exists", null, locale);
            return new ErrorResponse(message);
        }

        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public ErrorResponse handleGeneral(Exception e, Locale locale) {
            log.error("Unknown error occurred.", e);
            String message = messageSource.getMessage("error.internal", null, locale);
            return new ErrorResponse(message);
        }

    }
