package org.example.datn_sd69.modules.inventorylot.dto.response;

import lombok.*;
import org.example.datn_sd69.enums.InventoryLotLockActionType;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLotLockHistoryResponse {
    private Integer id;
    private InventoryLotLockActionType actionType;
    private String actionTypeLabel;
    private String reason;
    private Integer actionById;
    private String actionByName;
    private LocalDateTime actionAt;
}
