package com.starbuck.moneytracker.validation;

import com.starbuck.moneytracker.dto.RegisterRequestDto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, RegisterRequestDto>{

    @Override
    public boolean isValid(RegisterRequestDto dto, ConstraintValidatorContext context) {
        if (dto.password() != null && dto.passwordAgain() != null && dto.password().equals(dto.passwordAgain())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("Passwords do not match")
               .addPropertyNode("passwordAgain")
               .addConstraintViolation();

        return false;
    }
}
