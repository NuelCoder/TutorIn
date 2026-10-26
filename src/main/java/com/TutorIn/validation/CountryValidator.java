package com.TutorIn.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class CountryValidator implements ConstraintValidator<ValidCountry, String> {

    private static final Set<String> VALID_COUNTRIES;

    static {
        VALID_COUNTRIES = Arrays.stream(Locale.getISOCountries())
                .map(iso -> new Locale("", iso).getDisplayCountry(Locale.ENGLISH))
                .collect(Collectors.toSet());
        VALID_COUNTRIES.add("Global");
    }

    @Override
    public void initialize(ValidCountry validCountry) {
        // Needed due to the syntax of jdk17
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // STANDARD JAKARTA PRACTICE: Return true for null/empty.
        // Let @NotBlank or @NotNull handle the rejection if the field is mandatory.
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        // Checks if the user's input matches a real country (case-insensitive)
        return VALID_COUNTRIES.stream()
                .anyMatch(country -> country.equalsIgnoreCase(value.trim()));
    }
}