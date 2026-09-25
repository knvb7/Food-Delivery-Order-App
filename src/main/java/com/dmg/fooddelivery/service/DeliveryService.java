package com.dmg.fooddelivery.service;

import com.dmg.fooddelivery.dto.DeliveryDtos.*;
import com.dmg.fooddelivery.dto.PageResponse;

public interface DeliveryService {
    PartnerResponse create(long actorId, PartnerInput input);
    PartnerResponse update(long actorId, long id, PartnerUpdate input);
    PageResponse<PartnerResponse> list(long actorId, int page, int size);
    PartnerResponse me(long actorId);
    PageResponse<AvailableOrder> available(long actorId, int page, int size);
}
