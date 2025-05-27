package JavaProject.MoneyManagement_BE_SE330.helper;

import JavaProject.MoneyManagement_BE_SE330.models.dtos.error.ValidationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setType(URI.create("https://tools.ietf.org/html/rfc9110#section-15.5.4"));
        pd.setTitle(ex.getMessage());
        pd.setProperty("traceId", request.getAttribute("traceId", WebRequest.SCOPE_REQUEST));
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, List<String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        e -> e.getField(),
                        Collectors.mapping(e -> e.getDefaultMessage(), Collectors.toList())
                ));
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("https://tools.ietf.org/html/rfc9110#section-15.5.1"));
        pd.setTitle("One or more validation errors occurred.");
        pd.setProperty("errors", errors);
        pd.setProperty("traceId", request.getAttribute("traceId", WebRequest.SCOPE_REQUEST));
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setType(URI.create("https://tools.ietf.org/html/rfc9110#section-15.5.3"));
        pd.setTitle("Access denied");
        pd.setDetail(ex.getMessage());
        pd.setProperty("traceId", request.getAttribute("traceId", WebRequest.SCOPE_REQUEST));
        return pd;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setType(URI.create("https://tools.ietf.org/html/rfc9110#section-15.5.2"));
        pd.setTitle("Authentication failed");
        pd.setDetail(ex.getMessage());
        pd.setProperty("traceId", request.getAttribute("traceId", WebRequest.SCOPE_REQUEST));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        ex.printStackTrace();
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setType(URI.create("https://tools.ietf.org/html/rfc9110#section-15.6.1"));
        pd.setTitle("An unexpected error occurred");
        pd.setDetail(ex.getMessage());
        pd.setProperty("traceId", request.getAttribute("traceId", WebRequest.SCOPE_REQUEST));
        return pd;
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(ValidationException ex) {
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
                "One or more validation errors occurred.",
                ex.getErrors()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
