package org.example.collabo_creator_boot.review.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.collabo_creator_boot.qna.domain.QnAEntity;

@Entity
@Getter
@Table(name = "review_image")
public class ReviewImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_image_no")
    private int reviewImageNo;

    @Column(name = "review_image_url")
    private String reviewImageUrl;

    @Column(name = "review_image_ord")
    private int reviewImageOrd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_no", referencedColumnName = "review_no")
    private ReviewEntity reviewEntity;

}