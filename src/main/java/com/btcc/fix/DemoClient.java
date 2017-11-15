package com.btcc.fix;

import static quickfix.field.MDEntryType.BID;
import static quickfix.field.MDEntryType.OFFER;
import static quickfix.field.MDEntryType.TRADE;
import static quickfix.field.MDEntryType.TRADE_VOLUME;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.ApplicationAdapter;
import quickfix.ConfigError;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.MemoryStoreFactory;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.UnsupportedMessageType;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.MarketDepth;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.MarketDataRequest.NoMDEntryTypes;
import quickfix.fix44.MarketDataSnapshotFullRefresh;
import quickfix.mina.initiator.AbstractSocketInitiator;

public class DemoClient {

    private static final Logger logger = LoggerFactory.getLogger(DemoClient.class);

    private final MyApplicationAdapter applicationAdapter = new MyApplicationAdapter();
    private final AbstractSocketInitiator initiator;

    private DemoClient() throws ConfigError {
        initiator = getThreadedSocketInitiator();
    }


    public static void main(String[] args) throws Exception {
        new DemoClient().run();
    }

    private void run() throws InterruptedException {
        execute(initiator);

        while (!initiator.isLoggedOn()) {
            synchronized (initiator) {
                logger.info("+++ Waiting for logon");
                initiator.wait(100);
            }
        }

        applicationAdapter.sendMessage();

        while (initiator.isLoggedOn()) {
            synchronized (initiator) {
                initiator.wait(1000);
            }
        }

        logger.info("??? Logged off !!");
    }

    private AbstractSocketInitiator getThreadedSocketInitiator() throws ConfigError {
        SessionSettings settings = new SessionSettings("quickfix-client.properties");
        MessageFactory messageFactory = new quickfix.fix44.MessageFactory();
        MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();

        return new SocketInitiator(applicationAdapter,
            messageStoreFactory,
            settings, messageFactory);
    }

    private static AbstractSocketInitiator execute(final AbstractSocketInitiator initiator) {
        try {

            initiator.start();

            return initiator;
        } catch (ConfigError e) {
            logger.error("error = {}", e, e);
            throw new RuntimeException(e);
        }
    }

    private static MarketDataRequest getMarketDataRequest(String... symbols) {
        // 请求市场数据
        MarketDataRequest request = new MarketDataRequest();
        request.set(new MDReqID(UUID.randomUUID().toString()));
        request.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));
        request.set(new MarketDepth(0));
////
        for (String symbol : symbols) {
            MarketDataRequest.NoRelatedSym symbolGroup = new MarketDataRequest.NoRelatedSym();
            symbolGroup.set(new Symbol(symbol));
            request.addGroup(symbolGroup);
        }

        addType(request, BID);
        addType(request, OFFER);
        addType(request, TRADE);
        addType(request, TRADE_VOLUME);
        return request;
    }

    private static void addType(MarketDataRequest request, char type) {
        NoMDEntryTypes mdEntryTypes = new NoMDEntryTypes();
        mdEntryTypes.set(new MDEntryType(type));
        request.addGroup(mdEntryTypes);
    }

    private static class MyApplicationAdapter extends ApplicationAdapter {

        private volatile SessionID sessionId;

        MyApplicationAdapter() {
            sessionId = null;
        }

        @Override
        public void onLogon(SessionID sessionId) {
            this.sessionId = sessionId;
            logger.info("Logged in [session={}]", sessionId);
        }

        @Override
        public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
//            logger.info("Received fromApp:{}", message.toString());

            if (message instanceof MarketDataSnapshotFullRefresh) {
                MarketDataSnapshotFullRefresh marketDataSnapshot = (MarketDataSnapshotFullRefresh) message;
                logger.info(
                    "MarketData received for [symbol={}, bid={}, offer={}, last-trade={}, 24-hour-volume={}]",
                    marketDataSnapshot.get(new Symbol()),
                    getField(marketDataSnapshot, MDEntryType.BID),
                    getField(marketDataSnapshot, MDEntryType.OFFER),
                    getField(marketDataSnapshot, MDEntryType.TRADE),
                    getField(marketDataSnapshot, MDEntryType.TRADE_VOLUME));
            }
        }

        private String getField(MarketDataSnapshotFullRefresh marketDataSnapshot, char type)
            throws FieldNotFound {
            MarketDataSnapshotFullRefresh.NoMDEntries mdEntries = new MarketDataSnapshotFullRefresh.NoMDEntries();
            for (int i = 1; i <= marketDataSnapshot.getNoMDEntries().getValue(); i++) {
                MarketDataSnapshotFullRefresh.NoMDEntries entry = (MarketDataSnapshotFullRefresh.NoMDEntries) marketDataSnapshot
                    .getGroup(i, mdEntries);
                if (entry.getChar(MDEntryType.FIELD) == type) {
                    return "" + entry.getMDEntryPx().getValue();
                }

            }
            return "NOT-FOUND";
        }

        @Override
        public void toApp(Message message, SessionID sessionId) {
            logger
                .info("Sending toApp:{}", message.toString().replaceAll("\\x01", "|"));
        }

        @Override
        public void fromAdmin(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
            logger
                .info("Received fromAdmin:{}", message.toString().replaceAll("\\x01", "|"));
        }

        public void sendMessage() {
            MarketDataRequest request = getMarketDataRequest("BTCUSD");
            logger
                .info("Sending message:{}", request.toString().replaceAll("\\x01", "|"));
            Session.lookupSession(sessionId).send(request);
        }
    }
}
