package org.example.collabo_creator_boot.product.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ProductRegisterDTO {

    private String productName;
    private String productDescription;
    private Integer productPrice;
    private String stock;
    private Long categoryNo;
    private String creatorId;
    private String productStatus;
    private List<String> productImages;

}
