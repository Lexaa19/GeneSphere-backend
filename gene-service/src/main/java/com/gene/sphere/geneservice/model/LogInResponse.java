package com.gene.sphere.geneservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogInResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
}