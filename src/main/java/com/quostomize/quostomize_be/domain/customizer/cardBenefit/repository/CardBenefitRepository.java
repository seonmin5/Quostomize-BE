package com.quostomize.quostomize_be.domain.customizer.cardBenefit.repository;

import com.quostomize.quostomize_be.domain.customizer.cardBenefit.entity.CardBenefit;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Set;

@Repository
public interface CardBenefitRepository extends JpaRepository<CardBenefit, Long> {
    Set<CardBenefit> findCardBenefitsByCardDetailCardSequenceIdAndIsActive(Long cardId, boolean isActive);
}