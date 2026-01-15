package com.example.drools.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class Audit {
    private List<String> audits = new ArrayList<>();

    public void addAudit(String message) {
        this.audits.add(message);
    }
}
