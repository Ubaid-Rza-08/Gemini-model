package com.sih.farmer.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
@Data
@Configuration
public class TwilioConfig {

    @Value("${twilio.account_sid}")
    private String accountSid;
    @Value("${twilio.phoneNumber}")
    private String twilioPhoneNumber;
    @Value("${twilio.auth_token}")
    private String authToken;

    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
    }
}
