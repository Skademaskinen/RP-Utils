package wordcount;

import net.dv8tion.jda.api.entities.Message;

import java.io.File;

public class Cache {
    public static boolean imageCached(Message message, File file){
        return Database.getDatabase().hasMessage(message) && file.exists();
    }

    public static boolean messageCached(Message message){
        return Database.getDatabase().hasMessage(message);
    }

    public static void saveCache(Message message){
        Database.getDatabase().addMessage(message);
        new File(String.format(".cache/%s", message.getId())).mkdirs();
    }

    public static String getMessageContent(Message message){
        return Database.getDatabase().getMessageContent(message);
    }
}
