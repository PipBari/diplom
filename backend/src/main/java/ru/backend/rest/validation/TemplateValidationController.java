package ru.backend.rest.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.validation.dto.ValidationRequestDto;
import ru.backend.rest.validation.dto.ValidationResultDto;
import ru.backend.service.validation.TemplateValidationService;

import java.util.List;

@RestController
@RequestMapping("/validate")
@RequiredArgsConstructor
public class TemplateValidationController {

    private final TemplateValidationService validationService;

    @PostMapping("/{type}")
    public ResponseEntity<ValidationResultDto> validate(
            @PathVariable String type,
            @RequestBody List<ValidationRequestDto> requests
    ) {
        try {
            ValidationResultDto result = validationService.validate(type, requests);
            return result.isValid()
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ValidationResultDto(false, "Ошибка валидации: " + e.getMessage())
            );
        }
    }
}
