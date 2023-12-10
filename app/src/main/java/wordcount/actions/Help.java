package wordcount.actions;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import wordcount.Models.CommandResponse;

public class Help implements Action {
    Event event;
    JDA jda;
    EmbedBuilder builder = new EmbedBuilder();
    boolean test = false;
    public boolean testExecute = false;
    public boolean testRespond = false;

    public Help() {}
    public Help(Map<String, String> data, JDA jda) {
        test = true;
        this.jda = jda;
    }
    public Help(Event event) {
        this.event = event;
        jda = event.getJDA();
    }

    @Override @SuppressWarnings("unchecked")
    public CommandResponse execute() {
        Path path = Path.of("src/main/java/wordcount/actions").toAbsolutePath();
        System.out.println("Got actions path: " + path.toFile());
        int counter = 0;
        for(Class<Action> actionClass : Arrays.asList(path.toFile().listFiles())
            .stream()
            .filter(file -> !file.getName().equals("Action.java"))
            .map(file -> {
                try {
                    return (Class<Action>)Class.forName("wordcount.actions."+file.getName().replace(".java", ""));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toList())){

            Action action;
            try {
                action = actionClass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                e.printStackTrace();
                return null;
            }
            Map<String, String> info = action.helpInfo();
            builder.addField(info.get("header"), info.get("body"), counter != 3);
            counter = counter == 3 ? 0 : counter + 1;
        }
        testExecute = true;
        return new CommandResponse() {
            @Override
            public void execute(){
                builder.setColor(Color.orange);
                builder.setThumbnail(jda.getSelfUser().getAvatarUrl());
                builder.setTitle("Command list");
                builder.setAuthor(jda.getSelfUser().getName());
                if(!test){
                    ((SlashCommandInteractionEvent)event).replyEmbeds(builder.build()).setEphemeral(true).queue();
                }
                else{
                    System.out.println("Finished test\n"+builder.build());
                }
                testRespond = true;
            }
        };
    }

    @Override
    public boolean assertFunctionality(){
        assert testExecute;
        assert testRespond;
        assert !builder.isEmpty();
        return true;
    }
    
}
