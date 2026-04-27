package github.revanjay.partnershiprewards.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftData {
    private int id;
    private UUID sender;
    private UUID receiver;
    private String itemData;
    private long sentAt;
}
