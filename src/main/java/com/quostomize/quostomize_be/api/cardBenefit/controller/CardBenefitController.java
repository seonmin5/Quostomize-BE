package com.quostomize.quostomize_be.api.cardBenefit.controller;

import com.quostomize.quostomize_be.api.cardBenefit.dto.CardBenefitRequest;
import com.quostomize.quostomize_be.api.cardBenefit.dto.CardBenefitResponse;
import com.quostomize.quostomize_be.common.dto.ResponseDTO;
import com.quostomize.quostomize_be.domain.customizer.cardBenefit.entity.CardBenefit;
import com.quostomize.quostomize_be.domain.customizer.cardBenefit.repository.CardBenefitRepository;
import com.quostomize.quostomize_be.domain.customizer.cardBenefit.service.CardBenefitService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/benefit-change")
@RequiredArgsConstructor
public class CardBenefitController {
    private final CardBenefitService cardBenefitService;
    private final CardBenefitRepository cardBenefitRepository;

    @GetMapping()
    @Operation(summary = "카드 혜택 내역 조회", description = "로그인한 고객의 현재 적용된 카드 혜택을 조회합니다.")
    public ResponseEntity<ResponseDTO> getCardBenefitStatus() {
        List<CardBenefitResponse> benefits = cardBenefitService.findAll();
        return ResponseEntity.ok(new ResponseDTO<>(benefits));
    }

    @GetMapping("/{cardSequenceId}")
    @Operation(summary = "카드 혜택 변경 가능여부 일자 계산", description = "페이지 마운트 시 카드 혜택 변경 가능여부 일자를 계산하여 '변경' 혹은 '예약' 로직을 안내합니다.")
    public ResponseEntity<ResponseDTO> getBenefitChangeDate(@PathVariable Long cardSequenceId) {
        CardBenefit cardBenefit = cardBenefitRepository.findCardBenefitsByCardDetailCardSequenceIdAndIsActive(cardSequenceId, true)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("적용된 혜택 없음 - 카드: " + cardSequenceId));
        String buttonLabel = cardBenefitService.getBenefitChangeButtonLabel(cardBenefit);
        return ResponseEntity.ok(new ResponseDTO<>(buttonLabel));
    }

    @PatchMapping()
    @Operation(summary = "카드 혜택 상세 변경", description = "선택한 항목을 반영하여 카드 혜택을 변경합니다.")
    public ResponseEntity<Void> updateCardBenefitStatus(@RequestBody List<CardBenefitRequest> requests) {
        cardBenefitService.updateCardBenefits(requests);
        return ResponseEntity.ok().build();
    }

     @PatchMapping("/reserve")
     @Operation(summary = "카드 혜택 변경 예약", description = "변경 가능일 이전에 요청한 사항은 혜택적용일에 자동으로 적용됩니다.")
    public ResponseEntity<Void> reserveCardBenefits(@RequestBody List<CardBenefitRequest> requests) {
        cardBenefitService.reserveCardBenefits(requests);
        return ResponseEntity.ok().build();
     }
}