package system;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;

public class DecompiledMessage {
    private String msgContent;
    private MessageCreateEvent msgEvent;
    private Message msgObject;
    private String msgAuthorName;
    private User msgAuthorObject;
    private boolean broken;

    public DecompiledMessage(MessageCreateEvent msgEvent) {
        try {
            this.msgEvent = msgEvent;
            this.msgObject = this.msgEvent.getMessage();
            this.msgContent = this.msgObject.getContent();
            this.msgAuthorObject = this.msgObject.getAuthor().orElse(null);
            this.msgAuthorName = this.msgAuthorObject != null ? this.msgAuthorObject.getUsername() : "";
            this.broken = false;
        } catch (Exception e) {
            System.out.println("Something went wrong, while accepting event!");
            e.printStackTrace();
            this.broken = true;
        }
    }


    public Message getMessageObject(){
        return this.msgObject;
    }

    public String getContent(){
        return this.msgContent;
    }

    public MessageCreateEvent getEvent(){
        return this.msgEvent;
    }

    public String getAuthorName(){
        return this.msgAuthorName;
    }

    public User getUser(){
        return this.msgAuthorObject;
    }

    public boolean isBroken(){
        return this.broken;
    }


}
