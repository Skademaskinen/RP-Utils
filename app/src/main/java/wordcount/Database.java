package wordcount;

import java.io.IOException;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class Database {
    private static Database database;
    private String path = ".cache/db.db3";

    public static Database getDatabase(){
        if(database == null) database = new Database();
        return database;
    }
    
    public Database() {
        database = this;
    }

    private String doSQL(String query){
        String[] cmd = {"sqlite3", path, query};
        return doSQL(cmd);
    }

    private String doSQL(String query, String format){
        String[] cmd = {"sqlite3", path, query, format};
        return doSQL(cmd);
    }

    private String doSQL(String[] cmd){
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(cmd);
            process.waitFor();
            for(String line : process.inputReader().lines().toList()) System.out.println(line);
            for(String line : process.errorReader().lines().toList()){
                System.err.println(line);
            }
            return process.inputReader().lines().collect(Collectors.joining("\n"));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addChannel(MessageChannel channel){
        doSQL(String.format("create table if not exists m%s (id varchar primary key, content varchar)", channel.getId()));
    }

    public boolean hasMessage(Message message) {
        addChannel(message.getChannel());
        return doSQL(String.format("select * from m%s where id = '%s'", message.getChannelId(), message.getId())).length() > 0;
    }

    public String getMessageContent(Message message){
        return doSQL(String.format("select content from m%s where id = '%s'", message.getChannelId(), message.getId()));
    }

    public void addMessage(Message message){
        doSQL(String.format("insert into m%s values('%s', '%s')", message.getChannelId(), message.getId(), message.getContentDisplay()));
    }
}
