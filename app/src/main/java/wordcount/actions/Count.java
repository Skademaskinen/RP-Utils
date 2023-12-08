package wordcount.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.AttachedFile;
import wordcount.Models.CommandResponse;

import java.io.FileWriter;
import java.io.IOException;

public class Count implements Action {
    Event event;

    public Count(){} //for invokations only
    public Count(Event event) {
        this.event = event;
    }

    @Override
    public CommandData initialize() {
        return Commands.slash(this.getClass().getSimpleName().toLowerCase(), "Count a channel, or all channels if not specified")
            .addOption(OptionType.CHANNEL, "channel", "Channel to be counted", false);
    }

    @Override
    public CommandResponse execute() {
        ((SlashCommandInteractionEvent)event).reply("Don't worry, i am running, this will just take a long ass time...").setEphemeral(true).queue();
        List<MessageChannelUnion> channels = ((SlashCommandInteractionEvent)event)
            .getChannelType()
            .equals(ChannelType.PRIVATE) ? 
            new ArrayList<MessageChannelUnion>(){{add(((SlashCommandInteractionEvent)event).getChannel());}} : 
            ((SlashCommandInteractionEvent)event).getOptions().size() == 1 ?
            new ArrayList<MessageChannelUnion>(){{add((MessageChannelUnion)((SlashCommandInteractionEvent)event).getOptions().get(0).getAsChannel());}} :
            ((SlashCommandInteractionEvent)event).getGuild().getChannels().stream().filter(channel -> 
                channel.getType().equals(ChannelType.TEXT)).map(channel -> 
                    (MessageChannelUnion)channel).filter(channel -> 
                        channel.getType().equals(ChannelType.TEXT))
                .collect(Collectors.toList());
        Map<MessageChannelUnion, List<Message>> data = new HashMap<>();
        for(MessageChannelUnion channel : channels){
            System.out.println("Indexing channel: "+channel.getName());
            List<Message> messages = new ArrayList<>();
            channel.getIterableHistory().forEach(message -> messages.add(message));
            data.put(channel, messages);
            System.out.println("Messages: "+messages.size());
        }
        return new CommandResponse() {

            @Override
            public void execute() {
                String csv = "Channel,Messages,Words,Characters\n";
                for(MessageChannelUnion key : data.keySet()){
                    int words = data.get(key).stream().map(message ->{
                        return message.getContentRaw().replace("\n", " ").split(" ").length;
                    }).reduce(0, (a, b) -> a + b);
                    int characters = data.get(key).stream().map(message -> {
                        return message.getContentRaw().replace("\n", " ").replace(" ", "").length();
                    }).reduce(0, (a, b) -> a + b);
                    csv+=String.format("%s,%d,%d,%d\n", 
                    key.getName(), 
                    data.get(key).size(),
                    words,
                    characters);
                }
                File file = new File("/tmp/wordcount.csv");
                try(FileWriter writer = new FileWriter(file)) {
                    writer.write(csv);
                    writer.flush();
                }
                catch(IOException e){}
                ((SlashCommandInteractionEvent) event).getHook().editOriginal("Finished running!").setAttachments(AttachedFile.fromData(file)).queue();
            }
            
        };
    }
}
