package com.dmg.fooddelivery.delivery;

import static com.dmg.fooddelivery.delivery.DeliveryModels.*;
import com.dmg.fooddelivery.catalog.CatalogRepository;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.common.Page;
import com.dmg.fooddelivery.security.Accounts;
import com.dmg.fooddelivery.security.Actor;
import com.dmg.fooddelivery.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {
    private final DeliveryRepository partners;
    private final CatalogRepository catalog;
    private final Accounts accounts;
    public DeliveryService(DeliveryRepository partners, CatalogRepository catalog, Accounts accounts) {
        this.partners = partners; this.catalog = catalog; this.accounts = accounts;
    }
    @Transactional public Partner create(Actor actor, PartnerInput input) {
        actor.require(Role.ADMIN); catalog.city(input.cityId());
        if (accounts.find(input.userId()).role() != Role.PARTNER) throw ApiException.badRequest("userId must identify a PARTNER");
        return partners.create(input);
    }
    @Transactional public Partner update(Actor actor, long id, PartnerUpdate input) {
        actor.require(Role.ADMIN);
        var partner = partners.get(id, true);
        catalog.city(input.cityId());
        if (partner.activeOrderId() != null && (partner.cityId() != input.cityId() || !input.active())) {
            throw ApiException.conflict("Cannot relocate or deactivate a partner with an active order");
        }
        return partners.update(id, input);
    }
    public Page<AvailableOrder> available(Actor actor, int page, int size) {
        actor.require(Role.PARTNER);
        var partner = partners.byUser(actor.id(), false);
        if (!partner.active()) throw ApiException.conflict("Partner is inactive");
        return partners.available(partner.cityId(), page, size);
    }
}
