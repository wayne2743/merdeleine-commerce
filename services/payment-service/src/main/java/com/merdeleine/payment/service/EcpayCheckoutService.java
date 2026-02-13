// EcpayCheckoutService.java
package com.merdeleine.payment.service;

import com.merdeleine.payment.ecpay.EcpayCheckMacValue;
import com.merdeleine.payment.ecpay.EcpayProperties;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EcpayCheckoutService {

    private static final DateTimeFormatter ECPAY_DT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final EcpayProperties props;

    public EcpayCheckoutService(EcpayProperties props) {
        this.props = props;
    }

    public String buildAutoSubmitHtml(String orderId, int totalAmount, String itemName) {
        // 你自己的交易號（務必唯一；且符合綠界格式限制）
        String merchantTradeNo = "M" + orderId.replace("-", "").substring(0, 20);

        String returnUrl = props.publicBaseUrl() + "/payments/ecpay/return";
        String orderResultUrl = props.publicBaseUrl() + "/payments/ecpay/result";
        String clientBackUrl = props.publicBaseUrl() + "/"; // 回首頁

        Map<String, String> form = new LinkedHashMap<>();
        form.put("MerchantID", props.merchantId());
        form.put("MerchantTradeNo", merchantTradeNo);
        form.put("MerchantTradeDate", OffsetDateTime.now().format(ECPAY_DT));
        form.put("PaymentType", "aio");
        form.put("TotalAmount", String.valueOf(totalAmount));
        form.put("TradeDesc", "merdeleine order " + orderId);
        form.put("ItemName", itemName);
        form.put("ChoosePayment", "ALL"); // 讓消費者選付款方式:contentReference[oaicite:8]{index=8}
        form.put("ReturnURL", returnUrl);
        form.put("OrderResultURL", orderResultUrl);
        form.put("ClientBackURL", clientBackUrl);
        form.put("EncryptType", "1"); // SHA256:contentReference[oaicite:9]{index=9}

        String checkMac = EcpayCheckMacValue.gen(form, props.hashKey(), props.hashIv());
        form.put("CheckMacValue", checkMac);

        return buildHtml(props.cashierUrl(), form);
    }

    private static String buildHtml(String actionUrl, Map<String, String> form) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body onload='document.forms[0].submit()'>");
        sb.append("<form method='post' action='").append(actionUrl).append("'>");
        for (var e : form.entrySet()) {
            sb.append("<input type='hidden' name='")
                    .append(escape(e.getKey()))
                    .append("' value='")
                    .append(escape(e.getValue()))
                    .append("'/>");
        }
        sb.append("</form></body></html>");
        return sb.toString();
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }
}
