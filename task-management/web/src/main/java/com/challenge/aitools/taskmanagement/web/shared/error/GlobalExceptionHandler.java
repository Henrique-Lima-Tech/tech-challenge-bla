package com.challenge.aitools.taskmanagement.web.shared.error;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.challenge.aitools.taskmanagement.domain.task.exception.InvalidTaskException;
import com.challenge.aitools.taskmanagement.domain.task.exception.TaskNotFoundException;
import com.challenge.aitools.taskmanagement.domain.user.exception.EmailAlreadyRegisteredException;
import com.challenge.aitools.taskmanagement.domain.user.exception.InvalidCredentialsException;
import com.challenge.aitools.taskmanagement.domain.user.exception.InvalidUserException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String VALIDATION_FAILED = "Validation failed";

    private static final URI BLANK_TYPE = URI.create("about:blank");

    record FieldError(String field, String message) {
    }

    private static ProblemDetail problem(final HttpStatus status, final String detail) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(BLANK_TYPE);
        return problem;
    }

    private static ProblemDetail validationProblem(final List<FieldError> errors) {
        log.debug("Answering 400: validation failed {}", errors);
        final ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, VALIDATION_FAILED);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(TaskNotFoundException.class)
    ProblemDetail handleTaskNotFound(final TaskNotFoundException e) {
        log.debug("Answering 404: task not found or owned by another user");
        return problem(HttpStatus.NOT_FOUND, "Task not found");
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail handleEmailAlreadyRegistered(final EmailAlreadyRegisteredException e) {
        log.debug("Answering 409: email already registered");
        return problem(HttpStatus.CONFLICT, "Email already registered");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(final InvalidCredentialsException e) {
        log.debug("Answering 401: login failed");
        return problem(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(final AuthenticationException e) {
        log.debug("Answering 401: authentication required ({})", e.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(problem(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    @ExceptionHandler(InvalidTaskException.class)
    ProblemDetail handleInvalidTask(final InvalidTaskException e) {
        return validationProblem(List.of(new FieldError(e.getField(), e.getReason())));
    }

    @ExceptionHandler(InvalidUserException.class)
    ProblemDetail handleInvalidUser(final InvalidUserException e) {
        return validationProblem(List.of(new FieldError(e.getField(), e.getReason())));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException e,
            final HttpHeaders headers, final HttpStatusCode status, final WebRequest request) {
        final List<FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return handleExceptionInternal(e, validationProblem(errors), headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(final HandlerMethodValidationException e,
            final HttpHeaders headers, final HttpStatusCode status, final WebRequest request) {
        final List<FieldError> errors = e.getParameterValidationResults().stream()
                .flatMap(GlobalExceptionHandler::fieldErrorsOf)
                .toList();
        return handleExceptionInternal(e, validationProblem(errors), headers, HttpStatus.BAD_REQUEST, request);
    }

    private static Stream<FieldError> fieldErrorsOf(final ParameterValidationResult result) {
        if (result instanceof final ParameterErrors bodyErrors) {
            return bodyErrors.getFieldErrors().stream()
                    .map(error -> new FieldError(error.getField(), error.getDefaultMessage()));
        }
        return result.getResolvableErrors().stream()
                .map(error -> new FieldError(result.getMethodParameter().getParameterName(), error.getDefaultMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(final HttpMessageNotReadableException e,
            final HttpHeaders headers, final HttpStatusCode status, final WebRequest request) {
        if (e.getCause() instanceof final UnrecognizedPropertyException unrecognized) {
            final List<FieldError> errors = List.of(new FieldError(unrecognized.getPropertyName(), "must not be sent"));
            return handleExceptionInternal(e, validationProblem(errors), headers, HttpStatus.BAD_REQUEST, request);
        }
        log.debug("Answering 400: malformed request body");
        final ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Malformed request body");
        return handleExceptionInternal(e, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(final TypeMismatchException e, final HttpHeaders headers,
            final HttpStatusCode status, final WebRequest request) {
        final List<FieldError> errors = List.of(new FieldError(e.getPropertyName(), "must be an integer"));
        return handleExceptionInternal(e, validationProblem(errors), headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(final Exception e, final Object body,
            final HttpHeaders headers, final HttpStatusCode statusCode, final WebRequest request) {
        final ResponseEntity<Object> response = super.handleExceptionInternal(e, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof final ProblemDetail problem && problem.getType() == null) {
            problem.setType(BLANK_TYPE);
        }
        return response;
    }
}
