package com.btcc.fix;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacUtils;
import quickfix.*;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.btcc.*;
import quickfix.field.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class DemoClient {

    private static final String ACCESS_KEY = "";
    private static final String SECRET_KEY = "";

    private static class Params {
        public String tonce = "" + (System.currentTimeMillis() * 1000);
        public String accesskey;
        public String requestmethod = "post";
        public String id = "1";
        public String method = "getForwardsAccountInfo";
        public String params = "";

        @Override
        public String toString() {
            return String.format("tonce=%s&accesskey=%s&requestmethod=%s&id=%s&method=%s&params=%s",
                    tonce, accesskey, requestmethod, id, method, params);
        }

        public Params(String accesskey) {
            this.accesskey = accesskey;
        }
    }

    public static String generateAccountString(String accessKey, String secretKey) {
        Params params = new Params(accessKey);
        String hash = HmacUtils.hmacSha1Hex(secretKey, params.toString());
        String userpass = accessKey + ":" + hash;
        String basicAuth = "Basic " + Base64.encodeBase64String(userpass.getBytes());
        return params.tonce + ":" + basicAuth;
    }

    public static void main(String[] args) throws Exception {
        SessionSettings settings = new SessionSettings("quickfix-client.properties");
        MessageFactory messageFactory = new quickfix.btcc.MessageFactory();
        MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();

        DataDictionary dd = new DataDictionary("BTCC-FIX44.xml");

        Initiator initiator = new SocketInitiator(new ApplicationAdapter() {
            @Override
            public void onLogon(SessionID sessionId) {

                // 请求账户信息
                AccountInfoRequest request = new AccountInfoRequest();
                request.set(new AccReqID(UUID.randomUUID().toString()));
                request.set(new Account(generateAccountString(ACCESS_KEY, SECRET_KEY)));


                // 请求市场数据
//                MarketDataRequest request = new MarketDataRequest();
//                request.set(new MDReqID(UUID.randomUUID().toString()));
//                request.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT));
//                request.set(new MarketDepth(5));
////
//                MarketDataRequest.NoRelatedSym symbolGroup = new MarketDataRequest.NoRelatedSym();
//                symbolGroup.set(new Symbol("BTCUSD"));
//                request.addGroup(symbolGroup);
//
//                MarketDataRequest.NoMDEntryTypes typeGroup1 = new MarketDataRequest.NoMDEntryTypes();
//                typeGroup1.set(new MDEntryType(MDEntryType.BID));
//                request.addGroup(typeGroup1);
//
//                MarketDataRequest.NoMDEntryTypes typeGroup2 = new MarketDataRequest.NoMDEntryTypes();
//                typeGroup2.set(new MDEntryType(MDEntryType.OFFER));
//                request.addGroup(typeGroup2);

                // 下单
//                NewOrderSingle request = new NewOrderSingle();
//                request.set(new ClOrdID(UUID.randomUUID().toString()));
//                request.set(new Side(Side.BUY));
//                request.set(new TransactTime());
//                request.set(new OrdType(OrdType.MARKET));
//                request.set(new Account(generateAccountString(ACCESS_KEY, SECRET_KEY)));
//                request.set(new OrderQty(new BigDecimal("1")));
//                request.set(new Symbol("BTCUSD"));

                // 查询单笔订单状态
//                OrderStatusRequest request = new OrderStatusRequest();
//                request.set(new ClOrdID(UUID.randomUUID().toString()));
//                request.set(new Side(Side.BUY));
//                request.set(new Symbol("BTCUSD"));
//                request.set(new OrderID("Your Order ID"));
//                request.set(new Account(generateAccountString(ACCESS_KEY, SECRET_KEY)));

                // 查询多笔订单状态（过去一天中的新建订单）
//                OrderMassStatusRequest2 request = new OrderMassStatusRequest2();
//                request.set(new MassStatusReqID(UUID.randomUUID().toString()));
//                request.set(new Symbol("BTCUSD"));
//                request.set(new Account(generateAccountString(ACCESS_KEY, SECRET_KEY)));
//                request.set(new StartTime(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)));
//                request.set(new EndTime(new Date()));
//
//                OrderMassStatusRequest2.NoStatuses statusGroup = new OrderMassStatusRequest2.NoStatuses();
//                statusGroup.set(new OrdStatus(OrdStatus.NEW));
//                request.addGroup(statusGroup);

                // 修改订单
//                OrderCancelReplaceRequest request = new OrderCancelReplaceRequest();
//                request.set(new ClOrdID(UUID.randomUUID().toString()));
//                request.set(new OrderID("Your Order ID"));
//                request.set(new Account(generateAccountString(ACCESS_KEY, SECRET_KEY)));
//                request.set(new Symbol("BTCUSD"));
//                request.set(new Price(new BigDecimal("4000")));
//                request.set(new OrderQty(new BigDecimal("2"))); // 期望修改成的数量
//                request.set(new OrderQty2(new BigDecimal("1"))); // 原来的数量

                // 取消订单
//                OrderCancelRequest request = new OrderCancelRequest();
//                request.set(new ClOrdID(UUID.randomUUID().toString()));
//                request.set(new Side(Side.BUY));
//                request.set(new TransactTime());
//                request.set(new OrderID("Your Order ID"));
//                request.set(new Account(generateAccountString(ACCESS_KEY, SECRET_KEY)));
//                request.set(new Symbol("BTCUSD"));

                Session.lookupSession(sessionId).send(request);
            }

            @Override
            public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
                FixMessagePrinter.print(dd, message);
            }
        }, messageStoreFactory, settings, messageFactory);


        initiator.block();
    }

}
