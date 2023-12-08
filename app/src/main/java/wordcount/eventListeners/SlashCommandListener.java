package wordcount.eventListeners;

import java.lang.reflect.InvocationTargetException;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import wordcount.Models.CommandResponse;
import wordcount.actions.Action;

public class SlashCommandListener extends ListenerAdapter {
    @Override @SuppressWarnings("unchecked")
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(String.format("Slash command listener called!\nEvent name: %s\nEvent channel: %s\nEvent caller: %s", event.getName(), event.getChannel().getName(), event.getUser().getName()));
        Class<Action> actionClass;
        try {
            actionClass = (Class<Action>) Class.forName(String.format("wordcount.actions.%s", (event.getName().substring(0, 1).toUpperCase()+event.getName().substring(1))));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }
        Action action;
        try {
            action = actionClass.getConstructor(Event.class).newInstance(event);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            return;
        }
        Executor executor = new Executor(action);
        executor.start();
    }

    private class Executor extends Thread{
        Action action;
        public Executor(Action action) {
            this.action = action;
        }
        @Override
        public void run() {
            CommandResponse response = action.execute();
            response.execute();
        }
    }
}
