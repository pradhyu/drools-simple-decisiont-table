package com.example.drools.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Claim {
    private String id;
    private double amount;
    private String type; // e.g., "Dental", "Vision", "Medical"
}
