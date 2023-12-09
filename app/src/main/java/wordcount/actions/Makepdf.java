package wordcount.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.AttachedFile;
import wordcount.Models.CommandResponse;

public class Makepdf implements Action{
    private Event event;
    private boolean debug;
    private Runtime runtime = Runtime.getRuntime();
    private GuildChannelUnion target;
    private List<MessageChannel> channels;
    private boolean test = false;
    private String outfile;
    public boolean testExecute = false;
    public boolean testRespond = false;

    public Makepdf() {}
    public Makepdf(Map<String, String> data, JDA jda) {
        test = true;
        String channelId = data.get("id");
        target = (GuildChannelUnion)jda.getCategoryById(channelId);
        channels = ((Category)target).getChannels().stream().map(channel -> (MessageChannel)channel).toList();
    }
    public Makepdf(Event event) {
        this.event = event;
        ((SlashCommandInteractionEvent)event).deferReply(true).queue();
        target = ((SlashCommandInteractionEvent)event).getOption("target").getAsChannel();
        debug = ((SlashCommandInteractionEvent)event).getOption("debug") == null ? false : ((SlashCommandInteractionEvent)event).getOption("debug").getAsBoolean();

        if(((SlashCommandInteractionEvent)event).getMessageChannel().getType().equals(ChannelType.PRIVATE)){
            channels = new ArrayList<>(){{add(((SlashCommandInteractionEvent)event).getMessageChannel());}};
        }
        else if(((SlashCommandInteractionEvent)event).getOption("target").getAsChannel().getType().equals(ChannelType.CATEGORY)){
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File("/tmp/wordcount-document/index.tex")))){
                writer.append(String.format("\\chapter*{%s}", ((SlashCommandInteractionEvent)event).getGuild().getName()));
            }
            catch(IOException e){}
            channels = ((SlashCommandInteractionEvent)event).getOption("target").getAsChannel().asCategory().getChannels().stream().filter(channel -> channel.getType().equals(ChannelType.TEXT)).map(channel -> (MessageChannel)channel).toList();
        }
        else{
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File("/tmp/wordcount-document/index.tex")))){
                writer.append(String.format("\\chapter*{%s}\n", ((SlashCommandInteractionEvent)event).getGuild().getName()));
            }
            catch(IOException e){}
            channels = new ArrayList<>(){{add(((SlashCommandInteractionEvent)event).getOption("target").getAsChannel().asTextChannel());}};
        }
    }

    @Override
    public CommandData initialize() {
        return Commands.slash(this.getClass().getSimpleName().toLowerCase(), "Generate a PDF format of this server")
            .addOption(OptionType.CHANNEL, "target", "Specify a category or channel to generate from", true)
            .addOption(OptionType.BOOLEAN, "debug", "Enable debug mode", false);
    }

    @Override @SuppressWarnings("deprecation")
    public CommandResponse execute() {
        // prepare
        String[] command = {"bash", "../document/initialize-tex-env.sh"};
        try {
            Process process = runtime.exec(command);
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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
                        if(!content.equals("error") || debug){
                            writer.append(String.format("\\paragraph{%s} %s\n", paragraph, content));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // maybe do attachments as well?
                    for(Attachment attachment : message.getAttachments().stream().filter(attachment -> attachment.isImage()).collect(Collectors.toList())){
                        if(attachment.getFileName().length() > 50 || attachment.getFileExtension().matches("gif|webp")) continue;
                        File image = attachment.downloadToFile("/tmp/wordcount-document/"+message.getId()+"."+attachment.getFileName()).get();
                        System.out.println("Got image: "+image.getName());
                        writer.append(String.format("\\begin{figure}[H]\n\t\\centering\n\t\\includegraphics[width=\\textwidth]{%s}\n \\end{figure}\n", "/tmp/wordcount-document/"+image.getName()));
                    }
                };
                System.out.println("finished writing!");
            }
            catch(IOException | InterruptedException | ExecutionException e){e.printStackTrace();}
        }
        testExecute = true;
        return new CommandResponse() {
            private void execAndWait(String[] command){
                try {
                    System.out.println(String.format("Launching process with args: [%s]", Arrays.asList(command).stream().collect(Collectors.joining(", "))));
                    Process process = runtime.exec(command);
                    for(String line : process.inputReader().lines().toList()) System.out.println(line);
                    if(process.waitFor() != 0){
                        System.err.println("Process failed!");
                        for(String line : process.errorReader().lines().toList()) System.err.println(line);
                    }
                    System.out.println("Finished task!");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                
            }

            @Override
            public void execute() {
                System.out.println("Rendering PDF...");
                // generate
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                outfile = String.format("../archive/document%s.pdf", new File("../archive").listFiles().length == 0 ? 0 : new File("../archive").listFiles().length);
                String[] renderPdfCommand = {"xelatex", "-interaction=nonstopmode", "-shell-escape", "../document/master.tex"};
                String[] cleanupCommand = {"rm", "master.aux", "master.log"};
                String[] installCommand = {"mv", "master.pdf", outfile};
                execAndWait(renderPdfCommand);
                execAndWait(cleanupCommand);
                execAndWait(installCommand);
                if(!test){
                    ((SlashCommandInteractionEvent)event).getHook().editOriginal("Finished execution").setAttachments(AttachedFile.fromData(new File(outfile))).queue();
                }
                else{
                    System.out.println("Finished execution\n"+String.format("../archive/document%d.pdf", new File("../archive").listFiles().length-1));
                }
                testRespond = true;
            }
        };
    }

    @Override
    public boolean assertFunctionality(){
        assert testExecute;
        assert testRespond;
        assert new File(outfile).exists();
        return true;
    }
    
}
