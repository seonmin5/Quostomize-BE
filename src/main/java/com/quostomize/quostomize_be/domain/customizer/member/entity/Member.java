package com.quostomize.quostomize_be.domain.customizer.member.entity;

import com.quostomize.quostomize_be.common.entity.BaseTimeEntity;
import com.quostomize.quostomize_be.domain.customizer.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "members")
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "member_name", length = 20, nullable = false)
    private String memberName;

    @Column(name = "member_email", length = 50, nullable = false)
    private String memberEmail;

    @Column(name = "member_login_id", length = 15, nullable = false)
    private String memberLoginId;

    @Column(name = "member_password", length = 16, nullable = false)
    private String memberPassword;

    @Column(name = "residence_number", length = 13, nullable = false)
    private String residenceNumber;

    @Column(name = "zip_code", length = 20, nullable = false)
    private String zipCode;

    @Column(name = "member_address", nullable = false)
    private String memberAddress;

    @Column(name = "member_detail_address")
    private String memberDetailAddress;

    @Column(name = "member_phone_number", length = 20, nullable = false)
    private String memberPhoneNumber;

    @Column(name = "secondary_auth_code", length = 6, nullable = false)
    private String secondaryAuthCode;
}