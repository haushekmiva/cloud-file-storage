    package com.haushekmiva.cloudfilestorage.exception;

    import com.haushekmiva.cloudfilestorage.dto.ErrorDto;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.http.HttpStatus;
    import org.springframework.web.bind.annotation.ExceptionHandler;
    import org.springframework.web.bind.annotation.ResponseStatus;
    import org.springframework.web.bind.annotation.RestControllerAdvice;

    @RestControllerAdvice
    @Slf4j
    public class GlobalExceptionHandler {

        @ExceptionHandler(UserAlreadyExists.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ErrorDto handelUserAlreadyExists(UserAlreadyExists e) {
            log.error("User already exists: username = {}.", e.getUsername());
            return new ErrorDto("User already exists.");
        }

        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public ErrorDto handleGeneral(Exception e) {
            log.error("Unknown error occurred.", e);
            return new ErrorDto("Internal server error occurred.");
        }

    }
