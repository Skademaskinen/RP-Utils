package wordcount.actions;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import wordcount.Models.CommandResponse;

public interface Action {
    public default String serialize(){
        return "";
    }
    public default CommandData initialize(){
        return Commands.slash(this.getClass().getSimpleName().toLowerCase(), this.getClass().getName());
    }
    public CommandResponse execute();
    
}
