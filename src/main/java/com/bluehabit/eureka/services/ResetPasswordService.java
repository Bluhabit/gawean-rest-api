
/*
 * Copyright © 2023 Blue Habit.
 *
 * Unauthorized copying, publishing of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.bluehabit.eureka.services;

import com.bluehabit.eureka.common.AbstractBaseService;
import com.bluehabit.eureka.common.BaseResponse;
import com.bluehabit.eureka.common.Constant;
import com.bluehabit.eureka.common.MailUtil;
import com.bluehabit.eureka.common.TokenGenerator;
import com.bluehabit.eureka.component.data.UserCredential;
import com.bluehabit.eureka.component.data.UserCredentialRepository;
import com.bluehabit.eureka.component.data.UserVerification;
import com.bluehabit.eureka.component.data.UserVerificationRepository;
import com.bluehabit.eureka.component.model.LinkResetPasswordConfirmationRequest;
import com.bluehabit.eureka.component.model.LinkResetPasswordConfirmationResponse;
import com.bluehabit.eureka.component.model.RequestResetPasswordRequest;
import com.bluehabit.eureka.component.model.ResetPasswordRequest;
import com.bluehabit.eureka.component.VerificationType;
import com.bluehabit.eureka.exception.GeneralErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ResetPasswordService extends AbstractBaseService {
    @Autowired
    private UserVerificationRepository userVerificationRepository;
    @Autowired
    private UserCredentialRepository userCredentialRepository;
    @Autowired
    private MailUtil mailUtil;
    @Autowired
    private TokenGenerator tokenGenerator;

    @Autowired
    private BCryptPasswordEncoder encoder;

    public ResponseEntity<BaseResponse<Map<Object, Object>>> requestResetPassword(RequestResetPasswordRequest request) {
        validate(request);
        return userCredentialRepository.findByEmail(request.email())
            .map(userCredential -> {
                final UserVerification userVerification = new UserVerification();
                userVerification.setUser(userCredential);
                userVerification.setType(VerificationType.RESET);
                userVerification.setToken(TokenGenerator.generateToken());
                userVerificationRepository.save(userVerification);
                final boolean isMailed = mailUtil.sendEmail(
                    userCredential.getEmail(),
                    translate("auth.request.reset.password.subject"),
                    Constant.RESET_PASSWORD_REQUEST_FOLDER,
                    Map.of(
                        "link", String.format("example://gawean/%s", userVerification.getToken()),
                        "user", userCredential.getEmail()
                    ),
                    (success) -> success
                );
                if (!isMailed) {
                    throw new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), translate("auth.user.not.exist"));
                }
                return BaseResponse.success(translate("auth.success"), Map.of());
            })
            .orElseThrow(() -> new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), translate("auth.user.not.exist")));
    }

    public ResponseEntity<BaseResponse<LinkResetPasswordConfirmationResponse>> linkConfirmation(
        LinkResetPasswordConfirmationRequest request
    ) {
        validate(request);
        return userVerificationRepository.findByToken(request.token())
            .map(userVerification -> {
                    return BaseResponse.success(translate("auth.success"),
                        new LinkResetPasswordConfirmationResponse(request.token())
                    );
                }
            )
            .orElseThrow(() -> new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), translate("auth.token.invalid")));
    }

    public ResponseEntity<BaseResponse<Map<Object, Object>>> setNewPassword(String token, ResetPasswordRequest request) {
        validate(request);
        return userVerificationRepository.findByToken(token).map(userVerification -> {
                final UserCredential userCredential = userVerification.getUser();
                userCredential.setPassword(encoder.encode(request.newPassword()));
                userCredentialRepository.save(userCredential);

                final boolean isMailed = mailUtil.sendEmail(
                    userCredential.getEmail(),
                    translate("auth.reset_password.subject"),
                    "reset-password-notification",
                    Map.of("user", "user name"),
                    (success) -> success
                );

                if (!isMailed) {
                    throw new GeneralErrorException(HttpStatus.BAD_REQUEST.value(), translate("auth.invalid"));
                }
                userVerificationRepository.deleteById(userVerification.getId());
                return BaseResponse.success(translate("auth.success"), Map.of());
            })
            .orElseThrow(() -> new GeneralErrorException(HttpStatus.NOT_FOUND.value(), translate("auth.token.invalid")));
    }
}
