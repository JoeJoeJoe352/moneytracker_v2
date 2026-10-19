package com.starbuck.moneytracker.validation;

import com.starbuck.moneytracker.dto.TransactionDetailCreateRequestDto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TransactionDetailAttributeCoexistValidator
        implements ConstraintValidator<TransactionDetailAttributeCoexist, TransactionDetailCreateRequestDto> {

    @Override
    public boolean isValid(TransactionDetailCreateRequestDto dto, ConstraintValidatorContext context) {
        // Két eset elfogadható: price létezik és a többi null, vagy a weight és a
        // unitprice együtt létezik és a price null
        if ((dto.price() != null && (dto.weight() != null || dto.unitPrice() != null))
                || (dto.price() == null && (dto.weight() == null || dto.unitPrice() == null))) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Illegal attribute coexist")
                    .addPropertyNode("coExist")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
