package com.raktim.catalogue_service.common.web;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.raktim.catalogue_service.common.error.ConflictException;
import com.raktim.catalogue_service.common.error.ResourceNotFoundException;

/**
 * The one place where exceptions become HTTP responses.
 * The parent class already handles Spring MVC's own exceptions (unknown URL, wrong method,
 * unreadable JSON, type mismatch, ...) and returns them as ProblemDetail.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ---- our own exceptions -------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException ex, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        body.setTitle("Conflict");
        ex.details().forEach(body::setProperty);              // e.g. currentVersion
        return createResponseEntity(body, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    // ---- persistence --------------------------------------------------------------------

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleOptimisticLock(OptimisticLockingFailureException ex, WebRequest request) {
        return problem(HttpStatus.CONFLICT, "Concurrent modification",
                "The resource was changed by another request. Reload it and try again.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Data integrity violation [requestId={}]: {}",
                requestId(request), ex.getMostSpecificCause().getMessage());   // detail stays in the log
        return problem(HttpStatus.CONFLICT, "Conflict",
                "The request conflicts with existing data, for example a duplicate name.", request);
    }

    // An unknown ?sort= property. Depending on the version, Spring Data throws it directly or wrapped.
    @ExceptionHandler({PropertyReferenceException.class, InvalidDataAccessApiUsageException.class})
    public ResponseEntity<Object> handleUnknownSortProperty(RuntimeException ex, WebRequest request) {
        PropertyReferenceException cause = ex instanceof PropertyReferenceException direct ? direct
                : ex.getCause() instanceof PropertyReferenceException wrapped ? wrapped
                : null;
        if (cause == null) {
            return handleUnexpected(ex, request);             // some other misuse: a bug, not bad input
        }
        return problem(HttpStatus.BAD_REQUEST, "Invalid sort property",
                "Cannot sort by '" + cause.getPropertyName() + "'.", request);
    }

    // ---- validation: adds the list of invalid fields ------------------------------------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "One or more fields are invalid.");
        body.setTitle("Validation failed");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        body.setProperty("errors", errors);
        return createResponseEntity(body, headers, HttpStatus.BAD_REQUEST, request);
    }

    // ---- anything else: log everything, reveal nothing ----------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error [requestId={}]", requestId(request), ex);   // stack trace in the log only
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
                "An unexpected error occurred. Quote the requestId when reporting it.", request);
    }

    // ---- every ProblemDetail built here, including the parent's, gets the request id ----

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
                                                          HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problemDetail) {    // pattern matching for instanceof (Java 16+)
            problemDetail.setProperty("requestId", requestId(request));
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ResponseEntity<Object> problem(HttpStatus status, String title, String detail, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        return createResponseEntity(body, new HttpHeaders(), status, request);
    }

    private static Object requestId(WebRequest request) {
        return request.getAttribute(RequestIdFilter.ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    }
}