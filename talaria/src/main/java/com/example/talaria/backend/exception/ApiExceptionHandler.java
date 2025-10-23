package com.example.talaria.backend.exception;

import org.springframework.core.env.Profiles;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.RequiredArgsConstructor;


@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {
  private final org.springframework.core.env.Environment env;

  @ExceptionHandler(org.springframework.dao.DataAccessException.class)
  public ProblemDetail handleDataAccess(DataAccessException ex) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Database error");
    pd.setTitle("Database Error");
    if (env.acceptsProfiles(Profiles.of("local","dev"))) {
      var cause = ex.getMostSpecificCause();
      if (cause != null) {
        pd.setProperty("cause", cause.getClass().getSimpleName());
        pd.setProperty("message", cause.getMessage()); // ← 개발에서만
      }
    }
    return pd;
  }
}
