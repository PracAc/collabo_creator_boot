package org.example.collabo_creator_boot.creatorlogin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.collabo_creator_boot.creatorlogin.dto.CreatorLoginDTO;
import org.example.collabo_creator_boot.creatorlogin.dto.ErrorResponseDTO;
import org.example.collabo_creator_boot.creatorlogin.dto.TokenRequestDTO;
import org.example.collabo_creator_boot.creatorlogin.dto.TokenResponseDTO;
import org.example.collabo_creator_boot.creatorlogin.exception.CreatorLoginException;
import org.example.collabo_creator_boot.creatorlogin.exception.CreatorLoginTaskException;
import org.example.collabo_creator_boot.creatorlogin.service.CreatorLoginService;
import org.example.collabo_creator_boot.security.util.JWTUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/creatorlogin")
@Log4j2
@RequiredArgsConstructor
public class CreatorLoginController {

    private final CreatorLoginService creatorLoginService;

    private final JWTUtil jwtUtil;

    @Value("${org.oz.accessTime}")
    private int accessTime;

    @Value("${org.oz.refreshTime}")
    private int refreshTime;

    @Value("${org.oz.alwaysNew}")
    private boolean alwaysNew;

    @PostMapping("makeToken")
    public ResponseEntity<?> makeToken(@RequestBody @Validated TokenRequestDTO tokenRequestDTO) {
        log.info("Making token");
        log.info("------------------------");

        CreatorLoginDTO creatorLoginDTO = null;
        try {
            creatorLoginDTO = creatorLoginService.authenticate(
                    tokenRequestDTO.getCreatorId(),
                    tokenRequestDTO.getCreatorPassword()
            );

            if (tokenRequestDTO.getCreatorId() == null || tokenRequestDTO.getCreatorId().isEmpty() ||
                    tokenRequestDTO.getCreatorPassword() == null || tokenRequestDTO.getCreatorPassword().isEmpty()) {
                log.error("아이디나 비밀번호가 비어 있거나 null입니다.");
                CreatorLoginTaskException exception = CreatorLoginException.BAD_AUTH.getException();

                return ResponseEntity.status(exception.getStatus()).body(null);
            }
        } catch (Exception e) {
            log.error("authenticate에서 문제가 발생했습니다.: {}", e.getMessage());
            CreatorLoginTaskException exception = CreatorLoginException.BAD_AUTH.getException();
            ErrorResponseDTO errorResponse = new ErrorResponseDTO();
            errorResponse.setStatus(exception.getStatus());
            errorResponse.setMessage(exception.getMessage());
            return ResponseEntity.status(exception.getStatus()).body(errorResponse);
        }

        Map<String, Object> claimMap = new HashMap<>();
        claimMap.put("creatorId", creatorLoginDTO.getCreatorID());

        String accessToken = jwtUtil.createToken(claimMap, accessTime);
        String refreshToken = jwtUtil.createToken(claimMap, refreshTime);

        TokenResponseDTO tokenResponseDTO = new TokenResponseDTO();
        tokenResponseDTO.setAccessToken(accessToken);
        tokenResponseDTO.setRefreshToken(refreshToken);
        tokenResponseDTO.setCreatorId(creatorLoginDTO.getCreatorID());

        log.info("Tokens created successfully for creatorId: {}", creatorLoginDTO.getCreatorID());
        log.info("TOKEN: {}", accessToken);

        return ResponseEntity.ok(tokenResponseDTO);
    }

    @PostMapping(value = "refreshToken",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String accessToken,
                                          @RequestParam String refreshToken) {

        // accessToken 또는 refreshToken이 없는 경우 처리
        if (accessToken == null || refreshToken == null) {
            log.info("Please provide accessToken and refreshToken together.");
            return createErrorResponse(CreatorLoginException.TOKEN_NOT_ENOUGH);
        }

        // accessToken의 형식이 올바르지 않은 경우 처리
        if (!accessToken.startsWith("Bearer ")) {
            log.info("AccessToken has wrong format.");
            return createErrorResponse(CreatorLoginException.ACCESSTOKEN_TOO_SHORT);
        }

        // "Bearer "를 제외한 실제 토큰 부분 추출
        String accessTokenStr = accessToken.substring("Bearer ".length());

        // accessToken 검증
        try {
            Map<String, Object> payload = jwtUtil.validateToken(accessTokenStr);
            String creatorId = payload.get("creatorId").toString();

            TokenResponseDTO tokenResponseDTO = new TokenResponseDTO();
            tokenResponseDTO.setAccessToken(accessTokenStr);
            tokenResponseDTO.setCreatorId(creatorId);
            tokenResponseDTO.setRefreshToken(refreshToken);

            // accessToken 검증 성공 후, refreshToken 검증
            try {
                Map<String, Object> payloadRefresh = jwtUtil.validateToken(refreshToken);
                String refreshAdminId = payloadRefresh.get("creatorId").toString();

                String newAccessToken = null;
                String newRefreshToken = null;

                if (alwaysNew) {
                    Map<String, Object> claimMap = Map.of("creatorId", creatorId);
                    newAccessToken = jwtUtil.createToken(claimMap, accessTime);
                    newRefreshToken = jwtUtil.createToken(claimMap, refreshTime);
                }

                tokenResponseDTO.setAccessToken(newAccessToken);
                tokenResponseDTO.setRefreshToken(newRefreshToken);
                tokenResponseDTO.setCreatorId(creatorId);

                return ResponseEntity.ok(tokenResponseDTO);

            } catch (Exception refreshEx) {
                // refreshToken이 만료되었거나 문제가 있을 경우
                log.error("Refresh Token problem: {}", refreshEx.getMessage());
                return createErrorResponse(CreatorLoginException.REFRESHTOKEN_EXPIRED);
            }

        } catch (Exception accessEx) {
            // accessToken이 잘못되었으면 상태 메시지 반환
            log.error("Access Token problem: {}", accessEx.getMessage());
            try{
                Map<String, Object> payloadRefresh = jwtUtil.validateToken(refreshToken);
                String creatorId = payloadRefresh.get("creatorId").toString();

                // 새 액세스 토큰 발급
                String newAccessToken = jwtUtil.createToken(Map.of("creatorId", creatorId), accessTime);

                TokenResponseDTO tokenResponseDTO = new TokenResponseDTO();
                tokenResponseDTO.setAccessToken(newAccessToken);
                tokenResponseDTO.setRefreshToken(refreshToken);
                tokenResponseDTO.setCreatorId(creatorId);

                return ResponseEntity.ok(tokenResponseDTO);
            } catch (Exception refreshEx) {
                // refreshToken도 만료된 경우
                log.error("Both Access Token and Refresh Token have issues: {}", refreshEx.getMessage());
                return createErrorResponse(CreatorLoginException.REFRESHTOKEN_EXPIRED);
            }
        }
    }

    // ErrorResponse 생성 함수
    private ResponseEntity<ErrorResponseDTO> createErrorResponse(CreatorLoginException exception) {
        return createErrorResponse(exception, exception.getMessage());
    }

    // ErrorResponse 생성 함수 (커스터마이즈된 메시지 포함)
    private ResponseEntity<ErrorResponseDTO> createErrorResponse(CreatorLoginException exception, String customMessage) {
        ErrorResponseDTO errorResponse = new ErrorResponseDTO();
        errorResponse.setStatus(exception.getStatus());
        errorResponse.setMessage(customMessage);
        return ResponseEntity.status(exception.getStatus()).body(errorResponse);
    }

}
