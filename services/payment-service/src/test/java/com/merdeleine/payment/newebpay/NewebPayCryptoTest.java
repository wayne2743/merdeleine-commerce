package com.merdeleine.payment.newebpay;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NewebPayCryptoTest {

    private static final String KEY = "12345678901234567890123456789012";
    private static final String IV = "1234567890123456";

    @Test
    void shouldFormEncodeEncryptAndDecryptPayload() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("MerchantID", "MS123456");
        values.put("ItemDesc", "瑪德蓮 x 1");
        values.put("Empty", "");

        String encoded = NewebPayCrypto.formEncode(values);
        String encrypted = NewebPayCrypto.encrypt(encoded, KEY, IV);

        assertThat(encoded).isEqualTo("MerchantID=MS123456&ItemDesc=%E7%91%AA%E5%BE%B7%E8%93%AE+x+1");
        assertThat(encrypted).matches("[0-9a-f]+");
        assertThat(NewebPayCrypto.decrypt(encrypted, KEY, IV)).isEqualTo(encoded);
    }

    @Test
    void shouldGenerateStableTradeShaAndUseConstantTimeComparison() {
        String tradeSha = NewebPayCrypto.tradeSha("abcdef0123456789", KEY, IV);

        assertThat(tradeSha).isEqualTo("1A7E82BD37F9AB10A0EAB3E93545CCDDBDFF41DB363FF2072D0B7B6FB4753519");
        assertThat(NewebPayCrypto.secureEquals(tradeSha, tradeSha)).isTrue();
        assertThat(NewebPayCrypto.secureEquals(tradeSha, tradeSha.substring(1))).isFalse();
    }
}
