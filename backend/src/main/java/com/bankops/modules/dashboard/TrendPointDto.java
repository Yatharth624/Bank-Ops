package com.bankops.modules.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointDto {
    private String date; // YYYY-MM-DD
    private long total;
    private long completed;
    private long failed;
    private long exceptions;
}
