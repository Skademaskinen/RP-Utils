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
import java.util.HashMap;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.AttachedFile;
import wordcount.App;
import wordcount.Cache;
import wordcount.Models.CommandResponse;

public class Makepdf implements Action{
    private Event event;
    private boolean debug;
    private Runtime runtime = Runtime.getRuntime();
    private GuildChannelUnion target;
    private List<MessageChannel> channels;
    private boolean test = false;
    private boolean update = false;
    private String outfile;
    private Map<MessageChannel, Integer> percentages;
    private Message message;
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
        message = ((SlashCommandInteractionEvent)event).deferReply(false).complete().retrieveOriginal().complete();
        target = ((SlashCommandInteractionEvent)event).getOption("target").getAsChannel();
        debug = ((SlashCommandInteractionEvent)event).getOption("debug") == null ? false : ((SlashCommandInteractionEvent)event).getOption("debug").getAsBoolean();
        update = ((SlashCommandInteractionEvent)event).getOption("updatecache") == null ? false : ((SlashCommandInteractionEvent)event).getOption("updatecache").getAsBoolean();

        if(((SlashCommandInteractionEvent)event).getMessageChannel().getType().equals(ChannelType.PRIVATE)){
            channels = new ArrayList<>(){{add(((SlashCommandInteractionEvent)event).getMessageChannel());}};
        }
        else if(((SlashCommandInteractionEvent)event).getOption("target").getAsChannel().getType().equals(ChannelType.CATEGORY)){
            channels = ((SlashCommandInteractionEvent)event).getOption("target").getAsChannel().asCategory().getChannels().stream().filter(channel -> channel.getType().equals(ChannelType.TEXT)).map(channel -> (MessageChannel)channel).toList();
        }
        else{
            channels = new ArrayList<>(){{add(((SlashCommandInteractionEvent)event).getOption("target").getAsChannel().asTextChannel());}};
        }
        percentages = new HashMap<>(){{channels.stream().forEach(channel -> put(channel, 0));}};
    }

    @Override
    public CommandData initialize() {
        return Commands.slash(this.getClass().getSimpleName().toLowerCase(), "Generate a PDF format of this server")
            .addOption(OptionType.CHANNEL, "target", "Specify a category or channel to generate from", true)
            .addOption(OptionType.BOOLEAN, "debug", "Enable debug mode", false)
            .addOption(OptionType.BOOLEAN, "updatecache", "Update the cache of files", false);
    }

    @Override @SuppressWarnings("deprecation")
    public CommandResponse execute() {
        // prepare
        String[] command = {System.getenv("BASH_PATH") == null ? "bash" : System.getenv("BASH_PATH"), "../document/initialize-tex-env.sh"};
        if(event != null){
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File(".cache/build/index.tex")))){
                writer.append(String.format("\\begin{center}\n\\Huge{\\textbf{%s}}\n\\end{center}\n\\clearpage\n", ((SlashCommandInteractionEvent)event).getGuild().getName()));
            }
            catch(IOException e){e.printStackTrace();}
        }
        execAndWait(command);
        for(MessageChannel channel : channels){
            float counter = 0;
            float percentage = 0;
            System.out.println("generating for channel: "+ channel.getName());
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File(".cache/build/index.tex"), true))){
                writer.append(String.format("\\input{.cache/build/%s.tex}\n", channel.getName()));
            }
            catch(IOException e){e.printStackTrace();}
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File(String.format(".cache/build/%s.tex", channel.getName()))))){
                writer.append(String.format("\\chapter{%s}\n", channel.getName().replace("_", "\\_")));
                List<Message> messages = new ArrayList<>();
                channel.getIterableHistory().forEach(message -> {
                    messages.add(message);
                    System.out.println("["+channel.getName()+"] New message: "+message.getId());
                });
                for(Message message : messages.reversed()){
                    counter++;
                    if(((counter/messages.size())) > percentage){
                        percentage+=0.1;
                        percentages.put(channel, (int)(percentage*100));
                        if(!test){
                            this.message.editMessage("Progress\n```\n"+percentages.keySet().stream().sorted((e1, e2) -> Integer.compare(percentages.get(e2), percentages.get(e1))).map(key -> String.format("%s: %s%%", key.getName(), percentages.get(key))).collect(Collectors.joining("\n"))+"\n```").queue();
                        }
                    }
                    System.out.println("["+channel.getName()+"] Getting message content");
                    String paragraph, content;                        
                    String allowedCharsRegex = "[\\w\\s\\?.,/æøåÆØÅ\"#()<>:;']";
                    String messageContent = Cache.messageCached(message) && !update ? Cache.getMessageContent(message) : message.getContentDisplay().chars().mapToObj(c -> String.valueOf((char)c)).filter(c -> c.matches(allowedCharsRegex)).map(c -> c.matches("[_%#]") ? "\\"+c : c).collect(Collectors.joining());
                    if(messageContent.split(" ").length > 1){
                        paragraph = messageContent.replace("\n", " ").split(" ",2)[0];
                        content = messageContent.substring(paragraph.length());
                        System.out.println("["+channel.getName()+"] Checking cache");
                    }
                    else{
                        paragraph = "error";
                        content = "error";
                    }
                    if(!Cache.messageCached(message)) {
                        Cache.saveCache(message);
                    }
                    try {
                        if(!content.equals("error") || debug){
                            writer.append(String.format("\\paragraph{%s} %s\n", paragraph, content));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("["+channel.getName()+"] Processing attachments");
                    // maybe do attachments as well?
                    for(Attachment attachment : message.getAttachments().stream().filter(attachment -> attachment.isImage()).collect(Collectors.toList())){
                        if(attachment.getFileName().length() > 50 || attachment.getFileExtension().matches("gif|webp")) continue;
                        String filename = String.format(".cache/%s/%s", message.getId(), attachment.getFileName());
                        File image;
                        if(Cache.imageCached(message, new File(filename))){
                            image = new File(filename);
                        }
                        else{
                            image = attachment.downloadToFile(filename).get();
                            System.out.println("["+channel.getName()+"] Got image: "+image.getName());
                        }
                        writer.append(String.format("\\begin{figure}[H]\n\t\\centering\n\t\\includegraphics[width=\\textwidth]{%s}\n \\end{figure}\n", filename));
                    }
                };
                System.out.println("["+channel.getName()+"] finished writing chapter!");
                percentages.put(channel, 100);
            }
            catch(IOException | InterruptedException | ExecutionException e){e.printStackTrace();}
        }
        testExecute = true;
        return new CommandResponse() {

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
                String[] renderPdfCommand = {System.getenv("XELATEX_PATH") == null ? "xelatex" : System.getenv("XELATEX_PATH"), "-interaction=nonstopmode", "-shell-escape", "../document/master.tex"};
                String[] cleanupCommand = {"rm", "master.aux", "master.log", "master.toc"};
                String[] installCommand = {"mv", "master.pdf", outfile};
                execAndWait(renderPdfCommand);
                execAndWait(renderPdfCommand);
                execAndWait(cleanupCommand);
                execAndWait(installCommand);
                if(!test){
                    if(new File(outfile).length() <= ((SlashCommandInteractionEvent)event).getGuild().getMaxFileSize()){
                        message.editMessage("Finished execution").setAttachments(AttachedFile.fromData(new File(outfile))).queue();
                    }
                    else{
                        message.editMessage(String.format("Finished execution!\nFile size is too large for discord, here is a download link\n%s", App.debug ? "http://localhost:8123/"+outfile.split("/")[outfile.split("/").length-1] : "https://skademaskinen.win:11034/document/"+outfile.split("/")[outfile.split("/").length-1])).queue();
                    }
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
    
}
