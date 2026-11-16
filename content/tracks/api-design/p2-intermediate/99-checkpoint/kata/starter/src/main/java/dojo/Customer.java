package dojo;

import java.util.List;

/**
 * The customer as the business models it now: split names, several emails (the first is the
 * primary one), a status that can also be SUSPENDED, and a country (ISO 3166 alpha-2, or null
 * when unknown). The version goes up on every change.
 */
public record Customer(long id, String givenName, String familyName, List<String> emails, String phoneNumber,
        CustomerStatus status, String country, long version) {}
