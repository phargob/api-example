package dax.api_lib.models.exceptions;




public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
