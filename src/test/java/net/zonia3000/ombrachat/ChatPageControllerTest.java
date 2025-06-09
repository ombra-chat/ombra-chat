package net.zonia3000.ombrachat;

import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.zonia3000.ombrachat.chat.message.MessageBubble;
import net.zonia3000.ombrachat.controllers.ChatPageController;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.MessagesService;
import org.drinkless.tdlib.TdApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class ChatPageControllerTest {

    @Mock
    private VBox container;
    @Mock
    private VBox chatContent;
    @Mock
    private HBox newMessagesBox;
    @Mock
    private MessagesService messageService;
    @Mock
    private ChatsService chatsService;

    private ChatPageController controller;

    private ObservableList<Node> childrenList;

    @BeforeAll
    public static void before() {
        Platform.startup(() -> {
        });
    }

    @AfterAll
    public static void after() {
        Platform.exit();
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        ServiceLocator.registerService(MessagesService.class, messageService);
        ServiceLocator.registerService(ChatsService.class, chatsService);

        when(messageService.getSenderTitle(any(TdApi.MessageSender.class))).thenReturn("foo");

        childrenList = FXCollections.observableArrayList();
        when(chatContent.getChildren()).thenReturn(childrenList);

        controller = new ChatPageController(container);

        TestUtils.setPrivateField(controller, "chatContent", chatContent);
        var scrollPane = new ScrollPane();
        scrollPane.setContent(new VBox());
        TestUtils.setPrivateField(controller, "chatScrollPane", scrollPane);
        TestUtils.setPrivateField(controller, "newMessagesBox", newMessagesBox);
    }

    @Test
    public void testAddMessages_prepend() {
        childrenList.add(new MessageBubble(mockMessage(20)));
        childrenList.add(new MessageBubble(mockMessage(21)));

        controller.addMessages(List.of(mockMessage(18), mockMessage(19)));

        assertEquals(4, childrenList.size());
        assertEquals(18, ((MessageBubble) childrenList.get(0)).getMessage().id);
        assertEquals(19, ((MessageBubble) childrenList.get(1)).getMessage().id);
        assertEquals(20, ((MessageBubble) childrenList.get(2)).getMessage().id);
        assertEquals(21, ((MessageBubble) childrenList.get(3)).getMessage().id);
    }

    @Test
    public void testAddMessages_append() {
        childrenList.add(new MessageBubble(mockMessage(18)));
        childrenList.add(new MessageBubble(mockMessage(19)));

        controller.addMessages(List.of(mockMessage(20), mockMessage(21)));

        assertEquals(4, childrenList.size());
        assertEquals(18, ((MessageBubble) childrenList.get(0)).getMessage().id);
        assertEquals(19, ((MessageBubble) childrenList.get(1)).getMessage().id);
        assertEquals(20, ((MessageBubble) childrenList.get(2)).getMessage().id);
        assertEquals(21, ((MessageBubble) childrenList.get(3)).getMessage().id);
    }

    @Test
    public void testAddMessages_insertMiddle() {
        childrenList.add(new MessageBubble(mockMessage(18)));
        childrenList.add(new MessageBubble(mockMessage(21)));

        controller.addMessages(List.of(mockMessage(19), mockMessage(20)));

        assertEquals(4, childrenList.size());
        assertEquals(18, ((MessageBubble) childrenList.get(0)).getMessage().id);
        assertEquals(19, ((MessageBubble) childrenList.get(1)).getMessage().id);
        assertEquals(20, ((MessageBubble) childrenList.get(2)).getMessage().id);
        assertEquals(21, ((MessageBubble) childrenList.get(3)).getMessage().id);
    }

    private TdApi.Message mockMessage(long messageId) {
        var message = new TdApi.Message();
        message.id = messageId;
        message.senderId = mock(TdApi.MessageSender.class);
        var content = new TdApi.MessageText();
        content.text = new TdApi.FormattedText("foo", null);
        message.content = content;
        return message;
    }
}
