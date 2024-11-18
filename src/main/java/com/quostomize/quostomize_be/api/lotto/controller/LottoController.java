package com.quostomize.quostomize_be.api.lotto.controller;

import com.quostomize.quostomize_be.api.lotto.dto.LottoParticipantRequestDto;
import com.quostomize.quostomize_be.api.lotto.dto.LottoParticipantResponseDto;
import com.quostomize.quostomize_be.common.dto.ResponseDTO;
import com.quostomize.quostomize_be.domain.customizer.lotto.service.LottoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LottoController {

    private final LottoService lottoService;

    @PostMapping("/lottery")
    @Operation(summary = "로또 참여자 테이블에 등록", description = "자동으로 로또 참여 조건에 맞는 참여자들을 오늘 참여자 테이블에 등록합니다.")
    public ResponseEntity<ResponseDTO<List<LottoParticipantResponseDto>>> registerLottoParticipants() {
        List<LottoParticipantResponseDto> participantResponseDtos = lottoService.registerLottoParticipants();
        return ResponseEntity.ok(new ResponseDTO<>(participantResponseDtos));
    }

    @PostMapping("/my-card/change-lotto")
    @Operation(summary = "로또 참여 설정 변경", description = "나의 카드페이지에서 로또 참여 여부를 ON/OFF 시 참여자 수가 변경됩니다.")
    public ResponseEntity<String> toggleLottoParticipation(@Valid @RequestBody LottoParticipantRequestDto request) {
        lottoService.toggleLottoParticipation(request);
        return ResponseEntity.ok("로또 참여 설정이 변경되었습니다.");
    }

    @GetMapping("/lottery")
    @Operation(summary = "총 로또 참여자 수 조회", description = "현재 로또에 참여한 총 참여자 수를 반환합니다.")
    public ResponseEntity<Long> getTotalLottoParticipantsCount() {
        Long totalParticipants = lottoService.getTotalLottoParticipantsCount();
        return ResponseEntity.ok(totalParticipants);
    }
}
