/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package wordcount;

import org.junit.Test;

import net.dv8tion.jda.api.requests.GatewayIntent;

import java.nio.file.Path;

import org.junit.Assert;

public class AppTest {
    App app;
    String token;

    public AppTest() {
        app = new App();
    }
    @Test public void testInitialize(){
        System.out.println("Is envvar set correctly? "+ System.getenv("RP_UTILS_TOKEN") == null);
        token = System.getenv("RP_UTILS_TOKEN");
        app.initialize(token);
        Assert.assertEquals(1, app.jda.getRegisteredListeners().size());
        Assert.assertTrue(app.jda.getGatewayIntents().contains(GatewayIntent.MESSAGE_CONTENT));
        app.jda.shutdown();
    }
    @Test public void testRegisterActions(){
        app.initialize(token);
        app.registerActions();
        Assert.assertEquals(Path.of("src/main/java/wordcount/actions").toFile().listFiles().length-1, app.jda.retrieveCommands().complete().size());
        app.jda.shutdown();
    }
}
