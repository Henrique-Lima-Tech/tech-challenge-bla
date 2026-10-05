package com.tech.challenge.web.shared.error;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.tech.challenge.domain.pokemon.exception.LocalPokemonNotFoundException;
import com.tech.challenge.domain.pokemon.exception.PokemonAlreadySyncedException;
import com.tech.challenge.domain.pokemon.exception.PokemonNotFoundException;
import com.tech.challenge.domain.shared.exception.ExternalServiceUnavailableException;
import com.tech.challenge.domain.user.exception.EmailAlreadyUsedException;
import com.tech.challenge.domain.user.exception.InvalidCredentialsException;

import lombok.extern.slf4j.Slf4j;

/**
 * Every error as a {@link ProblemDetail} (D-17). Details are fixed English messages: no stack trace and
 * no upstream body ever reaches the client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Spring Framework 7 leaves {@code type} null; the contract shows it explicitly. */
    private static final URI BLANK_TYPE = URI.create("about:blank");

    record FieldError(String field, String message) {
    }

    private static ProblemDetail problem(final HttpStatus status, final String detail) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(BLANK_TYPE);
        return problem;
    }

    @ExceptionHandler(PokemonNotFoundException.class)
    ProblemDetail handlePokemonNotFound(final PokemonNotFoundException e) {
        log.debug("Answering 404: Pokemon '{}' not found", e.getIdOrName());
        return problem(HttpStatus.NOT_FOUND, "Pokemon not found");
    }

    @ExceptionHandler(LocalPokemonNotFoundException.class)
    ProblemDetail handleLocalPokemonNotFound(final LocalPokemonNotFoundException e) {
        log.debug("Answering 404: local Pokemon {} not found", e.getId());
        return problem(HttpStatus.NOT_FOUND, "Local Pokemon not found");
    }

    @ExceptionHandler(PokemonAlreadySyncedException.class)
    ProblemDetail handlePokemonAlreadySynced(final PokemonAlreadySyncedException e) {
        log.debug("Answering 409: Pokemon already synced");
        return problem(HttpStatus.CONFLICT, "Pokemon already synced");
    }

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    ProblemDetail handleExternalServiceUnavailable(final ExternalServiceUnavailableException e) {
        log.debug("Answering 502: {}", e.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "PokeAPI is unavailable");
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    ProblemDetail handleEmailAlreadyUsed(final EmailAlreadyUsedException e) {
        log.debug("Answering 409: email already used");
        return problem(HttpStatus.CONFLICT, "Email already used");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(final InvalidCredentialsException e) {
        log.debug("Answering 401: login failed: invalid credentials");
        return problem(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(final AuthenticationException e) {
        log.debug("Answering 401: authentication required ({})", e.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(problem(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException e,
            final HttpHeaders headers, final HttpStatusCode status, final WebRequest request) {
        final List<FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        log.debug("Answering 400: validation failed {}", errors);
        final ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(e, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * A constraint on a parameter makes Spring validate every parameter of the method, the {@code @Valid} body
     * included: its errors arrive here as {@link ParameterErrors} instead of as a
     * {@code MethodArgumentNotValidException}, and only they know the field path inside the body.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(final HandlerMethodValidationException e,
            final HttpHeaders headers, final HttpStatusCode status, final WebRequest request) {
        final List<FieldError> errors = e.getParameterValidationResults().stream()
                .flatMap(GlobalExceptionHandler::fieldErrorsOf)
                .toList();
        log.debug("Answering 400: validation failed {}", errors);
        final ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(e, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static Stream<FieldError> fieldErrorsOf(final ParameterValidationResult result) {
        if (result instanceof final ParameterErrors bodyErrors) {
            return bodyErrors.getFieldErrors().stream()
                    .map(error -> new FieldError(error.getField(), error.getDefaultMessage()));
        }
        return result.getResolvableErrors().stream()
                .map(error -> new FieldError(result.getMethodParameter().getParameterName(), error.getDefaultMessage()));
    }

    /**
     * Spring's default detail echoes the rejected value; the contract wants a fixed detail and {@code errors[]}.
     * The message assumes an integer parameter: today only {@code page}, {@code size} and the local Pokémon
     * path {@code id} can mismatch.
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(final TypeMismatchException e, final HttpHeaders headers,
            final HttpStatusCode status, final WebRequest request) {
        final List<FieldError> errors = List.of(new FieldError(e.getPropertyName(), "must be an integer"));
        log.debug("Answering 400: validation failed {}", errors);
        final ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(e, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(final Exception e, final Object body, final HttpHeaders headers,
            final HttpStatusCode statusCode, final WebRequest request) {
        final ResponseEntity<Object> response = super.handleExceptionInternal(e, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem && problem.getType() == null) {
            problem.setType(BLANK_TYPE);
        }
        return response;
    }
}
