package uz.md.apilimiter.errors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class CustomExceptionHandler {

    @ExceptionHandler(CodedException.class)
    public ResponseEntity<String> handleException(CodedException exception) {
        log.debug("Exception handler working for exception: " + exception);
        return new ResponseEntity<>(exception.getMessage(),
                HttpStatusCode.valueOf(exception.getErrorCode()));
    }

}
