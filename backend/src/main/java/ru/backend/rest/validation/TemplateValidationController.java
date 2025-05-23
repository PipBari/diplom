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
            if (result.isValid()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ValidationResultDto(false, "Неверный тип валидации: " + e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new ValidationResultDto(false, "Ошибка валидации: " + e.getMessage())
            );
        }
    }
}
