package net.zonia3000.ombrachat;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

public class ChatsLoader {

    private final ConcurrentMap<Integer, String> chatFolders = new ConcurrentHashMap<>();

    private final ConcurrentMap<Long, TdApi.Chat> chats = new ConcurrentHashMap<>();
    private final NavigableSet<OrderedChat> mainChatList = new TreeSet<>();
    private boolean haveFullMainChatList = false;

    private final Client client;

    private Consumer<TdApi.ChatFolderInfo[]> chatFoldersConsumer;

    private final Object chatFoldersLock = new Object();
    private boolean chatFoldersBoxReady = false;
    private TdApi.ChatFolderInfo[] chatFoldersInfo = null;

    public void setChatFoldersConsumer(Consumer<TdApi.ChatFolderInfo[]> chatFoldersConsumer) {
        this.chatFoldersConsumer = chatFoldersConsumer;
    }

    public ChatsLoader(Client client) {
        this.client = client;
    }

    public void loadChats() {
        getMainChatList(20);
    }

    public boolean onResult(TdApi.Object object) {
        if (object instanceof TdApi.UpdateChatFolders update) {
            synchronized (chatFoldersLock) {
                chatFoldersInfo = update.chatFolders;
                if (chatFoldersBoxReady) {
                    chatFoldersConsumer.accept(chatFoldersInfo);
                }
            }
            return true;
        }
        return false;
    }

    public void onChatFoldersBoxReady(Consumer<TdApi.ChatFolderInfo[]> chatFoldersConsumer) {
        synchronized (chatFoldersLock) {
            chatFoldersBoxReady = true;
            if (chatFoldersInfo != null) {
                chatFoldersConsumer.accept(chatFoldersInfo);
            }
        }
    }

    private void getMainChatList(final int limit) {
        synchronized (mainChatList) {
            if (!haveFullMainChatList && limit > mainChatList.size()) {
                // send LoadChats request if there are some unknown chats and have not enough known chats
                client.send(new TdApi.LoadChats(new TdApi.ChatListMain(), limit - mainChatList.size()), new Client.ResultHandler() {
                    @Override
                    public void onResult(TdApi.Object object) {
                        switch (object.getConstructor()) {
                            case TdApi.Error.CONSTRUCTOR:
                                if (((TdApi.Error) object).code == 404) {
                                    synchronized (mainChatList) {
                                        haveFullMainChatList = true;
                                    }
                                } else {
                                    System.err.println("Receive an error for LoadChats:\n" + object);
                                }
                                break;
                            case TdApi.Ok.CONSTRUCTOR:
                                // chats had already been received through updates, let's retry request
                                getMainChatList(limit);
                                break;
                            default:
                                System.err.println("Receive wrong response from TDLib:\n" + object);
                        }
                    }
                });
                return;
            }

            java.util.Iterator<OrderedChat> iter = mainChatList.iterator();
            System.out.println();
            System.out.println("First " + limit + " chat(s) out of " + mainChatList.size() + " known chat(s):");
            for (int i = 0; i < limit && i < mainChatList.size(); i++) {
                long chatId = iter.next().chatId;
                TdApi.Chat chat = chats.get(chatId);
                synchronized (chat) {
                    System.out.println(chatId + ": " + chat.title);
                }
            }
        }
    }

    private static class OrderedChat implements Comparable<OrderedChat> {

        final long chatId;
        final TdApi.ChatPosition position;

        OrderedChat(long chatId, TdApi.ChatPosition position) {
            this.chatId = chatId;
            this.position = position;
        }

        @Override
        public int compareTo(OrderedChat o) {
            if (this.position.order != o.position.order) {
                return o.position.order < this.position.order ? -1 : 1;
            }
            if (this.chatId != o.chatId) {
                return o.chatId < this.chatId ? -1 : 1;
            }
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            OrderedChat o = (OrderedChat) obj;
            return this.chatId == o.chatId && this.position.order == o.position.order;
        }
    }
}
