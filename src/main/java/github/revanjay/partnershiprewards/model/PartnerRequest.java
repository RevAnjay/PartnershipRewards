package github.revanjay.partnershiprewards.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class PartnerRequest {
    private UUID sender;
    private UUID target;
    private long timestamp;
    
    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > 60000;
    }
}
