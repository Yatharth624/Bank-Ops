package com.bankops.modules.exceptioncase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseNoteDto {
    private UUID id;
    private UUID caseId;
    private UUID authorId;
    private String authorUsername;
    private String authorFullName;
    private String noteText;
    private Instant createdAt;

    public static CaseNoteDto fromEntity(CaseNote note) {
        return CaseNoteDto.builder()
                .id(note.getId())
                .caseId(note.getExceptionCase().getId())
                .authorId(note.getAuthor().getId())
                .authorUsername(note.getAuthor().getUsername())
                .authorFullName(note.getAuthor().getFullName())
                .noteText(note.getNoteText())
                .createdAt(note.getCreatedAt())
                .build();
    }
}
