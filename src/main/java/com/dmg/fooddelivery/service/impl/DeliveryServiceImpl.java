package com.dmg.fooddelivery.service.impl;

import com.dmg.fooddelivery.common.*;
import com.dmg.fooddelivery.dto.DeliveryDtos.*;
import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.model.*;
import com.dmg.fooddelivery.repository.*;
import com.dmg.fooddelivery.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryServiceImpl implements DeliveryService {
    private final DeliveryPartnerRepository partners;
    private final CityRepository cities;
    private final OrderRepository orders;
    private final UserService users;

    @Override @Transactional public PartnerResponse create(long actorId, PartnerInput input) {
        Access.requireRole(users.get(actorId), Role.ADMIN);
        var user = users.get(input.userId());
        if (user.getRole() != Role.PARTNER) throw ApiException.badRequest("userId must identify a PARTNER");
        var partner = new DeliveryPartner(); partner.setUser(user);
        partner.setCity(city(input.cityId())); partner.setActive(input.active());
        return PartnerResponse.from(partners.save(partner));
    }
    @Override @Transactional public PartnerResponse update(long actorId, long id, PartnerUpdate input) {
        Access.requireRole(users.get(actorId), Role.ADMIN);
        var partner = partners.findLockedById(id).orElseThrow(() -> ApiException.notFound("Delivery partner"));
        if (partner.getActiveOrder() != null && (!partner.getCity().getId().equals(input.cityId()) || !input.active())) {
            throw ApiException.conflict("Cannot relocate or deactivate a partner with an active order");
        }
        partner.setCity(city(input.cityId())); partner.setActive(input.active());
        return PartnerResponse.from(partner);
    }
    @Override public PageResponse<PartnerResponse> list(long actorId, int page, int size) {
        Access.requireRole(users.get(actorId), Role.ADMIN);
        return PageResponse.from(partners.findAll(PageResponse.request(page, size)).map(PartnerResponse::from));
    }
    @Override public PartnerResponse me(long actorId) { return PartnerResponse.from(partnerForUser(actorId)); }
    @Override public PageResponse<AvailableOrder> available(long actorId, int page, int size) {
        var partner = partnerForUser(actorId);
        if (!partner.isActive()) throw ApiException.conflict("Partner is inactive");
        return PageResponse.from(orders.findAvailable(partner.getCity().getId(), PageResponse.request(page, size)).map(AvailableOrder::from));
    }
    private DeliveryPartner partnerForUser(long actorId) {
        Access.requireRole(users.get(actorId), Role.PARTNER);
        return partners.findByUserId(actorId).orElseThrow(() -> ApiException.notFound("Delivery partner profile"));
    }
    private City city(long id) { return cities.findById(id).orElseThrow(() -> ApiException.notFound("City")); }
}
