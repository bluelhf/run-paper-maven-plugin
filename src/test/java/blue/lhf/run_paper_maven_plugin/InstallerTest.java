package blue.lhf.run_paper_maven_plugin;

import org.junit.jupiter.api.*;

public class InstallerTest {
    @Test
    public void testRoundtrip() {
        final InstallMojo mojo = new InstallMojo();
        mojo.serverDirectory  = "mc_server";
        mojo.minecraftVersion = "1.19.3";

        Assertions.assertDoesNotThrow(mojo::execute);
    }
}
