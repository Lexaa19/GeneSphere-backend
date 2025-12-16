package com.gene.sphere.geneservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogInRequest {

    private String username;
    private String password;
}
