package net.zonia3000.ombrachat.events;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

public class SendClientMessage implements Event {

    private final TdApi.Function function;
    private final Client.ResultHandler resultHandler;

    public SendClientMessage(TdApi.Function function, Client.ResultHandler resultHandler) {
        this.function = function;
        this.resultHandler = resultHandler;
    }

    public TdApi.Function getFunction() {
        return function;
    }

    public Client.ResultHandler getResultHandler() {
        return resultHandler;
    }
}
