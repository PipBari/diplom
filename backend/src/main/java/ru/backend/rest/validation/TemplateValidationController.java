package ru.backend.rest.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.backend.rest.validation.dto.ValidationRequestDto;
import ru.backend.rest.validation.dto.ValidationResultDto;
import ru.backend.service.validation.TemplateValidationService;

@RestController
@RequestMapping("/validate")
@RequiredArgsConstructor
public class TemplateValidationController {

    private final TemplateValidationService validationService;

    @PostMapping("/{type}")
    public ResponseEntity<ValidationResultDto> validate(
            @PathVariable String type,
            @RequestBody ValidationRequestDto request
    ) {
        ValidationResultDto result = validationService.validate(type, request);
        if (result.isValid()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}
