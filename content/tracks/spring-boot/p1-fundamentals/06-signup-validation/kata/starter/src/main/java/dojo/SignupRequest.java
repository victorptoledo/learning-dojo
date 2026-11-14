package dojo;

/** The body of POST /signups. Add the constraints from the README. */
public record SignupRequest(String name, String email, Integer age) {}
