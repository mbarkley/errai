package org.jboss.errai.bus.client.tests;

import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.api.base.MessageBuilder;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.bus.client.api.messaging.MessageBus;
import org.jboss.errai.bus.client.api.messaging.MessageCallback;

import com.google.gwt.user.client.Timer;

/**
 * @author Max Barkley <mbarkley@redhat.com>
 */
public class MessageReplyCallbackTest extends AbstractErraiTest {

  private static final int TIMEOUT = 20000;
  private static final int POLL = 500;

  private Object messageValue;
  private MessageBus bus = ErraiBus.get();
  private boolean received;
  private MessageCallback callback = new MessageCallback() {
    @Override
    public void callback(Message message) {
      received = true;
    }
  };

  @Override
  public String getModuleName() {
    return "org.jboss.errai.bus.MessageReplyCallbackTests";
  }
  
  public MessageReplyCallbackTest() {
    bus.subscribe("ReplyTestService", new MessageCallback() {
      @Override
      public void callback(Message message) {
        reply(message);
      }
    });
  }
  
  private void reply(Message message) {
    MessageBuilder.createConversation(message).subjectProvided().done().sendNowWith(ErraiBus.get());
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    received = false;
  }

  public void testSendViaDefaultMessageBuilder() throws Exception {
    runAndWait(new Runnable() {
      @Override
      public void run() {
        MessageBuilder.createMessage().toSubject("ReplyTestService").withValue(messageValue).done().repliesTo(callback)
                .sendNowWith(bus);
      }
    });
  }
  
  public void testSendViaDefaultMessageBuilderAgain() throws Exception {
    runAndWait(new Runnable() {
      @Override
      public void run() {
        MessageBuilder.createMessage().toSubject("ReplyTestService").withValue(messageValue).done().repliesTo(callback)
                .sendNowWith(bus);
      }
    });
  }

  public void testSendViaMessage() throws Exception {
    runAndWait(new Runnable() {
      @Override
      public void run() {
        Message message = MessageBuilder.createMessage().toSubject("ReplyTestService").withValue(messageValue).done()
                .repliesTo(callback).getMessage();
        message.sendNowWith(bus);
      }
    });
  }

  private void runAndWait(Runnable test) {
    delayTestFinish(TIMEOUT + 2 * POLL);
    final long start = System.currentTimeMillis();
    test.run();
    new Timer() {
      @Override
      public void run() {
        if (System.currentTimeMillis() - start > TIMEOUT) {
          cancel();
          fail();
        }
        else if (received) {
          cancel();
          finishTest();
        }
      }
    }.scheduleRepeating(POLL);
  }
}
