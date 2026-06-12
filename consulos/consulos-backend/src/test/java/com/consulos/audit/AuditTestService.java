package com.consulos.audit;

import org.springframework.stereotype.Service;

@Service
public class AuditTestService {

    @com.consulos.audit.Auditable(action = "TEST_ACTION", entiteType = "TEST")
    public String success(String input) {
        return "ok-" + input;
    }

    @com.consulos.audit.Auditable(action = "TEST_FAIL", entiteType = "TEST")
    public void failure() {
        throw new IllegalStateException("boom");
    }
}
