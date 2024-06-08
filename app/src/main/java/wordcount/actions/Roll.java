package wordcount.actions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import wordcount.Models.CommandResponse;

/**
 * Roll
 */
public class Roll implements Action{

    private Event event;
    private String diesOption;


    @Override
    public CommandData initialize() {
        return Commands.slash("roll", "Roll a die")
            .addOption(OptionType.STRING, "dies", "Specify dies: <n>d<s> where n is number and s is size, multiple are seperated by space");
    }

    public Roll(){}
    public Roll(Event event) {
        this.event = event;
        this.diesOption = ((SlashCommandInteractionEvent)event).getOption("dies").getAsString();
    }

    @Override
    public CommandResponse execute() {
        return new CommandResponse() {

            @Override
            public void execute() {
                String outstr = "";
                int total = 0;
                String pattern = "^(\\d+d\\d+)|[+-]\\d+$";
                Map<Integer, List<Integer>> all_results = new HashMap<>();
                if(!(Arrays.asList(diesOption.split(" ")).stream().allMatch(item -> item.matches(pattern)))){
                    ((SlashCommandInteractionEvent)event).reply("Error, malformed input!\nInput should match: `"+pattern+"`").setEphemeral(true).queue();;
                    return;
                }
                for(String dies : diesOption.split(" ")){
                    if(dies.contains("d")){
                        int count = Integer.parseInt(dies.split("d")[0]);
                        int size = Integer.parseInt(dies.split("d")[1]);
                        all_results.put(size, roll(count, size));
                    }
                    else if(dies.matches("^[+-]?\\d+$")){
                        if(dies.charAt(0) == '+')
                            total += Integer.parseInt(dies.substring(1));
                        else
                            total -= Integer.parseInt(dies.substring(1));
                    }
                }
                if(total != 0)
                    outstr += "**Modifier:** "+total+"\n";
                for(Map.Entry<Integer, List<Integer>> entry : all_results.entrySet()){
                    int size = entry.getKey();
                    outstr += "**d"+size+":** ["+entry.getValue().stream().map(String::valueOf).collect(Collectors.joining(", "))+"]\n";
                    total += entry.getValue().stream().reduce(0, Integer::sum);
                }
                outstr += "**Total:** "+total;
                ((SlashCommandInteractionEvent)event).reply(outstr).queue();
            }
            
        };
    }

    @Override
    public boolean assertFunctionality() {
        return true;
    }

    List<Integer> roll(int n, int s){
        List<Integer> results = new LinkedList<>();
        for(int i = 0; i < n; i++){
            results.add((int)(Math.random() * ((s+1) - 1) + 1));
        }
        return results;
    }

    
}