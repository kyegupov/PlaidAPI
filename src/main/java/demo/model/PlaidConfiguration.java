package demo.model;

import com.plaid.client.PlaidClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class PlaidConfiguration {

    @Value("${PLAID_CLIENT_ID ? : *** }")
    private String plaidClientId;

    @Value("${PLAID_SECRET ?: *** }")
    private String plaidSecret;

    @Value("${PLAID_PUBLIC_KEY ?: *** }")
    private String plaidPublicKey;

    @Value("#{systemProperties['PLAID_ENV'] ?: 'sandbox'}")
    private String plaidEnv;


    @Bean
    public PlaidClient plaidClient() {
        PlaidClient plaidClient = PlaidClient.newBuilder()
                .clientIdAndSecret(plaidClientId, plaidSecret)
                .publicKey(plaidPublicKey) // optional. only needed to call endpoints that require a public key
                .sandboxBaseUrl() // or equivalent, depending on which environment you're calling into
                .build();

        return plaidClient;
    }
}
