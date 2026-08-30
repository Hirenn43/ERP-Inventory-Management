package com.erp.inventory.repository;

import com.erp.inventory.entity.MaterialStockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface MaterialStockMovementRepository extends JpaRepository<MaterialStockMovement, Long> {
    List<MaterialStockMovement> findByMaterialIdOrderByDateDesc(Long materialId);

    /** On-hand quantity for one material: Σ(IN) − Σ(OUT). Null when the material has no movements. */
    @Query("""
            select coalesce(sum(case when m.direction = com.erp.inventory.entity.enums.MovementDirection.IN
                                     then m.quantity else -m.quantity end), 0)
            from MaterialStockMovement m
            where m.material.id = :materialId
            """)
    BigDecimal sumSignedQuantityByMaterialId(@Param("materialId") Long materialId);

    /** Batch form of {@link #sumSignedQuantityByMaterialId} - each row is [materialId, signedQty]. */
    @Query("""
            select m.material.id,
                   coalesce(sum(case when m.direction = com.erp.inventory.entity.enums.MovementDirection.IN
                                     then m.quantity else -m.quantity end), 0)
            from MaterialStockMovement m
            where m.material.id in :materialIds
            group by m.material.id
            """)
    List<Object[]> sumSignedQuantityGrouped(@Param("materialIds") Collection<Long> materialIds);
}
