package wordcount.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import wordcount.Models.CommandResponse;

public interface Action {
    public default String serialize(){
        return "";
    }
    public default CommandData initialize(){
        return Commands.slash(this.getClass().getSimpleName().toLowerCase(), this.getClass().getName());
    }
    public CommandResponse execute();

    public default Map<String, String> helpInfo(){
        CommandData data = this.initialize();
        Map<String, String> result = new HashMap<>();
        result.put("header", this.getClass().getSimpleName());
        result.put("body", String.format("""
            **Command**:    %s
            **Args**: [%s]
            %s
        """, 
        data.getName(), 
        ((SlashCommandData)data).getOptions().stream().map(option -> option.getName()).collect(Collectors.joining(", ")),
        ((SlashCommandData)data).getDescription()));
        return result;
    }
    
}
