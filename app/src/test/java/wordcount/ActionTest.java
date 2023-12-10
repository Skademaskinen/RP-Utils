package wordcount;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import net.dv8tion.jda.api.JDA;
import wordcount.Models.CommandResponse;
import wordcount.actions.Action;

public class ActionTest {
    App app;
    String token = "ODU1MTQwNDg4NDk0NDQ4NjQw.Gw6Lty.a-tvq2DFozvwm1OgSHixRuVWg2e4F-ZG4Pktp4";

    public ActionTest() {
        app = new App();
        app.initialize(token);
    }

    @Test @SuppressWarnings("unchecked")
    public void TestActions(){        
        Path path = Path.of("src/main/java/wordcount/actions").toAbsolutePath();
        System.out.println("Got actions path: " + path.toFile());
        for(Class<Action> actionClass : Arrays.asList(path.toFile().listFiles())
            .stream()
            .filter(file -> !file.getName().equals("Action.java"))
            .map(file -> {
                try {
                    return (Class<Action>)Class.forName("wordcount.actions."+file.getName().replace(".java", ""));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    Assert.fail("Got Exception");
                }
                return null;
            }).collect(Collectors.toList())){

            Action action;
            try {
                action = actionClass.getConstructor(Map.class, JDA.class).newInstance(new HashMap<String, String>(){{put("id", "1183003435252076629");}}, app.jda);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                e.printStackTrace();
                Assert.fail("Got Exception");
                return;
            }
            CommandResponse response = action.execute();
            response.execute();            
            Assert.assertTrue(action.assertFunctionality());
        }
    }
}
