package com.erp.inventory.service.impl;

import com.erp.inventory.dto.response.PageResponse;
import com.erp.inventory.repository.spec.TextSearchSpec;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.MaterialStockMovementRequest;
import com.erp.inventory.dto.response.MaterialStockMovementResponse;
import com.erp.inventory.entity.Material;
import com.erp.inventory.entity.MaterialStockMovement;
import com.erp.inventory.entity.Party;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.MaterialRepository;
import com.erp.inventory.repository.MaterialStockMovementRepository;
import com.erp.inventory.repository.PartyRepository;
import com.erp.inventory.service.attributes.AttributeValidator;
import com.erp.inventory.service.MaterialStockMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MaterialStockMovementServiceImpl implements MaterialStockMovementService {

    private final MaterialStockMovementRepository movementRepository;
    private final AttributeValidator attributeValidator;
    private final MaterialRepository materialRepository;
    private final PartyRepository partyRepository;

    @Override
    @Transactional
    public MaterialStockMovementResponse create(MaterialStockMovementRequest request) {
        Material material = materialRepository.findOneById(request.getMaterialId())
                .orElseThrow(() -> ResourceNotFoundException.of("Material", request.getMaterialId()));
        Party party = partyRepository.findOneById(request.getPartyId())
                .orElseThrow(() -> ResourceNotFoundException.of("Party", request.getPartyId()));

        MaterialStockMovement movement = MaterialStockMovement.builder()
                .material(material)
                .party(party)
                .direction(request.getDirection())
                .date(request.getDate())
                .quantity(request.getQuantity())
                .invoiceNo(request.getInvoiceNo())
                .lotNo(request.getLotNo())
                .price(request.getPrice())
                .attributes(attributeValidator.validate("materialMovements", request.getAttributes(), null))
                .build();

        return toResponse(movementRepository.save(movement));
    }

    @Override
    public List<MaterialStockMovementResponse> getAll() {
        return movementRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public PageResponse<MaterialStockMovementResponse> getPage(Pageable pageable, String q) {
        return PageResponse.of(movementRepository.findAll(TextSearchSpec.contains(q, "invoiceNo", "lotNo"), pageable).map(this::toResponse));
    }

    @Override
    public List<MaterialStockMovementResponse> getByMaterial(Long materialId) {
        return movementRepository.findByMaterialIdOrderByDateDesc(materialId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public BigDecimal availableQuantity(Long materialId) {
        BigDecimal onHand = movementRepository.sumSignedQuantityByMaterialId(materialId);
        return onHand != null ? onHand : BigDecimal.ZERO;
    }

    @Override
    public Map<Long, BigDecimal> availableQuantities(Collection<Long> materialIds) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        if (materialIds == null || materialIds.isEmpty()) {
            return result;
        }
        for (Long id : materialIds) {
            result.put(id, BigDecimal.ZERO);
        }
        for (Object[] row : movementRepository.sumSignedQuantityGrouped(materialIds)) {
            Long id = ((Number) row[0]).longValue();
            BigDecimal signed = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            result.put(id, signed);
        }
        return result;
    }

    private MaterialStockMovementResponse toResponse(MaterialStockMovement movement) {
        return MaterialStockMovementResponse.builder()
                .id(movement.getId())
                .materialId(movement.getMaterial().getId())
                .materialCode(movement.getMaterial().getCode())
                .partyId(movement.getParty().getId())
                .partyName(movement.getParty().getName())
                .direction(movement.getDirection())
                .date(movement.getDate())
                .quantity(movement.getQuantity())
                .invoiceNo(movement.getInvoiceNo())
                .lotNo(movement.getLotNo())
                .price(movement.getPrice())
                .attributes(movement.getAttributes())
                .build();
    }
}
