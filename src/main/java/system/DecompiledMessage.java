package system;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import logging.QuickLogger;

public class DecompiledMessage {
    private String msgContent;
    private String msgAuthorName;
    private User msgAuthorObject;
    private Optional<Member> msgMember;
    private Optional<Message> messageObject = Optional.empty();
    private Optional<InteractionCreateEvent> interactionObject = Optional.empty();
    private MessageChannel channel;
    private Guild guild;
    private boolean broken = true;

    public DecompiledMessage(MessageCreateEvent event) {
        try {
            this.msgContent = event.getMessage().getContent();
            this.msgAuthorObject = event.getMessage().getAuthor().orElse(null);
            this.msgAuthorName = this.msgAuthorObject != null ? this.msgAuthorObject.getUsername() : "";
            this.msgMember = event.getMember();
            this.channel = event.getMessage().getChannel().block();
            this.messageObject = Optional.ofNullable(event.getMessage());
            this.interactionObject = Optional.empty();
            this.guild = event.getGuild().block();
            this.broken = false;
        } catch (Exception e) {
            QuickLogger.logFatalErr("Something went wrong, while accepting message event!");
            e.printStackTrace();
            this.broken = true;
        }
    }

    public DecompiledMessage(InteractionCreateEvent event, String prefix){
        try{
            ApplicationCommandInteraction command = event.getInteraction().getCommandInteraction().orElse(null);
            Interaction interaction = event.getInteraction();
            String content = prefix + command.getName().orElse("");
            List<String> optionVals = new ArrayList<>();

            command.getOptions().forEach(option -> {
                optionVals.add(DecompiledMessage.stringifyOption(option, ";"));
            });

            if(!optionVals.isEmpty()){
                content += " " + String.join(";", optionVals);
            }

            this.msgContent = content;
            this.msgAuthorObject = interaction.getUser();
            this.msgAuthorName = this.msgAuthorObject != null ? this.msgAuthorObject.getUsername() : "";
            this.msgMember = interaction.getMember();
            this.channel = interaction.getChannel().block();
            this.messageObject = Optional.empty();
            this.interactionObject = Optional.ofNullable(event);
            this.guild = interaction.getGuild().block();
            event.acknowledge().block();
            this.broken = false;
        } catch(Exception e){
            QuickLogger.logFatalErr("Something went wrong, while accepting interaction event!");
            e.printStackTrace();
            this.broken = true;
        }
    }

    public final static String stringifyOption(ApplicationCommandInteractionOption option, String delimiter){
        if(option.getValue().isPresent()){
            return option.getValue().get().getRaw();
        }
        else if(!option.getOptions().isEmpty()){
            String nextDelimiter = delimiter.equals(";") ? "-" : ";";
            List<String> optionVals = new ArrayList<>();
            option.getOptions().forEach(opt -> {
                optionVals.add(DecompiledMessage.stringifyOption(opt, nextDelimiter));
            });
            return !optionVals.isEmpty() ? String.join(delimiter, optionVals) : "";
        }
        else{
            return "";
        }
    }

    public String getContent(){
        return this.msgContent;
    }

    public String getAuthorName(){
        return this.msgAuthorName;
    }

    public User getUser(){
        return this.msgAuthorObject;
    }

    public Optional<Member> getMember(){
        return this.msgMember;
    }
    
    public MessageChannel getChannel() {
        return channel;
    }

    public Optional<Message> getMessage(){
        return this.messageObject;
    }

    public Optional<InteractionCreateEvent> getInteraction(){
        return this.interactionObject;
    }

    public Guild getGuild(){
        return this.guild;
    }

    public boolean isBroken(){
        return this.broken;
    }
}
