package com.bankops.modules.exceptioncase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCaseNoteRequest {

    @NotBlank(message = "Note text cannot be blank")
    @Size(max = 2000, message = "Note cannot exceed 2000 characters")
    private String noteText;
}
