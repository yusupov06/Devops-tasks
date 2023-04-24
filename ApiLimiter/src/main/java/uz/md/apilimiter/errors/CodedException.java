package uz.md.apilimiter.errors;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodedException extends RuntimeException {

    private int errorCode;

    public CodedException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
