package com.TutorIn.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;


public class CountryValidator implements ConstraintValidator<ValidCountry, String> {

    private static final Set<String> VALID_COUNTRIES;

    // This static block runs once when the app starts.
    // It grabs every official ISO country in the world and gets its English name (e.g., "Nigeria")
    static {
        VALID_COUNTRIES = Arrays.stream(Locale.getISOCountries())
                .map(iso -> new Locale("", iso).getDisplayCountry(Locale.ENGLISH))
                .collect(Collectors.toSet());

        // We add "Global" because your admin service uses this for global resources
        VALID_COUNTRIES.add("Global");
    }


    @Override
    public void initialize(ValidCountry validCountry) {
        //Needed due to the syntax of jdk17
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        // Checks if the user's input matches a real country (case-insensitive)
        return VALID_COUNTRIES.stream()
                .anyMatch(country -> country.equalsIgnoreCase(value.trim()));
    }
}
