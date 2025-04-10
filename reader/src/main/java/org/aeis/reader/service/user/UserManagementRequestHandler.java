package org.aeis.reader.service.user;


import org.aeis.reader.cache.TokenCache;
import org.aeis.reader.cache.UserSessionCache;
import org.aeis.reader.dto.TokenInfoDto;
import org.aeis.reader.dto.UserSessionDto;
import org.aeis.reader.dto.otpdto.ReaderVerifyOtpRequest;
import org.aeis.reader.dto.otpdto.VerifyOtpRequest;
import org.aeis.reader.dto.userdto.LoginResponse;
import org.aeis.reader.dto.otpdto.OtpRequest;
import org.aeis.reader.dto.userdto.UserDTO;
import org.aeis.reader.dto.userdto.UserLoginRequest;
import org.aeis.reader.service.handler.UrlServiceLocator;
import org.aeis.reader.util.ValidateTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.logging.Logger;

@Service
public class UserManagementRequestHandler {

  private final Logger log = Logger.getLogger(UserManagementRequestHandler.class.getName());

    @Autowired
    private UrlServiceLocator urlServiceLocator;

    @Autowired
    private UserSessionCache userSessionCache;


    @Autowired
    private TokenCache tokenCache;

    @Autowired
    private ValidateTokenService validateTokenService;
    @Autowired
    private RestTemplate restTemplate;

   public  ResponseEntity<LoginResponse> redirectLoginRequest(UserLoginRequest loginRequest) {
       try {
           ResponseEntity<LoginResponse> response =  restTemplate.postForEntity(urlServiceLocator.getLoginServiceUrl(), loginRequest, LoginResponse.class);
           return response;

       }catch (Exception e) {
           log.info("Error while authenticating user: {}");
           return ResponseEntity.badRequest().body(new LoginResponse("not found", "", "User not found"));
       }

    }


        public ResponseEntity<?> redirectUserInfoRequest(Long userId, String token) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            HttpEntity entity = new HttpEntity<>(headers);
            ResponseEntity<?> response = null;
            try {
                return restTemplate.exchange(
                        urlServiceLocator.getUserInfoServiceUrl(userId),
                        HttpMethod.GET,
                        entity,
                        Object.class
                );
            } catch (HttpClientErrorException e) {
                log.info("Client error while calling User Management service: {}");
                return ResponseEntity.status(e.getStatusCode()).body("Access Denied: " + e.getMessage());
            } catch (HttpServerErrorException e) {
                log.info("Server error from User Management service: {}");
                return ResponseEntity.status(e.getStatusCode()).body("User Management service error: " + e.getMessage());
            } catch (ResourceAccessException e) {
                log.info("Failed to reach User Management service: {}");
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Service Unavailable: " + e.getMessage());
            } catch (Exception e) {
                log.info("Unexpected error while calling User Management service: {}");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error: " + e.getMessage());
            }

        }

        public ResponseEntity<String> redirectOtpGenerationRequest( String token) {

            try {
               boolean isTokenValid = validateTokenService.checkTokenValidity(token);

               if (!isTokenValid)
                   return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Token");

               OtpRequest otpRequest = buildOtpRequest(token);

                ResponseEntity<String> response =  restTemplate.postForEntity(urlServiceLocator.getGenerateOtpServiceUrl(), otpRequest, String.class);

                return response;
            } catch (HttpClientErrorException e) {
                log.info("Client error while calling User Management service: {}");
                return ResponseEntity.status(e.getStatusCode()).body("Access Denied: " + e.getMessage());
            } catch (HttpServerErrorException e) {
                log.info("Server error from User Management service: {}");
                return ResponseEntity.status(e.getStatusCode()).body("User Management service error: " + e.getMessage());
            } catch (ResourceAccessException e) {
                log.info("Failed to reach User Management service: {}");
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Service Unavailable: " + e.getMessage());
            } catch (Exception e) {
                log.info("Unexpected error while calling User Management service: {}");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error: " + e.getMessage());
            }
        }

    private OtpRequest buildOtpRequest(String token) {
       OtpRequest otpRequest = new OtpRequest();
        TokenInfoDto tokenInfo = tokenCache.getTokenFromCache(token);
        UserDTO userDTO = userSessionCache.getUserFromSessionCache(tokenInfo);
        otpRequest.setEmail(userDTO.getEmail());
        otpRequest.setFirstName(userDTO.getFirstName());

        return otpRequest;
    }



    public ResponseEntity<String> redirectOtpVerificationRequest(ReaderVerifyOtpRequest otpRequest , String token)  {
        try{

            VerifyOtpRequest verifyOtpRequest = buildOtpVerificationRequest(token , otpRequest.getOtp());
            if (verifyOtpRequest == null)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Session");
            ResponseEntity<String> response =  restTemplate.postForEntity(urlServiceLocator.getVerifyOtpServiceUrl(), verifyOtpRequest, String.class);

            return response;
        } catch (HttpClientErrorException e) {
            log.info("Client error while calling User Management service: {}");
            return ResponseEntity.status(e.getStatusCode()).body("Access Denied: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            log.info("Server error from User Management service: {}");
            return ResponseEntity.status(e.getStatusCode()).body("User Management service error: " + e.getMessage());
        } catch (ResourceAccessException e) {
            log.info("Failed to reach User Management service: {}");
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Service Unavailable: " + e.getMessage());
        } catch (Exception e) {
            log.info("Unexpected error while calling User Management service: {}");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error: " + e.getMessage());
        }

    }

    private VerifyOtpRequest buildOtpVerificationRequest(String token , String otp) {

        try {
            UserDTO userDTO = userSessionCache.getUserFromSessionCache(tokenCache.getTokenFromCache(token));
            VerifyOtpRequest verifyOtpRequest = new VerifyOtpRequest();
            verifyOtpRequest.setEmail(userDTO.getEmail());
            verifyOtpRequest.setOtp(otp);
            return verifyOtpRequest;
        } catch (Exception e) {
            log.info("User DTO Is Empty  {}");
            e.printStackTrace();
            return null;


        }
    }
}
