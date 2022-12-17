package blue.lhf.run_paper_maven_plugin;

import org.junit.jupiter.api.*;

public class RunnerTest {

    public static void main(String[] args) {
        new InstallerTest().testRoundtrip();
        new RunnerTest().testRoundtrip();
    }

    @Test
    void testRoundtrip() {
        final ServerMojo mojo = new ServerMojo();
        mojo.serverDirectory  = "mc_server";
        mojo.acceptEula = true;

        Assertions.assertDoesNotThrow(mojo::execute);
    }
}
