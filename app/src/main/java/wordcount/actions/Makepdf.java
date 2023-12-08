package wordcount.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.AttachedFile;
import wordcount.Models.CommandResponse;

public class Makepdf implements Action{
    Event event;
    Runtime runtime = Runtime.getRuntime();

    public Makepdf() {}
    public Makepdf(Event event) {
        this.event = event;
    }

    @Override
    public CommandData initialize() {
        return Commands.slash(this.getClass().getSimpleName().toLowerCase(), "Generate a PDF format of this server")
            .addOption(OptionType.CHANNEL, "target", "Specify a category or channel to generate from", true);
    }

    @Override
    public CommandResponse execute() {
        // prepare
        ((SlashCommandInteractionEvent)event).deferReply(true).queue();
        String[] command = {"bash", "../document-template/initialize-tex-env.sh"};
        try {
            Process process = runtime.exec(command);
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        List<TextChannel> channels;
        if(((SlashCommandInteractionEvent)event).getOptions().get(0).getAsChannel().getType().equals(ChannelType.CATEGORY)){
            channels = ((SlashCommandInteractionEvent)event).getOptions().get(0).getAsChannel().asCategory().getChannels().stream().map(channel -> (TextChannel)channel).toList();
        }
        else{
            channels = new ArrayList<>(){{add(((SlashCommandInteractionEvent)event).getOptions().get(0).getAsChannel().asTextChannel());}};
        }
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File("/tmp/wordcount-document/index.tex")))){
            writer.append(String.format("\\chapter{%s}", ((SlashCommandInteractionEvent)event).getGuild().getName()));
        }
        catch(IOException e){}
        for(TextChannel channel : channels){
            System.out.println("generating for channel: "+ channel.getName());
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File("/tmp/wordcount-document/index.tex"), true))){
                writer.append(String.format("\\input{/tmp/wordcount-document/%s.tex}\n", channel.getName()));
            }
            catch(IOException e){}
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File(String.format("/tmp/wordcount-document/%s.tex", channel.getName()))))){
                writer.append(String.format("\\section{%s}\n", channel.getName()));
                List<Message> messages = new ArrayList<>();
                channel.getIterableHistory().forEach(message -> {
                    messages.add(message);
                });
                for(Message message : messages.reversed()){
                    String paragraph, content;
                    if(message.getContentDisplay().split(" ").length > 1){
                        paragraph = message.getContentDisplay().split(" ",2)[0];
                        content = message.getContentDisplay().split(" ", 2)[1];
                    }
                    else{
                        paragraph = message.getAuthor().getName() + message.getId();
                        content = "error";
                    }
                    try {
                        writer.append(String.format("\\paragraph{%s} %s\n", paragraph, content));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                };
            }
            catch(IOException e){}
        }
        return new CommandResponse() {
            @Override
            public void execute() {
                // generate
                String[] command = {"bash", "../document-template/generate-pdf.sh"};
                try {
                    Process process = runtime.exec(command);
                    if(process.waitFor() != 0){
                        System.err.println("Fuck");
                        for(String line : process.errorReader().lines().toList()){
                            System.err.println(line);
                        }
                        return;
                    } 
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                ((SlashCommandInteractionEvent)event).getHook().editOriginal("Finished execution").setAttachments(AttachedFile.fromData(new File(String.format("../version-log/document%d.pdf", new File("../version-log").listFiles().length-1)))).queue();
            }
        };
    }
    
}
