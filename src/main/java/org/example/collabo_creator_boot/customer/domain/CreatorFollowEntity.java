package org.example.collabo_creator_boot.customer.domain;

import jakarta.persistence.*;
import org.example.collabo_creator_boot.creator.domain.CreatorEntity;

@Entity
@Table(name = "creator_follow")
public class CreatorFollowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "creator_follow_no", nullable = false)
    private Long creatorFollowNo;

    @Column(name = "follow_status")
    private Boolean followStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", referencedColumnName = "creator_id")
    private CreatorEntity creatorEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id")
    private CustomerEntity customerEntity;
}
