package wordcount.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
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

    @Override @SuppressWarnings("deprecation")
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
        List<MessageChannel> channels;
        if(((SlashCommandInteractionEvent)event).getMessageChannel().getType().equals(ChannelType.PRIVATE)){
            channels = new ArrayList<>(){{add(((SlashCommandInteractionEvent)event).getMessageChannel());}};
        }
        else if(((SlashCommandInteractionEvent)event).getOptions().get(0).getAsChannel().getType().equals(ChannelType.CATEGORY)){
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File("/tmp/wordcount-document/index.tex")))){
                writer.append(String.format("\\chapter*{%s}", ((SlashCommandInteractionEvent)event).getGuild().getName()));
            }
            catch(IOException e){}
            channels = ((SlashCommandInteractionEvent)event).getOptions().get(0).getAsChannel().asCategory().getChannels().stream().filter(channel -> channel.getType().equals(ChannelType.TEXT)).map(channel -> (MessageChannel)channel).toList();
        }
        else{
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File("/tmp/wordcount-document/index.tex")))){
                writer.append(String.format("\\chapter*{%s}", ((SlashCommandInteractionEvent)event).getGuild().getName()));
            }
            catch(IOException e){}
            channels = new ArrayList<>(){{add(((SlashCommandInteractionEvent)event).getOptions().get(0).getAsChannel().asTextChannel());}};
        }
        for(MessageChannel channel : channels){
            System.out.println("generating for channel: "+ channel.getName());
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File("/tmp/wordcount-document/index.tex"), true))){
                writer.append(String.format("\\input{/tmp/wordcount-document/%s.tex}\n", channel.getName()));
            }
            catch(IOException e){e.printStackTrace();}
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File(String.format("/tmp/wordcount-document/%s.tex", channel.getName()))))){
                writer.append(String.format("\\section{%s}\n", channel.getName().replace("_", "\\_")));
                List<Message> messages = new ArrayList<>();
                channel.getIterableHistory().forEach(message -> {
                    messages.add(message);
                    System.out.println("Processing new message: "+message.getId());
                });
                for(Message message : messages.reversed()){
                    String paragraph, content;
                    if(message.getContentDisplay().split(" ").length > 1){
                        String allowedCharsRegex = "[\\w\\s\\?.,/æøåÆØÅ\"#()<>:;']";
                        String messageContent = message.getContentDisplay().chars().mapToObj(c -> String.valueOf((char)c)).filter(c -> c.matches(allowedCharsRegex)).map(c -> c.matches("[_%#]") ? "\\"+c : c).collect(Collectors.joining());
                        paragraph = messageContent.replace("\n", " ").split(" ",2)[0];
                        content = messageContent.substring(paragraph.length());
                    }
                    else{
                        paragraph = message.getAuthor().getName().replace("_", "\\_") + message.getId();
                        content = "error";
                    }
                    try {
                        writer.append(String.format("\\paragraph{%s} %s\n", paragraph, content));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // maybe do attachments as well?
                    for(Attachment attachment : message.getAttachments().stream().filter(attachment -> attachment.isImage()).collect(Collectors.toList())){
                        if(attachment.getFileName().length() > 50 || attachment.getFileExtension().equals("gif")) continue;
                        attachment.downloadToFile("/tmp/wordcount-document/"+attachment.getFileName()).complete(null);
                        writer.append(String.format("\\begin{figure}[H]\n\t\\centering\n\t\\includegraphics[width=\\textwidth]{%s}\n \\end{figure}", "/tmp/wordcount-document/"+attachment.getFileName()));
                    }
                };
                System.out.println("finished writing!");
            }
            catch(IOException e){e.printStackTrace();}
        }
        return new CommandResponse() {
            @Override
            public void execute() {
                // generate
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String[] command = {"bash", "../document-template/generate-pdf.sh"};
                try {
                    Process process = runtime.exec(command);

                    for(String line : process.errorReader().lines().toList()) System.err.println(line);
                    for(String line : process.inputReader().lines().toList()) System.err.println(line);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                ((SlashCommandInteractionEvent)event).getHook().editOriginal("Finished execution").setAttachments(AttachedFile.fromData(new File(String.format("../version-log/document%d.pdf", new File("../version-log").listFiles().length-1)))).queue();
            }
        };
    }
    
}
