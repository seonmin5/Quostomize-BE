package com.quostomize.quostomize_be.domain.customizer.stock.service;

import com.quostomize.quostomize_be.api.stock.dto.*;
import com.quostomize.quostomize_be.common.error.ErrorCode;
import com.quostomize.quostomize_be.common.error.exception.AppException;
import com.quostomize.quostomize_be.common.s3.S3Service;
import com.quostomize.quostomize_be.domain.customizer.cardBenefit.entity.CardBenefit;
import com.quostomize.quostomize_be.domain.customizer.cardBenefit.repository.CardBenefitRepository;
import com.quostomize.quostomize_be.domain.customizer.customer.entity.Customer;
import com.quostomize.quostomize_be.domain.customizer.customer.repository.CustomerRepository;
import com.quostomize.quostomize_be.domain.customizer.stock.common.*;
import com.quostomize.quostomize_be.domain.customizer.stock.entity.StockInformation;
import com.quostomize.quostomize_be.domain.customizer.stock.entity.StockInterest;
import com.quostomize.quostomize_be.domain.customizer.stock.repository.StockInformationRepository;
import com.quostomize.quostomize_be.domain.customizer.stock.repository.StockInterestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockInterestService {

    private final StockInterestRepository stockInterestRepository;
    private final S3Service s3Service;
    private final CardBenefitRepository cardBenefitRepository;
    private final StockInformationRepository stockInformationRepository;
    private final CustomerRepository customerRepository;

    // 위시리스트를 조회합니다.
    public List<StockInterestDto> getStockWishList(Long cardId) {
        // Step 1: Query로 Object[] 결과 받기
        List<Object[]> results = stockInterestRepository.findAllStockInterestDto(cardId);

        // Step 2: Object[] 결과를 StockInterestDto로 수동 변환
        List<StockInterestDto> allStockInterestDto = new ArrayList<>();
        for (Object[] result : results) {
            Integer priority = (Integer) result[0];
            String stockName = (String) result[1];
            Integer stockPresentPrice = (Integer) result[2];
            String stockImage = (String) result[3];

            // Step 3: DTO 객체 생성 후 리스트에 추가
            StockInterestDto dto = new StockInterestDto(priority, stockName, stockPresentPrice, stockImage);
            allStockInterestDto.add(dto);
        }
        List<StockInterestDto> collect = allStockInterestDto.stream()
                .map(stock -> {
                    // stockImage로 presigned URL을 생성
                    String preSignedUrl = s3Service.getPreSignedUrl(stock.stockImage());
                    // stock DTO를 수정하여 새로운 객체를 반환
                    return stock.withStockImage(preSignedUrl); // 새로운 객체 반환
                })
                .collect(Collectors.toList());
        return collect;
    }

    // 위시리스트 중 해당 id에 해당하는 특정항목을 제거합니다.
    @Transactional
    public void deleteStock(int order, Long cardId) {

        if (order == 1) {
            stockInterestRepository.DeleteStockById(order,cardId);
            stockInterestRepository.switchStockDeleteOrder1(cardId);
        } else {
            stockInterestRepository.DeleteStockById(order,cardId);
            stockInterestRepository.switchStockDeleteOrder2(cardId);
        }
    }

    // 위시리스트 중 해당 id에 해당 특정항목을 특정 순서(order)로 변경합니다.
    @Transactional
    public void switchStock(List<StockInterestRequestDto> stocks) {
        stocks.forEach(stock -> {
            if (stock.currentOrder() == 3) {
                if (stock.editOrder() == 1) { // 현순위 3순위에서 1순위로 바꾸는 경우,
                    stockInterestRepository.switchStock1(3, stock.cardId());
                } else { // 현순위 3순위 여기서 2순위로 바꾸는 경우
                    stockInterestRepository.switchStock3(3, stock.cardId());
                }
            } else if (stock.currentOrder() == 2) {
                if (stock.editOrder() == 1) { //2순위를 1순위로
                    stockInterestRepository.switchStock2(2, stock.cardId());
                } else { // 2순위를 3순위로
                    stockInterestRepository.switchStock4(2, stock.cardId());
                }
            } else if (stock.currentOrder() == 1) {
                if (stock.editOrder() == 2) { // 1순위를 2순의로 변경한다.
                    stockInterestRepository.switchStock5(1, stock.cardId());
                } else { // 1순위를 3순위로 변경한다.
                    stockInterestRepository.switchStock6(1, stock.cardId());
                }
            }
        });
    }


    @Transactional
    public List<StockRecommendResponse> getCardBenefit(Long cardId, Boolean isRecommendByCardBenefit) {

//        String preSignedUrl = s3Service.getPreSignedUrl("image/quokka.png");  // default 이미지 링크
        List<StockRecommendResponse> recommendResponses = new ArrayList<>(); // 추천 종목을 보관할 배열
        AcquiredCardBenefits cardBenefitDto = new AcquiredCardBenefits(); // 카드 혜택을 DTo 형식으로 저장할 수 있게 하는 메서드를 가진 객체
        ComputeEntry computeEntry = new ComputeEntry(); // 조회한 배열에서 적립률이 높은 순서대로 정렬 시키는 메서드를 가진 객체
        CategorizingBenefits categorizingBenefits = new CategorizingBenefits(); // 상위 분류와 하위 분류 배열을 따로 저장시키는 메서드를 가진 객체
        UpperClassfication upperClassfication = new UpperClassfication(); // 상위분류만 있을 떄, 종목 추천을 해주는 메서드를 가진 객체
        LowerClassfication lowerClassfication = new LowerClassfication(); // 하위분류만 있을 때, 종목 추천을 해주는 메서드를 가진 객체
        // 상위, 하윈 분류를 동시에 가질 때, 만일 2개 이상 항목을 가진 배열은 서로 비교하고,1개 이하 배열은 비교과정 없이 단일 배열을 나타내는 메서드를 가진 객체
        ComputeWithLowerAndUpper computeWithLowerAndUpper = new ComputeWithLowerAndUpper();
        // 상위, 하위 분류를 동시에 가질 떄, 종목 추천을 해주는 메서드를 가진 객체
        UpperAndLowerClassification upperAndLowerClassification =new UpperAndLowerClassification();

        // 카드 아이디 예외처리
        if (cardId == null) {
            throw new AppException(ErrorCode.ENTITY_NOT_FOUND,new Throwable("카드아이디가 유효하지 않습니다."));
        }

        // 카드 혜택 기반 주식 추천
        if (isRecommendByCardBenefit) {

            // 카드 혜택 조회
            List<CardBenefit> cardBenefits = cardBenefitRepository.findByCardDetail_CardSequenceId(cardId);
            // 카드 혜택 ResponseDto로 변환
            List<CardBenefitResponse> benefits = cardBenefitDto.getCardBenefitDto(cardBenefits);
            // 상위 분류 와 하위 분류 배열 및 개수 정렬
            categorizingBenefits.getListHashMap(benefits);

            // 혜택 적립률이 가장 높은 3가지를 선정하여, 하위분류 혹은 상위분류 주식 추천
            computeWithLowerAndUpper.computeLower(categorizingBenefits.getLowerName(), computeEntry); // 하위분류 정렬 - 혜택적립률이 높은 순서대로 정렬 단, 단일 배열의 경우 비교로직을 제외
            computeWithLowerAndUpper.computeUpper(categorizingBenefits.getUpperName(), computeEntry); // 상위분류 정렬 - 혜택적립률이 높은 순서대로 정렬 단, 단일 배열의 경우 비교로직을 제외

            if (categorizingBenefits.getUpperNumber() == 5) { // 상위 분류만 5개가 있을 떄,
                // 상위분류만 정렬한 배열을 적립률이 높은 순서대로 재정렬 , 이때 적립률이 동일한 경우 동일 적립률 객체간의 랜덤 위치 정렬을 진행
                upperClassfication.addRecommendList(computeWithLowerAndUpper,categorizingBenefits.getUpperName(), stockInformationRepository, recommendResponses,s3Service); // 해당 상위분류에 맞는 종목을 추천 - recommendResponses에 저장

            } else if (categorizingBenefits.getLowerNumber() == 5) { // 하위 분류만 5개가 있을 떄,
                // 하위분류만 정렬한 배열을 적립률이 높은 순서대로 재정렬, 이떄 적립률이 동일한 경우 동일 적립률 객체간의 랜덤 위치 정렬을 진행
                    lowerClassfication.addRecommendList(computeEntry,categorizingBenefits.getLowerName(), stockInformationRepository, recommendResponses, s3Service); // 해당 상위분류에 맞는 종목을 추천 - recommendResponses에 저장
            } else { // 상위 하위 분류가 동시에 존재할 떄,
                // 상위 정렬과 하위 정렬의 항목간 적립률 비교 후, 가장 높은 쪽의 테마주식을 추천 진행
                upperAndLowerClassification.computeByAll(stockInformationRepository, s3Service, recommendResponses, computeWithLowerAndUpper);
                }
            }
         else { // 결제 내역 기반 주식 추천
        }
        return recommendResponses; // 추천종목 리턴
    }



    @Transactional
    public void saveStockToStockInterest(Long memberId, String stockName){
        StockInformation stockInformation = findStockInformation(stockName);
        Customer customer = findCustomerWithLock(memberId);
        validateStockInterestLimit(new StockAddInterest(customer, customer.getStockInterests().size())); //관심주식이 3개 이상일때 막는 로직
        int nextPriority = calculateNextPriority(customer);
        StockInterest stockInterest = createStockInterest(nextPriority, stockInformation, customer);
        saveStockInterest(customer, stockInterest);
    }

    private StockInformation findStockInformation(String stockName) {
        return stockInformationRepository.findByStockName(stockName)
                .orElseThrow(() -> new EntityNotFoundException("해당 주식 정보를 찾을 수 없음"));
    }

    private Customer findCustomerWithLock(Long memberId) {
        return customerRepository.findCustomerWithLock(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
    }

    private void validateStockInterestLimit(StockAddInterest stockAddInterest) {
        if (stockAddInterest.stockInterestCount() >= 3) {
            throw new AppException(ErrorCode.MORE_THAN_LIMIT);
        }
    }

    private StockInterest createStockInterest(int priority, StockInformation stockInformation, Customer customer) {
        return StockInterest.builder()
                .priority(priority)
                .stockInformation(stockInformation)
                .customer(customer)
                .build();
    }

    private int calculateNextPriority(Customer customer) {
        return customer.getStockInterests().stream()
                .mapToInt(StockInterest::getPriority)
                .max()
                .orElse(0) + 1;
    }

    private void saveStockInterest(Customer customer, StockInterest stockInterest) {
        customer.addStockInterest(stockInterest);
        stockInterestRepository.save(stockInterest);
    }
}