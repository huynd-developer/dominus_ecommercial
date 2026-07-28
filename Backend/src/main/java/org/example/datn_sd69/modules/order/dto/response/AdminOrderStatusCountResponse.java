package org.example.datn_sd69.modules.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderStatusCountResponse {

    private long total;

    private long pending;
    private long confirmed;
    private long shipping;
    private long completed;
    private long cancelled;
    private long deliveryFailed;
    private long returnRequested;
    private long returnCompleted;
}